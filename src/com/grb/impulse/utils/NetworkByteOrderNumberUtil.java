/*
 * NetworkByteOrderNumberUtil.java
 * $Id$
 * 
 * Copyright 2006 Solace Systems, Inc.  All rights reserved.
 */
package com.grb.impulse.utils;

/**
 * Convert numbers to/from unsigned integers in network byte order.
 * @author jdaigle
 *
 */
public class NetworkByteOrderNumberUtil {

	private static final String ERR_ARRAY_OVERFLOW = "Array Overflow (len=%s, index=%s)";
	private static final String ERR_VALUE_OOB = "Value out of bounds (%s)";
	private static final String ERR_VALUE_INVALID = "Invalid input value (%s)";
	private static final String ERR_VALUE_NEGATIVE = "Invalid negative value (%s)";

	// unsigned numbers
	public static final int MAX_8BITS = 255;
	public static final int MAX_16BITS = 65535;
	public static final int MAX_24BITS = 16777215;
	public static final long MAX_32BITS = 4294967295L;

	public static byte[] intToEightByte(long value) {
		byte[] buf = new byte[8];
		intToEightByte(value, buf, 0);
		return buf;
	}
	
	public static void intToEightByte(long value, byte[] dest, int off) {
		long val1 = (value & 0xFFFFFFFF00000000L) >>>32;
		long val2 = (value & 0x00000000FFFFFFFFL);
		intToFourByte(val1, dest, 0 + off);
		intToFourByte(val2, dest, 4 + off);
	}
	
	public static long eightByteToUInt (byte[] value, int offset) {
		if ((offset + 8) > value.length)
			throw new IllegalArgumentException(String.format(ERR_ARRAY_OVERFLOW, value.length,
				offset));

		byte[] b1 = new byte[4];
		byte[] b2 = new byte[4];
		b1[0] = value[offset];
		b1[1] = value[offset+1];
		b1[2] = value[offset+2];
		b1[3] = value[offset+3];
		b2[0] = value[offset+4];
		b2[1] = value[offset+5];
		b2[2] = value[offset+6];
		b2[3] = value[offset+7];
		long uint1 = (long)NetworkByteOrderNumberUtil.fourByteToUInt(b1);
		long uint2 = (long)NetworkByteOrderNumberUtil.fourByteToUInt(b2);
		long uint = (uint1 <<32 | uint2);
		return uint;
	}
	
    public static long eightByteToUInt (byte[] value) {
        return eightByteToUInt(value, 0);
    }
    
	public static byte[] intToFourByte(long value) {
		byte[] buf = new byte[4];
		intToFourByte(value, buf, 0);
		return buf;
	}
	
	public static void intToFourByte(long value, byte[] dest, int off) {
		if ((value < 0) || (value > MAX_32BITS))
			throw new IllegalArgumentException(String.format(ERR_VALUE_OOB, value));

		dest[off + 0] = (byte) ((value & 0xFF000000L) >>24);
		dest[off + 1] = (byte) ((value & 0x00FF0000L) >>16);
		dest[off + 2] = (byte) ((value & 0x0000FF00L) >>8);
		dest[off + 3] = (byte) ((value & 0x000000FFL));
	}
	
	public static long fourByteToUInt (byte[] value, int offset) {
		if ((offset + 4) > value.length)
			throw new IllegalArgumentException(String.format(ERR_ARRAY_OVERFLOW, value.length,
				offset));

		int b0, b1, b2, b3;
		b0 = b1 = b2 = b3 = 0;
		
		b0 = (int)(0x000000FF & (value[offset]));
		b1 = (int)(0x000000FF & (value[offset+1]));
		b2 = (int)(0x000000FF & (value[offset+2]));
		b3 = (int)(0x000000FF & (value[offset+3]));
		long uint = ((long)b0 <<24 | (long)b1 <<16 | (long)b2 <<8 | (long)b3);
		return uint;
	}

    public static long fourByteToUInt (byte[] value) {
        return fourByteToUInt(value, 0);
    }

	public static byte[] intToThreeByte(long value) {
		if ((value < 0) || (value > MAX_24BITS))
			throw new IllegalArgumentException(String.format(ERR_VALUE_OOB, value));

		byte[] buf = new byte[3];
		intToThreeByte(value, buf, 0);
		return buf;
	}

