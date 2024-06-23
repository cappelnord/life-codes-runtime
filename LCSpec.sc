LCSpec {
	var <id;
	var <table;

	var <hasSubject = true;

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

		runtime.specs[familyId].isNil.if {
			runtime.specs[familyId] = super.new.init(familyId);
		};

		domain.isNil.if {
		   	runtime.specs[familyId];
		};

		runtime.specs[familyId].addDomainFunction(domain, function);
		^runtime.specs[familyId];
	}

	init {|familyId|
		id = familyId;
		domainFunctions = ();
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

	getLifecycleExecutionUnits {|phase|
		var ret = nil;
		table[phase].isNil.not.if({
			ret = table[phase].collect({|ref| ref.bind(this, LifeCodes.instance)});
		});
		^ret;
	}

	executeLifecyclePhase {|phase, queue=\runtime|
		"Execute Spec Lifecycle Phase: %/%".format(id, phase).postln;
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

	// here is where we look our own table and copy things into members
	buildIndex {
		// check if any of the blocks is a subject
		table.blocks.do {|block|
			hasSubject = hasSubject || (block[\type] == \subject);
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
	var <spec;

	*new {|function, domain, spec|
		^super.newCopyArgs(function, domain, spec).init;
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
