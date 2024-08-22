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

	prInitOSCListeners {
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

		OSCdef(\lcRequestSpecs, {|msg, time, addr, recvPort|
			"Block Specs requested via OSC ...".postln;
			this.prSendSpecs;
		}, '/lc/requestSpecs', recvPort: receivePort);

		OSCdef(\lcSceneManagerRush, {|msg, time, addr, recvPort|
			"Received Scene Rush from OSC".postln;
			lc.sceneManager.rush;
		}, '/lc/sceneManager/rush', recvPort: receivePort);

		OSCdef(\lcUsersActive, {|msg, time, addr, recvPort|
			lc.mixer.setInactivityAttenuation(msg[1] == 1);

			(msg[1] == 1).if ({
				"Users got active".postln;
			}, {
				"Users are inactive".postln;
			})
		}, '/lc/usersActive', recvPort: receivePort);
	}



	init {
		receivePort = lc.options[\guiReceivePort];
		net = lc.options[\guiHost];
		this.prSendSpecs;
		this.prInitOSCListeners;
		this.prInitData;
	}

	prSendSpecs {
		net.sendMsg("/lc/blocks/loadSpecs", lc.options[\specsExportPath]);
	}

	prOnExecuteCommand {|contextId, blockSourceListString, cmdId|
		(lc.runtime.contexts.includesKey(contextId).not && lc.options[\guiExecuteOnlyInitializedContexts]).if {
			"Received command for uninitialized context: %".format(contextId).warn;
			^nil;
		};
		lc.runtime.contexts[contextId].execute(blockSourceListString.split($;), cmdId: cmdId);
	}

	prOnContextDataUpdate {|contextId, data|
		lc.runtime.contexts[contextId].isNil.not.if {
			lc.runtime.contexts[contextId].updateData(data);
		}
	}

	sendCommandFeedback {|cmd, headBlockId|
		net.sendMsg("/lc/blocks/commandFeedback", headBlockId, cmd.id);
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

	setConnectionHints {|hints, interval=(-1)|
		net.sendMsg("/lc/blocks/hints", (interval: interval, hints: hints).jsonString);
	}

	clearConnectionHints {
		net.sendMsg("/lc/blocks/clearHints");
	}

	triggerConnectionHints {
		net.sendMsg("/lc/blocks/triggerHints");
	}

	addBlockSlot {|spec, startPosition, options|
		var object;
		var id;

		options = options ? ();

		// allow to combine spec with arguments
		(spec.class == Array).if {
			var array = spec;
			spec = array[0];
			options[\args] = array[1..];
	    };

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

		id = options[\id] ? LifeCodes.randomId;
		options.removeAt(\id);

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

			// don't wait if we are in a hurry
			(lc.sceneManager.inHurry.not).if {
				waitFunction.value;
			};
		};
	}

	popRegistry {|id|
		var ret = blockSlotRegistry[id];
		blockSlotRegistry[id] = nil;
		^ret;
	}

	registrySetProperties {|id, options|
		blockSlotRegistry[id].do {|slotRef|
			this.setBlockSlotProperties(slotRef.id, options);
		}
	}

	despawnBlockSlotsFromRegistry {|id, options, waitFunction|
		this.popRegistry(id).do {|slotRef|
			this.despawnBlockSlot(slotRef.id, options);

			// don't wait if we are in a hurry
			(lc.sceneManager.inHurry.not).if {
				waitFunction.value;
			};
		};
	}

	clear {
		this.clearAllBlockSlots;
	}

	cmdPeriod {
		this.clearAllBlockSlots;
	}

	recoverFromCmdPeriod {
		this.prInitOSCListeners;
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