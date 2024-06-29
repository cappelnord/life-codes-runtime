LCSpec(\universal_pattern, \meta, {|spec|
	spec.define((
		\compatibility: [\type_pattern],
		\color: Color.white,
	));

	spec.defineBlock(\mute, (
		\type: \action,

		\on_execute: {|block, cmd, ctx, family, spec|
			"universal_pattern mute".postln;
		}
	));

	nil;
});