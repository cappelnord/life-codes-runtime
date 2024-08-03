LCSceneDef {
	var <id;
	var <function;
	var <final;

	var lc;

	*new {|sceneId, function, final|
		final = final ? {};

		LifeCodes.instance.isNil.if {
			"LifeCodes is not initialized - cannot use LCSceneDef ...".warn;
			^nil;
		};

		function.isNil.if {
			^LifeCodes.instance.sceneManager.sceneDefs[sceneId];
		};

		^super.newCopyArgs(sceneId, function, final).init;
	}

	init {
		lc = LifeCodes.instance;
		lc.sceneManager.sceneDefs[id] = this;
	}
}

LCSceneManager {
	var <lc;

	var <sceneDefs;

	var <currentScene;
	var <lastScene;

	var <data;
	var <stepCounter = 0;

	var routine;

	var rushCurrentStep = false;
	var rushCurrentScene = false;

	*new {|lc|
		^super.newCopyArgs(lc).init;
	}

	rush {
		rushCurrentStep = true;
	}

	rushScene {
		rushCurrentScene = true;
	}

	prInitData {
		sceneDefs = ();
		data = ();
		currentScene = nil;
		lastScene = nil;
		routine = nil;
	}

	init {
		this.prInitData;
	}

	clear {
		routine.isPlaying.if {
			routine.stop;
		};
		this.prInitData;
	}

	waitForTime {|seconds|
		var then = lc.steadyClock.beats + seconds;
		this.prStepOnCondition({lc.steadyClock.beats >= then});
	}

	waitForCondition {|condition|
		this.prStepOnCondition(condition);
	}

	waitForBlock {|ctx, name, on_perform=false|
		this.prStepOnCondition(this.prBlockCondition(ctx, name, on_perform));
	}

	transitionAfterTime {|sceneId, seconds|
		var then = lc.steadyClock.beats + seconds;
		this.prTransitionOnCondition(sceneId, {lc.steadyClock.beats >= then});
	}

	transitionOnCondition {|sceneId, condition|
		this.prTransitionOnCondition(sceneId, condition);
	}

	transitionOnBlock {|sceneId, ctx, name, on_perform=false|
		this.prTransitionOnCondition(sceneId, this.prBlockCondition(ctx, name, on_perform));
	}

	prBlockCondition {|ctx, name, on_perform=false|
		var dict, old, condition;

		(ctx.class == Symbol).if {
			ctx = LCdef(ctx);
		};

		dict = ctx.blockHistory(on_perform);
		old = dict[name] ? 0;
		condition = {
			var new = dict[name] ? 0;
			(new < old).if {old = new;};
			new > old;
		};
		^condition;
	}

	// basically busy-waits until condition is true
	prStepOnCondition {|condition|
		{condition.value.not && rushCurrentStep.not && rushCurrentScene.not}.while {
			(1/60.0).wait;
		};
		stepCounter = stepCounter + 1;
		rushCurrentStep = false;
	}

	prTransitionOnCondition {|sceneId, condition|
		{condition.value.not && rushCurrentStep.not && rushCurrentScene.not}.while {
			(1/60.0).wait;
		};
		stepCounter = stepCounter + 1;
		rushCurrentStep = false;
		rushCurrentScene = false;
		{this.runScene(sceneId);}.defer;
	}

	runScene {|sceneId|
		// always start with a fresh routine
		routine.isPlaying.if {
			routine.stop;
		};

		// call final func
		currentScene.isNil.not.if {
			currentScene.final.value(this, lc.gui, lc);
		};

		routine = nil;
		lastScene = currentScene;
		currentScene = nil;

		// this would be a terminal clean state
		sceneId.isNil.if {
			^nil;
		};

		currentScene = sceneDefs[sceneId];

		currentScene.isNil.if {
			"No scene found with the name %".format(sceneId).error;
			^nil;
		};

		"Entering scene %:".format(sceneId).postln;

		rushCurrentStep = false;
		rushCurrentScene = false;

		routine = Routine({currentScene.function.value(this, lc)});
		routine.play(lc.steadyClock);
	}
}

