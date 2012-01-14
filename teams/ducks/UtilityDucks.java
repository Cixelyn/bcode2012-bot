package ducks;

import battlecode.common.Direction;

public final class UtilityDucks {

	/**
	 * Checks if 2 directions are neighbors, should only be called on actual ordinal directions.
	 * @param d1
	 * @param d2
	 * @return
	 */
	public static boolean neighboringDirsUnsafe(Direction d1, Direction d2)
	{
		int diff = (d1.ordinal()-d2.ordinal()+16)%8;
		return diff==1 || diff==7;
	}
}
