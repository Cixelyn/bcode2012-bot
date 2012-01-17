package ducks;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

/**
 * Handles the attacking and moving for robots.
 * 
 * @author jven
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
	
	/**
	 * Don't navigate, spin around to look for enemies
	 */
	public void setHoldPositionMode() {
		mode = MicroMode.HOLD_POSITION;
	}
	
	/**
	 * Navigate forwards.
	 */
	public void setNormalMode() {
		mode = MicroMode.NORMAL;
	}
	
	/**
	 * Navigate backwards.
	 */
	public void setMoonwalkMode() {
		mode = MicroMode.MOONWALK;
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
				br.eai.reportEnemyArchonKill(closestEnemy.robot.getID());
			}
			return true;
		} else {
			return false;
		}
	}
	
	private boolean holdPosition() throws GameActionException {
		if (br.myType.attackAngle != 360) {
			rc.setDirection(br.currDir.rotateLeft().rotateLeft().rotateLeft());
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
		if(dir==null) 
			return false;
		if(dir == br.currDir) {
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
		if(dir==null) 
			return false;
		if(dir == br.currDir.opposite()) {
			rc.moveBackward();
			br.directionToSenseIn = dir;
			dirAboutToMoveIn = null;
		} else {
			rc.setDirection(dir);
		}
		
		return true;
	}
	private boolean swarmTowards() throws GameActionException {
		// step towards closest archon if too far away, navigate normally otherwise
		MapLocation closestArchon = br.dc.getClosestArchon();
		if (closestArchon != null &&
				br.currLoc.distanceSquaredTo(closestArchon) > microDistance) {
			if (chargeTowards(closestArchon)) {
				rc.setIndicatorString(2, "Going to archon");
				return true;
			}
		}
		normalTowards();
		rc.setIndicatorString(2, "Going to target");
		return false;
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
		int distanceSquared = br.currLoc.distanceSquaredTo(target);
		for (Direction d : targetDirs) {
			if (distanceSquared < microDistance) {
				// move backwards
				if (rc.canMove(d.opposite())) {
					if (br.currDir != d) {
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
					if (br.currDir != d) {
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
		if(dir==null) 
			return false;
		if(dir == br.currDir) {
			rc.moveForward();
			br.directionToSenseIn = dir;
			dirAboutToMoveIn = null;
		} else {
			rc.setDirection(dir);
		}
		
		return true;
	}
}
