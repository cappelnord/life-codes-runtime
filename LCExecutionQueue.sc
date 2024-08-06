LCExecutionQueue {
	var <name;

	*new {|name|
		^super.newCopyArgs(name).init;
	}

	init {

	}


	// TODO: Deal with temporal things
	executeList {|list, neverTrace=false|
		var trace = LifeCodes.instance.options[\traceExecutionQueues] && neverTrace.not;
		list.do {|unit|
			trace.if {
				"TRACE: % - %".format(name, unit.asString).postln;
			};
			unit.execute;
		};
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

	asString {
		var familyString = ref.family.isNil.if("anonymous", {ref.family.id});

		ref.blockId.isNil.if({
			^"% - %/%".format(ref.key, familyString, ref.domain);
		}, {
			^"% - % - %/%".format(ref.key, ref.blockId, familyString, ref.domain);
		});
	}
}

LCFunctionReference {
	var <function;
	var <key;
	var <domain;
	var <family;
	var <blockId;

	*new {|function, key, domain, family, blockId|
		^super.newCopyArgs(function, key, domain, family, blockId).init;
	}

	init {

	}

	bind {|...args|
		^LCExecutionUnit(this, args);
	}
}