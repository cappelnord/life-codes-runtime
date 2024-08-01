LCFamilyDef(\universal_pattern, \music, {|domain, lc|

	// the shortest way on how to define a pattern modifier ...
	// ... this only works if the block has no parameters and
	// we don't need any pattern values from the pattern chain
	domain.defineBlock(\mute, Pbind(\amp, 0.0));

	// We can access the current pattern for a key from cmd.pattern
	// using indexing (this is not a base functionality but part of
	// the Pbind class extension). Here we take the old \dur and divide
	// it by the speed argument to make things go faster.

	domain.defineBlock(\faster, (
		on_execute: {|block, cmd, ctx, family|
			cmd.pattern.extend(Pbind(\dur, cmd.pattern[\dur] / block.args[\speed]));
		}
	));

	// We can write things a bit more concise at least:
	// If we provide defineBlock with a function instead of an event
	// it will assign it automatically to on_execute.

	domain.defineBlock(\transpose, {|block, cmd, ctx, family|
		cmd.pattern.extend(Pbind(\ctranspose, block.args[\interval]));
	});

	domain.defineBlock(\offbeat, {|block, cmd, ctx, family|
		cmd.pattern.extend(Pbind(\dur, Pseq([block.args[\beats], cmd.pattern[\dur]], 1)));
	});

});