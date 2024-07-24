LCFamilyDef(\bowl, \meta, {|domain|
	domain.define((
		type: \pattern,
		color: Color.blue,
	));

	domain.defineBlock(\bowl, (
		type: \subject,
		primary: true,
		on_execute: {|block, cmd, ctx, family, spec|
			"bowl subject".postln;
		}
	));

	domain.defineBlock(\play, (
		type: \action,
		on_execute: {|block, cmd, ctx, family, spec|
			"bowl play".postln;
		}
	));
});