package com.grb.impulse.parsers.tl1;

public enum TL1AckMessageType {
	IP("IP "),
	NA("NA "),
	NG("NG "),
	OK("OK "),
	PF("PF "),
	RL("RL ");

	private String _prtclStr;
	
	private TL1AckMessageType(String prtclStr) {
		_prtclStr = prtclStr;
	}
	
	public String getPrtclStr() {
		return _prtclStr;
	}
	
	public static TL1AckMessageType getType(String prtclStr) {
		if (prtclStr != null) {
			String upperPrtclStr = prtclStr.toUpperCase();
			TL1AckMessageType[] types = TL1AckMessageType.values();
			for(int i = 0; i < types.length; i++) {
				if (types[i].getPrtclStr().startsWith(upperPrtclStr)) {
					return types[i];
				}
			}
		}
		return null;
	}
}
