
+ Nil {
	jsonString {
		^"null"
	}
}

+ Boolean {
	jsonString {
		^this.asString;
	}
}

+ SimpleNumber {
	jsonString {
		^this.asString;
	}
}

+ String {
	jsonString {
		^"\"%\"".format(this);
	}
}

+ Symbol {
	jsonString {
		^"\"%\"".format(this);
	}
}

+ Point {
	jsonString {
		^(\x: this.x, \y: this.y).jsonString;
	}
}

+ SequenceableCollection {
	jsonString {|indent=1|
		var ret = "[";
		var i = 0;
		this.do {|item|
			(i > 0).if {
				ret = ret ++ ",";
			};
			ret = ret;
			indent.do {ret = ret;};
			ret = ret ++ item.jsonString(indent + 1);
			i = i + 1;
		};
		^(ret ++ "]");
	}
}

+ Dictionary {
	jsonString {|indent=1|
		var ret = "{";
		var i = 0;
		this.keys.do {|key|
			(i > 0).if {
				ret = ret ++ ",";
			};
			ret = ret;
			indent.do {ret = ret;};
			ret = ret ++ ("\"%\":%").format(key, this[key].jsonString(indent + 1));
			i = i + 1;
		};
		^(ret ++ "}");
	}
}