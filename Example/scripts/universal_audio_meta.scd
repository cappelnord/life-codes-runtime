LCFamilyDef(\universal_audio, \meta, {|domain|
	domain.define((
		extends: [\type_pattern],
		color: Color.gray,
	));

	domain.defineBlock(\ring, (
		type: \modifier,
		primary: true,

		parameters: [
			(id: \freq, type: \number, default: 440)
		]
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