LCdef {
	*new {|contextId, blockList, data|
		var runtime;
		var context;
		var contentIsBlockList = false;
		var family;

		LifeCodes.instance.isNil.if {
			"LifeCodes is not initialized - cannot use LCdef ...".warn;
			^nil;
		};

		runtime = LifeCodes.instance.runtime;

		context = runtime.contexts[contextId];

		(blockList.isNil || (blockList.class == Symbol)).if({
			family = blockList;
		}, {
			contentIsBlockList = true;
			family = blockList[0][0].asSymbol;
		});

		family = runtime.families[family];

		context.isNil.if {
			family.isNil.if {
				"Could not initialize context with family '%' ...".format(blockList).warn;
				^nil;
			};

			family.hasSubject.not.if {
				"Family '%' does not have a subject ...".format(family.id).warn;
				^nil;
			};

			runtime.contexts[contextId].isNil.if {
				runtime.addContext(LCContext(contextId, family));
			};

			context = runtime.contexts[contextId];
		};

		data.isNil.not.if {
			context.updateData(data, false);
		};

		contentIsBlockList.if {
			context.execute(blockList);
		};

		^context;
	}
}

LCContext {
	var <id;
	var <family;
	var <data;

	var <cmd = nil;
	var <lastCmd = nil;

	var <>prependModifiers;
	var <>appendModifiers;

	var runtime;

	*new {|id, family|
		^super.newCopyArgs(id, family).init;
	}

	init {
		data = ();
		prependModifiers = [];
		appendModifiers = [];
		runtime = LifeCodes.instance.runtime;
		this.executeLifecyclePhase(\on_ctx_create);
	}

	executeLifecyclePhase {|phase|
		"Execute Context Lifecycle Phase: %/%/%".format(id, family.id, phase).postln;
		runtime.executeList(
			family.getLifecycleFunctionReferences(phase)
			.collect {|f| f.bind(this, family, LifeCodes.instance)},
			\runtime
		);
	}

	// called by execute or manually
	load {
		family.load;
	}

	updateData {|data, executeFunctions=true|
		// TODO: Merge Data

		executeFunctions.if {
			this.executeLifecyclePhase(\on_ctx_create);
			// TODO: send up to active command (to notify blocks)
		};
	}

	execute {|blockList, cmdData, commandId|
		var newCmd;
		// this is just to ensure that the family is loaded
		this.load;

		// create a new command from the block list
		newCmd = LCCommand(commandId ? LifeCodes.randomId, this, blockList, cmdData ? (), prependModifiers, appendModifiers);

		// TODO: Check if it is all valid
		this.prExecuteCommand(newCmd);
	}

	prExecuteCommand {|newCmd|
		cmd.isNil.not.if {
			lastCmd = cmd;
			// TODO: class on_cmd_leave and on_leave on all blocks of the last command
		};

		// execute all the stuff of the new cmd

		// 'perform' the command

	}

	clear {|unloadFamily=true|
		runtime.removeContext(this, unloadFamily);
		this.executeLifecyclePhase(\on_ctx_clear);
	}
}

LCCommand {
	var <id;
	var <ctx;
	var <blockList;
	var <data;
	var <prependModifiers;
	var <appendModifiers;

	var <blockInstanceList;

	*new {|id, ctx, blockList, cmdData, prependModifiers, appendModifiers|
		^super.newCopyArgs(id, ctx, blockList, cmdData, prependModifiers, appendModifiers).init;
	}

	init {
		var didPrependModifiers = false;

		blockList.dump;
		// we should probably work through this list here to unify everything - strings to symbols, arguments, etc.
		// this is where we now create the block instances that can also be used to store info (arguments will be merged with data)

		blockInstanceList = List();
		blockList.do {|blockSource|
			var blockInstance = LCBlockInstance(blockSource, this);
			blockInstanceList.add(blockInstance);
			((blockInstance.spec.type == \modifier) && didPrependModifiers.not).if {
				prependModifiers.do {|prependBlockSource|
					blockInstanceList.add(LCBlockInstance(prependBlockSource, this));
					didPrependModifiers = true;
				};
			};
		};
		appendModifiers.do {|appendBlockSource|
			blockInstanceList.add(LCBlockInstance(appendBlockSource, this));
		};
		blockInstanceList.postln;
	}
}

LCBlockInstance {
	var <source;
	var <cmd;

	var <cleanSource;
	var <args;
	var <data;
	var <spec;

	*new {|source, cmd|
		^super.newCopyArgs(source, cmd).init;
	}

	init {
		this.prCleanSource;
		args = cleanSource[\args];
		data = cleanSource[\data];
		// something more to do here?
	}

	prCleanSource {
		cleanSource = source;
		(cleanSource.class == String).if {
			cleanSource = cleanSource.asSymbol;
		};

		(cleanSource.class == Symbol).if {
			cleanSource = [cleanSource];
		};

		// if it is a sequenceable collection we build an event here
		cleanSource.isSequenceableCollection.if {
			cleanSource = (
				\name: cleanSource[0],
				\positionalArgs: cleanSource[1..]
			);
		};

		// we must find/assign the spec here to resolve positional arguments
		spec = cmd.ctx.family.findBlockSpec(cleanSource[\name]);

		cleanSource[\args] = cleanSource[\args] ? ();

		spec.parameters.do {|parameter, i|
			cleanSource[\args][parameter.id].isNil.if {
				(cleanSource[\positionalArgs].size > i).if ({
					cleanSource[\args][parameter.id] = parameter.convert(cleanSource[\positionalArgs][i]);
			    }, {
					cleanSource[\args][parameter.id] = parameter.default;
				});
			};
		};

		cleanSource[\data] = cleanSource[\data] ? ();

		cleanSource.removeAt(\positionalArgs);
	}
}