LCBlockSpec {
	var <name;
	var <family;
	var <entry;

	var <id;
	var <type;
	var <display;

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
	}
}
