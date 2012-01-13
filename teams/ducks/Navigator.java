package ducks;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

public class Navigator {
	private final BaseRobot baseRobot;
	private final MapCache mapCache; 
	private final TangentBug tangentBug;
	private final BlindBug blindBug;
	private final MapLocation zeroLoc;
	private int roundLastReset;
	private NavigationMode mode;
	private static int[] wiggleDirectionOrder = new int[] {0, 1, -1, 2, -2};
	public Navigator(BaseRobot baseRobot) {
		this.baseRobot = baseRobot;
		mapCache = baseRobot.mc;
		tangentBug = new TangentBug(this);
		blindBug = new BlindBug(baseRobot);
		zeroLoc = new MapLocation(0,0);
		mode = NavigationMode.RANDOM;
		roundLastReset = -1;
	}
	
	/** Resets the navigator, clearing it of any state. */
	public void reset() {
		tangentBug.reset();
		//TODO reset blindbug
		roundLastReset = baseRobot.currRound;
	}
	
	public NavigationMode getNavigationMode() {
		return mode;
	}
	public void setNavigationMode(NavigationMode mode) {
		this.mode = mode;
		reset();
	}
	public Direction navigateTo(MapLocation destination) {
		Direction dir = Direction.NORTH;
		if(mode==NavigationMode.RANDOM) {
			dir = navigateCompletelyRandomly();
		} else if(mode==NavigationMode.BUG) {
			dir = navigateBug(destination);
		} else if(mode==NavigationMode.DIRECTIONAL_BUG) {
			dir = navigateDirectionalBug(dxdyToDirection(
					destination.x-baseRobot.currLoc.x, destination.y-baseRobot.currLoc.y));
		} else if(mode==NavigationMode.TANGENT_BUG) {
			dir = navigateTangentBug(destination);
		} else if(mode==NavigationMode.DSTAR) {
			dir = navigateDStar(destination);
		} 
		
		//WIGGLE! ^_^
		boolean[] movable = baseRobot.dc.getMovableDirections();
		int multiplier = ((int)(Math.random()*2))*2-1;
		int ord = dir.ordinal();
		for(int ddir : wiggleDirectionOrder) {
			if(movable[(ord+multiplier*ddir+8)%8]) return dir;
		}
		return Direction.NONE;
	}
	
	private Direction dxdyToDirection(int dx, int dy) {
		return zeroLoc.directionTo(zeroLoc.add(dx, dy));
	}
	private Direction navigateTangentBug(MapLocation destination) {
		if(mapCache.roundLastUpdated > roundLastReset) {
			reset();
		}
		int[] d = tangentBug.computeMove(mapCache.isWall, 
				mapCache.worldToCacheX(baseRobot.currLoc.x), mapCache.worldToCacheY(baseRobot.currLoc.y), 
				mapCache.worldToCacheX(destination.x), mapCache.worldToCacheY(destination.y));
		return dxdyToDirection(d[0], d[1]);
	}
	private Direction navigateCompletelyRandomly() {
		for(int tries=0; tries<32; tries++) {
			Direction dir = NavigatorUtilities.getRandomDirection();
			if(baseRobot.rc.canMove(dir))
				return dir;
		}
		return Direction.NONE;
	}
	private Direction navigateBug(MapLocation destination) {
		return blindBug.navigateToIgnoreBots(destination);
	}
	private Direction navigateDirectionalBug(Direction dir) {
		//TODO implement
		return navigateCompletelyRandomly();
	}
	private Direction navigateDStar(MapLocation destination) {
		//TODO implement
		return navigateCompletelyRandomly();
	}
}
