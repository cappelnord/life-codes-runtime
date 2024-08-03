// Defines the meta data of the pling family, a simple pattern.

LCFamilyDef(\pling, \meta, {|domain, l|

	// In the \meta domain we define meta data for the family:

	domain.define((
		// -> perform = play the Pbind
		type: \pattern,
		// -> color of the blocks
		color: Color.blue,
		// -> quantization
		quant: 4
	));

	// We also define meta data for the blocks:
	// A subject \pling and a modifier \cycleSpeakers

	// Both blocks primarily belong to this family
	// (hence: primary: true)

	domain.defineBlock(\pling, (
		type: \subject,
		primary: true
	));

	domain.defineBlock(\cycleSpeakers, (
		type: \modifier,
		primary: true
	));
});