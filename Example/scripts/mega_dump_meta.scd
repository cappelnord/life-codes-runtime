LCSpec(\mega_dump, \meta, {|spec|
	spec.define((
		\inheritsFrom: [\dump],
		\color: Color.red,
	));

	spec.defineBlock(\mega_dump, (
		\type: \subject,
		\primary: true
	));

	spec.defineBlock(\print, (
		\type: \action,
		\on_execute: {|block, cmd, ctx, family, spec|
			"print addon from mega dump".postln;
		}
	));

	spec.defineBlock(\mega, (
		\type: \modifier,
		\primary: true,

		\on_execute: {|block, cmd, ctx, family, spec|
			"mega modifier".postln;
		}
	));
});