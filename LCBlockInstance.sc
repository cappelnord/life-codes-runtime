
LCBlockInstance {
	var <source;
	var <cmd;

	var <id;

	var <cleanSource;
	var <name;
	var <args;
	var <data;
	var <spec;
	var <valid = true;

	*new {|source, cmd|
		^super.newCopyArgs(source, cmd).init;
	}

	init {
		cleanSource = LCBlockInstance.cleanSource(source);

		name = cleanSource[\name];

		// let's see if this block is defined at all in the family hierarchy
		cmd.ctx.family.matchesBlock(name).if ({
			this.prProcessParameters;
		}, {
			"There is no 'primary' block '%' that matches the family '%'".format(name, cmd.ctx.family.id).postln;
			cmd.valid = false;
			valid = false;
		});

		args = cleanSource[\args];
		data = cleanSource[\data];
		id = cleanSource[\id];
	}

	prProcessParameters {
		// we must find/assign the spec here to resolve positional arguments
		spec = cmd.ctx.family.findBlockSpec(cleanSource[\name]);

		cleanSource[\args] = cleanSource[\args] ? ();

		spec.parameters.do {|parameter, i|
			cleanSource[\args][parameter.id].isNil.if {
				(cleanSource[\positionalArgs].size > i).if ({
					cleanSource[\args][parameter.id] = parameter.convert(cleanSource[\positionalArgs][i]);
				}, {
					cleanSource[\args][parameter.id] = parameter.default;
				});
			};
		};

		cleanSource[\data] = cleanSource[\data] ? ();

		cleanSource.removeAt(\positionalArgs);
	}


	*cleanSource {|source|
		var cleanSource = source.deepCopy;

		// decode (bit weird) syntax to send arguments alongside codeblocks from gui layer
		(cleanSource.class == String).if {
			var x = cleanSource.split($,);

			cleanSource = [x[0].asSymbol];
			(x.size > 1).if {
				cleanSource = cleanSource.addAll(LCGUI.decodeOSCValues(x[1..]));
			};
		};

		(cleanSource.class == Symbol).if {
			cleanSource = [cleanSource];
		};

		// if it is a sequenceable collection we build an event here
		cleanSource.isSequenceableCollection.if {
			cleanSource = (
				name: cleanSource[0],
				positionalArgs: cleanSource[1..]
			);
		};

		cleanSource[\name].asString.includes($?).if {
			var tokens = cleanSource[\name].asString.split($?);
			cleanSource[\name] = tokens[0].asSymbol;
			cleanSource[\id] = tokens[1].asSymbol;
		};

		^cleanSource;
	}
}