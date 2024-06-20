LCSpec(\dump, \meta, {|spec|
	spec.define((
		\type: \pattern,
		\color: Color.green,
		\quant: 2,

		// TODO: how to express compatibility

		\on_init: {|spec|
			"on_init meta".postln;
		},

		\on_load: {|spec|
			"on_load meta".postln;
		},

		\on_ctx_create: {|ctx, spec|
			"on_ctx_create".postln;
		},

		\on_ctx_data_change: {|ctx, spec|
			"on_ctx_data_changed".postln;
		},

		\on_cmd_rush: {|cmd, ctx, spec|
			"on_cmd_rush".postln;
		},

		\on_cmd_leave: {|cmd, ctx, spec|
			"on_cmd_leave".postln;
		},

		\on_cmd_chain_head: {|cmd, ctx, spec|
			"on_cmd_chain_head".postln;
		},

		\on_cmd_chain_tail: {|cmd, ctx, spec|
			"on_cmd_chain_tail".postln;
		},

		\on_ctx_clear: {|ctx, spec|
			"on_ctx_clear".postln;
		},

		\on_unload: {|ctx, spec|
			"on_unload".postln;
		},

		\on_pattern_finish: {|event, cmd, ctx, spec|
			"on_pattern_finish".postln;
		},
	));

	spec.defineBlock(\dump, (
		\type: \subject,
		\display: "DumpsterDump",

		\on_rush: {|block, cmd, ctx, spec|
			"dump on_rush".postln;
		},

		\on_leave: {|block, cmd, ctx, spec|
			"dump on_leave".postln;
		},

		\on_ctx_data_change: {|block, cmd, ctx, spec|
			"dump on_ctx_data_changed".postln;
		},

		\on_enter: {|block, cmd, ctx, spec|
			"dump on_enter".postln;
		},

		\on_pre_execute: {|block, cmd, ctx, spec|
			"dump on_pre_execute".postln;
		},

		\on_once: {|block, cmd, ctx, spec|
			"dump on_once".postln;
		},

		\on_execute: {|block, cmd, ctx, spec|
			"dump on_execute".postln;
		},

		\on_quant_once: {|block, cmd, ctx, spec|
			"dump on_quant_once".postln;
		},

		\on_quant_execute: {|block, cmd, ctx, spec|
			"on_quant_execute".postln;
		},

		\on_post_execute: {|block, cmd, ctx, spec|
			"on_post_execute".postln;
		}
	));

	spec.defineBlock(\print, (
		\type: \action,

		\on_execute: {|block, cmd, ctx, spec|
			"print on_execute".postln;
		}
	));

	nil;
});