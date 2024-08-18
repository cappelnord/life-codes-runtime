(

var net = NetAddr("127.0.0.1", 57140);
var fps = 60;
var pressReleaseProbability = 0.05;

fork {
	inf.do {|i|
		var numFrames = 1000 + 1000.rand;
		var id = (i % 500).asString;
		var pressed = false;
		var startPosition = [5000.rand, 1000.rand];
		net.sendMsg("/lc/cursor/move", id, startPosition[0], startPosition[1]);
		{
			numFrames.do {
				net.sendMsg("/lc/cursor/moveDelta", id, 30.0.rand2, 30.0.rand2);
				pressReleaseProbability.coin.if {
					pressed.if({
						net.sendMsg("/lc/cursor/release", id);
						pressed = false;
					}, {
						net.sendMsg("/lc/cursor/press", id);
						pressed = true;
					});
				};
				(1/fps).wait;
			};
		}.fork;
		(0.2 + 0.4.rand * 0.2).wait;
	}
}
)