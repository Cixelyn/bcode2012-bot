package ducks;

import ducks.HibernationSystem.HibernationMode;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotLevel;

public class SoldierRobot extends BaseRobot {
	private enum BehaviorState {
		/** Want to hibernate, need to find a non-blocking place to do it. */
		LOOKING_TO_HIBERNATE,
		/** Hibernate until someone wakes it up. */
		HIBERNATE,
		/** Ran very low on flux, will hibernate until someone transfers it flux. */
		LOW_FLUX_HIBERNATE,
		/** Has too much flux (from being a battery), needs to give it back to archon. */
		POOL,
		/** No enemies to deal with, swarming with archon. */
		SWARM,
		/** Heard of an enemy spotted call, but no enemy info calls yet. */
		SEEK,
		/** Run away from enemy forces. */
		RETREAT, 
		/** Far from target. Use bug to navigate. */
		LOST,
		/** Need to refuel. Go to nearest archon. */
		REFUEL,
		/** Tracking closest enemy, even follow them for 12 turns. */
		ENEMY_DETECTED,
		/** Getting hit somehow, don't know from where. */
		LOOK_AROUND_FOR_ENEMIES,
	}
	int lockAcquiredRound;
	MapLocation target;
	MapLocation previousBugTarget;
	int closestSwarmTargetSenderDist;
	MapLocation archonSwarmTarget;
	BehaviorState behavior;
	MapLocation hibernateTarget;
	double energonLastTurn;
	boolean archonTargetIsEnemy;
	MapLocation enemySpottedTarget;
	int enemySpottedRound;
	
	public SoldierRobot(RobotController myRC) throws GameActionException {
		super(myRC);
		
		lockAcquiredRound = -1;
		closestSwarmTargetSenderDist = Integer.MAX_VALUE;
		nav.setNavigationMode(NavigationMode.GREEDY);
		io.setChannels(new BroadcastChannel[] {
				BroadcastChannel.ALL, 
				BroadcastChannel.SOLDIERS,
				BroadcastChannel.EXTENDED_RADAR,
		});
		fbs.setPoolMode();
		behavior = BehaviorState.SWARM;
		archonTargetIsEnemy = false;
		enemySpottedTarget = null;
		enemySpottedRound = -55555;
	}

