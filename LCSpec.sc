LCSpec {
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
			runtime.families[familyId] = LCFamily.new.init(familyId);
		};

		domain.isNil.if {
		   	runtime.families[familyId];
		};

		runtime.families[familyId].addDomainFunction(domain, function);
		^runtime.families[familyId];
	}
}

LCFamily {
	var <id;
	var <table;

	var <hasSubject = false;

	// which families does this block match to?
	var <matches;

	// which families are looked through for code functions?
	var <lookup;

	var extensionFamilies;

	var domainFunctions;
	var currentLoadDomain = nil;

	var runtime;

	var isLoaded = false;

	prInitData {
		domainFunctions = ();
		matches = List();
		lookup = List();
		extensionFamilies = List();
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

	addExtensionFamily {|extension|
		"Extension".postln;
		extension.id.postln;
		extensionFamilies.add(extension);
	}

	// TODO THIS MUST BE REWORKED

	// here is where we look our own table and copy things into members
	buildIndex {

		// check if any of the blocks is a subject
		// TODO: maybe also only if it is marked as primary
		table[\blocks].do {|block|
			hasSubject = hasSubject || (block[\type] == \subject);
		};

		// create a list of all compatible families
		lookup = OrderedIdentitySet();
		lookup.add(this);
		this.traverseLookupTree(lookup, 1);

		lookup = lookup.asList;
		id.postln;
		lookup.collect({|x| x.id}).postln;
	}

	buildMatches {
		matches = Set();
		// matches.addAll(lookup.collect(_.id));
		runtime.families.do {|family|
			family.lookup.includes(this).if {
				matches.add(family.id);
			};
		};
		matches = matches.asList();
	}

	traverseLookupTree {|set, depth|
		var candidates = List();

		table[\inheritsFrom].do {|key|
			candidates.add(runtime.families[key]);
		};

		candidates.addAll(extensionFamilies);

		candidates.do {|family|
			set.findMatch(family).isNil.if {
				set.add(family);
				family.traverseLookupTree(set, depth+1);
			};
		};
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
