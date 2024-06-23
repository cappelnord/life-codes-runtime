LCRuntime {
	var lc;

	var <specs;
	var <contexts;
	var index;

	var specKeys;


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
	}

	addContext {|context|
		contexts[context.id] = context;
	}

	removeContext {|context, unloadFamily|
		var family = context.family;
		contexts.removeAt(context.id);

		unloadFamily.if {
			var requiresUnload = true;
			contexts.do {|other|
				requiresUnload = requiresUnload && (other.family != context.family);
			};

			requiresUnload.do {
				family.unload;
			};
		};
	}
}