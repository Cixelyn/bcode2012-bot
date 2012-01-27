package ducks;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;

public class DisrupterRobotYP extends BaseRobot {
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
	MapLocation enemySpottedTarget;
	int enemySpottedRound;
	int roundLastWakenUp;
	
	MapLocation closestEnemyLocation;
	RobotType closestEnemyType;
	
	
	public DisrupterRobotYP(RobotController myRC) throws GameActionException {
		super(myRC);
		
		lockAcquiredRound = -1;
		closestSwarmTargetSenderDist = Integer.MAX_VALUE;
		nav.setNavigationMode(NavigationMode.BUG);
		io.setChannels(new BroadcastChannel[] {
				BroadcastChannel.ALL, 
				BroadcastChannel.SOLDIERS,
				BroadcastChannel.EXTENDED_RADAR,
		});
		fbs.setPoolMode();
		behavior = BehaviorState.SWARM;
		enemySpottedTarget = null;
		enemySpottedRound = -55555;
		roundLastWakenUp = -55555;
	}

	@Override
	public void run() throws GameActionException {
		
		// Scan everything every turn
		radar.scan(true, true);
		
		int closestEnemyID = er.getClosestEnemyID();
		closestEnemyLocation = closestEnemyID==-1 ? null : 
			er.enemyLocationInfo[closestEnemyID];
		closestEnemyType = closestEnemyID==-1 ? null : 
			er.enemyTypeInfo[closestEnemyID];
		if(closestEnemyLocation!=null && rc.canSenseSquare(closestEnemyLocation))
			closestEnemyLocation = null;
		RobotInfo radarClosestEnemy = radar.closestEnemy;
		if(radarClosestEnemy!=null && (closestEnemyLocation==null || (radarClosestEnemy.location!=null && 
				curLoc.distanceSquaredTo(closestEnemyLocation) <=
				curLoc.distanceSquaredTo(radarClosestEnemy.location)))) {
			closestEnemyLocation = radarClosestEnemy.location;
			closestEnemyType = radarClosestEnemy.type;
		}
		if(curRound%5 == myID%5)
			radar.broadcastEnemyInfo(closestEnemyLocation != null && 
					curLoc.distanceSquaredTo(closestEnemyLocation) <= 25);
		
		if(closestEnemyLocation != null) {
			
			if(curEnergon < energonLastTurn && curLoc.distanceSquaredTo(closestEnemyLocation) > 20) {
				// Current target is too far away, got hit from behind probably
				behavior = BehaviorState.LOOK_AROUND_FOR_ENEMIES;
			} else {
			
				// If we know of an enemy, lock onto it
				behavior = BehaviorState.ENEMY_DETECTED;
				target = closestEnemyLocation;
				nav.setNavigationMode(NavigationMode.BUG);
				lockAcquiredRound = curRound;
			}
			
		} else if(curEnergon < energonLastTurn) {
			// Got hurt since last turn.. look behind you
			behavior = BehaviorState.LOOK_AROUND_FOR_ENEMIES;
			
		} else if(behavior == BehaviorState.ENEMY_DETECTED && curRound < lockAcquiredRound + 12) {
			// Don't know of any enemies, stay chasing the last enemy we knew of
			behavior = BehaviorState.ENEMY_DETECTED;
			
		} else if(curRound < enemySpottedRound + Constants.ENEMY_SPOTTED_SIGNAL_TIMEOUT) {
			// Not even chasing anyone, try going to the enemy spotted signal
			behavior = BehaviorState.SEEK;
			target = enemySpottedTarget;
			
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
						closestSwarmTargetSenderDist <= 18 && 
						curLoc.distanceSquaredTo(target) <= 26 && 
						curRound > roundLastWakenUp + 10) { 
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
				behavior == BehaviorState.LOOKING_TO_HIBERNATE || behavior == BehaviorState.SEEK) {
			if(rc.getFlux() < 10) {
				if(rc.getFlux() < Math.sqrt(curLoc.distanceSquaredTo(dc.getClosestArchon()))) {
					// Too low flux, can't reach archon
					if(curRound > roundLastWakenUp + 10)
						behavior = BehaviorState.LOW_FLUX_HIBERNATE;
				} else {
					// Needs to get flux from archon
					behavior = BehaviorState.REFUEL;
					target = dc.getClosestArchon();
				}
			}
		} 
		
		// Attack an enemy if there is some unit in our attackable squares
		tryToAttack();
		
		// Check if we have too much flux
		if(behavior == BehaviorState.ENEMY_DETECTED || behavior == BehaviorState.SEEK) {
			if(rc.getFlux() > myMaxEnergon*2/3) {
				behavior = BehaviorState.POOL;
				target = dc.getClosestArchon();
			} 
		}
		
		// Set nav target
		nav.setDestination(target);
		
		// Set the flux balance mode
		if(behavior == BehaviorState.SWARM)
			fbs.setBatteryMode();
		else
			fbs.setPoolMode();
		
		// Set debug string
		dbg.setIndicatorString('e', 1, "Target=<"+(target.x-curLoc.x)+","+
				(target.y-curLoc.y)+">, Behavior="+behavior);
		
		// Enter hibernation if desired
		if(behavior == BehaviorState.HIBERNATE || behavior == BehaviorState.LOW_FLUX_HIBERNATE) {
			if(behavior == BehaviorState.HIBERNATE)
				hsys.setMode(HibernationSystem.MODE_NORMAL);
			else 
				hsys.setMode(HibernationSystem.MODE_LOW_FLUX);
			int ec = hsys.run();
			if(ec == HibernationSystem.EXIT_ATTACKED) {
				radar.scan(false, true);
				if(radar.closestEnemy==null)
					behavior = BehaviorState.LOOK_AROUND_FOR_ENEMIES;
				else
					tryToAttack();
			} else if(ec == HibernationSystem.EXIT_MESSAGED) {
				behavior = BehaviorState.SWARM;
			} else if(ec == HibernationSystem.EXIT_REFUELED) {
				behavior = BehaviorState.SWARM;
			}
			roundLastWakenUp = curRound;
			target = curLoc;
			nav.setDestination(target);
		}
		
		// Update end of turn variables
		closestSwarmTargetSenderDist = Integer.MAX_VALUE;
		energonLastTurn = curEnergon;
			
	}
	
