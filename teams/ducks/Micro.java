package ducks;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;

/**
 * Handles the attacking and moving for robots.
 */
public class Micro {
	
	private enum MicroMode {
		HOLD_POSITION,
		NORMAL,
		MOONWALK,
		SWARM,
		KITE,
		CHARGE
	}

	private BaseRobot br;
	private RobotController rc;
	
	private MicroMode mode;
	private MapLocation objective;
	
	private int microDistance;
	private Direction dirAboutToMoveIn; 
	
	private boolean avoidPowerNodes = false;
	
	public Micro(BaseRobot myBR) {
		br = myBR;
		rc = myBR.rc;
	}
	
	/**
	 * Set the MapLocation to attack move towards.
	 * 
	 * @param myObjective The MapLocation to attack move towards.
	 */
	public void setObjective(MapLocation myObjective) {
		if (objective == null || !objective.equals(myObjective)) {
			objective = myObjective;
			br.nav.setDestination(myObjective);
		}
	}
	
	/** Don't navigate, spin around to look for enemies */
	public void setHoldPositionMode() {
		mode = MicroMode.HOLD_POSITION;
	}
	
	/** Navigate forwards. */
	public void setNormalMode() {
		mode = MicroMode.NORMAL;
	}
	
	/** Navigate backwards. */
	public void setMoonwalkMode() {
		mode = MicroMode.MOONWALK;
	}
	
	/** Set whether the unit should avoid power nodes. */
	public void toggleAvoidPowerNodes(boolean b) {
		avoidPowerNodes = b;
	}
	
	/**
	 * Navigate forwards while maintaining a specified distance from the closest
	 * archon.
	 * @param mySwarmRadius The distance to maintain from the closest archon
	 */
	public void setSwarmMode(int swarmRadius) {
		mode = MicroMode.SWARM;
		microDistance = swarmRadius;
	}
	
	/**
	 * Navigate forwards while maintaining a specified distance from target.
	 * @param myKiteDistance The distance to maintain from target.
	 */
	public void setKiteMode(int kiteDistance) {
		mode = MicroMode.KITE;
		microDistance = kiteDistance;
	}
	
	/**
	 * Turn to target and run straight up to it.
	 */
	public void setChargeMode(){ 
		mode = MicroMode.CHARGE;
	}
	
	/**
	 * Attack the closest enemy and navigate towards the objective using the
	 * current mode.
	 * @throws GameActionException
	 */
	public void attackMove() throws GameActionException {
		// prepare
		br.nav.prepare();
		// attack
		if (!rc.isAttackActive()) {
			attackClosestEnemy();
		}
		// move
		if (!rc.isMovementActive()) {
			switch (mode) {
				case HOLD_POSITION:
					holdPosition();
					break;
				case NORMAL:
					normalTowards();
					break;
				case MOONWALK:
					moonwalkTowards();
					break;
				case SWARM:
					swarmTowards();
					break;
				case KITE:
					kiteTowards(objective);
					break;
				case CHARGE:
					chargeTowards(objective);
					break;
				default:
					break;
			}
		}
	}
	
	private boolean attackClosestEnemy() throws GameActionException {
		// ignore call if i'm an archon
		if (br.myType == RobotType.ARCHON) {
			return false;
		}
		// see if enemy in range
		RobotInfo closestEnemy = br.dc.getClosestEnemy();
		if (closestEnemy != null && rc.canAttackSquare(closestEnemy.location)) {
			rc.attackSquare(
					closestEnemy.location, closestEnemy.robot.getRobotLevel());
			if (br.myType != RobotType.SCOUT &&
					closestEnemy.type == RobotType.ARCHON &&
					closestEnemy.energon <= br.myType.attackPower) {
				br.eakc.reportEnemyArchonKill(closestEnemy.robot.getID());
			}
			return true;
		} else {
			return false;
		}
	}
	
	private boolean holdPosition() throws GameActionException {
		if (br.myType.attackAngle != 360) {
			rc.setDirection(br.curDir.rotateLeft().rotateLeft().rotateLeft());
			return true;
		} else {
			return false;
		}
	}
	
	/** Returns true iff we did some movement action (moving or turning). */
	private boolean normalTowards() throws GameActionException {
		if(dirAboutToMoveIn == null) {
			Direction dir = br.nav.navigateToDestination();
			if(dir==null || dir == Direction.OMNI || dir == Direction.NONE)
				return false;
			dirAboutToMoveIn = dir;
		}
		
		Direction dir = br.nav.wiggleToMovableDirection(dirAboutToMoveIn);
		if (avoidPowerNodes) {
			dir = avoidPowerNode(dir);
		}
		if(dir==null) 
			return false;
		if(dir == br.curDir) {
			rc.moveForward();
			br.directionToSenseIn = dir;
			dirAboutToMoveIn = null;
		} else {
			rc.setDirection(dir);
		}
		
		return true;
	}
	
