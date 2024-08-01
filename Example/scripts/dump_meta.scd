LCFamilyDef(\dump, \meta, {|domain, lc|
	domain.define((
		color: Color.green,
		quant: 2
	));

	domain.defineBlock(\dump, (
		primary: true,
		type: \subject
	));

	domain.defineBlock(\print, (
		type: \action,
		primary: true
	));
});