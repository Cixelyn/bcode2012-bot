package ducks;

import battlecode.common.GameConstants;
import battlecode.common.RobotController;

public class Debug {
	
	public enum Owner {
		HT,
		YP,
		CORYLI,
		JVEN,
		ALL
	}
	
	/** Set to false to turn off indicator strings and printlns. */
	private static boolean showDebug = true;
	
	private RobotController rc;
	private final Owner owner;
	
	private String[] indicatorStrings;
	
	public Debug(BaseRobot myBR, Owner myOwner) {
		rc = myBR.rc;
		owner = myOwner;
		indicatorStrings = new String[GameConstants.NUMBER_OF_INDICATOR_STRINGS];
	}
	
	public void logError(Exception e) {
		e.printStackTrace();
		rc.addMatchObservation(e.toString());
	}
	
	public void println(String message) {
		if (showDebug) {
			System.out.println(message);
		}
	}
	
	public void setIndicatorString(int row, String message, Owner owner) {
		if ((owner == this.owner || owner == Owner.ALL) && row >= 0 &&
				row < GameConstants.NUMBER_OF_INDICATOR_STRINGS) {
			indicatorStrings[row] = message;
		}
	}
	
	public void showIndicatorStrings() {
		for (int row = 0; row < GameConstants.NUMBER_OF_INDICATOR_STRINGS; row++) {
			if (showDebug) {
				rc.setIndicatorString(row, indicatorStrings[row]);
			} else {
				rc.setIndicatorString(row, "");
			}
		}
	}
}
