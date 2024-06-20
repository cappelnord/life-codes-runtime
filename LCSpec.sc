LCSpec {
	var <family;
	var <table;
	var domainFunctions;

	var currentLoadDomain = nil;

	var runtime;

	*new {|family, domain=nil, function=nil|
		var runtime;

		LifeCodes.instance.isNil.if {
			"LifeCodes is not initialized - cannot use LCSpec things ...".warn;
			^nil;
		};

		runtime = LifeCodes.instance.runtime;

		runtime.specs[family].isNil.if {
			runtime.specs[family] = super.new.init(family);
		};

		domain.isNil.if {
		   	runtime.specs[family];
		};

		runtime.specs[family].addDomainFunction(domain, function);
		^runtime.specs[family];
	}

	init {|id|
		family = id;
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

	getLifecycleFunctions {|phase|
		var ret = nil;
		table[phase].isNil.not.if({
			ret = table[phase].collect({|ref| ref.bind(this, LifeCodes.instance)});
		});
		^ret;
	}

	compileDomainFunctions {
		var domainKeys = domainFunctions.keys.asArray.sort;
		table = (
			\family: family,
			\blocks: (),
			\data: ()
		);

		domainKeys.do {|domainKey|
			var ret;
			"%/% ...".format(family, domainKey).postln;
			currentLoadDomain = domainKey;
			ret = domainFunctions[domainKey].value(this, LifeCodes.instance);
			table.postln;
			currentLoadDomain = nil;
		};
	}

	asString {
		^"LCSpec(\%)".format(family);
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
