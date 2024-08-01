
# Life Codes Quick Manual

## Prerequisites
### Binaural Room Simulation
In order to use the `binaural` output mode the [Ambisonics Toolkit (ATK)](https://github.com/ambisonictoolkit/atk-sc3/blob/master/README.md#installing) must be installed. This includes the Quark, sc3-plugins as well as the Atk kernels and matrices.

## Terminology
Here is a quick overview of terms and what they mean in the context of the Life Codes runtime.

**TODO**

## Startup and Loading

### Startup
A startup script will likely be called on installation startup which will start the loading process and specify options. Please check [Example/startup.scd](Example/start.scd) as a guiding point. Options are currently not yet documented but all options can be found in [LifeCodes.sc](LifeCodes.sc) with hopefully meaningful names.

### Scripts Folder
All content code is organized in a folder specified by `scriptsPath` in the startup script. Code files can be organized in folders as it seems sensible - they will all be treated equally. File names matter though:

* File names that start with an underscore (`_`) will be ignored, e.g. to temporarily ignore content.
* File names that start with `on_` are executed at a specific point in the lifecycle of the runtime.
* All other files should contain only family definitions to specify blocks and their behaviour.

Within their execution scope all files are executed in alphanumerical order (independent in which folder they are).

### Life Cycle Scripts
File names starting with `on_`. All scripts are run within a `Task` - so `.wait` can be used. They should ideally not spawn any other `Task` or `Tdef` so that all loading operations run sequentially and the order of operations is maintained.

The name of the lifecycle can be complemented by a domain using a following underscore. In case this domain is ignored using startup options the file will not be executed (e.g: `on_init_visuals.scd` or `on_init_visuals_blabla.scd` would not be executed if `ignoredDomains` contains `\visuals`).

A list of lifecycle phases can be found below.

### Family Definition Scripts
All other files are considerd family definition scripts. Their file names carry no further meaning (except that they are executed in alphanumerical order). Family definitions are explained below.

### Order of Loading Operations

## Family, Context, Command and Block Lifecycle Functions
Defining functions of the various stages in the lifecycle of a family, context, command and block is bringing Life Codes to life!

Currently only method and properties that are useful for defining functionality are mentioned here. Full documentation is still pending ...

### LCFamilyDef and Domains

**TODO**

### Family Lifecycle Functions

#### `family` methods and properties
* Object of class `LCFamily`
* `.id`: The id of the family as `Symbol`
* `.data`: Dictionary to store any data from within lifecycle functions
* `.table`: Holds all properties and lifecycle functions - use at own risk :)

#### `on_init: {|family| ...}`
Is called after the server is booted and the family/block index is generated. It is a good spot to load SynthDefs.

#### `on_load: {|family| ...}`
Is called when the family is first used in a context. It is either called explicitly with `.load` on a context or is called before the first command is executed.

#### `on_unload: {|family| ...}`
Is called when the family is not used in any context anymore.

*Please note that on_load and on_unload currently don't have a well defined behaviour in case these functions are inherited ...*

### Context Lifecycle Functions

#### `ctx` methods and properties
* Object of class `LCContext`
* `.id` and `.data`: see `family`
* `.audioChain`: object to add audio effects to the ctx
* `.updateData {|data, executeFunctions=true| ... }`: update context data and also call all `on_ctx_data_update` functions. `data` should be a Dictionary/Event and only keys that are present in `data` are updated.

#### `on_ctx_create: {|ctx, family| ...}`
Is called when a execution context (e.g. an `LCdef`) is created.

#### `on_ctx_data_update: {|data, ctx, family| ...}`
Is called when context data is changed externally. This can happen either via the OSC interface or the `.updateData` method of an `LCContext`. In case the context data was changed directly via accessing `ctx.data` this function will not be called.

The `data` arguments holds a Dictionary of updated values.

#### `on_ctx_clear: {|ctx, family| ...}`
Is called when a execution context (e.g. an `LCdef`) is cleared.

### Command Lifecycle Functions

