LCSpec(\pling, \music, {|spec|
	spec.define((
		\on_init: {
			SynthDef(\pling, {|out=0, freq=440, amp=0.1|
				var sig = SinOsc.ar(freq);
				var env = EnvGen.ar(Env.perc(0.01, 0.5), doneAction: 2);
				Out.ar(out, sig * env * amp);
			}).add;
		}
	));

	spec.defineBlock(\pling, Pbind(
		\instrument, \pling,
		\dur, 0.5
	));

	spec.defineBlock(\cycleSpeakers, Pbind(
		\channel, Pseries(0, 1, inf) % LifeCodes.instance.options[\numAudioChannels]
	));
});