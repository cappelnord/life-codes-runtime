LCFamilyDef(\universal_pattern, \meta, {|domain, lc|
	domain.define((
		extends: [\type_pattern],
		color: Color.gray,
	));

	domain.defineBlock(\play, (
		type: \action,
		primary: true,

		on_execute: {|block, cmd, ctx, family|
			cmd.doPerform = true;
		}
	));

	domain.defineBlock(\mute, (
		type: \modifier,
		primary: true
	));

	domain.defineBlock(\faster, (
		type: \modifier,
		primary: true,

		parameters: [
			(id: \speed, type: \number, default: 2)
		]
	));

	domain.defineBlock(\transpose, (
		type: \modifier,
		primary: true,

		parameters: [
			(id: \interval, type: \number, default: 12)
		]
	));

	domain.defineBlock(\offbeat, (
		type: \modifier,
		primary: true,

		parameters: [
			(id: \beats, type: \number, default: 0.5)
		]
	));
});