LCSpec(\universal_pattern, \meta, {|spec|
	spec.define((
		extends: [\type_pattern],
		color: Color.gray,
	));

	spec.defineBlock(\play, (
		type: \action,
		primary: true,

		on_execute: {|block, cmd, ctx, family, spec|
			cmd.doPerform = true;
		}
	));

	spec.defineBlock(\mute, (
		type: \modifier,
		primary: true,

		on_execute: {|block, cmd, ctx, family, spec|
			cmd.pattern.extend(Pbind(\amp, 0.0));
		}
	));

	spec.defineBlock(\faster, (
		type: \modifier,
		primary: true,

		\parameters: [
			(id: \speed, type: \number, default: 2)
		],

		on_execute: {|block, cmd, ctx, family, spec|
			cmd.pattern.extend(Pbind(\dur, cmd.pattern[\dur] * block.args[\speed]));
		}
	));
});