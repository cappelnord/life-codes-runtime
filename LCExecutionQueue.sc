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
		^"% - %/%".format(ref.key, ref.family.id, ref.domain);
	}
}

LCBlockFunctionReference {
	var <function;
	var <key;
	var <domain;
	var <family;

	*new {|function, key, domain, family|
		^super.newCopyArgs(function, key, domain, family).init;
	}

	init {

	}

	bind {|...args|
		^LCExecutionUnit(this, args);
	}
}