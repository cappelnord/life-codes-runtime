LCFamilyDef(\panning, \meta, {|domain, lc|
	domain.define((
		extends: [\type_pattern, \type_audio],
		color: Color.gray,
	));

	domain.defineBlock(\xPosPan, (
		type: \modifier,
		primary: true,

		parameters: [
			(id: \width, type: \number, default: 2.0)
		]
	));

	domain.defineBlock(\xPosPan2, (
		type: \modifier,
		primary: true,

		parameters: [
			(id: \spread, type: \number, default: pi/2),
			(id: \width, type: \number, default: 2.0)
		]
	));
});