LCSceneDef(\act1a, {|sceneManager, l|
	"act1a".postln;
	sceneManager.waitForTime(10);
	"10 seconds later".postln;
	sceneManager.waitForTime(10);
	"10 more seconds later".postln;
	LCdef(\pling, \pling);
	sceneManager.transitionOnBlock(\act1b, LCdef(\pling), \play, on_perform:true);
}, final: {|sceneManager, l|
	"act1a - final".postln;
});




LCSceneDef(\act1b, {|sceneManager, l|
	"act1b".postln;
	sceneManager.waitForTime(10);
	"10 seconds later".postln;
	sceneManager.waitForTime(10);
	"10 more seconds later".postln;
	sceneManager.transitionAfterTime(\act1a, 10);
}, final: {|sceneManager, l|
	"act1b - final".postln;
});

