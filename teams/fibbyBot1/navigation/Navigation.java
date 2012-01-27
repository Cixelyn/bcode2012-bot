package fibbyBot1.navigation;

import fibbyBot1.Util;
import battlecode.common.*;

public class Navigation {
	
	RobotController myRC;
	MapLocation initialLocation;
	Pathfinder pathfinder;
	int[][] map;
	
	public Navigation(RobotController RC) {
		this.myRC = RC;
		this.initialLocation = RC.getLocation();
		this.pathfinder = new Bug();
		int sizeX = 1 + 2 * GameConstants.MAP_MAX_WIDTH;
		int sizeY = 1 + 2 * GameConstants.MAP_MAX_HEIGHT;
		this.map = new int[sizeX][sizeY];
		for (int x = 0; x < map.length; x++) {
			for (int y = 0; y < map[0].length; y++) {
				this.map[x][y] = 1;
			}
		}
	}
	
	public int[] getMapIndices(MapLocation loc) {
		int dx = loc.x - this.initialLocation.x;
		int dy = loc.y - this.initialLocation.y;
		return new int[] {dx + GameConstants.MAP_MAX_WIDTH,
				dy + GameConstants.MAP_MAX_HEIGHT};
	}
	
	public MapLocation getMapLocation(int x, int y) {
		return new MapLocation(
				x - this.initialLocation.x, y - this.initialLocation.y);
	}
	
	public void updateMap() throws GameActionException{
		// get angle ordinal
		int ordinal;
		switch (myRC.getDirection()) {
			case NORTH_WEST:
				ordinal = 0;break;
			case NORTH:
				ordinal = 1;break;
			case NORTH_EAST:
				ordinal = 2;break;
			case EAST:
				ordinal = 3;break;
			case SOUTH_EAST:
				ordinal = 4;break;
			case SOUTH:
				ordinal = 5;break;
			case SOUTH_WEST:
				ordinal = 6;break;
			case WEST:
				ordinal = 7;break;
			default:
				ordinal = -1;break;
		}
		// sense squares in range
		for (int[] square : Util.getSquaresInRange(
				Math.min(10, this.myRC.getType().sensorRadiusSquared),
				(int)this.myRC.getType().sensorAngle, ordinal)) {
			MapLocation loc = this.myRC.getLocation().add(square[0], -square[1]);
			int[] mapIndices = getMapIndices(loc);
			int x = mapIndices[0];
			int y = mapIndices[1];
			TerrainTile tt = this.myRC.senseTerrainTile(loc);
			if (tt == TerrainTile.LAND) {
				GameObject r = this.myRC.senseObjectAtLocation(
						loc, this.myRC.getRobot().getRobotLevel());
				if (r == null) {
					this.map[x][y] = 1;
				} else {
					this.map[x][y] = 0;
				}
			} else {
				this.map[x][y] = 0;
			}
		}
	}
	
	public Direction computeDirection(
			MapLocation target) throws GameActionException{
		updateMap();
		int[] s = getMapIndices(this.myRC.getLocation());
		int[] t = getMapIndices(target);
		int sx = s[0];
		int sy = s[1];
		int tx = t[0];
		int ty = t[1];
		int[] ans = this.pathfinder.computeMove(map, sx, sy, tx, ty);
		Direction dir;
		switch (10 * (ans[0] + 1) + (ans[1] + 1)) {
			case 00:
				dir = Direction.NORTH_WEST;break;
			case 10:
				dir = Direction.NORTH;break;
			case 20:
				dir = Direction.NORTH_EAST;break;
			case 01:
				dir = Direction.WEST;break;
			case 21:
				dir = Direction.EAST;break;
			case 02:
				dir = Direction.SOUTH_WEST;break;
			case 12:
				dir = Direction.SOUTH;break;
			case 22:
				dir = Direction.SOUTH_EAST;break;
			default:
				dir = Direction.NONE;break;	
		}
		return dir;
	}
}
