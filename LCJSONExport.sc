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
			\blockSpecs: LCJSONExport.generateBlockSpecs(runtime)
		);
		^ret;
	}

	*generateFamilySpecs {|runtime|
		var ret = ();
		runtime.families.do {|family|
			ret[family.id] = LCJSONExport.familyContent(family);
		};
		^ret;
	}

	*generateBlockSpecs {|runtime|
		var ret = ();
		runtime.blockSpecs.do {|blockSpec|
			ret[blockSpec.id] = LCJSONExport.blockSpecContent(blockSpec);
		};
		^ret;
	}

	*familyContent {|family|
		var color = Color.white;

		family.table.color.isNil.not.if {
			color = family.table.color;
		};

		^(
			\id: family.id,
			\color: (\red: color.red, \green: color.green, \blue: color.blue),
			\matches: family.matches
		)
	}

	*blockSpecContent {|blockSpec|
		^(
			\id: blockSpec.id,
			\type: blockSpec.type,
			\name: blockSpec.name,
			\family: blockSpec.family,
			\display: blockSpec.display
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