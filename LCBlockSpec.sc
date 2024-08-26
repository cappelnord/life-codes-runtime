LCBlockSpec {
	var <name;
	var <family;
	var <entry;

	var <id;
	var <type;
	var <display;
	var <parameters;

	var <setsValues;
	var <modifiesValues;
	var <mutes;
	var <dontDecorate;
	var <invertTextColor;


	*identifier {|name, family|
		^(family ++ ":" ++ name).asSymbol;
	}

	*new {|name, family, entry|
		^super.newCopyArgs(name, family, entry).init;
	}

	init {

		id = LCBlockSpec.identifier(name, family);

		display = LifeCodes.instance.options[\ignoreDisplayNames].if({name}, {entry[\display] ? name});
		type = entry[\type];

		parameters = [];

		entry[\parameters].do {|parameterEntry|
			parameters = parameters.add(LCParameterSpec(parameterEntry));
		};

		setsValues = entry[\setsValues];
		modifiesValues = entry[\modifiesValues];
		mutes = entry[\mutes];
		dontDecorate = entry[\dontDecorate];
		invertTextColor  = entry[\invertTextColor];
	}
}

LCParameterSpec {
	var <id;
	var <type;
	var <default;
	var <hide;

	*new {|entry|
		[\number, \integer, \string, \object].includes(entry[\type]).if ({
			^super.newCopyArgs(entry[\id], entry[\type], entry[\default], entry[\hide]);
		}, {
			// TODO: throw an error
			"Invalid paremter definition: %".format(entry.cs).error;
			^nil;
	    });

	}

	init {
		type.switch(
			\number, {
				default = default ? 0.0;
				default = default.asFloat;
			},
			\integer, {
				default = default ? 0;
				default = default.asInteger;
			},
			\string, {
				default = default ? "";
				default = default.asString;
			},
			\object, {
				default = default ? nil;
				default = default.cs;
			}
		);
		default = this.convert(default);
	}

	convert {|value|
		type.switch(
			\number, {
				^value.asFloat;
			},
			\integer, {
				^value.asInteger;
			},
			\string, {
				^value.asString;
			},
			\object, {
				^value.cs;
			}
		);
	}
}