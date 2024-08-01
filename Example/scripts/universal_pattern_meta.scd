// Look at universal_audio_meta.scd for explanations.

// We define here blocks that work with all families of the pattern type.
// One is special though!

LCFamilyDef(\universal_pattern, \meta, {|domain, lc|
	domain.define((
		extends: [\type_pattern],
		color: Color.gray,
	));

	// This is the play action for patterns.
	domain.defineBlock(\play, (
		type: \action,
		primary: true,

		// It doesn't do anything else than to set doPerform to true
		// on this command. This will automatically play the pattern.
		// on_perform and on_perform_once will also only be executed
		// if doPerform is set to true at some point in the command chain.

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