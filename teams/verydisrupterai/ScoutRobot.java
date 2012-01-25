package verydisrupterai;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

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
		LOOK_FOR_MAP_EDGE,
		/** Reports the newfound map edge. */
		REPORT_TO_ARCHON,
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
	
	Direction mapEdgeToSeek;
	
	public ScoutRobot(RobotController myRC) throws GameActionException {
		super(myRC);
		enemySpottedTarget = null;
		enemySpottedRound = -55555;
		mapEdgeToSeek = null;
		fbs.setPoolMode();
		nav.setNavigationMode(NavigationMode.GREEDY);
		io.setChannels(new BroadcastChannel[] {
				BroadcastChannel.ALL,
				BroadcastChannel.SCOUTS,
				BroadcastChannel.EXPLORERS
		});
		strategy = StrategyState.INITIAL_EXPLORE;
		resetBehavior();
	}

	@Override
	public void run() throws GameActionException {
		if(Clock.getRoundNum()<600) {
			strategy = StrategyState.INITIAL_EXPLORE;
		} else {
			strategy = StrategyState.BATTLE;
		}
		
		// scan
		radar.scan(true, true);
		
		MapLocation closestEnemyLocation = radar.closestEnemy==null ? null : radar.closestEnemy.location;
		if(curRound%5 == myID%5)
			radar.broadcastEnemyInfo(false);
		
		
		if(behavior == BehaviorState.SCOUT_FOR_ENEMIES) {
			if(radar.closestEnemy != null) {
				behavior = BehaviorState.REPORT_TO_ARCHON;
				enemySpottedTarget = radar.closestEnemy.location;
				enemySpottedRound = curRound;
			}
		} else if(behavior == BehaviorState.REPORT_TO_ARCHON) {
			if(curLoc.distanceSquaredTo(dc.getClosestArchon()) <= 25) {
				resetBehavior();
			}
		} else if(behavior == BehaviorState.PET) {
			if(rc.getFlux() > 40)
				resetBehavior();
		} else if(behavior == BehaviorState.LOOK_FOR_MAP_EDGE) {
			if(mapEdgeToSeek == Direction.NORTH && mc.edgeYMin!=0 ||
					mapEdgeToSeek == Direction.SOUTH && mc.edgeYMax!=0 ||
					mapEdgeToSeek == Direction.WEST && mc.edgeXMin!=0 ||
					mapEdgeToSeek == Direction.EAST && mc.edgeXMax!=0)
				behavior = BehaviorState.REPORT_TO_ARCHON;
		}
		
		if(rc.getFlux() < 15) {
			behavior = BehaviorState.PET;
		}
				
		
		
		
		// set objective based on behavior
		switch (behavior) {
			case SCOUT_FOR_ENEMIES:
				objective = mc.guessEnemyPowerCoreLocation();
				break;
			case REPORT_TO_ARCHON:
			case PET:
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
		if (behavior == BehaviorState.REPORT_TO_ARCHON && 
				curLoc.distanceSquaredTo(dc.getClosestArchon()) <= 64) {
			if(enemySpottedTarget != null)
				io.sendUShorts(BroadcastChannel.ALL, BroadcastType.ENEMY_SPOTTED,
					new int[] {enemySpottedRound, enemySpottedTarget.x, enemySpottedTarget.y});
		}
		
		// indicator strings
		dbg.setIndicatorString('e', 1, "Target=<"+(objective.x-curLoc.x)+","+
					(objective.y-curLoc.y)+">, Behavior="+behavior);
	}
	
	private void resetBehavior() {
		if(strategy == StrategyState.INITIAL_EXPLORE) {
			if(birthday % 10 < 5) {
				if(mc.edgeXMax==0) {
					behavior = BehaviorState.LOOK_FOR_MAP_EDGE;
					mapEdgeToSeek = Direction.EAST;
				} else if(mc.edgeXMin==0) {
					behavior = BehaviorState.LOOK_FOR_MAP_EDGE;
					mapEdgeToSeek = Direction.WEST;
				} else {
					behavior = BehaviorState.SCOUT_FOR_ENEMIES;
				}
			} else {
				if(mc.edgeYMax==0) {
					behavior = BehaviorState.LOOK_FOR_MAP_EDGE;
					mapEdgeToSeek = Direction.SOUTH;
				} else if(mc.edgeYMin==0) {
					behavior = BehaviorState.LOOK_FOR_MAP_EDGE;
					mapEdgeToSeek = Direction.NORTH;
				} else {
					behavior = BehaviorState.SCOUT_FOR_ENEMIES;
				}
			}
		} else if(strategy == StrategyState.SCOUT_ENEMY) {
			behavior = BehaviorState.SCOUT_FOR_ENEMIES;
		} else if(strategy == StrategyState.HEAL) {
			behavior = BehaviorState.PET;
		} else {
			behavior = BehaviorState.SUPPORT_FRONT_LINES;
		}
			
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
		if (radar.closestEnemyWithFlux != null) {
			return new MoveInfo(curLoc.directionTo(radar.closestEnemyWithFlux.location).opposite(), true);
		}

		// Look for map edges
		if(behavior == BehaviorState.LOOK_FOR_MAP_EDGE)
			return new MoveInfo(mapEdgeToSeek, false);
		
		
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
