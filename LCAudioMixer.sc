// I forgot how to do proper inheritance in SC ...

LCAudioChain {
	var <mixer;
	var <group;
	var <parentGroup;
	var <fxGroup;
	var <bus;

	var <gainNode;
	var <gain;

	var <fxNodes;

	var <generator;

	prBaseInit {|mixer_, parentGroup_|
		mixer = mixer_;
		parentGroup = parentGroup_;
		fxNodes = ();
		gain = this.defaultGain;
	}

	prInstantiateBaseNodes {
		bus = Bus.audio(mixer.server, mixer.numChannels);
		group = Group(parentGroup, \addToHead);
		fxGroup = Group(group, \addToHead);

		gainNode = Synth(\lcam_gain, [\gain, gain, \bus, bus], group, \addToTail);
	}

	prBaseClear {
		bus.free;
		group.free();
		fxNodes = ();
	}

	prSetGenerator {|node|
		generator.isNil.not.if {
			"Generator node got replaced!".warn;
		};
		generator = node;
	}

	playEvent {|event, isGenerator=true|
		var node;
		event[\out] = bus.index + (event[\channel] ? 0);
		event[\group] = group;
		event[\addAction] = \addToHead;
		event.play;
		node = Synth.basicNew(event[\instrument], event[\server], event[\id]);
		isGenerator.if {
			this.prSetGenerator(node);
		};
		^node;
	}

	playSynth {|synthDef, args, isGenerator=true|
		var node;
		args = args ? ();
		args[\out] = bus.index + (args[\channel] ? 0);
		node = Synth(synthDef, args.getPairs, group, \addToHead);
		isGenerator.if {
			this.prSetGenerator(node);
		};
		^node;
	}

	// location is \head or \tail
	prAddNode {|synthDef, args, id, addAction, group, collection|
		var node;
		args = args ? ();

		id.isNil.not.if {
			collection[id].isNil.not.if {
				collection[id].free;
				collection[id] = nil;
			};
		};

		args[\bus] = bus;
		((addAction == \head) || (addAction == \addToHead)).if ({
			addAction = \addToHead;
		}, {
			addAction = \addToTail;
		});

		node = Synth(synthDef, args.getPairs, group, addAction);

		id.isNil.not.if {
			collection[id] = node;
		}
		^node;
	}

	addFxNode {|synthDef, args, id, addAction=\tail|
		^this.prAddNode(synthDef, args, id, addAction, fxGroup, fxNodes);
	}

	freeFxNode {|id|
		fxNodes[id].free;
		fxNodes.removeAt(id);
	}

	gain_ {|value, lag=0.5, mode=\absolute|
		(mode == \relative).if {
			value = gain * value;
		};
		gain = value;
		gainNode.set(\lag, lag, \gain, gain);
	}

	defaultGain {
		^1.0;
	}
}


