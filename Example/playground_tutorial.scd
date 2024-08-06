// Make sure Life Codes is started and ready!


// ###
// Live Coding with Life Codes in SuperCollider
// ###

// Let's start with defining a context and bind it to the \pling family
LCdef(\test, \pling)

// We can play the pling. \pling is defined in pling_meta.scd and pling_music.scd while
// \play is more general and defined in universal_pattern_meta.scd.
LCdef(\test1, [\pling, \play])

// Let's clear the context again just to see how this works:
LCdef(\test1).clear

// We can also directly start ahead and define a command already on startup
LCdef(\test1, [\pling, \play])

// Pling plays out only on one channel. In the startup you might have audioOutputMode: \binaural which
// creates a simulation of the speaker arrangement. With cycleSpeakers we can cycle through the speakers.
LCdef(\test1, [\pling, \play, \cycleSpeakers])

// The universal_pattern family defines more modifiers that are compatible with pling.
LCdef(\test1, [\pling, \play, \cycleSpeakers, [\faster, 2]])

// Let's start more contexts to also play pings - but with an offbeat/offset and some transposition (also defined in universal_pattern)
LCdef(\test2, [\pling, \play, \cycleSpeakers, [\offbeat, 0.25], [\transpose, 7]])
LCdef(\test3, [\pling, \play, \cycleSpeakers, [\faster, 0.5], [\offbeat, 0.25], [\transpose, -12]])

// They now all sound the same, let's add a ring modulator as audio effect (defined in unviersal_audio)
LCdef(\test1, [\pling, \play, \cycleSpeakers, [\faster, 2]]);
LCdef(\test2, [\pling, \play, \cycleSpeakers, [\offbeat, 0.25], [\transpose, 7], [\ring, 220]]);
LCdef(\test3, [\pling, \play, \cycleSpeakers, [\faster, 0.5], [\offbeat, 0.25], [\transpose, -12], [\ring, 110]]);

// let's mute test2 and test3 for now
LCdef(\test2, [\pling, \play, \cycleSpeakers, [\offbeat, 0.25], [\transpose, 7], [\ring, 220], \mute])
LCdef(\test3, [\pling, \play, \cycleSpeakers, [\faster, 0.5], [\offbeat, 0.25], [\transpose, -12], [\ring, 110], \mute])

// we can also bind modifiers to a context so that they will be automatically appended when we execute a new command
LCdef(\test1).appendModifiers = [[\ring, 1100]];

// the [\ring, 1100] block is now automatically appended - but we need to reevaluate the last command
// or execute a new command.
LCdef(\test1).reevaluate

// let's clean up
LCdef(\test1).clear;
LCdef(\test2).clear;
LCdef(\test3).clear;




// ###
// Live Coding with Life Codes using the Interaction Layer
// ###

// Make sure that the interaction layer is running.


(
// let's put all in random positions
var rp = {Point(0.8.rand + 0.1, 0.8.rand + 0.1)};

// let's initialize the execution contexts
LCdef(\test1, \pling);
LCdef(\test2, \pling);
LCdef(\test3, \pling);

// gets rid of all previous blocks
l.gui.clear;

// let's spawn our subject blocks and 3 plays
l.gui.registerBlockSlots(\test, [
	['pling', rp.value, (context: \test1)],
	['pling', rp.value, (context: \test2)],
	['pling', rp.value, (context: \test3)],
]);

// also 3 play blocks and 3 mute blocks
l.gui.registerBlockSlots(\test, [
	['play', rp.value],
	['play', rp.value],
	['play', rp.value],
	['mute', rp.value],
	['mute', rp.value],
	['mute', rp.value],
]);

// and a bunch of modifiers to play with
l.gui.registerBlockSlots(\test, [
	['transpose', rp.value, (args: [4])],
	['transpose', rp.value, (args: [7])],
	['transpose', rp.value, (args: [12])],
	['transpose', rp.value, (args: [-12])],
	['faster', rp.value, (args: [2])],
	['faster', rp.value, (args: [2])],
	['faster', rp.value, (args: [0.5])],
	['faster', rp.value, (args: [4])],
	['offbeat', rp.value, (args: [0.125])],
	['offbeat', rp.value, (args: [0.25])],
	['offbeat', rp.value, (args: [0.375])],
	['offbeat', rp.value, (args: [0.5])],

	// alternative way on how to express arguments
	[['ring', 110], rp.value],
	[['ring', 220], rp.value],
	[['ring', 440], rp.value],
	[['ring', 880], rp.value],
]);
)

