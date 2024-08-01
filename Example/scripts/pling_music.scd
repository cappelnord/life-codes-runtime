LCFamilyDef(\pling, \music, {|domain, lc|
	domain.define((
		on_init: {
			SynthDef(\pling, {|out=0, freq=440, amp=0.1|
				var sig = SinOsc.ar(freq);
				var env = EnvGen.ar(Env.perc(0.001, 0.5), doneAction: 2);
				Out.ar(out, sig * env * amp);
			}).add;
		}
	));

	domain.defineBlock(\pling, Pbind(
		\instrument, \pling,
		\dur, Pseq([0.5], inf)
	));

	domain.defineBlock(\cycleSpeakers, Pbind(
		\channel, Pseries(0, 1, inf) % lc.options[\numAudioChannels]
	));
});
