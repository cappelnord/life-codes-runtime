/*
This is a developer playground - don't expect structure and sense here :)

*/

s.plotTree

LCContext

LCdef(\test).audioChain.fxNodes

(
LCdef(\test, \pling);
LCdef(\test).appendModifiers = [\xPosPan];

l.gui.registerBlockSlots(\ok, [
	['pling', 0.3@0.5, (context: \test)],
	['play', 0.5@0.5, (canRespawn: true)],
	['mute', 0.6@0.5, (canRespawn: true)],
	['ring', 0.8@0.5, (args: [440])],
	['ring', 0.8@0.6, (args: [550])],
	['ring', 0.8@0.7, (args: [50])],
	['faster', 0.7@0.5, (args: [4])]
]);
)

(
fork {
	l.gui.popRegistry(\ok).do {|ref|
		l.gui.despawnBlockSlot(ref.id, (fadeTime: 1));
		0.5.wait;
	}
}
)

s.plotTree

LCdef(\robin).clear

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

LCdef(\ok, ["ok,f4"])
LCdef(\bra, [[\dump]])
LCdef(\bra, [[\dump, 123], "print", "print"])

LCdef(\bra).updateData((\a: 100))

LCdef(\bra).clear

LCdef(\bra).family == LCdef(\ok).family

LCdef(\ok, [\pling, \play])
LCdef(\okk, [\pling, \play, \cycleSpeakers])


LCdef(\mega, [\dump, \print])
LCdef(\mega).clear

LCdef(\ok).load
LCdef(\ok).clear
LCdef(\bra).clear

LCdef(\pling, [\pling, \play])

LCdef(\ok).family.lookup[1].id

l.runtime.blockSpecs['universal_pattern:play'].parameters

l.runtime.typesDict

l.options[\exportPath]


// OK - we want to spawn blocks; what do we need to know?

// the block slot id
// the full block spec ID
// a position and a behaviour key
// At least for subjects the context the block belongs to --> this will also trigger to send position info over
// potentially a different displayName
// potentially a different family
// potentially parameters
// potentially a respawn rule (TODO)

// Terminal blocks - how do they work?
// Difference: Respawn or that they also kill the slot itself.
// They should probably have a fuse time (or something) that dictates on how long they will be there.

// --> Maybe the spawning can return a reference so that we can collect references to spawned blocks and
// clean them up more easily, probably the scene manager and the scene itself can somehow be used to
// store these references.

// maybe a scene should be a chain of functions with a delay inbetween (to allow for fast-forward)
// + setup func
// + leave func