	private boolean moonwalkTowards() throws GameActionException {
		if(dirAboutToMoveIn == null) {
			Direction dir = br.nav.navigateToDestination();
			if(dir==null || dir == Direction.OMNI || dir == Direction.NONE)
				return false;
			dirAboutToMoveIn = dir;
		}
		
		Direction dir = br.nav.wiggleToMovableDirection(dirAboutToMoveIn);
		if (avoidPowerNodes) {
			dir = avoidPowerNode(dir);
		}
		if(dir==null) 
			return false;
		if(dir == br.curDir.opposite()) {
			rc.moveBackward();
			br.directionToSenseIn = dir;
			dirAboutToMoveIn = null;
		} else {
			rc.setDirection(dir.opposite());
		}
		
		return true;
	}
	
	private boolean swarmTowards() throws GameActionException {
		if (br.curLoc.distanceSquaredTo(br.dc.getClosestArchon()) <=
				microDistance) {
			return normalTowards();
		} else {
			return chargeTowards(br.dc.getClosestArchon());
		}
	}
	
	private boolean kiteTowards(MapLocation target) throws GameActionException {
		if(dirAboutToMoveIn == null) {
			Direction dir = br.nav.navigateGreedy(target);
			if(dir==null || dir == Direction.OMNI || dir == Direction.NONE)
				return false;
			dirAboutToMoveIn = dir;
		}
		
		Direction dir = dirAboutToMoveIn;
		// try the three appropriate directions
		Direction[] targetDirs = new Direction[] {
				dir,
				dir.rotateLeft(),
				dir.rotateRight()
		};
		int distanceSquared = br.curLoc.distanceSquaredTo(target);
		for (Direction d : targetDirs) {
			if (avoidPowerNodes) {
				d = avoidPowerNode(d);
			}
			if (distanceSquared < microDistance) {
				// move backwards
				if (rc.canMove(d.opposite())) {
					if (br.curDir != d) {
						rc.setDirection(d);
					} else {
						rc.moveBackward();
						br.directionToSenseIn = dir.opposite();
						dirAboutToMoveIn = null;
					}
					return true;
				}
			} else {
				// move forwards
				if (rc.canMove(d)) {
					if (br.curDir != d) {
						rc.setDirection(d);
					} else {
						rc.moveForward();
						br.directionToSenseIn = dir;
						dirAboutToMoveIn = null;
					}
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean chargeTowards(MapLocation target) throws GameActionException {
		if(dirAboutToMoveIn == null) {
			Direction dir = br.nav.navigateGreedy(target);
			if(dir==null || dir == Direction.OMNI || dir == Direction.NONE)
				return false;
			dirAboutToMoveIn = dir;
		}
		
		Direction dir = br.nav.wiggleToMovableDirection(dirAboutToMoveIn);
		if (avoidPowerNodes) {
			dir = avoidPowerNode(dir);
		}
		if(dir==null) 
			return false;
		if(dir == br.curDir) {
			rc.moveForward();
			br.directionToSenseIn = dir;
			dirAboutToMoveIn = null;
		} else {
			rc.setDirection(dir);
		}
		
		return true;
	}
	
	private Direction avoidPowerNode(Direction dir) throws GameActionException {
		if (dir == null || dir == Direction.NONE || dir == Direction.OMNI) {
			return null;
		}
		MapLocation loc = br.curLoc.add(dir);
		if (rc.canSenseSquare(loc) && rc.senseObjectAtLocation(
				loc, RobotLevel.POWER_NODE) != null) {
			Direction newDir = dir;
			for (int tries = 0; tries < 30; tries++) {
				if (Math.random() < 0.5) {
					newDir = newDir.rotateLeft();
				} else {
					newDir = newDir.rotateRight();
				}
				MapLocation newLoc = br.curLoc.add(newDir);
				if (rc.canMove(newDir) && !(rc.canSenseSquare(newLoc) &&
						rc.senseObjectAtLocation(newLoc, RobotLevel.POWER_NODE) != null)) {
					return newDir;
				}
			}
		}
		return dir;
	}
	
	public MapLocation getObjective() {
		return this.objective;
	}
}
