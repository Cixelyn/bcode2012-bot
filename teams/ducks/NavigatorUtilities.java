package ducks;

import battlecode.common.Direction;

/** Static utilities for navigation.
 */
public class NavigatorUtilities {
	private NavigatorUtilities() {}
	
	public final static Direction[] allDirections = Direction.values();
	
	/** Returns a direction at random from the eight standard directions. */
	public static Direction getRandomDirection() {
		return allDirections[(int)(Math.random()*8)];
	}
	
}
