package ducks;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

public class Navigator {
	private final BaseRobot baseRobot;
	private final MapCache mapCache; 
	private final TangentBug tangentBug;
	private final MapLocation zeroLoc;
	public Navigator(BaseRobot baseRobot) {
		this.baseRobot = baseRobot;
		mapCache = baseRobot.mc;
		tangentBug = new TangentBug(this);
		zeroLoc = new MapLocation(0,0);
	}
	
	/** Resets the navigator, clearing it of any state. */
	public void reset() {
		tangentBug.reset();
	}
	
	private Direction dxdyToDirection(int dx, int dy) {
		return zeroLoc.directionTo(zeroLoc.add(dx, dy));
	}
	public Direction navigateTangentBug(MapLocation destination) {
		int[] d = tangentBug.computeMove(mapCache.isWall, 
				mapCache.worldToCacheX(baseRobot.currLoc.x), mapCache.worldToCacheY(baseRobot.currLoc.y), 
				mapCache.worldToCacheX(destination.x), mapCache.worldToCacheY(destination.y));
		return dxdyToDirection(d[0], d[1]);
	}
	public Direction navigateCompletelyRandomly() {
		Direction dir;
		int tries = 0;
		do {
			tries++;
			if(tries>32) return Direction.NONE;
			dir = NavigatorUtilities.getRandomDirection();
		} while(baseRobot.rc.canMove(dir));
		return dir;
	}
	public Direction navigateBug(MapLocation destination) {
		//TODO implement
		return navigateCompletelyRandomly();
	}
	public Direction navigateDirectionalBug(Direction dir, boolean traceClockwise) {
		//TODO implement
		return dir;
	}
	public Direction navigateDStar(MapLocation destination) {
		//TODO implement
		return navigateCompletelyRandomly();
	}
	
	
}