	public static void intToThreeByte(long value, byte[] buf, int offset) {
		if ((value < 0) || (value > MAX_24BITS))
			throw new IllegalArgumentException(String.format(ERR_VALUE_OOB, value));
		buf[offset] = (byte) ((value & 0x00FF0000L) >> 16);
		buf[offset + 1] = (byte) ((value & 0x0000FF00L) >> 8);
		buf[offset + 2] = (byte) ((value & 0x000000FFL));
	}
	
	public static long threeByteToUInt (byte[] value, int offset) {
		if ((offset + 3) > value.length)
			throw new IllegalArgumentException(String.format(ERR_ARRAY_OVERFLOW, value.length,
				offset));

		int b0, b1, b2;
		b0 = b1 = b2 = 0;
		
		b0 = (int)(0x000000FF & (value[offset]));
		b1 = (int)(0x000000FF & (value[offset+1]));
		b2 = (int)(0x000000FF & (value[offset+2]));
		long uint = ((long)b0 <<16 | (long)b1 <<8 | (long)b2);
		return uint;
	}

    public static long threeByteToUInt (byte[] value) {
        return threeByteToUInt(value, 0);
    }

	public static byte[] intToTwoByte(int value) {
		if ((value < 0) || (value > MAX_16BITS))
			throw new IllegalArgumentException(String.format(ERR_VALUE_OOB, value));

		byte[] buf = new byte[2];
		intToTwoByte(value, buf, 0);
		return buf;
	}
	
	public static void intToTwoByte(int value, byte[] destination, int offset) {
		if ((value < 0) || (value > MAX_16BITS))
			throw new IllegalArgumentException(String.format(ERR_VALUE_OOB, value));
		destination[offset] = (byte) ((value & 0x0000FF00L) >>8);
		destination[offset+1] = (byte) ((value & 0x000000FFL));
	}
	
	public static long twoByteToUInt(byte[] value, int offset) {
		if ((offset + 2) >value.length)
			throw new IllegalArgumentException(String.format(ERR_ARRAY_OVERFLOW, value.length,
				offset));

		int b0, b1;
		b0 = b1 = 0;
		
		b0 = (0x000000FF & ((int)value[offset]));
		b1 = (0x000000FF & ((int)value[offset+1]));
		
		long uint = (long) (b0 <<8 | b1);
		return uint;
	}

    public static long twoByteToUInt (byte[] value) {
        return twoByteToUInt(value, 0);
    }

	public static byte intToOneByte(int value) {
		if ((value < 0) || (value > MAX_8BITS))
			throw new IllegalArgumentException(String.format(ERR_VALUE_OOB, value));
		
		return (byte)(value & 0xFF);
	}
	
	public static long oneByteToUInt(byte value) {
		int b0 = 0;
		b0 = (0x000000FF & ((int)value));
		return (long) b0;
	}

	public static int oneTwoThreeFourByteToUInt(final byte[] value, final int val_length, final int val_offset) {
		int val = 0;
		switch(val_length) {
		case 1:
			val = (int) oneByteToUInt(value[val_offset]);
			break;
		case 2:
			val = (int) twoByteToUInt(value, val_offset);
			break;
		case 3:
			val = (int) threeByteToUInt(value, val_offset);
			break;
		case 4:
			val = (int) fourByteToUInt(value, val_offset);
			break;
		}
		return val;
	}
	
	public static int oneTwoThreeFourByteToInt(byte[] value) {
		return oneTwoThreeFourByteToUInt(value, value.length, 0);
	}

	public static byte[] intToOneTwoThreeFourByte(long value) {
		byte[] buf = new byte[0];
		if (value < 0) {
			throw new IllegalArgumentException(String.format(ERR_VALUE_NEGATIVE, value)); //$NON-NLS-1$
		} else if (value <= NetworkByteOrderNumberUtil.MAX_8BITS) {
			buf = new byte[]{NetworkByteOrderNumberUtil.intToOneByte((int)value)};
		} else if (value <= NetworkByteOrderNumberUtil.MAX_16BITS) {
			buf = NetworkByteOrderNumberUtil.intToTwoByte((int)value);
		} else if (value <= NetworkByteOrderNumberUtil.MAX_24BITS) {
			buf = NetworkByteOrderNumberUtil.intToThreeByte(value);
		} else if (value <= NetworkByteOrderNumberUtil.MAX_32BITS) {
			buf = NetworkByteOrderNumberUtil.intToFourByte(value);
		} else {
			throw new IllegalArgumentException(String.format(ERR_VALUE_INVALID, value)); //$NON-NLS-1$
		}
		return buf;
	}
}
