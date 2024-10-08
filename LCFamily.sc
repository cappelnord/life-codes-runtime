LCFamilyDef {
	*new {|familyId, domain=nil, function=nil|
		var runtime;

		LifeCodes.instance.isNil.if {
			"LifeCodes is not initialized - cannot define families ...".warn;
			^nil;
		};

		LifeCodes.instance.options[\ignoreDomains].includes(domain).if {
			"Ignored domain '%'for '%' ...".format(domain, familyId).postln;
			^nil;
		};

		runtime = LifeCodes.instance.runtime;

		runtime.families[familyId].isNil.if {
			runtime.families[familyId] = LCFamily.new.init(familyId);
		};

		domain.isNil.if {
		   	runtime.families[familyId];
		};

		runtime.families[familyId].addDomainFunction(domain, function);
		^runtime.families[familyId];
	}
}

LCFamily {
	var <id;
	var <table;

	var <hasSubject = false;

	// which families does this block match to?
	var <matches;

	// which families are looked through for code functions?
	var <lookup;


	var <type;
	var <quant;
	var <patternQuant;


	var extensionFamilies;

	var domainFunctions;
	var currentLoadDomain = nil;

	var runtime;

	var isLoaded = false;

	var <data;

	var cache;

	prInitData {
		domainFunctions = ();
		matches = List();
		lookup = List();
		extensionFamilies = List();
		data = ();
		cache = ();
    }

	init {|familyId|
		id = familyId;
        this.prInitData;

		runtime = LifeCodes.instance.runtime;

		// "Created LCSpec with ID: %".format(id).postln;
		^this;
	}

	prMergeTable {|src, dst, blockId|
		var allKeys = src.keys.reject {|key| [\blocks, \data].includes(key) };

		allKeys.do {|key|
			key.asString.beginsWith("on_").if({
				// build a function list
				dst[key].isNil.if {
					dst[key] = List();
				};
				dst[key].add(LCFunctionReference(src[key], key, currentLoadDomain, this, blockId));
			}, {
				// check if we override something to warn
				dst[key].isNil.not.if {
					(dst[key] != src[key]).if {
						"Key '%' overwritten.".postln;
					};
				};
				dst[key] = src[key];
			});
		}
	}

	define {|def|
		this.prMergeTable(def, table);

		def[\data].isNil.not.if {
			this.prMergeTable(def[\data], table[\data]);
		};

		def[\blocks].isNil.not {
			def[\blocks].keys.do {|blockId|
				this.defineBlock(blockId, def[\blocks][blockId]);
			};
		};

		^this;
	}

	defineBlock {|blockId, def|
		table[\blocks][blockId].isNil.if {
			table[\blocks][blockId] = ();
		};

		// shortcut to allow pbinds to solely define a block
		(def.class == Pbind).if {
			var pbind = def;
			def = (
				on_execute: {|block, cmd, ctx, family|
					cmd.pattern.extend(pbind);
				}
			)
		};

		// shortcut to allow functions to solely define a block
		(def.class == Function).if {
			var func = def;
			def = (
				on_execute: func
			);
		};

		this.prMergeTable(def, table[\blocks][blockId], blockId);
	}

	addDomainFunction {|domain, function|

		// allow for events to be given instead of functions
		(function.class == Event).if {
			var event = function;
			function = {|spec|
				spec.define(event);
			};
		};

		domainFunctions[domain] = function;
	}

	// TODO: This should likely also aggregate from all related families
	getLifecycleFunctionReferences {|phase, traverseLookupTable=true|
		var ret;
		var cacheKey;

		traverseLookupTable.not.if({
			^table[phase];
		});


		cacheKey = "lifecycle-%".format(phase).asSymbol;
		cache[cacheKey].isNil.not.if {
			^cache[cacheKey];
		};

		ret = List();

		// TODO: Here we can add a way for blocks to break inheritance

		lookup.reverse.do {|family|
			ret.addAll(family.table[phase]);
		};

		cache[cacheKey] = ret;
		^ret;

	}

	// added a memo cache here not to traverse/build function lists
	// all the time ...

	getBlockFunctionReferences {|name, phase|
		var ret;

		var cacheKey = "block-%-%".format(name, phase).asSymbol;
		cache[cacheKey].isNil.not.if {
			^cache[cacheKey];
		};

		ret = List();

		// TODO: Here we can add a way for blocks to break inheritance

		lookup.reverse.do {|family|
			var blockTable = family.table[\blocks][name];
			blockTable.isNil.not.if {
				ret.addAll(blockTable[phase]);
			};
		};

		cache[cacheKey] = ret;
		^ret;
	}

	getLifecycleExecutionUnits {|phase, traverseLookupTable=true|
		^this.getLifecycleFunctionReferences(phase, traverseLookupTable).collect({|ref|
			ref.bind(this)
		});
	}

	executeLifecyclePhase {|phase, executionQueue, traverseLookupTable=true|
		var list = this.getLifecycleExecutionUnits(phase, traverseLookupTable);
		(list.size > 0).if {
			// "Execute Family Lifecycle Phase: %/%".format(id, phase).postln;
			executionQueue = executionQueue ? runtime.executionQueue;
			executionQueue.executeList(list);
	    }
	}

	compileDomainFunctions {
		var domainKeys = domainFunctions.keys.asArray.sort;

		table = (
			\family: id,
			\blocks: (),
			\data: ()
		);

		domainKeys.do {|domainKey|
			"%/% ...".format(id, domainKey).postln;
			currentLoadDomain = domainKey;
			domainFunctions[domainKey].value(this, LifeCodes.instance);
			currentLoadDomain = nil;
		};

		// table.postln;
	}

	asString {
		^"Family: %".format(id);
	}

	addExtensionFamily {|extension|
		// "Extension".postln;
		// extension.id.postln;
		extensionFamilies.add(extension);
	}


	// here is where we look our own table and copy things into members
	buildIndex {

		// check if any of the blocks is a subject
		table[\blocks].do {|block|
			hasSubject = hasSubject || (block[\type] == \subject);
		};

		// create block specs for each block that is a primary block

		table[\blocks].keys.do {|key|
			(table[\blocks][key][\primary] == true).if {
				runtime.blockSpecs[LCBlockSpec.identifier(key, id)] = LCBlockSpec(key, id, table[\blocks][key]);
			};
		};

		// create a list of all compatible families
		lookup = OrderedIdentitySet();
		lookup.add(this);
		this.traverseLookupTree(lookup, 1);

		lookup = lookup.asList;
		// id.postln;
		// lookup.collect({|x| x.id}).postln;

		type = table[\type];
		quant = table[\quant];
		patternQuant = table[\patternQuant] ? quant;
	}

	buildMatches {
		matches = Set();
		// matches.addAll(lookup.collect(_.id));
		runtime.families.do {|family|
			family.lookup.includes(this).if {
				matches.add(family.id);
			};
		};
		matches = matches.asList();
	}

	traverseLookupTree {|set, depth|
		var candidates = List();

		table[\inheritsFrom].do {|key|
			var family = runtime.families[key];
			family.isNil.not.if ({
				candidates.add(family);
			}, {
				"Could not find family to inherit from: %".format(key).warn;
			});
		};

		candidates.addAll(extensionFamilies);

		candidates.do {|family|
			set.findMatch(family).isNil.if {
				set.add(family);
				family.traverseLookupTree(set, depth+1);
			};
		};
	}

	findBlockSpec {|name|
		lookup.do {|candidate|
			var blockFullId = LCBlockSpec.identifier(name, candidate.id);
			var specCandidate = runtime.blockSpecs[blockFullId];
			// blockFullId.postln;
			specCandidate.isNil.not.if {
				^specCandidate;
			};
		}
		^nil;
	}

	matchesBlock {|name|
		^this.findBlockSpec(name).isNil.not;
	}

	load {|executionQueue|
		isLoaded.not.if {
			this.executeLifecyclePhase(\on_load, executionQueue);
			isLoaded = true;
		};
	}

	unload {|executionQueue|
		isLoaded.if {
			this.executeLifecyclePhase(\on_unload, executionQueue);
			isLoaded = false;
		};
	}

	isPatternType {
		^type.asString.find("pattern").isNil.not;
	}

	isAudioType {
		^type.asString.find("audio").isNil.not;
	}

	hasAudio {
		^(this.isPatternType || this.isAudioType);
	}
}