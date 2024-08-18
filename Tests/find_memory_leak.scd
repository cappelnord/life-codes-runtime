/*
Block Creation/Despawning test for memory leaks.

Result: Initial memory leak seems to be fixed.

*/

(
var blocks = [\pling, \play, \mute, [\faster, 2]];

{
	inf.do {
		var num = 500;
		var collection = num.collect {[blocks.choose, Point(1.0.rand, 1.0.rand)]};
		l.gui.registerBlockSlots(\ok, collection, (), {0.01.wait});
		1.wait;
		l.gui.despawnBlockSlotsFromRegistry(\ok, (), {0.01.wait});
		1.wait;
	};
}.fork;
)

/*
Do OSC messages leak memory?

Result: Inconclusive - it at least does not seem to be critical.

*/

(
{
	var net = NetAddr("127.0.0.1", 57140);
	inf.do {
		net.sendMsg("\ok", 123, 123, 144124, 123, 123, 123, 123, "abcd");
		0.0001.wait;
	}
}.fork;
)