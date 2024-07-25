LCFamilyDef(\bowl, \meta, {|domain, lc|
	domain.define((
		type: \pattern,
		color: Color.blue,
	));

	domain.defineBlock(\bowl, (
		type: \subject,
		primary: true,
		on_execute: {|block, cmd, ctx, family|
			"bowl subject".postln;
		}
	));

	domain.defineBlock(\play, (
		type: \action,
		on_execute: {|block, cmd, ctx, family|
			"bowl play".postln;
		}
	));
});