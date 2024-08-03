LCFamilyDef(\universal_audio, \music, {|domain, l|

	domain.define((
		// on_init is a good place to load SynthDefs.
		on_init: {
			// Audio effects only need to adhere to 3 rules:

			// 1) They need a bus argument which is an audio bus
			// from which they both read and then write
			SynthDef(\ring, {|bus=0, freq=440|

				// 2) They should always adhere to the number of channels
				// that is specified in the options (or can be retrieved
				// from the mixer object like here
				var sig = In.ar(bus, l.mixer.numChannels);

				sig = sig * SinOsc.ar(freq);

				// 3) Use ReplaceOut instead of Out as all audio operations
				// are done in sequence on the same Bus.
				ReplaceOut.ar(bus, sig);

			}).add;
		}
	));

	// When executing the block we add an audio effect node to the command
	// audio chain. We retrieve the freq argument of the block via block.args[\freq].

	// Adding audio effects to the command audio chain is nifty as they are discarded
	// with each new command, so one does not need to keep track of them (the system
	// will get rid of them itself).

	domain.defineBlock(\ring, (
		on_execute: {|block, cmd, ctx, family|
			cmd.audioChain.addFxNode(\ring, (freq: block.args[\freq]));
		}
	));
});