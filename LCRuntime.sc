LCRuntime {
	var lc;

	var <families;
	var <contexts;

	var index;
	var familyKeys;

	var <typesLookup;


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
		typesLookup = ();
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