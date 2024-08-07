LCdef {
	*new {|contextId, blockSourceList, data|
		var runtime;
		var context;
		var contentIsBlockSourceList = false;
		var family;

		LifeCodes.instance.isNil.if {
			"LifeCodes is not initialized - cannot use LCdef ...".warn;
			^nil;
		};

		runtime = LifeCodes.instance.runtime;

		context = runtime.contexts[contextId];

		// This is a bit a shmoo here but hopefully catches all cases
		// ...
		(blockSourceList.class == String).if { blockSourceList = blockSourceList.asSymbol};

		(blockSourceList.isNil || (blockSourceList.class == Symbol)).if({
			family = blockSourceList;
		}, {
			contentIsBlockSourceList = true;
			family = LCBlockInstance.cleanSource(blockSourceList[0])[\name];
		});
		// ...


		family = runtime.families[family];

		context.isNil.if {
			family.isNil.if {
				"Could not initialize context with family '%' ...".format(blockSourceList).warn;
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

		contentIsBlockSourceList.if {
			context.execute(blockSourceList);
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

	var <executedBlockHistory;
	var <performedBlockHistory;

	var functionTable;

	*new {|id, family|
		^super.newCopyArgs(id, family).init;
	}

	init {
		executedBlockHistory = ();
		performedBlockHistory = ();
		this.clearFunctionTable;

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
		"Executing Context Lifecycle Phase for %: %/%".format(id, family.id, phase).postln;
		executionQueue.executeList(
			this.getLifecycleFunctionReferences(phase)
			.collect {|f| f.bind(this, family)}
		);
	}

	getLifecycleFunctionReferences {|phase|
		var familyReferences = family.getLifecycleFunctionReferences(phase);
		var contextReferences = functionTable[phase];
		familyReferences = familyReferences ? [];
		contextReferences.isNil.not.if {
			familyReferences = familyReferences ++ contextReferences.values;
		};
		^familyReferences;
	}

	getBlockFunctionReferences {|blockId, phase|
		var familyReferences = family.getBlockFunctionReferences(blockId, phase);
		var contextReferences = nil;
		functionTable[\blocks][blockId].isNil.not.if {
			contextReferences = functionTable[\blocks][blockId][phase];
		};
		contextReferences.isNil.not.if {
			familyReferences = familyReferences ++ contextReferences.values;
		};
		^familyReferences;
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
				this.getLifecycleFunctionReferences(\on_ctx_data_update)
				.collect {|f| f.bind(data, this, family)},
				LifeCodes.instance.options[\alsoTraceRapidFunctions].not
			);
			// notify active command to forward to blocks
			cmd.isNil.not.if {
				cmd.notifyCtxDataUpdate(data);
			};
		};
	}

	execute {|blockSourceList, cmdData, cmdId|
		var newCmd;
		// this is just to ensure that the family is loaded
		this.load;

		// create a new command from the block list
		newCmd = LCCommand(cmdId ? LifeCodes.randomId, this, blockSourceList, cmdData ? (), prependModifiers, appendModifiers);

		newCmd.valid.if({
			this.prExecuteCommand(newCmd);
		}, {
			"Received invalid command: % - did not execute".format(blockSourceList.cs).error;
		});
	}

	reevaluate {
		cmd.isNil.not.if {
			this.execute(cmd.blockSourceList, cmd.data);
		}
	}

	prExecuteCommand {|newCmd|
		cmd.isNil.not.if {
			lastCmd = cmd;
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

		// should we also send the leave functions here?

		audioChain.isNil.not.if {
			// maybe this could be more graceful (to dismiss the chain, not to clear)
			LifeCodes.instance.mixer.clearContextChain(id);
		}
	}

	blockHistory {|performed=false|
		^(performed.if({performedBlockHistory}, {executedBlockHistory}));
	}

	// let's refactor it, call it blockHistory and count the occurences of blocks
	resetBlockHistory {
		executedBlockHistory.clear;
		performedBlockHistory.clear;
	}

	getOnceCandidates {|blockInstances, performed=false|
		var dict = this.blockHistory(performed);
		var ret = List();
		blockInstances.do {|blockInstance|
			dict[blockInstance.name].isNil.if {
				ret.add(blockInstance);
			}
		};
		^ret;
	}

	markBlockHistory {|blockInstances, performed=false|
		var dict = this.blockHistory(performed);
		var ret = List();
		blockInstances.do {|blockInstance|
			dict[blockInstance.name].isNil.if {
				dict[blockInstance.name] = 0;
			};
			dict[blockInstance.name] = dict[blockInstance.name] + 1;
		};
	}


	// this probably duplicates some functionality, but ... it is also fine.

	clearFunctionTable {
		functionTable = (blocks: ());
	}

	prPopulateFunctionTable {|table, functionTable, domain, blockId|
		table.keys.do {|key|
			functionTable[key].isNil.if {
				functionTable[key] = ();
			};
			functionTable[key][domain] = LCFunctionReference(table[key], key, domain, nil, blockId);
		}
	}

	define {|table, domain=\context|
		LifeCodes.instance.domainActive(domain).not.if {^nil;};
		this.prPopulateFunctionTable(table, functionTable, domain, nil);
	}

	defineBlock {|blockId, table, domain=\context|
		LifeCodes.instance.domainActive(domain).not.if {^nil;};

		(table.class == Pbind).if {
			var pbind = table;
			table = (
				on_execute: {|block, cmd, ctx, family|
					cmd.pattern.extend(pbind);
				}
			);
		};

		(table.class == Function).if {
			var function = table;
			table = (
				on_execute: function
			);
		};

		functionTable[\blocks][blockId].isNil.if {
			functionTable[\blocks][blockId] = ();
		};

		this.prPopulateFunctionTable(table, functionTable[\blocks][blockId], domain, blockId);
	}


	// convenience function to set the gain of a context directly

	gain {
		^audioChain.gain;
	}

	gain_ {|gain, lag=0.5|
		audioChain.gain_(gain, lag);
		^this;
	}
}
