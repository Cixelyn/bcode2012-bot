package ducks;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import ducks.Debug.Owner;

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
	
	private int tooFarDistance;
	private int tooCloseDistance;
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
	
	/**
	 * Navigate forwards while maintaining a specified distance from the closest
	 * archon.
	 * @param mySwarmRadius The distance to maintain from the closest archon
	 */
	public void setSwarmMode(int tooFarDistance, int tooCloseDistance) {
		mode = MicroMode.SWARM;
		this.tooFarDistance = tooFarDistance;
		this.tooCloseDistance = tooCloseDistance;
	}
	
	/**
	 * Navigate forwards while maintaining a specified distance from target.
	 * @param myKiteDistance The distance to maintain from target.
	 */
	public void setKiteMode(int kiteDistance) {
		mode = MicroMode.KITE;
		double sqrt = Math.sqrt(kiteDistance);
		tooFarDistance = (int)((sqrt+1)*(sqrt+1))+1;
		tooCloseDistance = (int)((sqrt-1)*(sqrt-1));
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
		System.out.println("WTFFF     FFFFFFFFFFFFFFF");
		// step towards closest archon if too far away, navigate normally otherwise
		MapLocation closestArchon = br.dc.getClosestArchon();
		if (closestArchon != null) {
			System.out.println("GGGGGGGGGGGGGGGGGGGGGGGGGGGF");
			if(br.curLoc.distanceSquaredTo(closestArchon) >= tooFarDistance) {
				br.debug.setIndicatorString(2, "Going to archon", Owner.YP);
				return randomTowards(closestArchon);
			} else if (br.curLoc.distanceSquaredTo(closestArchon) <= tooCloseDistance) {
				br.debug.setIndicatorString(2, "backing to archon", Owner.YP);
				MapLocation locAwayFromArchon = br.curLoc.add(closestArchon.directionTo(br.curLoc), 5);
				return randomTowards(locAwayFromArchon);
			}
		}
		System.out.println("eawwwwwwwwwwwwwwwwwwwwwww");
		normalTowards();
		br.debug.setIndicatorString(2, "normal towards", Owner.YP);
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
		int distanceSquared = br.curLoc.distanceSquaredTo(target);
		for (Direction d : targetDirs) {
			if (distanceSquared < tooFarDistance) {
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
	
	private boolean randomTowards(MapLocation target) throws GameActionException {
		if(dirAboutToMoveIn == null) {
			Direction dir = br.nav.navigateRandomly(target);
			if(dir==null || dir == Direction.OMNI || dir == Direction.NONE)
				return false;
			dirAboutToMoveIn = dir;
		}
		
		Direction dir = br.nav.wiggleToMovableDirection(dirAboutToMoveIn);
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
	
	public MapLocation getObjective() {
		return this.objective;
	}
}
