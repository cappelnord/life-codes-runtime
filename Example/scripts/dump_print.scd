LCFamilyDef(\dump, \print, {|domain, l|
	domain.define((
		on_init: {|family|
			"on_init meta".postln;
		},

		on_load: {|family|
			"on_load meta".postln;
		},

		on_ctx_create: {|ctx, family|
			"on_ctx_create".postln;
		},

		on_ctx_data_update: {|data, ctx, family|
			"CTX on_ctx_data_update".postln;
			data.postln;
		},

		on_cmd_rush: {|cmd, ctx, family|
			"on_cmd_rush".postln;
		},

		on_cmd_leave: {|cmd, ctx, family|
			"on_cmd_leave".postln;
		},

		on_cmd_chain_head: {|cmd, ctx, family|
			"on_cmd_chain_head".postln;
		},

		on_cmd_chain_tail: {|cmd, ctx, family|
			"on_cmd_chain_tail".postln;
		},

		on_ctx_clear: {|ctx, family|
			"on_ctx_clear".postln;
		},

		on_unload: {|family|
			"on_unload".postln;
		}
	));

	domain.defineBlock(\dump, (
		on_rush: {|block, cmd, ctx, family|
			"dump on_rush".postln;
		},

		on_ctx_data_update: {|data, block, cmd, ctx, family|
			"dump on_ctx_data_update".postln;
			data.postln;
		},

		on_pre_execute: {|block, cmd, ctx, family|
			"dump on_pre_execute".postln;
		},

		on_execute_once: {|block, cmd, ctx, family|
			"dump on_execute_once:".postln;
		},

		on_execute: {|block, cmd, ctx, family|
			"dump on_execute".postln;
		},

		on_perform_once: {|block, cmd, ctx, family|
			"dump on_perform_once:".postln;
		},

		on_perform: {|block, cmd, ctx, family|
			"on_perform".postln;
		},

		on_post_execute: {|block, cmd, ctx, family|
			"on_post_execute".postln;
		}
	));

	domain.defineBlock(\print, (

		on_execute: {|block, cmd, ctx, family|
			"print on_execute".postln;
			cmd.doPerform = true;
		},

		on_leave: {|block, cmd, ctx, family|
			"dump on_leave".postln;
		},

		on_enter: {|block, cmd, ctx, family|
			"dump on_enter".postln;
		}
	));
});