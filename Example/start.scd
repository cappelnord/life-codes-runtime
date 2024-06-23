(
var basePath = thisProcess.nowExecutingPath.dirname;
l = LifeCodes(
	scriptsPath: basePath +/+ "scripts",
	samplesPath: basePath +/+ "samples",
	options: (
		\runDry: true,
		\ignoreDomains: []
	)
)
)

LifeCodes.clear

// *** ACCESSING LOADED BUFFERS ***

// Buffers can be accessed either by a dictionary structure that mimics
// the folder structure ...
l.buffers.ok.play;

// ... or by accessing lists that are generated for each folder found
// in the samples folder.
l.bufferLists.water_bowl.choose.play;

// this is equivalent, written in a differnt style
l.bufferLists[\water_bowl].choose.play;

// sample folder can be reloaded to include new samples (or change samples)
// all loaded samples will first be unloaded not to accumulate memory over time
l.reloadSamples;





// *** Playground for LCdef ***

LCdef(\ok, \dump)

LCdef(\ok)

l.runtime.contexts[\ok]