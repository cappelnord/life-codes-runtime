LCFamilyDef(\universal_pattern, \meta, {|domain|
	domain.define((
		extends: [\type_pattern],
		color: Color.gray,
	));

	domain.defineBlock(\play, (
		type: \action,
		primary: true,

		on_execute: {|block, cmd, ctx, family, domain|
			cmd.doPerform = true;
		}
	));

	domain.defineBlock(\mute, (
		type: \modifier,
		primary: true,

		on_execute: {|block, cmd, ctx, family, domain|
			cmd.pattern.extend(Pbind(\amp, 0.0));
		}
	));

	domain.defineBlock(\faster, (
		type: \modifier,
		primary: true,

		parameters: [
			(id: \speed, type: \number, default: 2)
		],

		on_execute: {|block, cmd, ctx, family, domain|
			cmd.pattern.extend(Pbind(\dur, cmd.pattern[\dur] / block.args[\speed]));
		}
	));
});