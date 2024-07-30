
# Life Codes Quick Manual

## Prerequisites
### Binaural Room Simulation
In order to use the `binaural` output mode the [Ambisonics Toolkit (ATK)](https://github.com/ambisonictoolkit/atk-sc3/blob/master/README.md#installing) must be installed. This includes the Quark, sc3-plugins as well as the Atk kernels and matrices.

## Terminology
Here is a quick overview of terms and what they mean in the context of the Life Codes runtime.

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
...

### Family Lifecycle Functions

#### `family` methods and properties
* `id`: The id of the family as `Symbol`
* `data`: Dictionary to store any data from within lifecycle functions
* `table`: Holds all properties and lifecycle functions - use at own risk :)

#### `on_init: {|family| ...}`
Is called after the server is booted and the family/block index is generated. It is a good spot to load SynthDefs.

#### `on_load: {|family| ...}`
Is called when the family is first used in a context. It is either called explicitly with `.load` on a context or is called before the first command is executed.

#### `on_unload: {|family| ...}`
Is called when the family is not used in any context anymore.

*Please note that on_load and on_unload currently don't have a well defined behaviour in case these functions are inherited ...*

### Context Lifecycle Functions

#### `ctx` methods and properties
* `id` and `data`: see `family`
* `audioChain`: object to add audio effects to the ctx
* `updateData {|data, executeFunctions=true| ... }`: update context data and also call all `on_ctx_data_update` functions. `data` should be a Dictionary/Event and only keys that are present in `data` are updated.

#### `on_ctx_create: {|ctx, family| ...}`
Is called when a execution context (e.g. an `LCdef`) is created.

#### `on_ctx_data_update: {|data, ctx, family| ...}`
Is called when context data is changed externally. This can happen either via the OSC interface or the `updateData` method. In case the context data was changed directly via accessing `ctx.data` this function will not be called.

The `data` arguments holds a Dictionary of updated values.

#### `on_ctx_clear: {|ctx, family| ...}`
Is called when a execution context (e.g. an `LCdef`) is cleared.

### Command Lifecycle Functions

#### `cmd` methods and properties
...

#### `on_cmd_rush: {|cmd, ctx, family| ...}`
*Not yet implmeneted.* Called when a scene is rushed.

#### `on_cmd_enter: {|cmd, ctx, family| ...}`
*Not yet implmeneted.* Called before any blocks are evaluated.

#### `on_cmd_finish: {|cmd, ctx, family| ...}`
*Not yet implmeneted.* Called aftert all blocks are evaluated - right before the command is performed.

#### `on_cmd_leave: {|cmd, ctx, family| ...}`
*Not yet implmeneted.* Called when a command retires (is replaced by a new command).

#### `on_pattern_finish: {|event, cmd, ctx, family| ...}`
Is called for event type families for every event before it is played. `event` holds all values that were generated from the pattern chain of the command.

### Block Lifecycle Functions



FamilyDef: domain, lc
Family: family
Ctx: ctx, family
Cmd: cmd, ctx, family
Block: block, cmd, ctx, family
Data: data, ctx, family or data, block, cmd, ctx, family
pattern: event, cmd, ctx, family


## Class Overview
This is a (potentially) incomplete list of all classes currently used. Italic classes are considerend to be relevant only internally and are currently not further documented.

#### LifeCodes
#### LCdef
#### LCContext
#### LCBlockInstance
#### *LCBlockSpec*
#### *LCParameterSpec*
#### *LCExecutionQueue*
#### *LCExecutionUnit*
#### *LCBlockFunctionReference*
#### LCGUI
#### LCBlockSlotRef
#### *LCJSONExport*
#### LCRuntime
#### LCSceneDef
#### LCSceneManager
#### LCFamliyDef
#### LCFamily