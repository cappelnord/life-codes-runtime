LCSpec(\universal_audio, \meta, {|spec|
	spec.define((
		extends: [\type_pattern],
		color: Color.gray,
	));

	spec.defineBlock(\ring, (
		type: \modifier,
		primary: true,

		parameters: [
			(id: \freq, type: \number, default: 440)
		]
	));
});