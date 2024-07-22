LCInteractionLayer {
	var lc;
	var net;

	var receivePort;

	var blockSlotRegistry;

	*new {|lc|
		^super.newCopyArgs(lc).init;
	}

	prInitData {
		blockSlotRegistry = ();
	}

	init {
		receivePort = lc.options[\interactionReceivePort];

		net = lc.options[\interactionHost];
		net.sendMsg("/lc/blocks/loadSpecs", lc.options[\specsExportPath]);

		OSCdef(\lcExecuteCommand, {|msg, time, addr, recvPort|
			this.prOnExecuteCommand(msg[1].asSymbol, msg[2].asString, msg[3].asString, msg[4].asString);
		}, '/lc/executeCommand', recvPort: receivePort);

		this.prInitData;
	}

	prOnExecuteCommand {|contextId, blockListString, headId, cmdId|
		(lc.runtime.contexts.includesKey(contextId).not && lc.options[\interactionExecuteOnlyInitializedContexts]).if {
			"Received command for uninitialized context: %".format(contextId).warn;
			^nil;
		};
		lc.runtime.contexts[contextId].execute(blockListString.split($;), headId: headId, cmdId: cmdId);
	}

	sendCommandFeedback {|cmd|
		net.sendMsg("/lc/blocks/commandFeedback", cmd.headId, cmd.id);
	}

	setBlockSlotProperties {|slotId, options|
		options = options ? ();
		net.sendMsg("/lc/blocks/setSlotProperties", slotId, options.jsonString);
	}

	clearAllBlockSlots {
		net.sendMsg("/lc/blocks/clearAllSlots");
		blockSlotRegistry = ();
	}

	despawnBlockSlot {|slotId, options|
		options = options ? ();
		net.sendMsg("/lc/blocks/despawnSlot", slotId, options.jsonString);
	}

	addBlockSlot {|spec, startPosition, options, id|
		var object;

		spec.asString.includes($:).not.if {
			var name = spec.asSymbol;
			var blockSpecCandidates = lc.runtime.blockSpecsForName(name);
			(blockSpecCandidates.size > 0).if {
				spec = blockSpecCandidates[0].id;
			};
			(blockSpecCandidates.size > 1).if {
				"Ambigous block spec lookup for: % - found % candidates. First candidate selected: %".format(name, blockSpecCandidates.size, spec).warn;
			};
		};

		id = id ? LifeCodes.randomId;
		options = options ? ();

		object = (
			\spec: spec,
			\pos: startPosition,
			\id: id,
			\options: options
		);

		LifeCodes.instance.runtime.blockSpecs[spec.asSymbol].isNil.not.if({
			net.sendMsg("/lc/blocks/addSlot", object.jsonString);
			^LCInteractionSlotRef(spec, startPosition, id, options);
		}, {
			"Could not find spec identifier: %".format(spec).error;
			^nil;
		});
	}

	registerBlockSlots {|id, slots, waitFunction|
		blockSlotRegistry[id].isNil.if {
			blockSlotRegistry[id] = List();
		};

		slots.do {|slot|
			blockSlotRegistry[id].add(this.addBlockSlot(*slot));
			waitFunction.value;
		};
	}

	popRegistry {|id|
		var ret = blockSlotRegistry[id];
		blockSlotRegistry[id] = nil;
		^ret;
	}

	clear {
		this.clearAllBlockSlots;
	}
}

LCInteractionSlotRef {
	var <spec;
	var <startPosition;
	var <id;
	var <options;



	*new {|spec, startPosition, id, options|
		^super.newCopyArgs(spec, startPosition, id, options).init;
	}

	init {

	}
}