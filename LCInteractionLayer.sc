LCInteractionLayer {
	*new {|lc|
		^super.newCopyArgs().init;
	}

	init {

	}

	sendCommandFeedback {|cmd|
		"command feedback".postln;
	}
}