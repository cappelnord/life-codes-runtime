LCdef {
	*new {|contextId, command, data|
		var runtime;
		var context;
		var contentIsCommand = false;
		var family;

		LifeCodes.instance.isNil.if {
			"LifeCodes is not initialized - cannot use LCdef ...".warn;
			^nil;
		};

		runtime = LifeCodes.instance.runtime;

		context = runtime.contexts[contextId];

		context.isNil.if {
			(command.isNil || (command.class == Symbol)).if({
				family = command;
			}, {
				contentIsCommand = true;
				family = command[0][0].asSymbol;
			});

			family = runtime.specs[family];

			family.isNil.if {
				"Could not initialize context with family '%' ...".format(command).warn;
				^nil;
			};

			family.hasSubject.not.if {
				"Family '%' does not have a subject ...".format(family.id).warn;
				^nil;
			};

			runtime.contexts[contextId].isNil.if {
				runtime.contexts[contextId] = LCContext(contextId, family);
			};

			context = runtime.contexts[contextId];
		};

		data.isNil.not.if {
			context.updateData(data);
		};

		contentIsCommand.if {
			context.executeCommand(command);
		};

		^context;
	}
}

LCContext {
	var <id;
	var <family;

	var <isLoaded = false;

	*new {|id, family|
		^super.newCopyArgs(id, family).init;
	}

	init {

	}

	load {

		isLoaded = true;
	}

	unload {

		isLoaded = false;
	}

	updateData {|data|

	}

	executeCommand {|command|

	}
}