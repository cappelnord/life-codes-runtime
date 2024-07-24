LCSceneDef(\act1a, {|sceneManager, gui, lc|
	"act1a".postln;
	sceneManager.waitForTime(10);
	"10 seconds later".postln;
	sceneManager.waitForTime(10);
	"10 more seconds later".postln;
	sceneManager.transitionAfterTime(\act1b, 10);
}, final: {|sceneManager, gui, lc|
	"act1a - final".postln;
});




LCSceneDef(\act1b, {|sceneManager, gui, lc|
	"act1b".postln;
	sceneManager.waitForTime(10);
	"10 seconds later".postln;
	sceneManager.waitForTime(10);
	"10 more seconds later".postln;
	sceneManager.transitionAfterTime(\act1a, 10);
}, final: {|sceneManager, gui, lc|
	"act1b - final".postln;
});

