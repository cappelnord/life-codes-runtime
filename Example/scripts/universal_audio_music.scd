LCSpec(\universal_audio, \music, {|spec, lc|
	spec.define((
		\on_init: {
			SynthDef(\ring, {|bus=0, freq=440|
				var sig = In.ar(bus, lc.options[\numAudioChannels]);
				sig = sig * SinOsc.ar(freq);
				ReplaceOut.ar(bus, sig);
			}).add;
		}
	));

	spec.defineBlock(\ring, (
		\on_execute: {|block, cmd, ctx, family|
			cmd.audioChain.addFxNode(\ring, (freq: block.args[\freq]));
		}
	));
});