// make entries of a Pbind easily retrievable and add
// merge method to merge pbinds (similary to Pbindf but
// as things are copied over they can be access at any point

+ Pbind {
	at {|key|
		^Dictionary.newFrom(this.patternpairs)[key];
	}

	combine {|source|

		var destinationDict = Dictionary.newFrom(this.patternpairs);
		var sourceDict = Dictionary.newFrom(source.patternpairs);

		sourceDict.keys.do {|key|
			destinationDict[key] = sourceDict[key]
		};
		this.patternpairs = destinationDict.getPairs;
		^this;
	}
}