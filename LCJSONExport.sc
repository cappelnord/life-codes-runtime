LCJSONExport {
	*write {|path, runtime|
		var jsonObject = LCJSONExport.generate(runtime);
		var jsonString = jsonObject.jsonString;
		"\n*** EXPORTED SPECS TO % ***".format(path).postln;

		jsonString.postln;
	}

	*generate {|runtime|
		var ret = (
			\familySpecs: LCJSONExport.generateFamilySpecs(runtime),
			\blockSpecs: ()
		);
		^ret;
	}

	*generateFamilySpecs {|runtime|
		var ret = ();
		runtime.specs.do {|spec|
			ret[spec.id] = LCJSONExport.specContent(spec);
		};
		^ret;
	}

	*generateBlockSpecs {|runtime|
		^();
	}

	*specContent {|spec|
		var color = Color.white;

		spec.table.color.isNil.not.if {
			color = spec.table.color;
		};

		^(
			\id: spec.id,
			\color: (\red: color.red, \green: color.green, \blue: color.blue)
		)
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

+ SequenceableCollection {
	jsonString {|indent=1|
		var ret = "[";
		var i = 0;
		this.do {|item|
			(i > 0).if {
				ret = ret ++ ", ";
			};
			ret = ret ++ "\n";
			indent.do {ret = ret + "\t";};
			ret = ret ++ item.jsonString(indent + 1);
			i = i + 1;
		};
		ret = ret ++ "\n";
		(indent-1).do {ret = ret + "\t";};
		^(ret ++ "]\n");
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
			ret = ret ++ "\n";
			indent.do {ret = ret + "\t";};
			ret = ret ++ ("\"%\": %").format(key, this[key].jsonString(indent + 1));
			i = i + 1;
		};
		ret = ret ++ "\n";
		(indent-1).do {ret = ret + "\t";};
		^(ret ++ "}");
	}
}