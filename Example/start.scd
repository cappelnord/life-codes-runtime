(
var basePath = thisProcess.nowExecutingPath.dirname;
l = LifeCodes(
	scriptsPath: basePath +/+ "scripts",
	samplesPath: basePath +/+ "samples",
	options: (
		\runDry: true,
		\ignoreDomains: [],
		\exportPath: basePath +/+ "specs.json"
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


l.runtime.blockSpecs


// *** Playground for LCdef ***

LCdef(\ok, \universal_pattern)
LCdef(\bra, \dump)
LCdef(\bra, [[\dump, 123], "print"])

LCdef(\bra).family == LCdef(\ok).family

LCdef(\ok).load
LCdef(\ok).clear
LCdef(\bra).clear


l.runtime.blockSpecs['dump:dump'].parameters.

l.runtime.typesDict

l.options[\exportPath]