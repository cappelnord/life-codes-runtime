LCRuntime {
	var lc;

	var <specs;
	var <contexts;

	var index;
	var specKeys;

	var <typesDict;


	*new {|lc|
		^super.newCopyArgs(lc).init;
	}

	init {
		this.prInitData;
	}

	compile {
		"\n*** COMPILING SPECS ***".postln;

		specKeys = specs.keys.asArray.sort;

		specKeys.do {|key|
			specs[key].compileDomainFunctions;
		};
	}

	buildIndex {
		"\n*** BUILD INDEX ***".postln;



		// the index is a lookup of all code blocks with a reference
		// to all specs that have definitions of the codeblock - this will
		// be the basis to retrieve functions to execute for each block/command

		specKeys.do {|key|
			var spec = specs[key];
			spec.table[\blocks].keys.asArray.sort.do {|blockKey|
				index[blockKey].isNil.if {
					index[blockKey] = List();
				};
				index[blockKey].add(spec);
			};

			// at the same time we keep an index of all specs per type
			spec.table[\type].isNil.not.if {
			    typesDict[spec.table[\type]].isNil.if {typesDict[spec.table[\type]] = List()};
			    typesDict[spec.table[\type]].add(spec.id);
		    };
		};

		// let all specs index themselves in order to copy data from the table into member variables
		specKeys.do {|key|
			specs[key].buildIndex;
		};
	}

	executeList {|list, queue=\runtime|
		// TODO: Deal with temporal things and the actual execution queue - this is super tricky business in the end ...
		list.do {|unit|
			unit.execute;
		};
	}

	prInitData {
		specs = ();
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