#### `cmd` methods and properties
* Object of class `LCCommand`
* `.id` and `.data`: see `family`
* `.audioChain`: object to add audio effects to a command audio chain
* `.blockList`: list of all blocks in the command (represented by `BlockListInstance`s)
* `.pattern`: for families that are of a `\pattern` type this is the `Pbind` that is built up by the blocks and modifiers. Use `cmd.pattern.extend(Pbind(...))` to add pattern values. You can retrieve pattern values (to modify them) by indexing the pattern, e.g. `cmd.pattern[\dur]`
* `.doPerform`: a boolean that specifies if the command should 'play' or not. Usually a action block (e.g. `[play]`) will set doPerform to `true`.


#### *`on_cmd_rush: {|cmd, ctx, family| ...}`*
*Not yet implmeneted.* Called when a scene is rushed.

#### *`on_cmd_enter: {|cmd, ctx, family| ...}`*
*Not yet implmeneted.* Called before any blocks are evaluated.

#### *`on_cmd_finish: {|cmd, ctx, family| ...}`*
*Not yet implmeneted.* Called aftert all blocks are evaluated - right before the command is performed.

#### *`on_cmd_perform: {|cmd, ctx, family| ...}`*
*Not yet implmeneted.* Called when a command is performed.

#### *`on_cmd_leave: {|cmd, ctx, family| ...}`*
*Not yet implmeneted.* Called when a command retires (is replaced by a new command).

#### `on_pattern_finish: {|event, cmd, ctx, family| ...}`
Is called for event type families for every event before it is played. `event` holds all values that were generated from the pattern chain of the command.

### Block Lifecycle Functions

The order of functions as it is presented here is also representing the order of execution.

#### `block` methods and properties
* Object of class `LCBlockInstance`
* `id` and `data`: see `family` - but be aware that `id` can be nil if not explicitly specified in the command.
* `args`: a dictionary that holds all arguments of the block. If an argument is not specified it will contain its default value.

#### *`on_rush: {|block, cmd, ctx, family| ...}`*
*Not yet implmeneted.* Called when a scene is rushed.

#### `on_leave: {|block, cmd, ctx, family| ...}`
Called when a block is added to the context. Be aware that `on_enter` and `on_leave` will not receive the same `block` argument, therefore all state must be saved within `ctx.data`.

#### `on_enter: {|block, cmd, ctx, family| ...}`
Called when a block is added to the context. It will only be called once, even if 2 blocks with the same name are present.

#### `on_pre_execute: {|block, cmd, ctx, family| ...}`
Executes before `on_execute` - use in case something must happen before everything else.

#### `on_execute_once: {|block, cmd, ctx, family| ...}`
Executes only for the first time a block with this name is active within a context. A context can be reset using the `.resetBlockHistory` method (e.g. `LCdef(\bla).resetBlockHistory;`)

#### `on_execute: {|block, cmd, ctx, family| ...}`
This is the general place to modify a pattern, the audio chain or any other aspect of the current command. Use `on_pre_execute` and `on_post_execute` only as exceptions.

#### `on_post_execute: {|block, cmd, ctx, family| ...}`
Executes after `on_execute` - use in case something must happen after everything else.

#### `on_perform_once: {|block, cmd, ctx, family| ...}`
Executes only for the first time a block with this name is performed (e.g. `cmd.doPerform` was set to `true`)  within a context. If the family uses `quant` its execution will be delayed. The pattern or audio chain of the current command should not be modified anymore at this point.

#### `on_perform: {|block, cmd, ctx, family| ...}`
Executes if the command is performed (e.g. `cmd.doPerform` was set to `true`). If the family uses `quant` its execution will be delayed. The pattern or audio chain of the current command should not be modified anymore at this point.

#### `on_ctx_data_update: {|data, block, cmd, ctx, family| ...}`
Please refer to the function with the same name above. The only difference is, that a block and command reference is given as arguments as well.

## Playing with LifeCodes / Executing Blocks

**TODO**

## Class Overview
This is a (potentially) incomplete list of all classes currently used with some brief remarks. Italic classes are considerend to be currently irrelevant to understand and not needed for defining block functionality and trying things out.

Full documentation would be great but there is no time for that currently I fear ...

