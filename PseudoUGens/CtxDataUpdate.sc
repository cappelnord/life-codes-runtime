CtxDataUpdate {
	*kr {|dataUpdateId, freq, values|
		^SendReply.kr(Impulse.kr(freq), '/lc/audio/updateCtxData', [dataUpdateId] ++ values);
	}
}