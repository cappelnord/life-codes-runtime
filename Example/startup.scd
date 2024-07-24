(
var basePath = thisProcess.nowExecutingPath.dirname;
LifeCodes.clear;
l = LifeCodes(
	scriptsPath: basePath +/+ "scripts",
	samplesPath: basePath +/+ "samples",
	options: (
		specsExportPath: basePath +/+ "_specs.json",
		traceExecutionQueues: true,
		guiHost: NetAddr("127.0.0.1", 57140),
		audioOutputMode: \binaural,
		entryScene: \act1a
	)
);
)