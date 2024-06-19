LCSpec {

	var <family;
	var <table;
	var domainFunctions;

	var currentLoadDomain = nil;

	*new {|family, domain=nil, function=nil|
		LifeCodes.instance.isNil.if {
			"LifeCodes is not initialized - cannot use LCSpec things ...".warn;
			^nil;
		};

		LifeCodes.instance.specs[family].isNil.if {
			LifeCodes.instance.specs[family] = super.new.init(family);
		};

		domain.isNil.if {
		   	LifeCodes.instance.specs[family];
		};

		LifeCodes.instance.specs[family].addDomainFunction(domain, function);
		^LifeCodes.instance.specs[family];
	}

	init {|id|

		family = id;

		domainFunctions = ();

		// "Created LCSpec with ID: %".format(id).postln;
		^this;
	}

	prMergeTable {|src, dst|
		var allKeys = src.keys.reject {|key| key == \blocks};

		allKeys.do {|key|
			key.asString.beginsWith("on_").if({
				// build a function list
				dst[key].isNil.if {
					dst[key] = List();
				};
				dst[key].add(src[key]);
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

	compileDomainFunctions {
		var domainKeys = domainFunctions.keys.asArray.sort;
		table = (
			\family: family,
			\blocks: ()
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
}