LCRuntime {
	var lc;

	var <families;
	var <contexts;
	var <blockSpecs;

	var familyKeys;

	var <typesLookup;

	var <executionQueue;


	*new {|lc|
		^super.newCopyArgs(lc).init;
	}

	blockSpecsForName {|name|
		var ret = List();
		blockSpecs.do {|spec|
			(spec.name == name).if {
				ret.add(spec);
			};
		};
		^ret;
	}

	init {
		executionQueue = LCExecutionQueue("RUN");
		this.prInitData;
	}

	compile {
		"\n*** COMPILING DOMAINS ***".postln;

		familyKeys = families.keys.asArray.sort;

		familyKeys.do {|key|
			families[key].compileDomainFunctions;
		};
	}

	buildIndex {
		"\n*** BUILDING INDEX ***".postln;

		// at the same time we keep an index of all families per type

		familyKeys.do {|key|
			var family = families[key];

			family.table[\type].isNil.not.if {
			    typesLookup[family.table[\type]].isNil.if {typesLookup[family.table[\type]] = List()};
			    typesLookup[family.table[\type]].add(family);
		    };
		};

		// let's add all the extension - we have the type index by now
		familyKeys.do {|key|
			var family = families[key];
			family.table[\extends].do {|key|
				key.asString.beginsWith("type_").if ({
					var type = key.asString[5..].asSymbol;
					typesLookup[type].do {|other|
						other.addExtensionFamily(family);
					};
				}, {
					families[key].addExtensionFamily(family);
				})
			};
		};

		// let all families index themselves in order to copy data from the table into member variables
		// and recursively build the inheritance tree

		familyKeys.do {|key|
			families[key].buildIndex;
		};

		// from the inheritance tree we deduct which families match to which family

		familyKeys.do {|key|
			families[key].buildMatches;
		};
	}

	prInitData {
		families = ();
		contexts = ();
		typesLookup = ();
		blockSpecs = ();
	}

	addContext {|context|
		contexts[context.id] = context;
	}

	// don't call manually - only call from context clearing
	removeContext {|context, unloadFamily|
		var family = context.family;
		contexts.removeAt(context.id);

		unloadFamily.if {
			var contextsWithSameFamily = 0;

			contexts.do {|other|
				(other.family == family).if {
					contextsWithSameFamily = contextsWithSameFamily + 1;
				}
			};

			(contextsWithSameFamily == 0).if {
				family.unload;
			};
		};
	}

	clearAllContexts {
		contexts.do {|ctx|
			ctx.clear;
		};
	}

	clear {
		this.clearAllContexts;
	}
}