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
		"command feedback".postln;
	}
}