package ducks;

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
		/** Want to low flux hibernate, need to find a non-blocking place to do it. */
		LOOKING_TO_LOW_FLUX_HIBERNATE,
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
	boolean archonSwarmTargetIsMoving;
	int archonSwarmTime;
	BehaviorState behavior;
	MapLocation hibernateTarget;
	double energonLastTurn;
	double fluxLastTurn;
	MapLocation enemySpottedTarget;
	int enemySpottedRound;
	int roundLastWakenUp;
	boolean checkedBehind;
	boolean movingTarget;
	
	public SoldierRobot(RobotController myRC) throws GameActionException {
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
		archonSwarmTime = -55555;
		energonLastTurn = 0;
		fluxLastTurn = 0;
		checkedBehind = false;
	}

	@Override
	public void run() throws GameActionException {
		if((behavior==BehaviorState.SWARM || behavior==BehaviorState.LOOKING_TO_HIBERNATE ||
				behavior==BehaviorState.LOST || behavior==BehaviorState.REFUEL) && 
				rc.isMovementActive() && !msm.justMoved()) {
			// No action if unnecessary
			
			energonLastTurn = curEnergon;
			fluxLastTurn = rc.getFlux();
			return;
		}
		
		
		
		// Scan everything
		radar.scan(true, true);
		
		int closestEnemyID = er.getClosestEnemyID();
		MapLocation closestEnemyLocation = closestEnemyID==-1 ? null : 
			er.enemyLocationInfo[closestEnemyID];
		if(closestEnemyLocation!=null && rc.canSenseSquare(closestEnemyLocation))
			closestEnemyLocation = null;
		RobotInfo radarClosestEnemy = radar.closestEnemy;
		if(radarClosestEnemy!=null && (closestEnemyLocation==null || 
				(radar.closestEnemyDist <= curLoc.distanceSquaredTo(closestEnemyLocation)))) {
			closestEnemyLocation = radarClosestEnemy.location;
		}
		boolean enemyNearby = closestEnemyLocation != null && 
				curLoc.distanceSquaredTo(closestEnemyLocation) <= 25;
		if(curRound%ExtendedRadarSystem.ALLY_MEMORY_TIMEOUT == myID%ExtendedRadarSystem.ALLY_MEMORY_TIMEOUT)
			radar.broadcastEnemyInfo(enemyNearby);
		
		movingTarget = true;
		if(behavior == BehaviorState.LOOKING_TO_LOW_FLUX_HIBERNATE) { 
			// don't hibernate on powernodes
			if (rc.senseObjectAtLocation(curLoc, RobotLevel.POWER_NODE)==null)
			{
				// Hibernate once we're no longer adjacent to any allies
				int adjacentMovable = 0;
				if(!rc.canMove(Direction.NORTH)) adjacentMovable++;
				if(!rc.canMove(Direction.EAST)) adjacentMovable++;
				if(!rc.canMove(Direction.WEST)) adjacentMovable++;
				if(!rc.canMove(Direction.SOUTH)) adjacentMovable++;
				if(adjacentMovable<=1)
					behavior = BehaviorState.LOW_FLUX_HIBERNATE;
			}
		} else if(closestEnemyLocation != null) {
			if((curEnergon < energonLastTurn || rc.getFlux() < fluxLastTurn-1) && 
					curLoc.distanceSquaredTo(closestEnemyLocation) > 20) {
				// Current target is too far away, got hit from behind probably
				behavior = BehaviorState.LOOK_AROUND_FOR_ENEMIES;
				checkedBehind = false;
				
			} else if(enemyNearby) {
				// If we know of an enemy, lock onto it
				behavior = BehaviorState.ENEMY_DETECTED;
				target = closestEnemyLocation;
				lockAcquiredRound = curRound;
			} else {
				// Look for enemy from the ER
				behavior = BehaviorState.SEEK;
				target = closestEnemyLocation; 
			}
			
		} else if((curEnergon < energonLastTurn || rc.getFlux() < fluxLastTurn-1) || 
				(behavior == BehaviorState.LOOK_AROUND_FOR_ENEMIES && !checkedBehind)) {
			// Got hurt since last turn.. look behind you
			behavior = BehaviorState.LOOK_AROUND_FOR_ENEMIES;
			checkedBehind = false;
			
		} else if(behavior == BehaviorState.ENEMY_DETECTED && curRound < lockAcquiredRound + 12) {
			// Don't know of any enemies, stay chasing the last enemy we knew of
			behavior = BehaviorState.ENEMY_DETECTED;
			
		} else if(curRound < enemySpottedRound + Constants.ENEMY_SPOTTED_SIGNAL_TIMEOUT) {
			// Not even chasing anyone, try going to the enemy spotted signal
			behavior = BehaviorState.SEEK;
			target = enemySpottedTarget;
			movingTarget = false;
			
		} else {
			int distToClosestArchon = curLoc.distanceSquaredTo(dc.getClosestArchon());
			if((behavior==BehaviorState.LOST && distToClosestArchon>32) || 
					distToClosestArchon>64) {
				// If all allied archons are far away, move to closest one
				behavior = BehaviorState.LOST;
				target = dc.getClosestArchon();
				
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
					
				} else if(curRound > archonSwarmTime+12) { 
					// We did not receive any swarm target broadcasts from our archons
					behavior = BehaviorState.SWARM;
					target = dc.getClosestArchon();
					
				} else {
					// Follow target of closest archon's broadcast
					behavior = BehaviorState.SWARM ;
					target = archonSwarmTarget;
					movingTarget = archonSwarmTargetIsMoving;
				}
				
				if(behavior == BehaviorState.SWARM && 
						closestSwarmTargetSenderDist <= 18 && 
						curLoc.distanceSquaredTo(target) <= 26 && 
						curRound > roundLastWakenUp + 10) { 
					// Close enough to swarm target, look for a place to hibernate
					behavior = BehaviorState.LOOKING_TO_HIBERNATE;
					hibernateTarget = target;
				} 
			}
		}
		
		// Check if we need more flux
		if(behavior == BehaviorState.SWARM || behavior == BehaviorState.LOST ||
				behavior == BehaviorState.LOOKING_TO_HIBERNATE || behavior == BehaviorState.SEEK) {
			if(rc.getFlux() < 10) {
				if(rc.getFlux() < Math.sqrt(curLoc.distanceSquaredTo(dc.getClosestArchon()))) {
					// Too low flux, can't reach archon
					if(curRound > roundLastWakenUp + 10)
						behavior = BehaviorState.LOOKING_TO_LOW_FLUX_HIBERNATE;
				} else {
					// Needs to get flux from archon
					behavior = BehaviorState.REFUEL;
					target = dc.getClosestArchon();
					movingTarget = true;
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
				movingTarget = true;
			} 
		}
		
		// Set nav target - if we have a moving target, don't change target 
		// 		unless it's 20 dist away from previous target or the bug is not tracing or we're adjacent to the old target
		if(!movingTarget || previousBugTarget==null || 
				!nav.isBugTracing() ||
				target.distanceSquaredTo(previousBugTarget)>20 || 
				curLoc.distanceSquaredTo(previousBugTarget)<=2) {
			nav.setDestination(target);
			previousBugTarget = target;
		}
		
		
		// Set the flux balance mode
		if(behavior == BehaviorState.SWARM)
			fbs.setBatteryMode();
		else
			fbs.setPoolMode();
		
		// Set debug string
		dbg.setIndicatorString('e', 1, "Target="+locationToVectorString(target)+", Behavior="+behavior);
		
		// Enter hibernation if desired
		if(behavior == BehaviorState.HIBERNATE || behavior == BehaviorState.LOW_FLUX_HIBERNATE) {
			if(behavior == BehaviorState.HIBERNATE)
				hsys.setMode(HibernationSystem.MODE_NORMAL);
			else 
				hsys.setMode(HibernationSystem.MODE_LOW_FLUX);
			
			
			msm.reset();
			er.reset();
			int ec = hsys.run();
			
			// Come out of hibernation
			if(ec == HibernationSystem.EXIT_ATTACKED) {
				radar.scan(false, true);
				if(radar.closestEnemy==null) {
					behavior = BehaviorState.LOOK_AROUND_FOR_ENEMIES;
					checkedBehind = false;
				} else {
					behavior = BehaviorState.ENEMY_DETECTED;
					tryToAttack();
				}
			} else if(ec == HibernationSystem.EXIT_MESSAGED) {
				behavior = BehaviorState.SWARM;
			} else if(ec == HibernationSystem.EXIT_REFUELED) {
				behavior = BehaviorState.SWARM;
			}
			roundLastWakenUp = curRound;
			target = (behavior == BehaviorState.ENEMY_DETECTED) ? radar.closestEnemy.location : curLoc;
			nav.setDestination(target);
			
			// Set debug string upon coming out of hibernation
			dbg.setIndicatorString('e', 1, "Target=<"+(target.x-curLoc.x)+","+
					(target.y-curLoc.y)+">, Behavior="+behavior);
		}
		
		// Update end of turn variables
		energonLastTurn = curEnergon;
		fluxLastTurn = rc.getFlux();
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
			if(dist<closestSwarmTargetSenderDist || curRound > archonSwarmTime+5) {
				closestSwarmTargetSenderDist = dist;
				archonSwarmTargetIsMoving = shorts[0] != 0;
				archonSwarmTarget = new MapLocation(shorts[1], shorts[2]);
				archonSwarmTime = curRound;
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
			checkedBehind = true;
			return new MoveInfo(curDir.opposite());
			
		} else if(behavior == BehaviorState.LOOKING_TO_HIBERNATE) {
			// If we're looking to hibernate, move around randomly
			if(Util.randDouble()<0.2)
				return new MoveInfo(curLoc.directionTo(target).opposite(), false);
			else
				return new MoveInfo(nav.navigateCompletelyRandomly(), false);
			
		} else if(behavior == BehaviorState.LOOKING_TO_LOW_FLUX_HIBERNATE) {
			// If we're looking to low flux hibernate, move around randomly
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
				if(behavior == BehaviorState.SWARM && !nav.isBugTracing()) {
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
			int strengthDifference = er.getStrengthDifference(midpoint, 24);
			boolean weHaveBiggerFront = strengthDifference > 0;
			boolean targetIsRanged = radar.numEnemyDisruptors + radar.numEnemyScorchers > 0;
			int tooClose = weHaveBiggerFront ? -1 : (targetIsRanged ? 10 : 5);
			int tooFar = weHaveBiggerFront ? 4 : (targetIsRanged ? 26 : 26);
			int distToTarget = curLoc.distanceSquaredTo(target);
			Direction dirToTarget = curLoc.directionTo(target);
			
			// If we are much stronger and my energon is low, retreat to nearest archon
			if(curEnergon <= 12 && strengthDifference > Util.getOwnStrengthEstimate(rc)) {
				return new MoveInfo(curLoc.directionTo(dc.getClosestArchon()), true);
				
			// If we aren't turned the right way, turn towards target
			} else if(distToTarget <= 13 && (curDir.ordinal()-dirToTarget.ordinal()+9)%8 > 2) {
				return new MoveInfo(dirToTarget);
				
			// If we are too close to the target, back off
			} else if(distToTarget <= tooClose) {
				if(targetIsRanged) {
					return new MoveInfo(dirToTarget.opposite(), true);
				} else {
					if(rc.canMove(curDir.opposite()))
						return new MoveInfo(curDir.opposite(), true);
				}
				
			// If we are too far from the target, advance
			} else if(distToTarget >= tooFar) {
				if(distToTarget <= 5) {
					if(rc.canMove(dirToTarget))
						return new MoveInfo(dirToTarget, false);
					else if(rc.canMove(dirToTarget.rotateLeft()) && 
							isOptimalAdvancingDirection(dirToTarget.rotateLeft(), target, dirToTarget))
						return new MoveInfo(dirToTarget.rotateLeft(), false);
					else if(rc.canMove(dirToTarget.rotateRight()) && 
							isOptimalAdvancingDirection(dirToTarget.rotateRight(), target, dirToTarget))
						return new MoveInfo(dirToTarget.rotateRight(), false);
					else
						return new MoveInfo(dirToTarget);
				} else if(distToTarget >= 26) {
					return new MoveInfo(nav.navigateToDestination(), false);
				} else {
					return new MoveInfo(dirToTarget, false);
				}
			}
			
		} else {
			// Go towards target
			return new MoveInfo(nav.navigateToDestination(), false);
		}
		
		// Default action is turning towards target
		return new MoveInfo(curLoc.directionTo(target));
	}
	
	/** If the enemy is <dx, dy> away from me, is the given direction a reasonable direction to move? 
	 * I want to ensure that I can still attack him if I move in that direction, without having to turn
	 * in a different direction afterwards.
	 */
	private boolean isOptimalAdvancingDirection(Direction dir, MapLocation target, Direction dirToTarget) {
		int dx = target.x-curLoc.x;
		int dy = target.y-curLoc.y;
		switch(dx) {
		case -2:
			if(dy==1) return dir==Direction.WEST || dir==Direction.SOUTH_WEST;
			else if(dy==-1) return dir==Direction.WEST || dir==Direction.NORTH_WEST;
			break;
		case -1:
			if(dy==1) return dir==Direction.SOUTH || dir==Direction.SOUTH_WEST;
			else if(dy==-1) return dir==Direction.NORTH || dir==Direction.NORTH_WEST;
			break;
		case 1:
			if(dy==2) return dir==Direction.SOUTH || dir==Direction.SOUTH_EAST;
			else if(dy==-2) return dir==Direction.NORTH || dir==Direction.NORTH_EAST;
			break;
		case 2:
			if(dy==1) return dir==Direction.EAST || dir==Direction.SOUTH_EAST;
			else if(dy==-1) return dir==Direction.EAST || dir==Direction.NORTH_EAST;
			break;
		default:
			break;
		}
		return dir == dirToTarget;
	}
	
	@Override
	public void useExtraBytecodes() throws GameActionException {
		super.useExtraBytecodes();
		fluxLastTurn = rc.getFlux();
	}
}
