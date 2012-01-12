package ducks;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

public class Navigator {
	private final MapCache mapCache; 
	private final TangentBug tangentBug;
	private final MapLocation zeroLoc;
	private final Direction[] allDirections;
	public Navigator(MapCache mapCache) {
		this.mapCache = mapCache;
		tangentBug = new TangentBug();
		zeroLoc = new MapLocation(0,0);
		allDirections = Direction.values();
	}
	
	private Direction getRandomDirection() {
		return allDirections[(int)(Math.random()*8)];
	}
	private Direction dxdyToDirection(int dx, int dy) {
		return zeroLoc.directionTo(zeroLoc.add(dx, dy));
	}
	public Direction navigateTangentBug(MapLocation source, MapLocation destination) {
		int[] d = tangentBug.computeMove(mapCache.isWall, 
				mapCache.worldToCacheX(source.x), mapCache.worldToCacheY(source.y), 
				mapCache.worldToCacheX(destination.x), mapCache.worldToCacheY(destination.y));
		return dxdyToDirection(d[0], d[1]);
	}
	public Direction navigateCompletelyRandomly(MapLocation source, MapLocation destination) {
		return getRandomDirection();
	}
	public Direction navigateBug(MapLocation source, MapLocation destination) {
		//TODO implement
		return getRandomDirection();
	}
	public Direction navigateDirectionalBug(MapLocation source, MapLocation destination) {
		//TODO implement
		return getRandomDirection();
	}
	public Direction navigateDStar(MapLocation source, MapLocation destination) {
		//TODO implement
		return getRandomDirection();
	}
}
