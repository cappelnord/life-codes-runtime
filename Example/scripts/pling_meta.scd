LCFamilyDef(\pling, \meta, {|domain, lc|
	domain.define((
		type: \pattern,
		color: Color.blue,
		quant: 4
	));

	domain.defineBlock(\pling, (
		type: \subject,
		primary: true
	));

	domain.defineBlock(\cycleSpeakers, (
		type: \modifier,
		primary: true
	));
});