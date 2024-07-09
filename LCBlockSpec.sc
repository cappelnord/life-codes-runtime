LCBlockSpec {
	var <name;
	var <family;
	var <entry;

	var <id;
	var <type;
	var <display;
	var <parameters;

	*identifier {|name, family|
		^(family ++ ":" ++ name).asSymbol;
	}

	*new {|name, family, entry|
		^super.newCopyArgs(name, family, entry).init;
	}

	init {

		id = LCBlockSpec.identifier(name, family);
		display = entry[\display] ? name;
		type = entry[\type];

		parameters = [];

		entry[\parameters].do {|parameterEntry|
			parameters = parameters.add(LCParameterSpec(parameterEntry));
		};
	}
}

LCParameterSpec {
	var <id;
	var <type;
	var <default;

	*new {|entry|
		[\number, \integer, \string, \object].includes(entry[\type]).if ({
			^super.newCopyArgs(entry[\id], entry[\type], entry[\default]);
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