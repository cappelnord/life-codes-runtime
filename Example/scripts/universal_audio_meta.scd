LCFamilyDef(\universal_audio, \meta, {|domain, lc|
	domain.define((
		extends: [\type_pattern, \type_audio],
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