LCAudioMixer : LCAudioChain {
	var <duckNode;
	var <delayNode;
	var <outputNode;

	var clipDetectNode;

	var <delay = 0.0;

	var <numChannels;
	var <server;
	var <outputMode;

	var <submixGroup;

	var ctxChains;
	var cmdChains;
	var submixChains;

	var sentinelTable;
	var audioDataUpdates;

	classvar <channelAzimuths;

	*new {
		^super.newCopyArgs().init;
	}

	prInitOSCListeners {
		var receivePort = LifeCodes.instance.options[\guiReceivePort];

		(outputMode == \binaural).if {
			OSCdef(\lcHeadRotation, {|msg, time, addr, recvPort|
				this.updateHeadRotation(msg[1]);
			}, '/lc/headRotation', recvPort: receivePort);
		};

		LifeCodes.instance.options[\detectAudioClipping].if {
			OSCdef(\lcClipDetect, {|msg, time, addr, recvPort|
				"Audio peak went above -2 dB on output channel: % !".format(msg[3].asInteger).warn;
			}, '/lc/audio/clipDetected');
		};

		OSCdef(\lcSentinel, {|msg, time, addr, recvPort|
			var sentinelId = msg[3].asInteger;
			sentinelTable[sentinelId].value;
			sentinelTable[sentinelId] = nil;
		}, '/lc/audio/sentinel');

		OSCdef(\lcAudioCtxDataUpdate, {|msg, time, addr, recvPort|
			var dataUpdateId = msg[3].asInteger;
			var values = msg[4..];
			// msg.postln;
			audioDataUpdates[dataUpdateId].value(values);
		}, '/lc/audio/updateCtxData');
	}

	init {
		numChannels = LifeCodes.instance.options[\numAudioChannels];
		server = LifeCodes.instance.options[\server];
		outputMode = LifeCodes.instance.options[\audioOutputMode];
		delay = LifeCodes.instance.options[\audioDelay];
		ctxChains = ();
		cmdChains = ();
		submixChains = ();
		sentinelTable = ();
		audioDataUpdates = ();

		this.prBaseInit(this, server.defaultGroup);
		this.prInstantiateNodes;
		this.prInitOSCListeners;

		submixGroup = Group(fxGroup, \addBefore);
	}

	registerAudioDataUpdate {|ctx, keys|
		var id = 5000000.rand;
		audioDataUpdates[id] = {|values|
			var data = ();
			values.do {|value, i|
				data[keys[i]] = value;
			};
			ctx.updateData(data);
		};
		^id;
	}

	unregisterAudioDataUpdate {|id|
		audioDataUpdates[id] = nil;
	}

	prInstantiateNodes {
		server.sync;

		this.prInstantiateBaseNodes;

		LifeCodes.instance.options[\detectAudioClipping].if {
			clipDetectNode = Synth(\lcam_clip_detect, [\bus, bus], this.gainNode, \addBefore);
		};

		duckNode = Synth(\lcam_gain, [\bus, bus, \gain, 1.0, \fadeTime, 3], group, \addToTail);
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

		SynthDef(\lcam_fade, {|bus=0, gain=1.0, fadeTime=3|
			var sig = In.ar(bus, numChannels);
			gain = VarLag.kr(gain, fadeTime);
			ReplaceOut.ar(bus, sig * gain);
		}).add;

		SynthDef(\lcam_gain, {|bus=0, gain=1.0, lag=0.5|
			var sig = In.ar(bus, numChannels);
			gain = Lag2.kr(gain, lag);
			ReplaceOut.ar(bus, sig * gain);
		}).add;

		SynthDef(\lcam_send, {|bus=0, out=0, gain=1|
			var sig = In.ar(bus, numChannels);
			Out.ar(out, sig * gain);
		}).add;

		SynthDef(\lcam_splay, {|bus=0, out=0|
			var sig = In.ar(bus, numChannels);
			Out.ar(out, Splay.ar(sig));
		}).add;

		SynthDef(\lcam_delay, {|bus=0, delay=0|
			var sig = In.ar(bus, numChannels);
			Out.ar(bus, DelayC.ar(sig, 2.0, delay));
		}).add;

		SynthDef(\lcam_clip_detect, {|bus=0|
			var sig = In.ar(bus, numChannels);
			numChannels.do {|i|
				var channel = sig[i];
				var clipTrigger = Trig.ar(sig.abs > -2.dbamp, 0.25);
				SendReply.ar(clipTrigger, '/lc/audio/clipDetected', i);
			};
		}).add;

		SynthDef(\lcam_sentinel, {|bus=0, time=1, sentinelId|
			var sig = In.ar(bus, numChannels);
			var sum = DC.ar(0);
			var trigger;
			sig = LeakDC.ar(sig);
			numChannels.do {|i|
				sum = sum + sig[i].abs;
			};
			trigger = DetectSilence.ar(sum + Impulse.ar(0), time: time);
			SendReply.ar(trigger, '/lc/audio/sentinel', [sentinelId]);
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
						var channel = FoaEncode.ar(sig[i] * 0.25, encoder); // 0.25 a bit arbritrary chosen
						ambi = ambi + FoaTransform.ar(channel, 'push', 0.5*pi, LCAudioMixer.channelAzimuths[i]);
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

	getSubmixChain {|id|
		submixChains[id].isNil.if {
			submixChains[id] = LCSubmixAudioChain(id, this);
		};
		^submixChains[id];
	}

	getCommandChain {|id, ctxId|
		cmdChains[id].isNil.if {
			cmdChains[id] = LCCommandAudioChain(id, this.getContextChain(ctxId), this);
		};
		^cmdChains[id];
	}

	getContextChain {|id|
		ctxChains[id].isNil.if {
			ctxChains[id] = LCContextAudioChain(id, this);
		};
		^ctxChains[id];
	}

	// probably best not to call these clear methods manually

	clearSubmixChain {|id|
		submixChains[id].isNil.not.if {
			submixChains[id].clear;
			submixChains.removeAt(id);
		};
	}

	clearContextChain {|id|
		ctxChains[id].isNil.not.if {
			ctxChains[id].clear;
			ctxChains.removeAt(id);
		};
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

	updateHeadRotation {|azimuth|
		var listenerAzimuth = azimuth.degrad;
		// listenerAzimuth.postln;
		outputNode.set(\listenerAzimuth, listenerAzimuth);
	}

	setInactivityAttenuation {|active|
		active.if ({
			duckNode.set(\lag, 3, \gain, 1);
		}, {
			duckNode.set(\lag, 10, \gain, LifeCodes.instance.options[\inactivityAudioAttenuation]);
		});
	}

	startSentinel {|chain, function|
		var time = (chain.class == LCCommandAudioChain).if(3, 5);
		var id = 5000000.rand;
		Synth(\lcam_sentinel, [\bus, chain.bus, \time, time, \sentinelId, id], chain.group, \addToTail);
		sentinelTable[id] = function;
	}
}

LCFadeableAudioChain : LCAudioChain {
	var <id;
	var <sendNode;
	var <fadeNode;
	var <postFaderGroup;
	var <postFaderNodes;

	var dismissed = false;

	addPostFaderNode {|synthDef, args, id, addAction=\tail|
		^this.prAddNode(synthDef, args, id, addAction, postFaderGroup, postFaderNodes);
	}

	prInstantiateFaderNodes {|targetBus|
		fadeNode = Synth(\lcam_fade, [\bus, bus], group, \addToTail);
		sendNode = Synth(\lcam_send, [\bus, bus, \out, targetBus], group, \addToTail);
		postFaderGroup = Group(sendNode, \addAfter);

		postFaderNodes = ();
	}

	fadeOut {|fadeTime|
		fadeNode.set(\fadeTime, fadeTime, \gain, 0);
	}

	fadeIn {|fadeTime|
		fadeNode.set(\fadeTime, fadeTime, \gain, 1);
	}

	dismiss {|fadeTime|
		fadeTime.isNil.not.if {
			this.fadeOut(fadeTime);
		};

		dismissed.not.if {
			mixer.startSentinel(this, {
				this.mixerClearChain(id);
				"Cleared %: %".format(this.class, id).postln;
			});
		};
		dismissed = true;
	}

	clear {
		this.prBaseClear;
	}

	routeAudio {|chain|
		sendNode.set(\out, chain.bus);
	}

	mixerClearChain {|chain|

	}
}

// this is also used for submixes
LCSubmixAudioChain : LCFadeableAudioChain {

	*new {|id, mixer|
		^super.new.init(id, mixer);
	}

	init {|id_, mixer|
		id = id_;
		this.prBaseInit(mixer, mixer.submixGroup);
		this.prInstantiateBaseNodes;
		this.prInstantiateFaderNodes(mixer.bus);
	}

	mixerClearChain {|id|
		mixer.clearSubmixChain(id);
	}
}

LCContextAudioChain : LCFadeableAudioChain {
	*new {|id, mixer|
		^super.new.initContextChain(id, mixer);
	}

	initContextChain {|id_, mixer|
		id = id_;
		this.prBaseInit(mixer, mixer.group);
		this.prInstantiateBaseNodes;
		this.prInstantiateFaderNodes(mixer.bus);
	}

	defaultGain {
		^LifeCodes.instance.options[\defaultContextAudioGain];
	}

	mixerClearChain {|id|
		mixer.clearContextChain(id);
	}
}

LCCommandAudioChain : LCFadeableAudioChain {
	var <id;
	var <ctxChain;

	*new {|id, ctxChain, mixer|
		^super.new.initCommandChain(id, ctxChain, mixer);
	}

	initCommandChain {|id_, ctxChain_, mixer|
		id = id_;
		ctxChain = ctxChain_;
		this.prBaseInit(mixer, ctxChain.group);
		this.prInstantiateBaseNodes;
		this.prInstantiateFaderNodes(ctxChain.bus);
	}

	mixerClearChain {|id|
		mixer.clearCommandChain(id);
	}
}
