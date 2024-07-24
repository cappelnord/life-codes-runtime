LCGUI {
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
		receivePort = lc.options[\guiReceivePort];

		net = lc.options[\guiHost];
		net.sendMsg("/lc/blocks/loadSpecs", lc.options[\specsExportPath]);

		OSCdef(\lcExecuteCommand, {|msg, time, addr, recvPort|
			this.prOnExecuteCommand(msg[1].asSymbol, msg[2].asString, msg[3].asString, msg[4].asString);
		}, '/lc/executeCommand', recvPort: receivePort);

		OSCdef(\lcContextDataUpdate, {|msg, time, addr, recvPort|
			var list = LCGUI.decodeOSCValues(msg[2].asString.split($,));
			var data = ();
			list.clump(2).do {|item|
				data[item[0].asSymbol] = item[1];
			};
			this.prOnContextDataUpdate(msg[1].asSymbol, data);
		}, '/lc/contextDataUpdate', recvPort: receivePort);


		this.prInitData;
	}

	prOnExecuteCommand {|contextId, blockListString, headId, cmdId|
		(lc.runtime.contexts.includesKey(contextId).not && lc.options[\guiExecuteOnlyInitializedContexts]).if {
			"Received command for uninitialized context: %".format(contextId).warn;
			^nil;
		};
		lc.runtime.contexts[contextId].execute(blockListString.split($;), headId: headId, cmdId: cmdId);
	}

	prOnContextDataUpdate {|contextId, data|
		lc.runtime.contexts.includesKey(contextId).not {
			// fail silently
			^nil;
		};
		lc.runtime.contexts[contextId].updateData(data);
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
			^LCBlockSlotRef(spec, startPosition, id, options);
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

	*decodeOSCValues {|x|
		x.size.do {|i|
			var type = x[i][0];
			(type == $f).if {
				x[i] = x[i][1..].asFloat;
			};
			(type == $i).if {
				x[i] = x[i][1..].asInteger;
			};
			(type == $s).if {
				x[i] = x[i][1..];
			};
			(type == $b).if {
				x[i] = x[i][1] == $t;
			};
		};
		^x;
	}
}

LCBlockSlotRef {
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