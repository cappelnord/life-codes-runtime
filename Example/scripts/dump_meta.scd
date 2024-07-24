LCFamilyDef(\dump, \meta, {|domain|
	domain.define((
		color: Color.green,
		quant: 2,

		on_init: {|family|
			"on_init meta".postln;
		},

		on_load: {|family|
			"on_load meta".postln;
		},

		on_ctx_create: {|ctx, family, domain|
			"on_ctx_create".postln;
		},

		on_ctx_data_update: {|data, ctx, family, domain|
			"CTX on_ctx_data_update".postln;
			data.postln;
		},

		on_cmd_rush: {|cmd, ctx, family, domain|
			"on_cmd_rush".postln;
		},

		on_cmd_leave: {|cmd, ctx, family, domain|
			"on_cmd_leave".postln;
		},

		on_cmd_chain_head: {|cmd, ctx, family, domain|
			"on_cmd_chain_head".postln;
		},

		on_cmd_chain_tail: {|cmd, ctx, family, domain|
			"on_cmd_chain_tail".postln;
		},

		on_ctx_clear: {|ctx, family, domain|
			"on_ctx_clear".postln;
		},

		on_unload: {|ctx, family, domain|
			"on_unload".postln;
		}
	));

	domain.defineBlock(\dump, (
		primary: true,
		type: \subject,
		display: "DumpsterDump",
		parameters: [
			(id: \times,  type: \integer, default: 2),
			(id: \prefix, type: \string,  default: "JO"),
			(id: \chance, type: \number,  default: 1.0)
		],

		on_rush: {|block, cmd, ctx, family, domain|
			"dump on_rush".postln;
		},

		on_leave: {|block, cmd, ctx, family, domain|
			"dump on_leave".postln;
		},

		on_ctx_data_update: {|data, block, cmd, ctx, family, domain|
			"dump on_ctx_data_update".postln;
			data.postln;
		},

		on_enter: {|block, cmd, ctx, family, domain|
			"dump on_enter".postln;
		},

		on_pre_execute: {|block, cmd, ctx, family, domain|
			"dump on_pre_execute".postln;
		},

		on_once: {|block, cmd, ctx, family, domain|
			"dump on_once".postln;
		},

		on_execute: {|block, cmd, ctx, family, domain|
			"dump on_execute".postln;
		},

		on_quant_once: {|block, cmd, ctx, family, domain|
			"dump on_quant_once".postln;
		},

		on_quant: {|block, cmd, ctx, family, domain|
			"on_quant".postln;
		},

		on_post_execute: {|block, cmd, ctx, family, domain|
			"on_post_execute".postln;
		}
	));

	domain.defineBlock(\print, (
		type: \action,
		primary: true,

		on_execute: {|block, cmd, ctx, family, domain|
			"print on_execute".postln;
		}
	));
});