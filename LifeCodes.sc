/*
Temporal Roles:
\sequential
\paralell
\paralell but not stopped when scope is
// (do we need more actually? TemporalFunction should maybe also have the ability to as well store a non-temporal function

--> maybe it is actually not so hard!


// Should the subject block be just known as _subject in order to allow for inheritance of subject functionality? this would of course break multi inheritance (or actually also not - would make it weird though haha)


// Issue of resetting keys in Patterns:

If a block states what keys it (primarily) (re)sets and/or modifies we can mark blocks that were superseeded visually.



// Is spec/domain used internally correctly?


// Does extends and inheritsFrom work as it is thought?


*/

LifeCodes {
	// keep these 3 at the beginning
	var <scriptsPath;
	var <samplesPath;
	var <options;

	var <server;
	var <runtime;
	var <gui;
	var <mixer;
    var <sceneManager;

	// keeping track of things that are loaded
	var <scriptFiles;
	var <buffers;
	var <bufferLists;
	var <bufferInfo;

	var loadingTask = nil;

	var <steadyClock;

	var cmdPeriodFunc;

	classvar <instance = nil;

	// some things we might want to cache
	classvar <steadyClock = nil;
	classvar <>bufferCacheServerId = nil;
	classvar <bufferCache;

	// will be used to notify the watchdog and/or terminate sclang
	*fatal {|message, quit=false|
		// TODO: Kill in case the option is set
		instance.isNil.not.if {
			^instance.fatal(message);
		};
		message.error;
	}

	fatal {|message|
		// TODO: Kill in case the option is set
		message.error;
	}

	*new {|scriptsPath="", samplesPath="", options|

		var defaultOptions = (
			server: Server.default,
			loadSamples: true,
			runDry: false,
			numAudioChannels: 4,
			// unit circle; forward is 1@0
			speakerPositions: [
				Point(-2,  2.5),
				Point(2,   2.5),
				Point(2,  -2.5),
				Point(-2, -2.5)
			],
			guiXTranslation: [
				(x: 0.0, position: Point(-2, 2.5)),
				(x: 0.307, position: Point(2, 2.5)),
				(x: 0.692, position: Point(2, -2.5)),
				(x: 1.0, position: Point(-2, -2.5))
			],
			audioOutputMode: \direct,
			audioOutputDelay: 0,
			assignGlobalVariables: true,
			outDevice: nil,
			sampleRate: 48000,
			serverLatency: 0.2,
			onStartAction: {},
			quitOnFatalError: false,
			ignoreDomains: [],
			exportPath: nil,
			traceExecutionQueues: false,
			alsoTraceRapidFunctions: false,
			clock: TempoClock.default,
			guiHost: nil,
			guiReceivePort: 57150,
			guiExecuteOnlyInitializedContexts: true,
			reuseBuffers: false,
			inactivityAudioAttenuation: -9.dbamp,
			entryScene: nil
		);

		instance.isNil.not.if {
			"LifeCodes instance already exists.\nPlease clear the instance forst before creating a new one.".warn;
			^instance;
		};

		options = options ? ();
		options.parent = defaultOptions;

		// check if the paths exist
		PathName(scriptsPath).isFolder.not.if {
			LifeCodes.fatal("Scripts path folder does not exist:" + scriptsPath, options.quitOnFatalError);
			^nil;
		};

		// check if the paths exist
		PathName(samplesPath).isFolder.not.if {
			LifeCodes.fatal("Samples path folder does not exist:" + scriptsPath, options.quitOnFatalError);
			^nil;
		};

		instance = super.newCopyArgs(scriptsPath, samplesPath, options).init;

		options.assignGlobalVariables.if {
			thisProcess.interpreter.l = instance;
		};

		bufferCache.isNil.if {
			bufferCache = Dictionary.new;
		}

		^instance;
	}

	*clear {
		instance.isNil.not.if {
			instance.clear;
		};
		instance = nil;
		^nil;
	}

	clear {
		CmdPeriod.remove(cmdPeriodFunc);
		this.prStopSkipJack;
		loadingTask.stop;
		loadingTask = nil;
		this.prExecuteScriptsForLifecyclePhase(\on_unload);
		options[\reuseBuffers].not.if {
			this.prFreeBuffers;
		};
		gui.clear;
		runtime.clear;
		mixer.clear;
		sceneManager.clear;
		^nil;
	}

	prGetScriptFileList {
		// let's get a flat list of all .scd files in here
		var files = [];

		var recursiveAddFiles = {|path|
			path.entries.do {|entry|
				entry.isFolder.if ({
					recursiveAddFiles.value(entry);
				}, {
					entry.fileName.endsWith(".scd").if {
						files = files.add(entry);
					}
				});
			};
		};

		var addScriptFile = {|key, file|
			scriptFiles[key] = scriptFiles[key].add(file);
		};

		var scriptExecutionPhases = ["on_init", "on_preload", "on_load", "on_postload", "on_unload"];

		recursiveAddFiles.value(PathName(scriptsPath));

		// let's sort the files ...
		files.sort({|a, b| a.fileName < b.fileName});

		// ... and bin them
		scriptFiles = (
			\ignored: [],
			\defs: []
		);

		scriptExecutionPhases.do {|phase| scriptFiles[phase.asSymbol] = [];};

		files.do {|file|
			var fileName = file.fileName;
			fileName.beginsWith("on").if ({
				scriptExecutionPhases.do {|phase|
					fileName.beginsWith(phase).if {
						var tokens = fileName.split($_);
						(tokens.size > 2).if({
							// take off file extension if needed
							options[\ignoreDomains].includes(tokens.last.split($.)[0].asSymbol).not.if ({
								addScriptFile.value(phase.asSymbol, file);
							}, {
								addScriptFile.value(\ignored, file);
							});
						}, {
							// always add - bit a fringe case
							addScriptFile.value(phase.asSymbol, file);
						});
					};
				};
			}, {
				fileName.beginsWith("_").if ({
					addScriptFile.value(\ignored, file);
				}, {
					addScriptFile.value(\defs, file);
				});
			});
		};

		"Found % files in script repository (% ignored) ...".format(files.size - scriptFiles[\ignored].size, scriptFiles[\ignored].size).postln;
		// scriptFiles.collect({|x| x.collect {|file| file.fileName}}).postln;
		"".postln;
	}

	// user-callable method, creates a fork to load the samples
	reloadSamples {|action=nil|
		// TODO: don't do it if there is already a loading task running

		loadingTask = {
			this.prLoadSamples;
			action.value;
		}.fork;
	}

	prExecuteScripts {|scripts, errorAction=nil|
		scripts.do {|script|
			"% ...".format(script.fileName).postln;
			this.prExecutScript(script.absolutePath).not.if {
				errorAction.value;
				^false;
			}
		};
		^true;
	}

	prFatalScriptExecutionError {
		^{"\n".postln;this.fatal("Aborting loading LifeCodes - please fix the error an load again!")}
	}

	// taken over from execute file - don't use load to be able to handle
	// the exception myself.
	prExecutScript {|pathName|
		var saveExecutingPath = thisProcess.nowExecutingPath;
		var success = false;

		thisProcess.nowExecutingPath = pathName;

		try({
			var func = thisProcess.interpreter.compileFile(pathName);
			func.isNil.not.if {
				func.value;
				success = true;
			};
		}, {|exception|
			exception.reportError;
		});

		thisProcess.nowExecutingPath = saveExecutingPath;

		^success;
	}

	prFreeBuffers {
		var recursiveFreeBuffers = {|node|
			node.do {|x|
				x.isCollection.if ({
					recursiveFreeBuffers.value(x);
				}, {
					x.free;
				});
			};
		};

		(buffers.size > 0).if {
			"Freeing audio buffers ...".postln;
			recursiveFreeBuffers.value(buffers);

			buffers.clear;
			bufferLists.clear;
		};
	}

	prBuildBufferInfo {
		bufferInfo = ();
		// iterate over lists - then store index and key in there
		bufferLists.keys.do {|key|
			bufferLists[key].do {|buffer, i|
				bufferInfo[buffer] = (key: key, index: i);
			};
		};
	}

	prLoadSamples {
		var samplesLoaded = 0;

		var recursiveLoadSamples = {|path, node, list, relPath|
			path.files.select({|file| ["wav", "aiff", "aif"].includesEqual(file.extension.toLower) }).do {|file|
				var key = file.fileNameWithoutExtension.asSymbol;
				var condition = Condition();
				var stringPath = file.absolutePath;
				var postFileName = relPath ++ PathName(stringPath).fileName;
				var buffer;
				var reuseBuffer = false;

				options[\reuseBuffers].if {
					reuseBuffer = LifeCodes.bufferCache[stringPath].isNil.not;
				};

				reuseBuffer.if({
					buffer = LifeCodes.bufferCache[stringPath];
					"% ... (reused)".format(postFileName).postln;
				}, {
					buffer = Buffer.read(server, stringPath, action: {condition.test = true; condition.signal});
					"% ...".format(postFileName).postln;
					condition.wait;
				});

				node[key] = buffer;
				list.add(buffer);
				samplesLoaded = samplesLoaded + 1;

				options[\reuseBuffers].if {
					LifeCodes.bufferCache[stringPath] = buffer;
				};
			};

			path.folders.do {|folder|
				var key = folder.folderName.asSymbol;

				node[key] = ();
				bufferLists[key].isNil.if {
					bufferLists[key] = List();
				};

				recursiveLoadSamples.value(folder, node[key], bufferLists[key], relPath ++ key ++ "/");
			};
		};

		options[\reuseBuffers].not.if({
			this.prFreeBuffers;
		}, {
			(server.pid != LifeCodes.bufferCacheServerId).if {
				LifeCodes.bufferCache.clear;
				LifeCodes.bufferCacheServerId = server.pid;
			}
		});

		"\n*** LOADING SAMPLES ***".postln;
		options[\loadSamples].if ({
			recursiveLoadSamples.value(PathName(samplesPath), buffers, nil, "");
			"... % samples loaded.".format(samplesLoaded).postln;
		}, {
			"... no samples loaded as loadSamples was set to false.".postln;
	    });
	}

	prInitData {
		runtime = LCRuntime(this);
		buffers = ();
		bufferLists = ();
	}

	*assureSteadyClock {
		steadyClock.isNil.if {
			steadyClock = TempoClock(1);
			steadyClock.permanent = true;
		};
		^steadyClock;
	}

	init {
		steadyClock = LifeCodes.assureSteadyClock;

		this.prInitData;

		"\n\nStarting scene manager".postln;
		sceneManager = LCSceneManager(this);

		loadingTask = {

			options[\runDry].if {
				"\n\nDry run: Skipping booting server as well as on_preload and on_load scripts.".postln;
			};

			"\n\nStarting and loading LifeCodes runtime ...\n".postln;

			this.prLoad;

			options[\guiHost].isNil.not.if {
				"\n\nStarting GUI layer on % - listening on port %".format(options[\guiHost], options[\guiReceivePort]).postln;
				options[\specsExportPath].isNil.if({
					"Cannot initialize GUI layer without 'specsExportPath' set!".error;
				}, {
					gui = LCGUI(this);
			    })
			};

			"\nLifeCodes Runtime loaded and running!".postln;

			cmdPeriodFunc = {this.prCmdPeriod};
			CmdPeriod.add(cmdPeriodFunc);
			this.prStartSkipJack;

			options[\onStartAction].value(this);

			options[\entryScene].isNil.not.if {
				sceneManager.runScene(options[\entryScene]);
			};

		}.fork;
	}

	prCmdPeriod {
		gui.cmdPeriod;
	}

	prStartSkipJack {
		var era = CmdPeriod.era;
		SkipJack({
			(era != CmdPeriod.era).if {

				gui.recoverFromCmdPeriod;

				era = CmdPeriod.era;
			};
		}, 0.05, name: \lifecodes, autostart: true);
	}

	prStopSkipJack {
		SkipJack.stop(\lifecodes);
	}

	prExecuteScriptsForLifecyclePhase {|phase|
		var result;

		(scriptFiles[phase].size == 0).if {
			^true; // nothing to do
		};

		"\n*** EXECUTING % SCRIPT FILES ***".format(phase.asString.toUpper).postln;
		result = this.prExecuteScripts(scriptFiles[phase], this.prFatalScriptExecutionError);
		result.not.if {LifeCodes.clear};
		"".postln;
		^result;
	}

	prLoad {

		this.prGetScriptFileList;

		this.prExecuteScriptsForLifecyclePhase(\on_init);

		options[\runDry].not.if {
			this.prBootServer;
			this.prExecuteScriptsForLifecyclePhase(\on_preload);

			LCAudioMixer.buildSynthDefs;
			"\n\nStarting audio mixer".postln;
			mixer = LCAudioMixer();

			this.prLoadSamples;
			this.prBuildBufferInfo;
			this.prExecuteScriptsForLifecyclePhase(\on_load);
		};

		this.prExecuteScriptsForLifecyclePhase(\defs);

		runtime.compile;

		this.prExecuteAllFamiliesInit;

		runtime.buildIndex;

		this.prExecuteScriptsForLifecyclePhase(\on_postload);

		options[\specsExportPath].isNil.not.if {
			LCJSONExport.write(options[\specsExportPath], runtime);
		};
	}

	prExecuteAllFamiliesInit {
		"\n*** EXECUTING ALL FAMILIES ON_INIT ***".postln;

		runtime.families.keys.asArray.sort.do {|key|
			var family = runtime.families[key];
			family.executeLifecyclePhase(\on_init);
		};
	}

	prBootServer {
		server = options[\server];
		server.options.outDevice = options[\outDevice];
		server.options.sampleRate = options[\sampleRate];
		server.latency = options[\serverLatency];

		// let's increase some internal server options to not run into issues ...
		server.options.numBuffers = 1024 * 8;
		server.options.numWireBufs = 128;
		server.options.memSize = 8192 * 4;
		server.options.maxNodes = 1024 * 4;

		server.bootSync;

		server.hasBooted.not.if {
			"\n".postln;
			LifeCodes.fatal("Could not start audio server!");
			this.clear;
		};
	}

	domainActive {|domain|
		^options[\ignoreDomains].includes(domain).not;
	}

	*randomId {
		^((2**31).rand.asInteger.asHexString ++ (2**31).rand.asInteger.asHexString).asSymbol;
	}
}