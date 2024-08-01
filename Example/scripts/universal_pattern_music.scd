LCFamilyDef(\universal_pattern, \music, {|domain, lc|

	domain.defineBlock(\mute, (
		on_execute: {|block, cmd, ctx, family|
			cmd.pattern.extend(Pbind(\amp, 0.0));
		}
	));

	domain.defineBlock(\faster, (
		on_execute: {|block, cmd, ctx, family|
			cmd.pattern.extend(Pbind(\dur, cmd.pattern[\dur] / block.args[\speed]));
		}
	));

	domain.defineBlock(\transpose, (
		on_execute: {|block, cmd, ctx, family|
			cmd.pattern.extend(Pbind(\ctranspose, block.args[\interval]));
		}
	));

	domain.defineBlock(\offbeat, (
		on_execute: {|block, cmd, ctx, family|
			cmd.pattern.extend(Pbind(\dur, Pseq([block.args[\beats], cmd.pattern[\dur]], 1)));
		}
	));

});