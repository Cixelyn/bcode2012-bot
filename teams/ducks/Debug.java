package ducks;

import battlecode.common.GameConstants;
import battlecode.common.RobotController;

public class Debug {
	
	/** Set to false to turn off indicator strings and printlns. */
	private static boolean showDebug = true;
	
	private RobotController rc;
	private final String tag;
	
	private String[] indicatorStrings;
	
	public Debug(BaseRobot myBR, String myTag) {
		rc = myBR.rc;
		tag = myTag;
		indicatorStrings = new String[GameConstants.NUMBER_OF_INDICATOR_STRINGS];
	}
	
	public void println(String message) {
		if (showDebug) {
			System.out.println(message);
		}
	}
	
	public void setIndicatorString(int row, String message, String owner) {
		if (owner == tag && row >= 0 &&
				row < GameConstants.NUMBER_OF_INDICATOR_STRINGS) {
			indicatorStrings[row] = message;
		}
	}
	
	public void showIndicatorStrings() {
		for (int row = 0; row < GameConstants.NUMBER_OF_INDICATOR_STRINGS; row++) {
			rc.setIndicatorString(row, "");
			if (showDebug) {
				rc.setIndicatorString(row, indicatorStrings[row]);
			}
		}
	}
}
