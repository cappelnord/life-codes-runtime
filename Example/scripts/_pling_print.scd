/*
As this file starts with _ it is ignored! Remove the underscore to add a
\print domain to pling which will post "pling!" on each pling ...
*/

LCFamilyDef(\pling, \print, (
		on_pattern_finish: {|event, cmd, ctx, family|
			"pling!".postln;
		}
	)
);