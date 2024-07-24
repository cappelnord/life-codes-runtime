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
});