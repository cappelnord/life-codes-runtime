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

		// This is a bit a shmoo here but hopefully catches all cases
		// ...
		(blockList.class == String).if { blockList = blockList.asSymbol};

		(blockList.isNil || (blockList.class == Symbol)).if({
			family = blockList;
		}, {
			contentIsBlockList = true;
			family = LCBlockInstance.cleanSource(blockList[0])[\name];
		});
		// ...


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
	var <executionQueue;
	var <>clock;
	var <>quant;

	var <>prependModifiers;
	var <>appendModifiers;

	var runtime;

	var executedBlocksSet;
	var quantExecutedBlocksSet;

	*new {|id, family|
		^super.newCopyArgs(id, family).init;
	}

	init {
		executedBlocksSet = Set();
		quantExecutedBlocksSet = Set();

		executionQueue = LCExecutionQueue("CTX:%".format(id));
		data = ();
		prependModifiers = [];
		appendModifiers = [];
		runtime = LifeCodes.instance.runtime;
		quant = family.quant;
		clock = LifeCodes.instance.options[\clock] ? TempoClock.default;

		this.executeLifecyclePhase(\on_ctx_create);
	}

	executeLifecyclePhase {|phase|
		"Execute Context Lifecycle Phase: %/%/%".format(id, family.id, phase).postln;
		executionQueue.executeList(
			family.getLifecycleFunctionReferences(phase)
			.collect {|f| f.bind(this, family, LifeCodes.instance)}
		);
	}

	// called by execute or manually
	load {
		family.load(executionQueue);
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

		cmd = newCmd;

		lastCmd.isNil.not.if {
			lastCmd.executeLeave;
		};
		cmd.execute;
	}

	clear {|unloadFamily=true|
		runtime.removeContext(this, unloadFamily);
		this.executeLifecyclePhase(\on_ctx_clear);
	}

	resetBlockSets {
		executedBlocksSet.clear;
		quantExecutedBlocksSet.clear;
	}

	getOnceCandidates {|blockInstances, quant=false|
		var set = executedBlocksSet;
		var ret = List();
		quant.if { set = quantExecutedBlocksSet };
		blockInstances.do {|blockInstance|
			set.includes(blockInstance.name).not.if {
				ret.add(blockInstance);
				set.add(blockInstance.name);
			};
		};
		^ret;
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

	var <pattern;

	var <>doPerform = false;

	var executionQueue;

	var active = true;

	*new {|id, ctx, blockList, cmdData, prependModifiers, appendModifiers|
		^super.newCopyArgs(id, ctx, blockList, cmdData, prependModifiers, appendModifiers).init;
	}

	init {
		var didPrependModifiers = false;

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

		executionQueue = LCExecutionQueue("CMD:%".format(id));
	}

	prPrepare {
		(ctx.family.type == \pattern).if {
			pattern = Pbind(
				\clock, ctx.clock,
				\channel, 0
			);
		}
	}

	prFinalize {
		// if it is a pattern then let's set group and out channel here

		// add the finish func
		(ctx.family.type == \pattern).if {
			var functionReferences = ctx.family.getLifecycleFunctionReferences(\on_pattern_finish);
			var patternFinishFunc = {|event|
				functionReferences.do {|ref|
					ref.function.value(event, this, ctx, ctx.family, ctx.family)
				};
			};
			pattern.extend(Pbind(\finish, patternFinishFunc));
		};
	}

	prPerform {
		// this is quite temporary so that we can see that things are actually working ...
		(ctx.family.type == \pattern).if {
			var key = this.prPdefKey;
			doPerform.not.if ({
				Pdef(key, nil);
			}, {
				Pdef(key).quant = ctx.family.quant;
				Pdef(key, pattern).play;
				// todo: queue up quant stuff
			});
		};
	}

	prPdefKey {
		^("lc_" + ctx.id).asSymbol;
	}

	execute {
		var quantFunc = {
			active.if {
				LifeCodes.instance.interaction.sendCommandFeedback(this);
				executionQueue.executeList(this.prGetBlockLifecycleExecutionUnits(\on_quant_once, ctx.getOnceCandidates(blockInstanceList, true)));
				executionQueue.executeList(this.prGetBlockLifecycleExecutionUnits(\on_quant));
			};
		};

		this.prPrepare;

		executionQueue.executeList(this.prGetBlockLifecycleExecutionUnits(\on_pre_execute));
		executionQueue.executeList(this.prGetBlockLifecycleExecutionUnits(\on_once, ctx.getOnceCandidates(blockInstanceList, false)));
		executionQueue.executeList(this.prGetBlockLifecycleExecutionUnits(\on_execute));
		executionQueue.executeList(this.prGetBlockLifecycleExecutionUnits(\on_post_execute));

		this.prFinalize;

		// 'perform' the command
		doPerform.if {
			this.prPerform;
		};

		// see that all quant stuff is set and done
		ctx.family.quant.isNil.not.if ({
			ctx.clock.schedAbs(ctx.quant.asQuant.nextTimeOnGrid(ctx.clock), quantFunc);
		}, {
			quantFunc.value;
		});
	}

	executeLeave {
		active = false;
	}

	prGetBlockLifecycleExecutionUnits {|phase, instanceList|
		var ret = List();
		instanceList = instanceList ? blockInstanceList;
		instanceList.do {|blockInstance|
			var functionReferences = ctx.family.getBlockFunctionReferences(blockInstance.name, phase);
			ret.addAll(functionReferences.collect {|ref| ref.bind(blockInstance, this, this.ctx, this.ctx.family, ref.family)});
		};
		^ret;
	}
}

LCBlockInstance {
	var <source;
	var <cmd;

	var <cleanSource;
	var <name;
	var <args;
	var <data;
	var <spec;

	*new {|source, cmd|
		^super.newCopyArgs(source, cmd).init;
	}

	init {
		cleanSource = LCBlockInstance.cleanSource(source);
		this.prProcessParameters;
		args = cleanSource[\args];
		data = cleanSource[\data];
		name = cleanSource[\name];
	}

	prProcessParameters {
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


	*cleanSource {|source|
		var cleanSource = source.deepCopy;

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

		^cleanSource;
	}
}