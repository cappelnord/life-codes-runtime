// tested up to 3 million node id's:
// The installation can run for multiple days!

(
SynthDef(\nop, {
	var sig = Line.ar(0, 1, 0.1, doneAction: 2) * DC.ar(0);
	Out.ar(0, 0);
}).add;
)

(
fork {
	var lastID = 0;
	var id = 1;
	{lastID < id}.while {
		var x = Synth(\nop);
		lastID = id;
		id = x.nodeID;
		id.postln;
		0.0001.wait;
	}
}
)