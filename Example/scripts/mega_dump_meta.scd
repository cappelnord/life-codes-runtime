LCFamilyDef(\mega_dump, \meta, {|domain, lc|
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
		on_execute: {|block, cmd, ctx, family|
			"print addon from mega dump".postln;
		}
	));

	domain.defineBlock(\mega, (
		type: \modifier,
		primary: true,

		on_execute: {|block, cmd, ctx, family|
			"mega modifier".postln;
		}
	));
});