LCPanAz {
	*azimuthFromGuiX {|x|
		var positions = LifeCodes.instance.options[\guiXTranslation];
		var lowerIndex = nil;
		var pointDistance;
		var targetPoint;

		x = x.min(1).max(0);

		positions.size.do {|i|
			lowerIndex.isNil.if {
				((x >= positions[i][\x]) && (x <= positions[i+1][\x])).if {
					lowerIndex = i;
				};
			};
		};

		// normalized distance between the two vertices
		pointDistance = positions[lowerIndex+1][\x] - positions[lowerIndex][\x];
		pointDistance = (x - positions[lowerIndex][\x]) / pointDistance;

		// interpolate the two vertices
		targetPoint = Point(
			pointDistance.linlin(0, 1, positions[lowerIndex][\position].x, positions[lowerIndex+1][\position].x),
			pointDistance.linlin(0, 1, positions[lowerIndex][\position].y, positions[lowerIndex+1][\position].y),
		);

		// atan2 to the rescue (best friend)
		^(targetPoint.y.atan2(targetPoint.x) * -1);
	}

	// the orientation is 1.5 - then a pos of 0 is azimuth 0

	*ar {|in, pos=0.0, level=1.0, width=2.0|
		pos = pos % 2pi;
		pos = pos.linlin(0, 2pi, 0, 2);
		^PanAz.ar(LifeCodes.instance.options[\numAudioChannels], in, pos, level, width, 1.5);
	}
}
/* above seems to work:

{
	var sig = WhiteNoise.ar * 0.1;
	var multi = LCPanAz.ar(sig, Line.kr(-pi, pi, 60).poll);
	Out.ar(0, Splay.ar(multi));
}.play;

// rotates from back to back, clockwise
*/


LCPanAz2 {
	*ar {|in, pos=0.0, spread=0.78539816339745, level=1.0, width=2.0|
		^(LCPanAz.ar(in[0], pos-spread, level, width) + LCPanAz.ar(in[1], pos+spread, level, width));
	}
}

/*
This now also seems to work:

{
	var sig = Pan2.ar(WhiteNoise.ar, SinOsc.kr(0.1).poll) * 0.1;
	var multi = LCPanAz2.ar(sig, 0, pi/2);
	Out.ar(0, Splay.ar(multi));
}.play;

*/