LCSpec(\bowl, \meta, {|spec|
	spec.define((
		\type: \pattern,
		\color: Color.blue,
	));

	spec.defineBlock(\bowl, (
		\type: \subject,
		\primary: true,
		\on_execute: {|block, cmd, ctx, family, spec|
			"bowl subject".postln;
		}
	));

	spec.defineBlock(\play, (
		\type: \action,
		\on_execute: {|block, cmd, ctx, family, spec|
			"bowl play".postln;
		}
	));
});