### LifeCodes
* The main class that houses all functionality. Instantiating creates a singleton object (accessible via `LifeCodes.instance` or `l`).
* `LifeCodes.clear` will clear the instance. 
* Subcomponents can be accessed via `l.runtime`, `l.gui`, `l.mixer`, `l.sceneManager`.
* Options can be accessed via `l.options`.
* A TempoClock that stays at 1 BPS can be accessed via `l.steadyClock`.
* `l.buffers` contains all buffers in a dictionary structure (folder/file name as keys).
* `l.bufferLists` contains all folders with audio samples as arrays of buffers.

### LCdef
* Easy way on how to reference a `LCContext`, e.g. `LCdef(\test)`.
* Can initialize an LCContext with a Family, e.g. `LCdef(\test, \pling)`.
* Can set a new command in form of a block list, e.g. `LCdef(\test, [\pling, \play])`.

### LCContext
* Provides context in which commands are executed in - it is generally accessed via `LCdef`.
* Contexts are initialized with one family and can then not switch to another family without being cleared (e.g. `LCdef(\test).clear`).
* `on_load` family lifecycle function can be triggered explicity: `LCdef(\test).load`.
* Update the `.data` property throught `.updateData` to trigger `on_ctx_data_update` functions, (e.g. `LCdef(\test).updateData((bla: 123))`).
* Clear the block history in order to re-evaluate `_once` functions with `.clearBlockHistory` (e.g. `LCdef(\test).clearBlockHistory`).
* You can access the current command and the last command via `.cmd` and `.lastCmd`.
* You can add modifiers to always be automatically added either at the end of the modifier chain or at the beginning (e.g. `LCdef(\test).appendModifiers = [\reverb, [\faster, 2]]` or `LCdef(\test).prependModifiers = [[\euclid, 3, 8]]`.
* `.audioChain` can be used to add effect nodes or to do other manipulations to the audio chain.

### LCCommand
* Theyr are mostly manipulated by lifecycle functions (look above for more infos)
* Commands are generally generated by executing a block source list via an `LCdef`, e.g. `LCdef(\test, [\pling, \play])`.
* `.blockSourceList` is the original source for the command (use `.blockList` if you want to actually use data from it as it is easier).

### LCBlockInstance
* They are mostly manipulated by lifecycle functions (look above for more infos)
* `.name` will return the name of the block as a `Symbol`
* `.spec`: returns the `LCBlockSpec` object that defines this block
* `.source` will return the source for this block while `.cleanSource` will return a normalized Dictionary with all data encoded.

### LCFamliyDef
* Used to define families (and blocks). See **LCFamilyDef and Domains**.

### LCFamily
* See **LCFamilyDef and Domains** as well as **`family` methods and properties**.

## More Classes

*I would love to document them all but I will do it later as I will most likely only use them myself. So I only give brief words of what they are doing.*

### LCGUI
* Handles all OSC communication and interfaces with the interaction layer. Used to spawn and despawn blocks.

### LCBlockSlotRef
* Holds a reference (and some info) of a spawned block. Mostly use to keep track of things and to automatically despawn.

### LCSceneDef
* Defines and stores scene functions.

### LCSceneManager
* Controls (conditional) scene transitions (from scene to scene and within scenes).

### LCAudioMixer
* The root audio chain where all context audio chains will be mixed into.

### LCContextAudioChain
* The audio chain for each context.

### LCCommandAudioChain
* The audio chain for each command (which will be automatically freed when a new command takes over). In the future there might be also a fade out but currently not needed.

### LCBlockSpec
* Holds all meta information for a block which will be exported as JSON to the interaction layer.

### LCParameterSpec
* Holds all information on block parameters which will also be exported as JSON to the interaction layer.

### LCJSONExport
* Class to export block meta data as JSON. The file also includes class extension of SuperCollider base types in order to build JSON strings out of Dictionaries.

### LCExecutionQueue
* Executes all the lifecycle functions. Currently this is not very spectacular but if/when temporal functions are introduced this will make more sense to have it.

### LCBlockFunctionReference
* Function with reference to its family and domain.

### LCExecutionUnit
* Function bound to values with some additional meta data to be executed in the execution queue.

### LCRuntime
* Manages contexts, blocks and whatnot.