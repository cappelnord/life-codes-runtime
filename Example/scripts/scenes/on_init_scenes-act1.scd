LCSceneDef(\act1a, {|sceneManager, gui, l|
	"act1a".postln;
	sceneManager.waitForTime(10);
	"10 seconds later".postln;
	sceneManager.waitForTime(10);
	"10 more seconds later".postln;
	LCdef(\pling, \pling);
	sceneManager.transitionOnBlock(\act1b, \pling, \play, true);
}, final: {|sceneManager, gui, l|
	"act1a - final".postln;
});




LCSceneDef(\act1b, {|sceneManager, gui, l|
	"act1b".postln;
	sceneManager.waitForTime(10);
	"10 seconds later".postln;
	sceneManager.waitForTime(10);
	"10 more seconds later".postln;
	sceneManager.transitionAfterTime(\act1a, 10);
}, final: {|sceneManager, gui, l|
	"act1b - final".postln;
});

