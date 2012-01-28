package veryharderai;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class ScoutRobot extends BaseRobot {
	
	private enum StrategyState {
		/** Look for map edges so we have an idea where to find the enemy. */
		INITIAL_EXPLORE,
		/** Look around for the enemy. */
		SCOUT_ENEMY,
		/** Battle. Help target and transfer flux and heal. */
		BATTLE,
		/** No enemies around, heal up the army. */
		HEAL,
	}
	private enum BehaviorState {
		/** Go in a given direction until we see a new map edge or any enemy unit, then go back. */
		LOOK_FOR_MAP_EDGE_OR_ENEMY,
		/** Reports the newfound map edge. */
		REPORT_MAP_EDGE,
		/** Reports the enemy sighting. */
		REPORT_ENEMY_SIGHTING,
		/** Go around looking for enemies. */
		SCOUT_FOR_ENEMIES,
		/** Stand near the front lines and help target and heal. Kite enemies. */
		SUPPORT_FRONT_LINES,
		/** Found an ally to give flux to, go give him flux. */
		SENDING_ALLY_FLUX,
		/** Stand on top of an archon. Follow it around. Do nothing else. */
		PET,
		/** At swarm target, hibernate until attacked or messaged to wake up. */
		HIBERNATE,
		/** Low on flux, need another scout to come give it some flux. */
		LOW_FLUX_HIBERNATE,
	}
	
	private StrategyState strategy;
	private BehaviorState behavior;
	private MapLocation objective;
	
	MapLocation enemySpottedTarget;
	int enemySpottedRound;
	
	MapLocation closestEnemyLocation;
	RobotType closestEnemyType;
	
	public ScoutRobot(RobotController myRC) throws GameActionException {
		super(myRC);
		strategy = StrategyState.BATTLE;
		behavior = BehaviorState.PET;
		enemySpottedTarget = null;
		enemySpottedRound = -55555;
		fbs.setPoolMode();
		nav.setNavigationMode(NavigationMode.GREEDY);
		io.setChannels(new BroadcastChannel[] {
				BroadcastChannel.ALL,
				BroadcastChannel.SCOUTS,
				BroadcastChannel.EXPLORERS
		});
	}

	@Override
	public void run() throws GameActionException {
		if(Clock.getRoundNum()<500) {
			strategy = StrategyState.INITIAL_EXPLORE;
		} else {
			strategy = StrategyState.BATTLE;
		}
		
		// scan
		radar.scan(true, true);
		
		MapLocation closestEnemyLocation = radar.closestEnemy==null ? null : radar.closestEnemy.location;
		if(curRound%5 == myID%5)
			radar.broadcastEnemyInfo(false);
		
		
		if(behavior == BehaviorState.LOOK_FOR_MAP_EDGE_OR_ENEMY || 
				behavior == BehaviorState.SCOUT_FOR_ENEMIES) {
			if(radar.closestEnemy != null) {
				behavior = BehaviorState.REPORT_ENEMY_SIGHTING;
				enemySpottedTarget = radar.closestEnemy.location;
				enemySpottedRound = curRound;
			}
		} else if(behavior == BehaviorState.REPORT_ENEMY_SIGHTING || 
				behavior == BehaviorState.REPORT_MAP_EDGE) {
			if(curLoc.distanceSquaredTo(dc.getClosestArchon()) <= 25) {
				if(strategy == StrategyState.INITIAL_EXPLORE) 
					behavior = BehaviorState.LOOK_FOR_MAP_EDGE_OR_ENEMY;
				else if(strategy == StrategyState.BATTLE)
					behavior = BehaviorState.SUPPORT_FRONT_LINES;
			}
		} else if(behavior == BehaviorState.PET) {
			if(strategy == StrategyState.INITIAL_EXPLORE) 
				behavior = BehaviorState.LOOK_FOR_MAP_EDGE_OR_ENEMY;
		}
		
		
		
		// set objective based on behavior
		switch (behavior) {
			case SCOUT_FOR_ENEMIES:
			case LOOK_FOR_MAP_EDGE_OR_ENEMY:
				objective = mc.guessEnemyPowerCoreLocation();
				break;
			case REPORT_MAP_EDGE:
			case REPORT_ENEMY_SIGHTING:
			case PET:
				// go to nearest archon
				objective = dc.getClosestArchon();
				break;
			case SUPPORT_FRONT_LINES:
				if(closestEnemyLocation != null)
					objective = closestEnemyLocation;
				else if(enemySpottedTarget != null)
					objective = enemySpottedTarget;
				else
					objective = dc.getClosestArchon();
				break;
			case SENDING_ALLY_FLUX:
				// find a friend
				if (radar.closestLowFluxAlly != null) {
					objective = radar.closestLowFluxAlly.location;
				} else {
					objective = dc.getClosestArchon();
				}
				break;
			default:
				break;
		}
		
		if(objective==null) 
			objective = curLoc;
		
		// attack if you can
		if (!rc.isAttackActive() && radar.closestEnemyDist <= 5) {
			RobotInfo bestInfo = null;
			double bestValue = 0;
			for(int n=0; n<radar.numEnemyRobots; n++) {
				RobotInfo ri = radar.enemyInfos[radar.enemyRobots[n]];
				if(!rc.canAttackSquare(ri.location)) 
					continue;
				if(ri.flux > bestValue) {
					bestInfo = ri;
					bestValue = ri.flux;
				}
			}
			
			if(bestValue >= 0.15) {
				rc.attackSquare(bestInfo.location, bestInfo.type.level);
			}
		}
		
		
		// heal if you should
		if (rc.getFlux() > 1.0 && ((curEnergon < myMaxEnergon - 0.2) || radar.numAllyToRegenerate > 0)) {
			rc.regenerate();
		}
		
		// broadcast enemy spotting
		if (behavior == BehaviorState.REPORT_ENEMY_SIGHTING && 
				curLoc.distanceSquaredTo(dc.getClosestArchon()) <= 64) {
			io.sendUShorts(BroadcastChannel.ALL, BroadcastType.ENEMY_SPOTTED,
					new int[] {enemySpottedRound, enemySpottedTarget.x, enemySpottedTarget.y});
		}
		
		// indicator strings
		dbg.setIndicatorString('e', 1, "Target=<"+(objective.x-curLoc.x)+","+
					(objective.y-curLoc.y)+">, Behavior="+behavior);
	}
	
	@Override
	public void processMessage(BroadcastType msgType, StringBuilder sb) throws GameActionException {
		switch(msgType) {
		case MAP_EDGES:
			ses.receiveMapEdges(BroadcastSystem.decodeUShorts(sb));
			break;
		case POWERNODE_FRAGMENTS:
			ses.receivePowerNodeFragment(BroadcastSystem.decodeInts(sb));
			break;
		default:
			super.processMessage(msgType, sb);
		}
	}
	
	@Override
	public MoveInfo computeNextMove() throws GameActionException {
		if(rc.getFlux() < 0.5) 
			return null;
		
		// Retreat from enemies
		if (radar.closestEnemyWithFlux != null && radar.closestEnemyWithFluxDist <= 5) {
			return new MoveInfo(curLoc.directionTo(radar.closestEnemyWithFlux.location).opposite(), true);
		} else if (radar.closestEnemyWithFlux != null && radar.closestEnemyWithFluxDist <= 13) {
			return null;
		}
		
		// Go to objective
		return new MoveInfo(curLoc.directionTo(objective), false);
	}
	
	@Override
	public void useExtraBytecodes() throws GameActionException {
		if (strategy == StrategyState.INITIAL_EXPLORE) {
			if (curRound == Clock.getRoundNum() &&
					Clock.getBytecodesLeft() > 6000 && Math.random() < 0.05) {
				ses.broadcastMapFragment();
			}
			if (curRound == Clock.getRoundNum() &&
					Clock.getBytecodesLeft() > 5000 && Math.random() < 0.05) {
				ses.broadcastPowerNodeFragment();
			}
			if (curRound == Clock.getRoundNum() &&
					Clock.getBytecodesLeft() > 2000 && Math.random() < 0.05) {
				ses.broadcastMapEdges();
			}
		}
		super.useExtraBytecodes();
	}
	
}
