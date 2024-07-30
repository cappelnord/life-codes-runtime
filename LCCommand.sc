
LCCommand {
	var <id;
	var <ctx;
	var <blockSourceList;
	var <data;
	var <prependModifiers;
	var <appendModifiers;

	var <blockList;

	var <pattern;
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
				blockList.add(blockInstance);
				((blockInstance.spec.type == \modifier) && didPrependModifiers.not).if {
					prependModifiers.do {|prependBlockSource|
						blockList.add(LCBlockInstance(prependBlockSource, this));
						didPrependModifiers = true;
					};
				};
			}
		};
		appendModifiers.do {|appendBlockSource|
			blockList.add(LCBlockInstance(appendBlockSource, this));
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
					ref.function.value(event, this, ctx, ctx.family)
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

	// TODO TODO TODO
	headBlockId {
		^nil;
	}

	execute {
		var quantFunc = {
			active.if {

				this.headBlockId.isNil.not.if {
					LifeCodes.instance.gui.sendCommandFeedback(this, this.headBlockId);
				};

				executionQueue.executeList(this.prGetBlockLifecycleExecutionUnits(\on_quant_once, ctx.getOnceCandidates(blockList, true)));
				executionQueue.executeList(this.prGetBlockLifecycleExecutionUnits(\on_quant));
			};
		};

		this.prPrepare;

		executionQueue.executeList(this.prGetBlockLifecycleExecutionUnits(\on_pre_execute));
		executionQueue.executeList(this.prGetBlockLifecycleExecutionUnits(\on_once, ctx.getOnceCandidates(blockList, false)));
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
		blockList.do {|blockInstance|
			var functionReferences = ctx.family.getBlockFunctionReferences(blockInstance.name, \on_ctx_data_update);
			executionQueue.executeList(
				functionReferences.collect {|ref| ref.bind(data, blockInstance, this, this.ctx, this.ctx.family)},
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
		instanceList = instanceList ? blockList;
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