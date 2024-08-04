SwitchChannels {
	*ar {|sig, switch=0|
		switch = DC.kr(switch) > 0.5;
		^[
			(sig[0] * (1.0 - switch)) + (sig[1] * switch),
			(sig[1] * (1.0 - switch)) + (sig[0] * switch)
		];
	}
}