LCFamilyDef(\universal_audio, \music, {|domain, lc|
	domain.define((
		on_init: {
			SynthDef(\ring, {|bus=0, freq=440|
				var sig = In.ar(bus, lc.mixer.numChannels);
				sig = sig * SinOsc.ar(freq);
				ReplaceOut.ar(bus, sig);
			}).add;
		}
	));

	domain.defineBlock(\ring, (
		on_execute: {|block, cmd, ctx, family|
			cmd.audioChain.addFxNode(\ring, (freq: block.args[\freq]));
		}
	));
});