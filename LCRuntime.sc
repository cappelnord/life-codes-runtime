LCRuntime {
	var lc;

	var <families;
	var <contexts;

	var index;
	var familyKeys;

	var <typesDict;


	*new {|lc|
		^super.newCopyArgs(lc).init;
	}

	init {
		this.prInitData;
	}

	compile {
		"\n*** COMPILING SPECS/FAMILIES ***".postln;

		familyKeys = families.keys.asArray.sort;

		familyKeys.do {|key|
			families[key].compileDomainFunctions;
		};
	}

	buildIndex {
		"\n*** BUILD INDEX ***".postln;



		// the index is a lookup of all code blocks with a reference
		// to all families that have definitions of the codeblock - this will
		// potentially be obselete, now that "inheritance" is redesigned

		familyKeys.do {|key|
			var family = families[key];
			family.table[\blocks].keys.asArray.sort.do {|blockKey|
				index[blockKey].isNil.if {
					index[blockKey] = List();
				};
				index[blockKey].add(family);
			};

			// at the same time we keep an index of all families per type
			family.table[\type].isNil.not.if {
			    typesDict[family.table[\type]].isNil.if {typesDict[family.table[\type]] = List()};
			    typesDict[family.table[\type]].add(family.id);
		    };
		};

		// let all families index themselves in order to copy data from the table into member variables
		familyKeys.do {|key|
			families[key].buildIndex;
		};
	}

	executeList {|list, queue=\runtime|
		// TODO: Deal with temporal things and the actual execution queue - this is super tricky business in the end ...
		list.do {|unit|
			unit.execute;
		};
	}

	prInitData {
		families = ();
		contexts = ();
		index = ();
		typesDict = ();
	}

	addContext {|context|
		contexts[context.id] = context;
	}

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
}