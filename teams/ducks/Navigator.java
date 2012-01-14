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
	private MapLocation destination;
	private int movesOnSameTarget;
	private int expectedMovesToReachTarget;
	private static int[] wiggleDirectionOrder = new int[] {0, 1, -1, 2, -2};
	public Navigator(BaseRobot baseRobot) {
		this.baseRobot = baseRobot;
		mapCache = baseRobot.mc;
		tangentBug = new TangentBug(mapCache.isWall);
		blindBug = new BlindBug(baseRobot);
		zeroLoc = new MapLocation(0,0);
		mode = NavigationMode.RANDOM;
		roundLastReset = -1;
	}
	
	/** Resets the navigator, clearing it of any state. */
	public void reset() {
		tangentBug.reset(1, 0);
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
	public void setDestination(MapLocation destination) {
		if(destination.equals(this.destination)) 
			return;
		movesOnSameTarget = 0;
		expectedMovesToReachTarget = (int)(Math.sqrt(baseRobot.currLoc.distanceSquaredTo(destination)) *
				TangentBug.MAP_UGLINESS_WEIGHT);
		this.destination = destination;
		tangentBug.setTarget(mapCache.worldToCacheX(destination.x), 
				mapCache.worldToCacheY(destination.y));
		reset();
	}
	public void prepare() {
		if(mode==NavigationMode.TANGENT_BUG) {
			tangentBug.prepare(
					mapCache.worldToCacheX(baseRobot.currLoc.x), 
					mapCache.worldToCacheY(baseRobot.currLoc.y));
		} 
	}
	public Direction navigateToDestination() {
		Direction dir = Direction.NONE;
		if(mode==NavigationMode.RANDOM) {
			dir = navigateCompletelyRandomly();
		} else if(mode==NavigationMode.BUG) {
			dir = navigateBug();
		} else if(mode==NavigationMode.TANGENT_BUG) {
			dir = navigateTangentBug();
			if(movesOnSameTarget % expectedMovesToReachTarget == 0) {
				int n = movesOnSameTarget / expectedMovesToReachTarget;
				if(n>=2) {
					tangentBug.reset(Math.min(6+n, 50), 0.4);
				}
				movesOnSameTarget++;
			}
		} else if(mode==NavigationMode.DSTAR) {
			dir = navigateDStar();
		} 
		
		
		if(dir==Direction.NONE) return dir;
		//WIGGLE! ^_^
		boolean[] movable = baseRobot.dc.getMovableDirections();
		int multiplier = ((int)(Math.random()*2))*2-1; // 1 or -1 with equal probability
		int ord = dir.ordinal();
		for(int ddir : wiggleDirectionOrder) {
			if(movable[(ord+multiplier*ddir+8)%8]) {
				movesOnSameTarget++;
				return dir;
			}
		}
		return Direction.NONE;
	}
	
	private Direction dxdyToDirection(int dx, int dy) {
		return zeroLoc.directionTo(zeroLoc.add(dx, dy));
	}
	private Direction navigateTangentBug() {
		int[] d = tangentBug.computeMove(
				mapCache.worldToCacheX(baseRobot.currLoc.x), 
				mapCache.worldToCacheY(baseRobot.currLoc.y));
		if(d==null) return Direction.NONE;
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
	private Direction navigateBug() {
		return blindBug.navigateToIgnoreBots(destination);
	}
	private Direction navigateDStar() {
		//TODO implement
		return navigateCompletelyRandomly();
	}
}
