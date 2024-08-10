LCJSONExport {
	*write {|path, runtime|
		var jsonObject, jsonString, file;

		"\n*** EXPORTING SPECS ***\nPath: %".format(path).postln;

		jsonObject = LCJSONExport.generate(runtime);
		jsonString = jsonObject.jsonString;
		file = File.open(path, "w");
		file.write(jsonString);
		file.close;
	}

	*generate {|runtime|
		var ret = (
			\familySpecs: LCJSONExport.generateFamilySpecs(runtime),
			\blockSpecs: LCJSONExport.generateBlockSpecs(runtime),
			\cornerVertices: LifeCodes.instance.options[\guiXTranslation]
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
			\matches: family.matches,
			\quant: family.quant != nil
		)
	}

	*blockSpecContent {|blockSpec|
		var parameters = List();

		blockSpec.parameters.do {|parameter|
			parameters.add((
				\id: parameter.id,
				\type: parameter.type,
				\default: parameter.default,
				\hide: parameter.hide
			));
		}

		^(
			\id: blockSpec.id,
			\code_string: blockSpec.name,
			\display_string: blockSpec.display,
			\type: blockSpec.type,
			\family: blockSpec.family,
			\parameters: parameters,
			\setsValues: blockSpec.setsValues,
			\modifiesValues: blockSpec.modifiesValues,
			\mutes: blockSpec.mutes
		)
	}
}