	private void tryToAttack() throws GameActionException {
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
	}
	
	@Override
	public void processMessage(BroadcastType msgType, StringBuilder sb) throws GameActionException {
		int[] shorts;
		switch(msgType) {
		case ENEMY_SPOTTED:
			shorts = BroadcastSystem.decodeUShorts(sb);
			if(shorts[0] > enemySpottedRound) {
				enemySpottedRound = shorts[0];
				enemySpottedTarget = new MapLocation(shorts[1], shorts[2]);
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
		if(rc.getFlux()<1.6) return new MoveInfo(curLoc.directionTo(target));
		
		if(behavior == BehaviorState.LOOK_AROUND_FOR_ENEMIES) {
			// Just turn around once
			return new MoveInfo(curDir.opposite());
		} else if(behavior == BehaviorState.LOOKING_TO_HIBERNATE) {
			// If we're looking to hibernate, move around randomly
			if(Util.randDouble()<0.2)
				return new MoveInfo(curLoc.directionTo(target).opposite(), false);
			else
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
					if(radar.alliesInFront==0 && Util.randDouble()<0.6) 
						return null;
					if(radar.alliesInFront > 3 && Util.randDouble()<0.05 * radar.alliesInFront) 
						dir = nav.navigateCompletelyRandomly();
					if(radar.alliesOnLeft > radar.alliesOnRight && Util.randDouble()<0.4) 
						dir = dir.rotateRight();
					else if(radar.alliesOnLeft < radar.alliesOnRight && Util.randDouble()<0.4) 
						dir = dir.rotateLeft();
				}
				return new MoveInfo(dir, false);
				
			// If we're fairly close, and there's lots of allies around, move randomly
			} else if(curLoc.distanceSquaredTo(target) >= 2) {
				if(radar.alliesInFront > 3 && Util.randDouble()<0.05 * radar.alliesInFront) 
					return new MoveInfo(nav.navigateCompletelyRandomly(), false);
			}
			
		} else if(behavior == BehaviorState.ENEMY_DETECTED) {
			// Fighting an enemy, kite target
			MapLocation midpoint = new MapLocation((curLoc.x+target.x)/2, (curLoc.y+target.y)/2);
			boolean weHaveBiggerFront = er.getStrengthDifference(midpoint, 25) > 0;
			boolean targetIsRanged = closestEnemyType==RobotType.DISRUPTER || 
					closestEnemyType==RobotType.SCORCHER;
			int tooCloseCantRetreat = targetIsRanged ? 3 : 1;
			int tooClose = weHaveBiggerFront ? (targetIsRanged ? 7 : 5) : (targetIsRanged ? 10 : 8);
			int tooFar = weHaveBiggerFront ? 10 : (targetIsRanged ? 26 : 26);
			int distToTarget = curLoc.distanceSquaredTo(target);
			Direction dirToTarget = curLoc.directionTo(target);
			boolean turnToFaceEnemyFirst = distToTarget <= 13;
			
			if(turnToFaceEnemyFirst && dirToTarget!=curDir) {
				return new MoveInfo(dirToTarget);
			} else if(distToTarget>tooCloseCantRetreat && distToTarget <= tooClose) {
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
