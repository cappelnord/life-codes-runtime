// We can also put definitions for multiple families in one file; no problem!
// \meta also includes functions? Also no problem!


LCFamilyDef(\base, \meta, {|domain, l|
	domain.define((
		color: Color.red
	));

	domain.defineBlock(\base, (
		primary: true,
		type: \subject,

		on_execute: {|block, cmd, ctx, family|
			"base subject".postln;
		}
	));

	domain.defineBlock(\act, (
		primary: true,
		type: \action,

		on_execute: {|block, cmd, ctx, family|
			"base action: act".postln;
			cmd.doPerform = true;
		}
	));
});

// We can extend our base family with another block through another family
LCFamilyDef(\extension, \meta, {|domain, l|
	domain.define((
		color: Color.red,
		extends: [\base]
    ));

	domain.defineBlock(\mod, (
		primary: true,
		type: \modifier,

		on_execute: {|block, cmd, ctx, family|
			"extension modifier: mod".postln;
		}
	));
});

// We can also define a new family that will inherit everything from another family

LCFamilyDef(\inheritance, \meta, {|domain, l|
	domain.define((
		color: Color.red,
		inheritsFrom: [\base]
	));

	domain.defineBlock(\inheritance, (
		primary: true,
		type: \subject,

		on_execute: {|block, cmd, ctx, family|
			"inheritance subject: inheritance".postln;
		}
	));

	// We can also add functionality to blocks that are not directly part of our
	// family. These functions will be executed after the original functions.

	// This is a powerful feature to add functions to very generic blocks such as
	// \play.

	domain.defineBlock(\act, {|block, cmd, ctx, family|
		"function added by inheritance to act".postln;
	});

	// of course we can not only add on_execute lifecycle functions

	domain.defineBlock(\mod, (
		on_execute: {|block, cmd, ctx, family|
			"function added by inheritance to mod".postln;
		},
		on_enter: {|block, cmd, ctx, family|
			"on_enter added by inheritance to mod".postln;
		},
		on_leave: {|block, cmd, ctx, family|
			"on_leave added by inheritance to mod".postln;
		}
	));
});