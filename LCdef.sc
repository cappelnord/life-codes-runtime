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

	var <audioChain;

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

		family.hasAudio.if {
			audioChain = LifeCodes.instance.mixer.getContextChain(id);
		};

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
		data.keys.do {|key|
			this.data[key] = data[key];
		};

		executeFunctions.if {
			// context update function
			executionQueue.executeList(
				family.getLifecycleFunctionReferences(\on_ctx_data_update)
				.collect {|f| f.bind(data, this, family, LifeCodes.instance)},
				LifeCodes.instance.options[\alsoTraceRapidFunctions].not
			);
			// notify active command to forward to blocks
			cmd.isNil.not.if {
				cmd.notifyCtxDataUpdate(data);
			};
		};
	}

	execute {|blockList, cmdData, headId, cmdId|
		var newCmd;
		// this is just to ensure that the family is loaded
		this.load;

		// create a new command from the block list
		newCmd = LCCommand(cmdId ? LifeCodes.randomId, headId, this, blockList, cmdData ? (), prependModifiers, appendModifiers);

		newCmd.valid.if({
			this.prExecuteCommand(newCmd);
		}, {
			"Received invalid command: % - did not execute".format(blockList.cs).error;
		});
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

		lastCmd.isNil.not.if {
			lastCmd.clear;
		};
		cmd.isNil.not.if {
			cmd.clear;
		};

		audioChain.isNil.not.if {
			// maybe this could be more graceful (to dismiss the chain, not to clear)
			LifeCodes.instance.mixer.clearContextChain(id);
		}
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
	// this is the block id of the head(subject) of a group used for feedback; it seems a bit redudant to have so many different ids (command, head, ctx, ...)
	// but it is currently easiest to retrieve the block by its (internal?) id than to see which context a group belongs to
	var <headId;
	var <ctx;
	var <blockList;
	var <data;
	var <prependModifiers;
	var <appendModifiers;

	var <blockInstanceList;

	var <pattern;
	var <audioChain;

	var <>doPerform = false;

	// should only be modified by blockinstances on generation
	var <>valid = true;

	var executionQueue;

	var active = true;

	*new {|id, headId, ctx, blockList, cmdData, prependModifiers, appendModifiers|
		^super.newCopyArgs(id, headId, ctx, blockList, cmdData, prependModifiers, appendModifiers).init;
	}

	init {
		try({
			this.prBuildBlockInstanceList;
		}, {|exception|
			valid = false;
			exception.reportError;
		});

		executionQueue = LCExecutionQueue("CMD:%".format(id));
	}

	prBuildBlockInstanceList {
		var didPrependModifiers = false;

		blockInstanceList = List();
		blockList.do {|blockSource|
			var blockInstance = LCBlockInstance(blockSource, this);
			blockInstance.valid.if {
				blockInstanceList.add(blockInstance);
				((blockInstance.spec.type == \modifier) && didPrependModifiers.not).if {
					prependModifiers.do {|prependBlockSource|
						blockInstanceList.add(LCBlockInstance(prependBlockSource, this));
						didPrependModifiers = true;
					};
				};
			}
		};
		appendModifiers.do {|appendBlockSource|
			blockInstanceList.add(LCBlockInstance(appendBlockSource, this));
		};
	}

	prPrepare {
		(ctx.family.type == \pattern).if {
			pattern = Pbind(
				\clock, ctx.clock,
				\channel, 0
			);
		};

		ctx.family.hasAudio.if {
			audioChain = ctx.audioChain.mixer.getCommandChain(id, ctx.id);
		};
	}

	prFinalize {
		(ctx.family.type == \pattern).if {
			// add the finish func
			var functionReferences = ctx.family.getLifecycleFunctionReferences(\on_pattern_finish);
			var patternFinishFunc = {|event|
				functionReferences.do {|ref|
					ref.function.value(event, this, ctx, ctx.family, ctx.family)
				};
			};

			// we route into the right group and output channel and append the finish func
			pattern.extend(Pbind(
				\finish, patternFinishFunc,
				\group, audioChain.group,
				\out, pattern[\channel] + audioChain.bus.index
			));

		};
	}

	prTryPerform {
		// this is quite temporary so that we can see that things are actually working ...
		(ctx.family.type == \pattern).if {
			var key = this.prPdefKey;
			doPerform.not.if ({
				Pdef(key).stop;
			}, {
				Pdef(key).quant = ctx.family.quant;
				Pdef(key, pattern).play;
			});
		};
	}

	prPdefKey {
		^("lc_" + ctx.id).asSymbol;
	}

	execute {
		var quantFunc = {
			active.if {
				headId.isNil.not.if {
					LifeCodes.instance.gui.sendCommandFeedback(this);
				};
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
		this.prTryPerform;

		// see that all quant stuff is set and done
		ctx.family.quant.isNil.not.if ({
			ctx.clock.schedAbs(ctx.quant.asQuant.nextTimeOnGrid(ctx.clock), quantFunc);
		}, {
			quantFunc.value;
		});
	}

	notifyCtxDataUpdate {|data|
		blockInstanceList.do {|blockInstance|
			var functionReferences = ctx.family.getBlockFunctionReferences(blockInstance.name, \on_ctx_data_update);
			executionQueue.executeList(
				functionReferences.collect {|ref| ref.bind(data, blockInstance, this, this.ctx, this.ctx.family, ref.family)},
				LifeCodes.instance.options[\alsoTraceRapidFunctions].not
			);
		};
	}

	executeLeave {
		active = false;
		audioChain.isNil.not.if {
			audioChain.dismiss;
		};
	}

	prGetBlockLifecycleExecutionUnits {|phase, instanceList|
		var ret = List();
		instanceList = instanceList ? blockInstanceList;
		instanceList.do {|blockInstance|
			var functionReferences = ctx.family.getBlockFunctionReferences(blockInstance.name, phase);
			ret.addAll(functionReferences.collect {|ref| ref.bind(blockInstance, this, this.ctx, this.ctx.family)});
		};
		^ret;
	}

	clear {
		active.if {
			this.executeLeave;
			(ctx.family.type == \pattern).if {
				Pdef(this.prPdefKey).clear;
			};
		};
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
	var <valid = true;

	*new {|source, cmd|
		^super.newCopyArgs(source, cmd).init;
	}

	init {
		cleanSource = LCBlockInstance.cleanSource(source);

		name = cleanSource[\name];

		// let's see if this block is defined at all in the family hierarchy
		cmd.ctx.family.matchesBlock(name).if ({
			this.prProcessParameters;
		}, {
			"There is no 'primary' block '%' that matches the family '%'".format(name, cmd.ctx.family.id).postln;
			cmd.valid = false;
			valid = false;
		});

		args = cleanSource[\args];
		data = cleanSource[\data];
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

		// decode (bit weird) syntax to send arguments alongside codeblocks from gui layer
		(cleanSource.class == String).if {
			var x = cleanSource.split($,);

			cleanSource = [x[0].asSymbol];
			(x.size > 1).if {
				cleanSource = cleanSource.addAll(LCGUI.decodeOSCValues(x[1..]));
			};
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