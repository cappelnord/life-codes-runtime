/*
Temporal Roles:
\sequential
\paralell
\paralell but not stopped when scope is
// (do we need more actually? TemporalFunction should maybe also have the ability to as well store a non-temporal function

--> maybe it is actually not so hard!
--> Maybe we could have a seperate object which represents the runtime queue


Command sequences (and lookup rules)

[subject] <- always also controls the family - subjects cannot inherit
[action]
[modifiers]

[block arg1 arg2 arg3] arguments are always positional, if not present default values will be taken (or loaded from block instance data block)

[block:id arg1 arg2 arg3] id allows to reference specific blocks (so that blocks can be discerned or specific data can be attached to a block)
*/

LifeCodes {
	// keep these 3 at the beginning
	var <scriptsPath;
	var <samplesPath;
	var <options;

	var <server;
	var <runtime;

	// keeping track of things that are loaded
	var <scriptFiles;
	var <buffers;
	var <bufferLists;

	var loadingTask = nil;

	classvar <instance = nil;

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
			\server: Server.default,
			\runDry: false,
			\numAudioChannels: 4,
			\audioMixMode: \passThrough,
			\port: 57150,
			\assignGlobalVariables: true,
			\outDevice: nil,
			\sampleRate: 48000,
			\action: {},
			\quitOnFatalError: false,
			\ignoreDomains: []
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
		loadingTask.stop;
		loadingTask = nil;
		this.prExecuteScriptsForLifecyclePhase(\on_unload);
		this.prFreeBuffers;
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
		files.sort({|a, b| a.fileName > b.fileName});

		// ... and bin them
		scriptFiles = (
			\ignored: [],
			\spec: []
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
							options[\ignoreDomains].includes(tokens[2].split($.)[0].asSymbol).not.if ({
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
					addScriptFile.value(\spec, file);
				});
			});
		};

		"Found % files in script repository (% ignored):".format(files.size - scriptFiles[\ignored].size, scriptFiles[\ignored].size).postln;
		scriptFiles.collect({|x| x.collect {|file| file.fileName}}).postln;
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

	prLoadSamples {
		var samplesLoaded = 0;

		var recursiveLoadSamples = {|path, node, list, relPath|
			path.files.select({|file| ["wav", "aiff", "aif"].includesEqual(file.extension.toLower) }).do {|file|
				var key = file.fileNameWithoutExtension.asSymbol;
				var condition = Condition();
				var stringPath = file.absolutePath;
				var buffer = Buffer.read(server, stringPath, action: {condition.test = true; condition.signal});
				"% ...".format(relPath ++ PathName(stringPath).fileName).postln;
				condition.wait;
				node[key] = buffer;
				list.add(buffer);
				samplesLoaded = samplesLoaded + 1;
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

		this.prFreeBuffers;

		"\n*** LOADING SAMPLES ***".postln;

		recursiveLoadSamples.value(PathName(samplesPath), buffers, nil, "");
		"... % samples loaded.".format(samplesLoaded).postln;
	}

	prInitData {
		runtime = LCRuntime(this);
		buffers = ();
		bufferLists = ();
	}

	init {
		this.prInitData;

		loadingTask = {

			options[\runDry].if {
				"\n\nDry run: Skipping booting server as well as on_preload and on_load scripts.".postln;
			};

			"\n\nStarting and loading LifeCodes runtime ...\n".postln;

			this.prLoad;

			// START OSC
			"\nLifeCodes Runtime loaded and running!".postln;

			options.action.value(this);
		}.fork;
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
			this.prLoadSamples;
			this.prExecuteScriptsForLifecyclePhase(\on_load);
		};

		this.prExecuteScriptsForLifecyclePhase(\spec);

		this.runtime.compile;

		this.runtime.executeSpecLifecyclePhase(\on_init, \loader);

		this.runtime.buildIndex;

		this.prExecuteScriptsForLifecyclePhase(\on_postload);
	}

	prBootServer {
		server = options.server;
		server.options.outDevice = options.outDevice;
		server.options.sampleRate = options.sampleRate;

		server.bootSync;

		server.hasBooted.not.if {
			"\n".postln;
			LifeCodes.fatal("Could not start audio server!");
			this.clear;
		};
	}
}