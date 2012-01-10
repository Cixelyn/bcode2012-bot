package fibbyBot1;

import java.util.Arrays;

import fibbyBot1.navigation.Navigation;
import battlecode.common.*;

public class Util {
	
	RobotController myRC;
	Navigation nav;
	
	public Util(RobotController RC) {
		this.myRC = RC;
		this.nav = new Navigation(RC);
	}
	
	public void navigate(MapLocation destination) throws GameActionException {
		Direction dir = this.nav.computeDirection(destination);
		if (myRC.getDirection() == dir) {
			if (myRC.canMove(dir)) {
				myRC.moveForward();
			}
		} else {
			if (dir != Direction.OMNI && dir != Direction.NONE) {
				myRC.setDirection(dir);
			}
		}
	}
	
	public void navigate(Direction direction) throws GameActionException {
		navigate(myRC.getLocation().add(
				direction,
				Math.max(GameConstants.MAP_MAX_HEIGHT, GameConstants.MAP_MAX_WIDTH)));
	}
	
	public boolean spawn(RobotType robotType) throws GameActionException {
		MapLocation loc = this.myRC.getLocation().add(this.myRC.getDirection());
		GameObject obj = this.myRC.senseObjectAtLocation(loc, robotType.level);
		if (this.myRC.senseTerrainTile(loc) == TerrainTile.LAND &&
				obj == null) {
			this.myRC.spawn(robotType);
			double fluxToTransfer = Math.min(
					robotType.maxFlux, this.myRC.getFlux());
			this.myRC.yield();
			this.myRC.transferFlux(loc, robotType.level, fluxToTransfer);
			return true;
		} else {
			for (Direction d : Direction.values()) {
				if (d == Direction.OMNI || d == Direction.NONE) {
					continue;
				}
				loc = this.myRC.getLocation().add(d);
				obj = this.myRC.senseObjectAtLocation(loc, robotType.level);
				if (this.myRC.senseTerrainTile(loc) == TerrainTile.LAND &&
						obj == null) {
					this.myRC.setDirection(d);
					break;
				}
			}
			return false;
		}
	}
	
	public RobotInfo senseNearbyEnemy() throws GameActionException {
		Robot[] nearbyRobots = this.myRC.senseNearbyGameObjects(Robot.class);
		RobotInfo[] nearbyRobotInfos = new RobotInfo[nearbyRobots.length];
		if (this.myRC.getType() != RobotType.ARCHON) {
			// get an enemy in attack range
			for (int i = 0; i < nearbyRobots.length; i++) {
				Robot r = nearbyRobots[i];
				if (r.getTeam() != this.myRC.getTeam()) {
					RobotInfo rInfo = this.myRC.senseRobotInfo(r);
					nearbyRobotInfos[i] = rInfo;
					if (this.myRC.canAttackSquare(rInfo.location)) {
						this.myRC.attackSquare(rInfo.location, r.getRobotLevel());
						return rInfo;
					}
				}
			}
		}
		// no enemies in attack range, get one in sensor range
		for (RobotInfo rInfo : nearbyRobotInfos) {
			if (rInfo == null) {
				continue;
			} else if (rInfo.team != this.myRC.getTeam()) {
				return rInfo;
			}
		}
		return null;
	}
	
	public void broadcastRally(
			Direction bearing,
			MapLocation target,
			int confidence) throws GameActionException {
		Message message = new Message();
		message.ints = new int[] {1337, bearing.ordinal(), confidence};
		message.locations = new MapLocation[] {target, this.myRC.getLocation()};
		this.myRC.broadcast(message);
	}
	
	public void distributeFlux() throws GameActionException {
		for (Direction d : Direction.values()) {
			if (d == Direction.OMNI || d == Direction.NONE) {
				continue;
			}
			for (RobotLevel level : RobotLevel.values()) {
				if (level == RobotLevel.POWER_NODE) {
					continue;
				}
				MapLocation loc = this.myRC.getLocation().add(d);
				GameObject obj = this.myRC.senseObjectAtLocation(loc, level);
				if (obj != null && obj.getTeam() == this.myRC.getTeam()
						&& obj instanceof Robot) {
					RobotInfo rInfo = this.myRC.senseRobotInfo((Robot)obj);
					if (rInfo.type != RobotType.ARCHON && rInfo.type != RobotType.TOWER &&
							this.myRC.getFlux() >
							rInfo.type.moveCost * (1.0 / rInfo.type.moveDelayOrthogonal)) {
						this.myRC.transferFlux(loc, level, rInfo.type.moveCost);
					}
				}
			}
		}
	}
	
	public static int[][] getSquaresInRange(int distance, int angle, int myDirection) {
		// get square size to look in
		int squareSize = 0;
		while (squareSize * squareSize < distance) {
			squareSize++;
		}
		// initialize answer
		int[][] answer = new int[(1 + 2 * squareSize) * (1 + 2 * squareSize)][2];
		int subarrayIndex = 0;
		// get answer
		for (int y = -squareSize; y <= squareSize; y++) {
			for (int x = -squareSize; x <= squareSize; x++) {
				// don't consider yourself
				if (x == 0 && y == 0) {
					continue;
				}
				// check distance
				if (x * x + y * y > distance) {
					continue;
				}
				// check angle
				double offsetX = 0;
				double offsetY = 0;
				switch (myDirection) {
					case 0:
						offsetX = (Math.sqrt(2.0) / 2) * (x + y);
						offsetY = (Math.sqrt(2.0) / 2) * (-x + y);
						break;
					case 1:
						offsetX = x;
						offsetY = y;
						break;
					case 2:
						offsetX = (Math.sqrt(2.0) / 2) * (x - y);
						offsetY = (Math.sqrt(2.0) / 2) * (x + y);
						break;
					case 3:
						offsetX = -y;
						offsetY = x;
						break;
					case 4:
						offsetX = (Math.sqrt(2.0) / 2) * (-x - y);
						offsetY = (Math.sqrt(2.0) / 2) * (x - y);
						break;
					case 5:
						offsetX = -x;
						offsetY = -y;
						break;
					case 6:
						offsetX = (Math.sqrt(2.0) / 2) * (-x + y);
						offsetY = (Math.sqrt(2.0) / 2) * (-x - y);
						break;
					case 7:
						offsetX = y;
						offsetY = -x;
						break;
				}
				// consider points to my right... for points on the left, mirror them
				if (offsetX < 0) {
					offsetX *= -1;
				}
				// consider 0 and 360 angles separately
				if (angle == 0) {
					// only stuff right in front of me is valid
					if (offsetX != 0 || offsetY <= 0) continue;
				} else if (angle == 360) {
					// everything is valid
				} else {
					// if offsetX == 0, offsetY must be >= 0
					if (offsetX == 0 && offsetY < 0) continue;
					// if offsetX > 0, offsetY / offsetX must be >= cot(angle / 2.0)
					if (offsetY / offsetX < 1.0 / Math.tan(
							Math.toRadians(angle / 2.0))) continue;
				}
				// add to answer
				answer[subarrayIndex++] = new int[] {x, y};
			}
		}
		// return answer
		answer = Arrays.copyOfRange(answer, 0, subarrayIndex);
		return answer;
	}
}
