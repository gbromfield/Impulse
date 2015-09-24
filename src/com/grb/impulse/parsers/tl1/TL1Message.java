package com.grb.impulse.parsers.tl1;

import java.nio.ByteBuffer;

public interface TL1Message {
	public boolean parse(ByteBuffer readBuffer);
}