	@Override
	public void run() throws GameActionException {
		
		// Scan everything every turn
		radar.scan(true, true);
		
		MapLocation closestEnemyLocation = er.getClosestEnemyLocation();
		if(closestEnemyLocation!=null && rc.canSenseSquare(closestEnemyLocation))
			closestEnemyLocation = null;
		MapLocation radarClosestEnemyLocation = radar.closestEnemy==null ? 
				null : radar.closestEnemy.location;
		if(closestEnemyLocation==null || (radarClosestEnemyLocation!=null && 
				curLoc.distanceSquaredTo(closestEnemyLocation) < 
				curLoc.distanceSquaredTo(radarClosestEnemyLocation)))
			closestEnemyLocation = radarClosestEnemyLocation;
		if(curRound%5 == myID%5)
			radar.broadcastEnemyInfo(closestEnemyLocation != null && 
					curLoc.distanceSquaredTo(closestEnemyLocation) < 25);
		
		if(closestEnemyLocation != null) {
			// If we know of an enemy, lock onto it
			behavior = BehaviorState.ENEMY_DETECTED;
			target = closestEnemyLocation;
			nav.setNavigationMode(NavigationMode.GREEDY);
			lockAcquiredRound = curRound;
			
		} else if(curEnergon < energonLastTurn) {
			// Got hurt since last turn.. look behind you
			behavior = BehaviorState.LOOK_AROUND_FOR_ENEMIES;
			
		} else if(behavior == BehaviorState.ENEMY_DETECTED && curRound < lockAcquiredRound + 12) {
			// Don't know of any enemies, stay chasing the last enemy we knew of
			behavior = BehaviorState.ENEMY_DETECTED;
			
		} else {
			int distToClosestArchon = curLoc.distanceSquaredTo(dc.getClosestArchon());
			if((behavior==BehaviorState.LOST && distToClosestArchon>25) || 
					distToClosestArchon>64) {
				// If all allied archons are far away, move to closest one
				behavior = BehaviorState.LOST;
				nav.setNavigationMode(NavigationMode.BUG);
				target = dc.getClosestArchon();
				if(previousBugTarget!=null && target.distanceSquaredTo(previousBugTarget)<=25)
					target = previousBugTarget;
				else
					previousBugTarget = target;
				
			} else {	
				if(behavior == BehaviorState.LOOKING_TO_HIBERNATE && 
						archonSwarmTarget.equals(hibernateTarget) && !curLoc.equals(hibernateTarget)) {
					// Hibernate once we're no longer adjacent to any allies
					int adjacentMovable = 0;
					if(!rc.canMove(Direction.NORTH)) adjacentMovable++;
					if(!rc.canMove(Direction.EAST)) adjacentMovable++;
					if(!rc.canMove(Direction.WEST)) adjacentMovable++;
					if(!rc.canMove(Direction.SOUTH)) adjacentMovable++;
					if(adjacentMovable<=1)
						behavior = BehaviorState.HIBERNATE;
					
				} else if(closestSwarmTargetSenderDist == Integer.MAX_VALUE) { 
					// We did not receive any swarm target broadcasts from our archons
					behavior = BehaviorState.SWARM;
					target = dc.getClosestArchon();
					
				} else {
					// Follow target of closest archon's broadcast
					behavior = BehaviorState.SWARM ;
					target = archonSwarmTarget;
				}
				
				if(behavior == BehaviorState.SWARM && 
						closestSwarmTargetSenderDist <= 10 && 
						curLoc.distanceSquaredTo(target) <= 10) { 
					// Close enough to swarm target, look for a place to hibernate
					behavior = BehaviorState.LOOKING_TO_HIBERNATE;
					hibernateTarget = target;
				} 
				nav.setNavigationMode(NavigationMode.GREEDY);
				previousBugTarget = null;
			}
		}
		
		// Check if we need more flux
		if(behavior == BehaviorState.SWARM || behavior == BehaviorState.LOST ||
				behavior == BehaviorState.LOOKING_TO_HIBERNATE) {
			if(rc.getFlux() < 10) {
				if(rc.getFlux() < Math.sqrt(curLoc.distanceSquaredTo(dc.getClosestArchon()))) {
					// Too low flux, can't reach archon
					behavior = BehaviorState.LOW_FLUX_HIBERNATE;
				} else {
					// Needs to get flux from archon
					behavior = BehaviorState.REFUEL;
					target = dc.getClosestArchon();
				}
			}
		} 
		
		// Attack an enemy if there is some unit in our attackable squares
		if(!rc.isAttackActive()) {
			RobotInfo bestInfo = null;
			double bestValue = Double.MAX_VALUE;
			for(int n=0; n<radar.numEnemyRobots; n++) {
				RobotInfo ri = radar.enemyInfos[radar.enemyRobots[n]];
				if(!rc.canAttackSquare(ri.location)) 
					continue;
				if((bestValue <= myType.attackPower && ri.energon <= myType.attackPower) ?
						ri.energon > bestValue : ri.energon < bestValue) {
					// Say a soldier does 6 damage. We prefer hitting units with less energon, but we also would rather hit a unit with 5 energon than a unit with 1 energon.
					bestInfo = ri;
					bestValue = ri.energon;
				}
			}
			
			if(bestInfo!=null) {
				if(bestValue <= myType.attackPower) {
					er.broadcastKill(bestInfo.robot.getID());
				}
				rc.attackSquare(bestInfo.location, bestInfo.type.level);
			}
		}
		
		// Check if we have too much flux
		if(behavior == BehaviorState.ENEMY_DETECTED) {
			if(rc.getFlux() > myMaxEnergon*2/3) {
				behavior = BehaviorState.POOL;
				target = dc.getClosestArchon();
			} 
		}
		
		// Set nav target
		nav.setDestination(target);
		
		// Set the flux balance mode
		if(behavior == BehaviorState.SWARM && !archonTargetIsEnemy)
			fbs.setBatteryMode();
		else
			fbs.setPoolMode();
		
		// Set debug string
		dbg.setIndicatorString('e', 1, "Target=<"+(target.x-curLoc.x)+","+(target.y-curLoc.y)+">, Behavior="+behavior);
		
		// Enter hibernation if desired
		if(behavior == BehaviorState.HIBERNATE) {
			hsys.setMode(HibernationMode.NORMAL);
			HibernationSystem.ExitCode ec = hsys.run();
			if(ec == HibernationSystem.ExitCode.ATTACKED)
				behavior = BehaviorState.LOOK_AROUND_FOR_ENEMIES;
			else if(ec == HibernationSystem.ExitCode.MESSAGED)
				behavior = BehaviorState.SWARM;
			target = curLoc;
			nav.setDestination(target);
		} else if(behavior == BehaviorState.LOW_FLUX_HIBERNATE) {
			hsys.setMode(HibernationMode.LOW_FLUX);
			HibernationSystem.ExitCode ec = hsys.run();
			if(ec == HibernationSystem.ExitCode.ATTACKED)
				behavior = BehaviorState.LOOK_AROUND_FOR_ENEMIES;
			else if(ec == HibernationSystem.ExitCode.MESSAGED)
				behavior = BehaviorState.SWARM;
			else if(ec == HibernationSystem.ExitCode.REFUELED)
				behavior = BehaviorState.LOST;
			target = curLoc;
			nav.setDestination(target);
		}
		
		// Update end of turn variables
		closestSwarmTargetSenderDist = Integer.MAX_VALUE;
		energonLastTurn = curEnergon;
			
	}
	
