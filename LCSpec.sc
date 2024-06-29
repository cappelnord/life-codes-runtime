LCSpec {
	var <id;
	var <table;

	var <hasSubject = false;
	var <matches;

	var domainFunctions;
	var currentLoadDomain = nil;

	var runtime;

	var isLoaded = false;

	*new {|familyId, domain=nil, function=nil|
		var runtime;

		LifeCodes.instance.isNil.if {
			"LifeCodes is not initialized - cannot use LCSpec things ...".warn;
			^nil;
		};

		LifeCodes.instance.options[\ignoreDomains].includes(domain).if {
			"Ignored domain '%'for '%' ...".format(domain, familyId).postln;
			^nil;
		};

		runtime = LifeCodes.instance.runtime;

		runtime.families[familyId].isNil.if {
			runtime.families[familyId] = super.new.init(familyId);
		};

		domain.isNil.if {
		   	runtime.families[familyId];
		};

		runtime.families[familyId].addDomainFunction(domain, function);
		^runtime.families[familyId];
	}

	prInitData {
		domainFunctions = ();
		matches = [];
    }

	init {|familyId|
		id = familyId;
        this.prInitData;

		runtime = LifeCodes.instance.runtime;

		// "Created LCSpec with ID: %".format(id).postln;
		^this;
	}

	prMergeTable {|src, dst|
		var allKeys = src.keys.reject {|key| [\blocks, \data].includes(key) };

		allKeys.do {|key|
			key.asString.beginsWith("on_").if({
				// build a function list
				dst[key].isNil.if {
					dst[key] = List();
				};
				dst[key].add(LCBlockFunctionReference(src[key], currentLoadDomain, this));
			}, {
				// check if we override something to warn
				dst[key].isNil.not.if {
					(dst[key] != src[key]).if {
						"Key '%' overwritten.".postln;
					};
				};
				dst[key] = src[key];
			});
		}
	}

	define {|def|
		this.prMergeTable(def, table);

		def[\data].isNil.not.if {
			this.prMergeTable(def[\data], table[\data]);
		};

		def[\blocks].isNil.not {
			def[\blocks].keys.do {|blockId|
				this.defineBlock(blockId, def[\blocks][blockId]);
			};
		};

		^this;
	}

	defineBlock {|blockId, def|
		table[\blocks][blockId].isNil.if {
			table[\blocks][blockId] = ();
		};

		this.prMergeTable(def, table[\blocks][blockId]);
	}

	addDomainFunction {|domain, function|
		domainFunctions[domain] = function;
	}

	// TODO: This should likely also aggregate from all related families
	getLifecycleFunctionReferences {|phase|
		^table[phase];
	}

	getLifecycleExecutionUnits {|phase|
		^this.getLifecycleFunctionReferences(phase).collect({|ref|
			ref.bind(this, LifeCodes.instance)
		});
	}

	executeLifecyclePhase {|phase, queue=\runtime|
		"Execute Family Lifecycle Phase: %/%".format(id, phase).postln;
		runtime.executeList(this.getLifecycleExecutionUnits(phase), queue);
	}

	compileDomainFunctions {
		var domainKeys = domainFunctions.keys.asArray.sort;
		table = (
			\family: id,
			\blocks: (),
			\data: ()
		);

		domainKeys.do {|domainKey|
			var ret;
			"%/% ...".format(id, domainKey).postln;
			currentLoadDomain = domainKey;
			ret = domainFunctions[domainKey].value(this, LifeCodes.instance);
			table.postln;
			currentLoadDomain = nil;
		};
	}

	asString {
		^"LCSpec(\%)".format(id);
	}

	// TODO THIS MUST BE REWORKED

	// here is where we look our own table and copy things into members
	buildIndex {
		// check if any of the blocks is a subject
		table[\blocks].do {|block|
			hasSubject = hasSubject || (block[\type] == \subject);
		};

		// create a list of all compatible families

		matches = IdentitySet();

		// let's first add our own matches
		table[\compatibility].do {|match|
			(match == \_all).if ({
				matches.addAll(runtime.specs.keys);
			}, {
				match.asString.beginsWith("type_").if({
					matches.addAll(runtime.typesDict[match.asString[5..].asSymbol]);
				}, {
					matches.add(match);
				});
		    });
		};

		// let's see if we are compatible with any other family
		runtime.families.do {|family|
			family.table[\compatibility].isNil.not.if {
				var cands = [\_all, id, ("type_" ++ table[\type]).asSymbol];
				family.table[\compatibility].includesAny(cands).if {
					matches.add(family.id);
				};
			};
		};

		matches.remove(id);
		matches = matches.asList.sort;
	}

	load {
		isLoaded.not.if {
			this.executeLifecyclePhase(\on_load);
			isLoaded = true;
		};
	}

	unload {
		isLoaded.if {
			this.executeLifecyclePhase(\on_unload);
			isLoaded = false;
		};
	}
}

/*
Somehow this seems a bit too much structure .. let's see if it is really needed.
*/

LCBlockFunctionReference {
	var <function;
	var <domain;
	var <family;

	*new {|function, domain, family|
		^super.newCopyArgs(function, domain, family).init;
	}

	init {

	}

	bind {|...args|
		^LCExecutionUnit(this, args);
	}
}

LCExecutionUnit {
	var <ref;
	var <args;

	*new {|ref, args|
		^super.newCopyArgs(ref, args).init;
	}

	init {

	}

	execute {
		^ref.function.value(*args);
	}
}