(
// this makes the audio pan according to the screen position.
// pling creates a mono signal, so xPosPan is good - for stereo xPosPan2
// see panning_meta.scd and panning_music.scd
LCdef(\test1).appendModifiers = [\xPosPan];
LCdef(\test2).appendModifiers = [\xPosPan];
LCdef(\test3).appendModifiers = [\xPosPan];
// these modifiers only come in action after the next command is executed
)

(
// let's slowly despawn the blocks
fork {
	l.gui.popRegistry(\test).scramble.do {|slot|
		l.gui.despawnBlockSlot(slot.id);
		0.25.wait;
	};
	LCdef(\test1).clear;
	LCdef(\test2).clear;
	LCdef(\test3).clear;
}
)




// ###
// Dump Things
// ###


// The dump family is just posting something on each of the life cycles. So let's try that out!

LCdef(\dump, \dump)

LCdef(\dump).load

LCdef(\dump).updateData((ok: 10))

LCdef(\dump, [\dump, \print])

LCdef(\dump).updateData((ok: 20))




// ###
// Inheritance / Extensions
// ###

// check scripts/inheritance.scd

// \act is a native block in the base family
LCdef(\test, [\base, \act])

// \mod is not defined in the base family but in the extension family (which declares to extend base)
LCdef(\test, [\base, \act, \mod])


// \inheritance inherits from \base
LCdef(\test2, \inheritance)

// so while it needs its own subject it can use the \act block from \base
LCdef(\test2, [\inheritance, \act])

// as well as the \mod block from extension
LCdef(\test2, [\inheritance, \act, \mod])




// ###
// Attaching functions directly to a context (anonymous functions?)
// ###

LCdef(\test, [\pling, \play])

// Let's define a on_pattern_finish function for \test domain.

(
LCdef(\test).define((
	on_pattern_finish: {|event, cmd, ctx|
		"pling".postln;
    }
), \domain);
)

// Let's also define a on_perform function for the \play block in this context.
// We can ommit the domain and it will default to \context.

// Context functions are always executed after the regular family functions

(
LCdef(\test).defineBlock(\play, (
	on_perform: {|block, cmd, ctx|
		"PERFORM".postln;
	}
));
)

// For most behaviours a new command must be executed for the context functions
// to be taken into account ...

LCdef(\test, [\pling, \play, \faster])


// We can redefine functions at any time

(
LCdef(\test).defineBlock(\play, (
	on_perform: {|block, cmd, ctx|
		"PERFORM".scramble.postln;
	}
));
)

// Attach extra behaviour:
(
LCdef(\test).defineBlock(\play, Pbind(
	\degree, Pseq([0, 1, 2, 3], inf)
))
)

// And also access all arguments

(
LCdef(\test).defineBlock(\ring, (
	on_perform: {|block, cmd, ctx|
		"Frequency: %".format(block.args[\freq]).postln;
	}
));
)


// We can remove it by setting the function to an empty function ...
// Make sure to use the right domain though!

(
LCdef(\test).define((
	on_pattern_finish: {}
), \domain);
)


LCdef(\test, [\pling, \play, [\ring, 1000.rand]])
LCdef(\test, [\pling, \play, \faster, [\ring, 1000.rand]])
LCdef(\test, [\pling, \play, [\ring, 1000.rand]])


// Please note that this functionality will not be present in other LCdefs
LCdef(\test, [\pling])
LCdef(\test2, [\pling, \play])

// ... alternatively we can also remove all extra functionality.

LCdef(\test).clearFunctionTable

LCdef(\test, [\pling, \play])

LCdef(\test).clear
LCdef(\test2).clear
