// Another family is defined (\universal_audio) with
// one block that adds a ring modulator effect.

LCFamilyDef(\universal_audio, \meta, {|domain, l|

	// With 'extends' we make blocks of this family
	// available to other families. Families can either
	// be extended explicitly via an id or, like here,
	// all families that are of a certain type can be extended.

	domain.define((
		extends: [\type_pattern, \type_audio],
		color: Color.gray,
	));

	// we define the ring block

	domain.defineBlock(\ring, (
		type: \modifier,
		primary: true,

		// the ring block has a parameter with the id \freq
		// which is a number (float) with a default value of 440

		parameters: [
			(id: \freq, type: \number, default: 440)
		]
	));
});