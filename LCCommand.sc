
LCCommand {
	var <id;
	var <ctx;
	var <blockSourceList;
	var <data;
	var <prependModifiers;
	var <appendModifiers;

	var <blockList;

	var <>pattern;
	var <event;
	var <audioChain;

	var <>doPerform = false;

	// should only be modified by blockinstances on generation
	var <>valid = true;

	var executionQueue;

	var active = true;

	*new {|id, ctx, blockSourceList, cmdData, prependModifiers, appendModifiers|
		^super.newCopyArgs(id, ctx, blockSourceList, cmdData, prependModifiers, appendModifiers).init;
	}

	init {
		try({
			this.prBuildBlockList;
		}, {|exception|
			valid = false;
			exception.reportError;
		});

		executionQueue = LCExecutionQueue("CMD:%".format(id));
	}

	prBuildBlockList {
		var didPrependModifiers = false;

		blockList = List();
		blockSourceList.do {|blockSource|
			var blockInstance = LCBlockInstance(blockSource, this);
			blockInstance.valid.if {
				((blockInstance.spec.type == \modifier) && didPrependModifiers.not).if {
					prependModifiers.do {|prependBlockSource|
						blockList.add(LCBlockInstance(prependBlockSource, this));
						didPrependModifiers = true;
					};
				};
				blockList.add(blockInstance);
			}
		};

		didPrependModifiers.not.if {
			prependModifiers.do {|prependBlockSource|
				blockList.add(LCBlockInstance(prependBlockSource, this));
			};
		};

		appendModifiers.do {|appendBlockSource|
			blockList.add(LCBlockInstance(appendBlockSource, this));
		};
	}

	prPrepare {
		ctx.family.hasAudio.if {
			audioChain = ctx.audioChain.mixer.getCommandChain(id, ctx.id);
		};

		ctx.family.isPatternType.if {
			var functionReferences = ctx.getLifecycleFunctionReferences(\on_pattern_finish);
			var patternFinishFunc = {|event|
				// don't execute if the context got cleared
				ctx.cleared.not.if {
					LifeCodes.instance.baseEnvironment.use({
						functionReferences.do {|ref|
							ref.function.value(event, this, ctx, ctx.family)
						};
					});
				};
			};

			pattern = Pbind(
				\clock, ctx.clock,
				\group, audioChain.group,
				\finish, patternFinishFunc,
				\channel, 0,
				\out, audioChain.bus.index,
				\amp, 1
			);
		};
	}

	prFinalize {
		ctx.family.isPatternType.if {
			// only works if we have still a Pbind here ...
			(pattern.class == Pbind).if {
				pattern.extend(Pbind(
					\out, pattern[\channel] + audioChain.bus.index
				));
			};
		};
	}

	prTryPerform {
		// this is quite temporary so that we can see that things are actually working ...
		ctx.family.isPatternType.if {
			var key = this.prPdefKey;
			doPerform.not.if ({
				Pdef(key).stop;
			}, {
				Pdef(key).quant = ctx.patternQuant;
				Pdef(key, pattern).play;
			});
		};
	}

	prPdefKey {
		^("lc_" ++ ctx.id).asSymbol;
	}


	execute {
		var quantFunc = {
			active.if {
				this.blockList.first.id.isNil.not.if {
					LifeCodes.instance.gui.sendCommandFeedback(this, this.blockList.first.id);
				};
				doPerform.if {
					executionQueue.executeList(this.prGetBlockLifecycleExecutionUnits(\on_perform_once, ctx.getOnceCandidates(blockList, true)));
					executionQueue.executeList(this.prGetBlockLifecycleExecutionUnits(\on_perform));
					executionQueue.executeList(this.prGetLifecycleExecutionUnits(\on_cmd_perform));
					ctx.markBlockHistory(blockList, true);
				}
			};
		};

		// early exit in case this context is actually cleared
		ctx.cleared.if {
			^nil;
		};

		this.prPrepare;

		this.prExecuteLifecycleComplement(\on_enter, ctx.lastCmd.isNil.not.if({ctx.lastCmd.blockList}, nil));

		executionQueue.executeList(this.prGetBlockLifecycleExecutionUnits(\on_pre_execute));
		executionQueue.executeList(this.prGetBlockLifecycleExecutionUnits(\on_execute_once, ctx.getOnceCandidates(blockList, false)));
		executionQueue.executeList(this.prGetBlockLifecycleExecutionUnits(\on_execute));
		ctx.markBlockHistory(blockList, false);
		executionQueue.executeList(this.prGetBlockLifecycleExecutionUnits(\on_post_execute));

		this.prFinalize;

		// 'perform' the command
		this.prTryPerform;

		// see that all quant stuff is set and done
		ctx.quant.isNil.not.if ({
			ctx.clock.schedAbs(ctx.quant.asQuant.nextTimeOnGrid(ctx.clock), quantFunc);
		}, {
			quantFunc.value;
		});
	}

	notifyCtxDataUpdate {|data|
		blockList.do {|blockInstance|
			var functionReferences = ctx.getBlockFunctionReferences(blockInstance.name, \on_ctx_data_update);
			executionQueue.executeList(
				functionReferences.collect {|ref| ref.bind(data, blockInstance, this, this.ctx, this.ctx.family)},
				LifeCodes.instance.options[\alsoTraceRapidFunctions].not
			);
		};
	}

	prExecuteLifecycleComplement {|phase, otherList|
		var list = List();
		var set = IdentitySet();
		blockList.do {|blockInstance|
			var include = true;
			// if otherList is nil it should still work
			otherList.do {|otherInstance|
				(blockInstance.name == otherInstance.name).if {
					include = false;
				};
			};
			include.if {
				set.includes(blockInstance.name).not.if {
					set.add(blockInstance.name);
					list.add(blockInstance);
				};
			};
		};
		executionQueue.executeList(this.prGetBlockLifecycleExecutionUnits(phase, list));
	}

	executeLeave {
		active = false;
		audioChain.isNil.not.if {
			audioChain.dismiss;
		};
		this.prExecuteLifecycleComplement(\on_leave, ctx.cmd.blockList);
	}

	prGetBlockLifecycleExecutionUnits {|phase, instanceList|
		var ret = List();
		instanceList = instanceList ? blockList;
		instanceList.do {|blockInstance|
			var functionReferences = ctx.getBlockFunctionReferences(blockInstance.name, phase);
			ret.addAll(functionReferences.collect {|ref| ref.bind(blockInstance, this, this.ctx, this.ctx.family)});
		};
		^ret;
	}

	prGetLifecycleExecutionUnits {|phase|
		var list = ctx.getLifecycleFunctionReferences(phase);
		^(list.collect {|ref|
			ref.bind(this, this.ctx, this.ctx.family);
		});
	}

	clear {
		active.if {
			this.executeLeave;
			ctx.family.isPatternType.if {
				Pdef(this.prPdefKey).stop.clear;
			};
		};
	}

	playEvent {|event_, isGenerator=true|
		event = event_;
		audioChain.playEvent(event_, isGenerator);
	}
}