	@Override
	public void processMessage(BroadcastType msgType, StringBuilder sb) throws GameActionException {
		int[] shorts;
		switch(msgType) {
		case ENEMY_SPOTTED:
			shorts = BroadcastSystem.decodeUShorts(sb);
			MapLocation newEnemySpottedTarget = new MapLocation(shorts[1], shorts[2]);
			if(enemySpottedTarget==null || curLoc.distanceSquaredTo(enemySpottedTarget) <
					curLoc.distanceSquaredTo(enemySpottedTarget)) {
				enemySpottedRound = shorts[0];
				enemySpottedTarget = newEnemySpottedTarget;
			}
			break;
		case SWARM_TARGET:
			shorts = BroadcastSystem.decodeUShorts(sb);
			int dist = curLoc.distanceSquaredTo(BroadcastSystem.decodeSenderLoc(sb));
			if(dist<closestSwarmTargetSenderDist) {
				closestSwarmTargetSenderDist = dist;
				archonSwarmTarget = new MapLocation(shorts[1], shorts[2]);
			}
			break;
		case ENEMY_INFO:
			er.integrateEnemyInfo(BroadcastSystem.decodeUShorts(sb));
			break;
		case ENEMY_KILL:
			er.integrateEnemyKill(BroadcastSystem.decodeShort(sb));
			break;
		default:
			super.processMessage(msgType, sb);
		} 
	}
	
	@Override
	public MoveInfo computeNextMove() throws GameActionException {
		if(rc.getFlux()<0.8) return null;
		
		if(behavior == BehaviorState.LOOK_AROUND_FOR_ENEMIES) {
			// Just turn around once
			return new MoveInfo(curDir.opposite());
		} else if(behavior == BehaviorState.LOOKING_TO_HIBERNATE) {
			// If we're looking to hibernate, move around randomly
			return new MoveInfo(nav.navigateCompletelyRandomly(), false);
		} else if(behavior == BehaviorState.SWARM ) {
			// If we're on top of our target power node, move around randomly
			if(curLoc.equals(target) && 
					rc.senseObjectAtLocation(curLoc, RobotLevel.POWER_NODE)!=null) {
				return new MoveInfo(nav.navigateCompletelyRandomly(), false);
			}
			// If we're far from the swarm target, follow normal swarm rules
			if(curLoc.distanceSquaredTo(target) >= 11) {
				Direction dir = nav.navigateToDestination();
				if(dir==null) return null;
				if(behavior == BehaviorState.SWARM) {
					if(radar.alliesInFront==0 && Math.random()<0.75) 
						return null;
					if(radar.alliesInFront > 3 && Math.random()<0.05 * radar.alliesInFront) 
						dir = nav.navigateCompletelyRandomly();
					if(radar.alliesOnLeft > radar.alliesOnRight && Math.random()<0.3) 
						dir = dir.rotateRight();
					else if(radar.alliesOnLeft < radar.alliesOnRight && Math.random()<0.3) 
						dir = dir.rotateLeft();
				}
				return new MoveInfo(dir, false);
				
			// If we're fairly close, and there's lots of allies around, move randomly
			} else if(curLoc.distanceSquaredTo(target) >= 2) {
				if(radar.alliesInFront > 3 && Math.random()<0.05 * radar.alliesInFront) 
					return new MoveInfo(nav.navigateCompletelyRandomly(), false);
			}
			
		} else if(behavior == BehaviorState.ENEMY_DETECTED) {
			// Fighting an enemy, kite target
			MapLocation midpoint = new MapLocation((curLoc.x+target.x)/2, (curLoc.y+target.y)/2);
			boolean weHaveBiggerFront = er.getEnergonDifference(midpoint, 25) > 0;
			int tooClose = weHaveBiggerFront ? -1 : 5;
			int tooFar = weHaveBiggerFront ? 4 : 25;
			int distToTarget = curLoc.distanceSquaredTo(target);
			Direction dirToTarget = curLoc.directionTo(target);
			boolean turnToFaceEnemyFirst = distToTarget <= 13;
			
			if(turnToFaceEnemyFirst && dirToTarget!=curDir) {
				return new MoveInfo(dirToTarget);
			} else if(distToTarget <= tooClose) {
				Direction dir = dirToTarget.opposite();
				if(rc!=null && rc.canMove(dir))
					return new MoveInfo(dir, true);
			} else if(distToTarget >= tooFar) {
				return new MoveInfo(nav.navigateToDestination(), false);
			}
		} else {
			return new MoveInfo(nav.navigateToDestination(), false);
		}
		
		return new MoveInfo(curLoc.directionTo(target));
	}
}
