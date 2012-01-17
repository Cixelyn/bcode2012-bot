package ducks;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

public class Navigator {
	private final BaseRobot baseRobot;
	private final MapCache mapCache; 
	private final TangentBug tangentBug;
	private final BlindBug blindBug;
	private final MapLocation zeroLoc;
	private NavigationMode mode;
	private MapLocation destination;
	private int movesOnSameTarget;
	private int expectedMovesToReachTarget;
	private final static int[] wiggleDirectionOrder = new int[] {0, 1, -1, 2, -2};
	public Navigator(BaseRobot baseRobot) {
		this.baseRobot = baseRobot;
		mapCache = baseRobot.mc;
		tangentBug = new TangentBug(mapCache.isWall);
		blindBug = new BlindBug(baseRobot);
		zeroLoc = new MapLocation(0,0);
		mode = NavigationMode.RANDOM;
	}
	
	/** Resets the navigator, clearing it of any state. */
	public void reset() {
		tangentBug.reset(1, 0);
		//TODO reset blindbug
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
				TangentBug.MAP_UGLINESS_WEIGHT)+1;
		this.destination = destination;
		tangentBug.setTarget(mapCache.worldToCacheX(destination.x), 
				mapCache.worldToCacheY(destination.y));
		reset();
	}
	/** Does precomputation to allow the navigationToDestination to
	 * return a more accurate result. Only matters for tangent bug right now.
	 */
	public void prepare() {
		if(mode==NavigationMode.TANGENT_BUG) {
			if(mapCache.edgeXMin!=0) tangentBug.edgeXMin = mapCache.edgeXMin;
			if(mapCache.edgeXMax!=0) tangentBug.edgeXMax = mapCache.edgeXMax;
			if(mapCache.edgeYMin!=0) tangentBug.edgeYMin = mapCache.edgeYMin;
			if(mapCache.edgeYMax!=0) tangentBug.edgeYMax = mapCache.edgeYMax;

			tangentBug.prepare(
					mapCache.worldToCacheX(baseRobot.currLoc.x), 
					mapCache.worldToCacheY(baseRobot.currLoc.y));
		} 
	}
	/** Returns direction to go next in order to reach the destination. */
	public Direction navigateToDestination() {
		if(destination==null) 
			return navigateCompletelyRandomly(); 
		
		Direction dir = Direction.NONE;
		if(mode==NavigationMode.RANDOM) {
			dir = navigateRandomly();
		} else if(mode==NavigationMode.GREEDY) {
			if(movesOnSameTarget > 2 * expectedMovesToReachTarget) 
				dir = navigateRandomly();
			else
				dir = navigateGreedy();
		} else if(mode==NavigationMode.BUG) {
			dir = navigateBug();
		} else if(mode==NavigationMode.TANGENT_BUG) {
			dir = navigateTangentBug();
			if(movesOnSameTarget % expectedMovesToReachTarget == 0) {
				int n = movesOnSameTarget / expectedMovesToReachTarget;
				if(n>=2) {
					tangentBug.reset(Math.min(4*n, 50), 0.4);
				}
				movesOnSameTarget++;
			}
		} else if(mode==NavigationMode.DSTAR) {
			dir = navigateDStar();
		} 
		
		if(dir==Direction.NONE || dir==Direction.OMNI) 
			return Direction.NONE; 
		movesOnSameTarget++;
		return dir;
	}
	/** Given a direction, randomly perturbs it to a direction that the 
	 * robot can move in. 
	 * TODO perhaps we should put this in the Micro class
	 */
	public Direction wiggleToMovableDirection(Direction dir) {
		boolean[] movable = baseRobot.dc.getMovableDirections();
		int multiplier = ((int)(Math.random()*2))*2-1; // 1 or -1 with equal probability
		int ord = dir.ordinal();
		for(int ddir : wiggleDirectionOrder) {
			int index = (ord+multiplier*ddir+8) % 8;
			if(movable[index]) {
				return NavigatorUtilities.allDirections[index];
			}	
		}
		return null;
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
			if(!mapCache.isWall(baseRobot.currLoc.add(dir)))
				return dir;
		}
		return Direction.NONE;
	}
	private Direction navigateRandomly() {
		// With 1/4 probability, reset heading towards destination.
		// Otherwise, randomly perturb current direction by up to 90 degrees.
		double d = Math.random();
		if(d*1000-(int)(d*1000)<0.25) return baseRobot.currLoc.directionTo(destination);
		d=d*2-1;
		d = d*d*Math.signum(d);
		Direction dir = baseRobot.currDir;
		if(d<0) {
			do {
				d++;
				dir = dir.rotateLeft();
			} while(d<0 || mapCache.isWall(baseRobot.currLoc.add(dir)));
		} else {
			do {
				d++;
				dir = dir.rotateRight();
			} while(d<0 || mapCache.isWall(baseRobot.currLoc.add(dir)));
		}
		return dir;
	}
	private Direction navigateGreedy() {
		Direction dir = baseRobot.currLoc.directionTo(destination);
		if(Math.random()<0.5) {
			while(mapCache.isWall(baseRobot.currLoc.add(dir)))
				dir = dir.rotateLeft();
		} else {
			while(mapCache.isWall(baseRobot.currLoc.add(dir)))
				dir = dir.rotateRight();
		}
		return dir;
	}
	private Direction navigateBug() {
		return blindBug.navigateToIgnoreBots(destination);
	}
	private Direction navigateDStar() {
		//TODO implement
		throw new RuntimeException("DStar navigation not yet implemented!");
	}
}
