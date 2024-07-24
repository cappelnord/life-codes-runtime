LCFamilyDef(\mega_dump, \meta, {|domain|
	domain.define((
		inheritsFrom: [\dump],
		color: Color.red,
	));

	domain.defineBlock(\mega_dump, (
		type: \subject,
		primary: true
	));

	domain.defineBlock(\print, (
		type: \action,
		on_execute: {|block, cmd, ctx, family, domain|
			"print addon from mega dump".postln;
		}
	));

	domain.defineBlock(\mega, (
		type: \modifier,
		primary: true,

		on_execute: {|block, cmd, ctx, family, domain|
			"mega modifier".postln;
		}
	));
});