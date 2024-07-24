
# Life Codes Quick Manual

## Terminology
Here is a quick overview of terms and what they mean in the context of the Life Codes runtime.

## Startup and Loading

### Startup
A startup script will likely be called on installation startup which will start the loading process and specify options. Please check [Example/startup.scd](Example/start.scd?plain=1) as a guiding point. Options are currently not yet documented but all options can be found in [LifeCodes.sc](LifeCodes.sc) with hopefully meaningful names.

### Scripts Folder
All content code is organized in a folder specified by `scriptsPath` in the startup script. Code files can be organized in folders as it seems sensible - they will all be treated equally. File names matter though:

* File names that start with an underscore (`_`) will be ignored, e.g. to temporarily ignore content.
* File names that start with `on_` are executed at a specific point in the lifecycle of the runtime.
* All other files should contain only family definitions to specify blocks and their behaviour.

Within their execution scope all files are executed in alphanumerical order (independent in which folder they are).

### Lifecycle Scripts
File names starting with `on_`. All scripts are run within a `Task` - so `.wait` can be used. They should ideally not spawn any other `Task` or `Tdef` so that all loading operations run sequentially and the order of operations is maintained.

### Order of Loading Operations


## Class Overview
This is a (potentially) incomplete list of all classes currently used. Italic classes are considerend to be relevant only internally and are currently not further documented.

### LifeCodes
### LCdef
### LCContext
### LCBlockInstance
### *LCBlockSpec*
### *LCParameterSpec*
### *LCExecutionQueue*
### *LCExecutionUnit*
### *LCBlockFunctionReference*
### LCGUI
### LCBlockSlotRef
### *LCJSONExport*
### LCRuntime
### LCSceneDef
### LCSceneManager
### LCFamliyDef
### LCFamily