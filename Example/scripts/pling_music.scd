// Defines how \pling should sound (the music domain of pling)

LCFamilyDef(\pling, \music, {|domain, l|

	// Within the domain we define a life cycle function on_init
	// that is called after the server is booted. This is a good
	// place to add SynthDefs!

	domain.define((
		on_init: {
			SynthDef(\pling, {|out=0, freq=440, amp=0.1|
				var sig = SinOsc.ar(freq);
				var env = EnvGen.ar(Env.perc(0.001, 0.5), doneAction: 2);
				Out.ar(out, sig * env * amp);
			}).add;
		}
	));

	// In pattern type families we often operate on a Pbind. cmd.pattern
	// Is the current state of the Pbind and .extend will replace keys
	// of the current pattern chain with keys of a new Pbind.

	// this is a long form - we define the block using a table and
	// then defining the on_execute function.
	domain.defineBlock(\pling, (
		on_execute: {|block, cmd, context, family|
			cmd.pattern.extend(Pbind(
				\instrument, \pling,
				\dur, Pseq([0.5], inf)
			));
		}
	));

	// this is a shorter form - instead of a function we can just define
	// a block by a Pbind (it will automatically be assumed to be a
	// on_execute function that extends the cmd.pattern with this Pbind
	domain.defineBlock(\cycleSpeakers, Pbind(
		\channel, Pseries(0, 1, inf) % lc.options[\numAudioChannels]
	));
});
