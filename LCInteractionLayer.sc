LCInteractionLayer {
	var lc;
	var net;

	*new {|lc|
		^super.newCopyArgs(lc).init;
	}

	init {
		net = lc.options[\interactionHost];
		net.sendMsg("/lc/blocks/loadSpecs", lc.options[\specsExportPath]);
	}

	sendCommandFeedback {|cmd|
		["/lc/blocks/commandFeedback", cmd.headId, cmd.id].postln;
		net.sndMsg("/lc/blocks/commandFeedback", cmd.headId, cmd.id);
	}

	addBlockSlot {|spec, startPosition, id, options|
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

		object = (
			\spec: spec,
			\pos: startPosition,
			\id: id ? LifeCodes.randomId,
			\options: options ? ()
		);

		LifeCodes.instance.runtime.blockSpecs[spec.asSymbol].isNil.not.if({
			net.sendMsg("/lc/blocks/addSlot", object.jsonString);
			^LCInteractionSlotRef(spec, startPosition, id, options);
		}, {
			"Could not find spec identifier: %".format(spec).error;
			^nil;
		});
	}
}

LCInteractionSlotRef {
	var <id;
	var <spec;
	var <startPosition;
	var <options;

	*new {|spec, startPosition, id, options|
		^super.newCopyArgs(spec, startPosition, id, options).init;
	}

	init {

	}
}