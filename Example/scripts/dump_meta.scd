LCSpec(\dump, \meta, {|spec|
	spec.define((
		\type: \pattern,
		\color: Color.green,
		\quant: 2,

		\on_init: {|spec|
			"on_init meta".postln;
		},

		\on_load: {|spec|
			"on_load meta".postln;
		},

		\on_ctx_create: {|ctx, family, spec|
			"on_ctx_create".postln;
		},

		\on_ctx_data_change: {|ctx, family, spec|
			"on_ctx_data_changed".postln;
		},

		\on_cmd_rush: {|cmd, ctx, family, spec|
			"on_cmd_rush".postln;
		},

		\on_cmd_leave: {|cmd, ctx, family, spec|
			"on_cmd_leave".postln;
		},

		\on_cmd_chain_head: {|cmd, ctx, family, spec|
			"on_cmd_chain_head".postln;
		},

		\on_cmd_chain_tail: {|cmd, ctx, family, spec|
			"on_cmd_chain_tail".postln;
		},

		\on_ctx_clear: {|ctx, family, spec|
			"on_ctx_clear".postln;
		},

		\on_unload: {|ctx, family, spec|
			"on_unload".postln;
		},

		\on_pattern_finish: {|event, cmd, ctx, family, spec|
			"on_pattern_finish".postln;
		},
	));

	spec.defineBlock(\dump, (
		\primary: true,
		\type: \subject,
		\display: "DumpsterDump",
		\parameters: [
			(\id: \times, \type: \integer, \default: 2),
			(\id: \prefix, \type: \string, \default: "JO"),
			(\id: \chance, \type: \number, \default: 1.0)
		],

		\on_rush: {|block, cmd, ctx, family, spec|
			"dump on_rush".postln;
		},

		\on_leave: {|block, cmd, ctx, family, spec|
			"dump on_leave".postln;
		},

		\on_ctx_data_change: {|block, cmd, ctx, family, spec|
			"dump on_ctx_data_changed".postln;
		},

		\on_enter: {|block, cmd, ctx, family, spec|
			"dump on_enter".postln;
		},

		\on_pre_execute: {|block, cmd, ctx, family, spec|
			"dump on_pre_execute".postln;
		},

		\on_once: {|block, cmd, ctx, family, spec|
			"dump on_once".postln;
		},

		\on_execute: {|block, cmd, ctx, family, spec|
			"dump on_execute".postln;
		},

		\on_quant_once: {|block, cmd, ctx, family, spec|
			"dump on_quant_once".postln;
		},

		\on_quant: {|block, cmd, ctx, family, spec|
			"on_quant".postln;
		},

		\on_post_execute: {|block, cmd, ctx, family, spec|
			"on_post_execute".postln;
		}
	));

	spec.defineBlock(\print, (
		\type: \action,
		\primary: true,

		\on_execute: {|block, cmd, ctx, family, spec|
			"print on_execute".postln;
		}
	));

	nil;
});