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
	/** Returns the direction that is equivalent to the given dx, dy value, 
	 * or clockwise of it by as little as possible.
	 */
	public static int getDirClockwiseOf(int dx, int dy) {
		if(dx==0) {
			if(dy>0) return 5;
			else return 1;
		}
		double slope = ((double)dy)/dx;
		if(dx>0) {
			if(slope>=1) return 4;
			else if(slope>=0) return 3;
			else if(slope>=-1) return 2;
			else return 1;
		} else {
			if(slope>=1) return 0;
			else if(slope>=0) return 7;
			else if(slope>=-1) return 6;
			else return 5;
		}
	}
	/** Returns the direction that is equivalent to the given dx, dy value, 
	 * or counterclockwise of it by as little as possible.
	 */
	public static int getDirCounterclockwiseOf(int dx, int dy) {
		if(dx==0) {
			if(dy>0) return 5;
			else return 1;
		}
		double slope = ((double)dy)/dx;
		if(dx>0) {
			if(slope>1) return 5;
			else if(slope>0) return 4;
			else if(slope>-1) return 3;
			else return 2;
		} else {
			if(slope>1) return 1;
			else if(slope>0) return 0;
			else if(slope>-1) return 7;
			else return 6; 
		}
	}
	/** Returns the direction that is equivalent to the given dx, dy value, 
	 * or as close to it as possible.
	 */
	public static int getDirTowards(int dx, int dy) {
		if(dx==0) {
			if(dy>0) return 5;
			else return 1;
		}
		double slope = ((double)dy)/dx;
		if(dx>0) {
			if(slope>2.414) return 5;
			else if(slope>0.414) return 4;
			else if(slope>-0.414) return 3;
			else if(slope>-2.414) return 2;
			else return 1;
		} else {
			if(slope>2.414) return 1;
			else if(slope>0.414) return 0;
			else if(slope>-0.414) return 7;
			else if(slope>-2.414) return 6;
			else return 5;
		}
	}
}
