// I forgot how to do proper inheritance in SC ...

LCAudioChain {
	var <mixer;
	var <group;
	var <parentGroup;
	var <fxGroup;
	var <bus;

	var <gainNode;
	var <gain = 1.0;

	var <fxNodes;

	prBaseInit {|mixer_, parentGroup_|
		mixer = mixer_;
		parentGroup = parentGroup_;
		fxNodes = ();
	}

	prInstantiateBaseNodes {
		bus = Bus.audio(mixer.server, mixer.numChannels);
		group = Group(parentGroup, \addToHead);
		fxGroup = Group(group, \addToHead);

		gainNode = Synth(\lcam_gain, [\gain, 1.0, \bus, bus], group, \addToTail);
	}

	prBaseClear {
		bus.free;
		group.free;
		fxNodes = ();
	}

	// location is \head or \tail
	addFxNode {|synthDef, args, addAction=\tail, id|
		var node;
		args = args ? ();

		id.isNil.not.if {
			fxNodes[id].isNil.not.if {
				fxNodes[id].free;
				fxNodes[id] = nil;
			};
		};

		args[\bus] = bus;
		((addAction == \head) || (addAction == \addToHead)).if ({
			addAction = \addToHead;
		}, {
			addAction = \addToTail;
		});

		node = Synth(synthDef, args.getPairs, fxGroup, addAction);

		id.isNil.not.if {
			fxNodes[id] = node;
		}
		^node;
	}

	gain_ {|value|
		gainNode.set(\gain, value);
		gain = value;
	}
}


LCAudioMixer : LCAudioChain {
	var <duckNode;
	var <delayNode;
	var <outputNode;

	var <delay = 0.0;

	var <numChannels;
	var <server;
	var <outputMode;

	var ctxChains;
	var cmdChains;

	classvar <channelAzimuths;

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

		this.prBaseInit(this, server.defaultGroup);
		this.prInstantiateNodes;
	}

	prInstantiateNodes {
		server.sync;

		this.prInstantiateBaseNodes;

		duckNode = Synth(\lcam_gain, [\bus, bus, \gain, 1.0, \lag, 3], group, \addToTail);
		delayNode = Synth(\lcam_delay, [\bus, bus, \delay, delay], group, \addToTail);

		(outputMode == \splay).if {
			outputNode = Synth(\lcam_splay, [\bus, bus, \out, 0], group, \addToTail);
		};

		(outputMode == \binaural).if {
			outputNode = Synth(\lcam_binaural, [\bus, bus, \out, 0], group, \addToTail);
		};

		// kind of the catch-all case
		(outputNode == nil).if {
			outputNode = Synth(\lcam_send, [\bus, bus, \out, 0], group, \addToTail);
		};
	}

	*buildSynthDefs {
		var numChannels = LifeCodes.instance.options[\numAudioChannels];
		channelAzimuths = LifeCodes.instance.options[\speakerPositions].collect {|position|
			(position.y).atan2(position.x)
		};

		// TODO: Calculate channel Azimuths

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

		(LifeCodes.instance.options[\audioOutputMode] == \binaural).if {
			(Class.allClasses.collect {|class| class.asSymbol}).includes(\Atk).if({
				var decoder = FoaDecoderKernel.newCIPIC;
				var encoder = FoaEncoderMatrix.newOmni;
				// bit stupid but most likely needed
				1.wait;

				SynthDef(\lcam_binaural, {|bus=0, out=0, listenerAzimuth=0|
					var sig = In.ar(bus, numChannels);
					var ambi = Silent.ar(4);

					numChannels.do {|i|
						var channel = FoaEncode.ar(sig[i] * 0.5, encoder);
						ambi = ambi + FoaPush.ar(channel, pi/2, LCAudioMixer.channelAzimuths[i], LifeCodes.instance.options[\binauralElevation]);
						// TODO: should the signal be focues or are plane waves OK?
					};

					ambi = FoaRotate.ar(ambi, listenerAzimuth);

					Out.ar(out, FoaDecode.ar(ambi, decoder));
				}).add;
			}, {
				"Ambisonics Toolkit is not installed - reverting to audioOutputMode: \splay".warn;
				LifeCodes.instance.options[\audioOutputMode] = \splay;
			});
		}
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

	clear {
		group.free;
	}
}

LCContextAudioChain : LCAudioChain {
	var <id;
	var <sendNode;

	*new {|id, mixer|
		^super.new.init(id, mixer);
	}

	init {|id_, mixer|
		id = id_;
		this.prBaseInit(mixer, mixer.group);
		this.prInstantiateBaseNodes;
		sendNode = Synth(\lcam_send, [\bus, bus, \out, mixer.bus], group, \addToTail);
	}

	clear {
		this.prBaseClear;
	}
}

LCCommandAudioChain : LCAudioChain {
	var <id;
	var <ctxChain;
	var <fadeNode;
	var <sendNode;

	var <audioTail = 3.0;
	var <>fadeOutTime = 3.0;

	var dismissed = false;

	*new {|id, ctxChain, mixer|
		^super.new.init(id, ctxChain, mixer);
	}

	init {|id_, ctxChain_, mixer|
		id = id_;
		ctxChain = ctxChain_;
		this.prBaseInit(mixer, ctxChain.group);
		this.prInstantiateBaseNodes;
		sendNode = Synth(\lcam_send, [\bus, bus, \out, ctxChain.bus], group, \addToTail);

		// TODO: FADE NODE
	}

	clear {
		this.prBaseClear;
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
