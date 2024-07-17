LCAudioMixer {
	var <group;
	var <fxGroup;
	var <bus;

	var <gainNode;
	var <duckNode;
	var <delayNode;
	var <outputNode;

	var <gain = 1.0;
	var <delay = 0.0;

	var <numChannels;
	var <server;
	var <outputMode;

	var ctxChains;
	var cmdChains;

	*new {
		^super.newCopyArgs().init;
	}

	init {
		numChannels = LifeCodes.instance.options[\numAudioChannels];
		server = LifeCodes.instance.options[\server];
		outputMode = LifeCodes.instance.options[\audioOutputMode];
		delay = LifeCodes.instance.options[\audioDelay];
		ctxChains = ();
		cmdChains = ();
		this.prInstantiateNodes;
	}

	prInstantiateNodes {
		server.sync;

		bus = Bus.audio(server, numChannels);
		group = Group();
		fxGroup = Group(group, \addToHead);
		gainNode = Synth(\lcam_gain, [\bus, bus, \gain, gain], group, \addToTail);
		duckNode = Synth(\lcam_gain, [\bus, bus, \gain, 1.0, \lag, 3], group, \addToTail);
		delayNode = Synth(\lcam_delay, [\bus, bus, \delay, delay], group, \addToTail);

		(outputMode == \splay).if {
			outputNode = Synth(\lcam_splay, [\bus, bus, \out, 0], group, \addToTail);
		};

		// kind of the catch-all case
		(outputNode == nil).if {
			outputNode = Synth(\lcam_send, [\bus, bus, \out, 0], group, \addToTail);
		};
	}

	*buildSynthDefs {
		var numChannels = LifeCodes.instance.options[\numAudioChannels];

		SynthDef(\lcam_gain, {|bus=0, gain=1.0, lag=0.5|
			var sig = In.ar(bus, numChannels);
			gain = Lag2.kr(gain, lag);
			ReplaceOut.ar(bus, sig * gain);
		}).add;

		SynthDef(\lcam_send, {|bus=0, out=0|
			var sig = In.ar(bus, numChannels);
			Out.ar(out, sig);
		}).add;

		SynthDef(\lcam_splay, {|bus=0, out=0|
			var sig = In.ar(bus, numChannels);
			Out.ar(out, Splay.ar(sig));
		}).add;

		SynthDef(\lcam_delay, {|bus=0, delay=0|
			var sig = In.ar(bus, numChannels);
			Out.ar(bus, DelayC.ar(sig, 2.0, delay));
		}).add;
	}

	gain_ {|value|
		gainNode.set(\gain, value);
		gain = value;
	}

	getContextChain {|id|
		ctxChains[id].isNil.if {
			ctxChains[id] = LCContextAudioChain(id, this);
		};
		^ctxChains[id];
	}

	clearContextChain {|id|
		ctxChains[id].isNil.not.if {
			ctxChains[id].clear;
			ctxChains.removeAt(id);
		};
	}

	getCommandChain {|id, ctxId|
		cmdChains[id].isNil.if {
			cmdChains[id] = LCCommandAudioChain(id, this.getContextChain(ctxId), this);
		};
		^cmdChains[id];
	}

	clearCommandChain {|id|
		cmdChains[id].isNil.not.if {
			cmdChains[id].clear;
			cmdChains.removeAt(id);
		};
	}
}

LCContextAudioChain {
	// TODO: better realize the actual connection to the context and its gain
	var <id;
	var <mixer;

	var <group;
	var <fxGroup;
	var <bus;

	var <gainNode;
	var <sendNode;


	*new {|id, mixer|
		^super.newCopyArgs(id, mixer).init;
	}

	init {
		this.prInstantiateNodes;
	}

	prInstantiateNodes {
		bus = Bus.audio(mixer.server, mixer.numChannels);
		group = Group(mixer.group, \addToHead);
		fxGroup = Group(group, \addToHead);

		gainNode = Synth(\lcam_gain, [\gain, 1.0, \bus, bus], group, \addToTail);
		sendNode = Synth(\lcam_send, [\bus, bus, \out, mixer.bus], group, \addToTail);
	}

	clear {
		bus.free;
		group.free;
	}
}

LCCommandAudioChain {
	var <id;
	var <ctxChain;
	var <mixer;

	var <group;
	var <fxGroup;
	var <bus;

	var <gainNode;
	var <fadeNode;
	var <sendNode;

	var <audioTail = 3.0;
	var <>fadeOutTime = 3.0;

	var dismissed = false;

	*new {|id, ctxChain, mixer|
		^super.newCopyArgs(id, ctxChain, mixer).init;
	}

	init {
		this.prInstantiateNodes;
	}

	prInstantiateNodes {
		bus = Bus.audio(mixer.server, mixer.numChannels);
		group = Group(ctxChain.group, \addToHead);
		fxGroup = Group(group, \addToHead);

		gainNode = Synth(\lcam_gain, [\gain, 1.0, \bus, bus], group, \addToTail);
		sendNode = Synth(\lcam_send, [\bus, bus, \out, ctxChain.bus], group, \addToTail);
		// TODO: FADE NODE
	}

	clear {
		bus.free;
		group.free;
	}

	dismiss {
		dismissed.not.if {
			LifeCodes.instance.steadyClock.play(Routine({
				audioTail.wait;
				// TODO: FADE OUT
				fadeOutTime.wait;
				mixer.clearCommandChain(id);
			}));
		};
		dismissed = true;
	}

	setMinAudioTail {|value|
		(value > audioTail).if {
			audioTail = value;
		};
	}
}
