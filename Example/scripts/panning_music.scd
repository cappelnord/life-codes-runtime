LCFamilyDef(\panning, \music, {|domain, lc|
	domain.define((
		on_init: {
			SynthDef(\xPosPan, {|bus=0, azimuth=0, width=2.0|
				var sig = In.ar(bus, lc.mixer.numChannels);
				sig = LCPanAz.ar(sig[0], azimuth, width: width);
				ReplaceOut.ar(bus, sig);
			}).add;

			SynthDef(\xPosPan2, {|bus=0, azimuth=0, spread=0.7, width=2.0|
				var sig = In.ar(bus, lc.mixer.numChannels);
				sig = LCPanAz2.ar([sig[0], sig[1]], azimuth, spread, width: width);
				ReplaceOut.ar(bus, sig);
			}).add;
		}
	));

	domain.defineBlock(\xPosPan, (
		on_enter: {|block, cmd, ctx, family|
			ctx.audioChain.addFxNode(\xPosPan, (
				width: block.args[\width],
				azimuth: LCPanAz.azimuthFromGuiX(ctx.data[\headPosX] ? 0.5)
			), \xPosPan);
		},
		on_leave: {|block, cmd, ctx, family|
			ctx.audioChain.freeFxNode(\xPosPan);
		},
		on_ctx_data_update: {|data, block, cmd, ctx, family|
			data[\headPosX].isNil.not.if {
				ctx.audioChain.fxNodes[\xPosPan].set(\azimuth, LCPanAz.azimuthFromGuiX(data[\headPosX]));
			};
		}
	));

	domain.defineBlock(\xPosPan2, (
		on_enter: {|block, cmd, ctx, family|
			ctx.audioChain.addFxNode(\xPosPan2, (
				width: block.args[\width],
				spread: block.args[\spread],
				azimuth: LCPanAz.azimuthFromGuiX(ctx.data[\headPosX] ? 0.5)
			), \xPosPan2);
		},
		on_leave: {|block, cmd, ctx, family|
			ctx.audioChain.freeFxNode(\xPosPan2);
		},
		on_ctx_data_update: {|data, block, cmd, ctx, family|
			data[\headPosX].isNil.not.if {
				ctx.audioChain.fxNodes[\xPosPan2].set(\azimuth, LCPanAz.azimuthFromGuiX(data[\headPosX]));
			};
		}
	));
});