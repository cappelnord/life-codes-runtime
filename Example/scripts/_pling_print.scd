/*
As this file starts with _ it is ignored! Remove the underscore to add a
\print domain to pling which will post "pling!" on each pling ...
*/

// The keen observer will notice that we can also just define a domain
// using an event.

LCFamilyDef(\pling, \print, (
	    // the on_pattern_finish function will be called for each event
		on_pattern_finish: {|event, cmd, ctx, family|
			"pling!".postln;
		}
	)
);