package ducks;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Message;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class ScoutRobot extends BaseRobot {
	
	private enum StrategyState {
		/** Look for map edges so we have an idea where to find the enemy. */
		INITIAL_EXPLORE,
		/** Battle. Help target and transfer flux and heal. */
		BATTLE,
		/** Wander around the map helping archons to support units */
		SUPPORT,
	}
	
	private enum BehaviorState {
		
		// SPECIAL STATES USED FOR INITIAL_EXPLORE
		/** Go in a given direction until we see a new map edge or any enemy unit, then go back. */
		LOOK_FOR_MAP_EDGE,
		/** Reports the newfound map edge. */
		REPORT_TO_ARCHON,
		/** Go around looking for enemies. */
		SCOUT_FOR_ENEMIES,
		
		// GENERAL PURPOSE STATES 
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

	private StrategyState lastStrategy;
	private StrategyState strategy;
	private BehaviorState behavior;
	private MapLocation objective;
	
	MapLocation enemySpottedTarget;
	int enemySpottedRound;
	
	MapLocation closestEnemyLocation;
	RobotType closestEnemyType;
	
	MapLocation helpAllyLocation = null;
	int helpAllyRound;

	Direction mapEdgeToSeek;
	
	private final static int RETREAT_RADIUS = 3;
	
	
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
		
		// scan every round in all conditions
		radar.scan(true, true);
		if(radar.closestEnemy != null) {
			enemySpottedTarget = radar.closestEnemy.location;
			enemySpottedRound = curRound;
		} else {
			enemySpottedTarget = null;
		}
	
		// Setup strategy transitions
		if(Clock.getRoundNum()<1000) { strategy = StrategyState.INITIAL_EXPLORE; }
		else if(radar.numEnemyRobots > 0 || radar.numEnemyScouts < radar.numEnemyRobots) {
			strategy = StrategyState.BATTLE;
		} else {
			strategy = StrategyState.SUPPORT;
		}
		if(lastStrategy != strategy) {
			resetBehavior();
		}
		lastStrategy = strategy;
		
	
		// report enemy info in all conditions
		MapLocation closestEnemyLocation = radar.closestEnemy==null ? null : radar.closestEnemy.location;
		if(curRound%5 == myID%5)
			radar.broadcastEnemyInfo(false);
		
		
		// logic for initial explore
		if(strategy == StrategyState.INITIAL_EXPLORE) { 
			if(behavior == BehaviorState.SCOUT_FOR_ENEMIES) {
			if(radar.closestEnemy != null && radar.closestEnemy.type != RobotType.SCOUT) {
					behavior = BehaviorState.REPORT_TO_ARCHON;
				}
			} else if(behavior == BehaviorState.REPORT_TO_ARCHON) {
				if(curLoc.distanceSquaredTo(dc.getClosestArchon()) <= 25) {
					resetBehavior();
				}
			} else if(behavior == BehaviorState.LOOK_FOR_MAP_EDGE) {
				if(mapEdgeToSeek == Direction.NORTH && mc.edgeYMin!=0 ||
						mapEdgeToSeek == Direction.SOUTH && mc.edgeYMax!=0 ||
						mapEdgeToSeek == Direction.WEST && mc.edgeXMin!=0 ||
						mapEdgeToSeek == Direction.EAST && mc.edgeXMax!=0)
					behavior = BehaviorState.REPORT_TO_ARCHON;
			}
		}
	
		
		// flux support logic
		if(helpAllyLocation != null) {
			if(curLoc.distanceSquaredTo(helpAllyLocation) <= 2) {
				helpAllyLocation = null;  // we should have healed him
				if(behavior == BehaviorState.SENDING_ALLY_FLUX) resetBehavior();
			}
		}
		
		
		// fast behavior switch if we're going to get G'ed
		if(rc.getFlux() < 15 || rc.getEnergon() < 7) {
			behavior = BehaviorState.PET;
		}
		
		// received flux from ally
		if(behavior == BehaviorState.PET) {
			if(rc.getFlux() > 40)
				resetBehavior();
		}
		
		// set objective based on behavior
		switch (behavior) {
			case SENDING_ALLY_FLUX:
				objective = helpAllyLocation;
				break;
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
		
		// perform message attack every few rounds if possible
		if (mas.isLoaded() && curRound % 20 == myID % 20) {
			Message m = mas.getEnemyMessage();
			if (m != null) {
				io.forceSend(mas.getEnemyMessage());
			}
		}
		
		// broadcast enemy spotting
		if (curRound%4==myID%4 && behavior == BehaviorState.REPORT_TO_ARCHON && 
				curLoc.distanceSquaredTo(dc.getClosestArchon()) <= 64) {
			if(enemySpottedTarget != null)
				io.sendUShorts(BroadcastChannel.ALL, BroadcastType.ENEMY_SPOTTED,
					new int[] {enemySpottedRound, enemySpottedTarget.x, enemySpottedTarget.y});
		}
		
		// indicator strings
		dbg.setIndicatorString('e', 1, "Target="+locationToVectorString(objective)+
				", Strat=" + strategy + ", Behavior="+behavior);
	}
	
	private void resetBehavior() {
		switch(strategy) {
		case INITIAL_EXPLORE:
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
			break;
		case BATTLE:
			behavior = BehaviorState.SUPPORT_FRONT_LINES;
			break;
		case SUPPORT:
			if(helpAllyLocation != null)
				behavior = BehaviorState.SENDING_ALLY_FLUX;
			else
				behavior = BehaviorState.PET;
			break;
		default:
			break;
		}
			
	}
	
	@Override
	public void processMessage(BroadcastType msgType, StringBuilder sb) throws GameActionException {
		switch(msgType) {
		case LOW_FLUX_HELP:
			if(helpAllyLocation == null) {
				helpAllyLocation = BroadcastSystem.decodeSenderLoc(sb);
				helpAllyRound = curRound;
				System.out.println("GOING TO HEAL ALLY @ "+ helpAllyLocation.toString());
			}
			if(strategy == StrategyState.SUPPORT) { //go flux ally if we're in support mode
				behavior = BehaviorState.SENDING_ALLY_FLUX;
			}
			break;
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
	
		// ALWAYS RETREAT FROM ENEMEY
		if (radar.closestAAEnemyWithFlux != null && radar.closestAAEnemyWithFluxDist <= 20) {

			Direction dir = getRetreatDir();
//			Direction dir = curLoc.directionTo(radar.closestEnemyWithFlux.location).opposite();
//			Direction dir = curLoc.directionTo(radar.getEnemySwarmCenter()).opposite();
			return new MoveInfo(dir, true);
		}

		// INITIAL_EXPLORE STATES
		if(behavior == BehaviorState.LOOK_FOR_MAP_EDGE)
			return new MoveInfo(mapEdgeToSeek, false);
		else if(behavior == BehaviorState.SCOUT_FOR_ENEMIES) {
			if(radar.closestAllyScoutDist < 25)
				return new MoveInfo(curLoc.directionTo(radar.closestAllyScout.location).opposite(), false);
			else
				return new MoveInfo(nav.navigateRandomly(objective), false);
		}
		
		// Go to objective
		return new MoveInfo(curLoc.directionTo(objective), false);
	}
	
	@Override
	public void useExtraBytecodes() throws GameActionException {
		if (strategy == StrategyState.INITIAL_EXPLORE) {
			if (curRound == Clock.getRoundNum() &&
					Clock.getBytecodesLeft() > 6000 && Util.randDouble() < 0.05) {
				ses.broadcastMapFragment();
			}
			if (curRound == Clock.getRoundNum() &&
					Clock.getBytecodesLeft() > 5000 && Util.randDouble() < 0.05) {
				ses.broadcastPowerNodeFragment();
			}
			if (curRound == Clock.getRoundNum() &&
					Clock.getBytecodesLeft() > 2000 && Util.randDouble() < 0.05) {
				ses.broadcastMapEdges();
			}
		}
		// If we have identified the enemy team, load past message data.
		// This method call will occur at most once per Scout.
		if (mas.guessEnemyTeam() != -1 && !mas.isLoaded() &&
				Clock.getBytecodesLeft() > 5000) {
			mos.rememberString("I think we are facing team " +
				mas.guessEnemyTeam() + ".", true);
			mas.load();
		}
		super.useExtraBytecodes();
	}
	
	private Direction getRetreatDir()
	{
//		7 0 1
//		6   2
//		5 4 3
		int[] wall_in_dir = new int[8];
		
//		now, deal with when we are close to map boundaries
		if (mc.edgeXMax!=0 && mc.cacheToWorldX(mc.edgeXMax) < curLoc.x+RETREAT_RADIUS)
		{
			if (mc.edgeYMax!=0 && mc.cacheToWorldY(mc.edgeYMax) < curLoc.y+RETREAT_RADIUS)
			{
//				we are near the SOUTH_EAST corner
				wall_in_dir[1] = wall_in_dir[2] = wall_in_dir[3] = wall_in_dir[4] = wall_in_dir[5] = 1;
			} else if (mc.edgeYMin!=0 && mc.cacheToWorldY(mc.edgeYMin) > curLoc.y-RETREAT_RADIUS)
			{
//				we are near the NORTH_EAST corner
				wall_in_dir[1] = wall_in_dir[2] = wall_in_dir[3] = wall_in_dir[0] = wall_in_dir[7] = 1;
			} else
			{
//				we are near the EAST edge
				wall_in_dir[1] = wall_in_dir[2] = wall_in_dir[3] = 1;
			}
		} else if (mc.edgeXMin!=0 && mc.cacheToWorldX(mc.edgeXMin) > curLoc.x-RETREAT_RADIUS)
		{
			if (mc.edgeYMax!=0 && mc.cacheToWorldY(mc.edgeYMax) < curLoc.y+RETREAT_RADIUS)
			{
//				we are near the SOUTH_WEST corner
				wall_in_dir[7] = wall_in_dir[6] = wall_in_dir[5] = wall_in_dir[4] = wall_in_dir[3] = 1;
			} else if (mc.edgeYMin!=0 && mc.cacheToWorldY(mc.edgeYMin) > curLoc.y-RETREAT_RADIUS)
			{
//				we are near the NORTH_WEST corner
				wall_in_dir[7] = wall_in_dir[6] = wall_in_dir[5] = wall_in_dir[0] = wall_in_dir[1] = 1;
			} else
			{
//				we are near the WEST edge
				wall_in_dir[7] = wall_in_dir[6] = wall_in_dir[5] = 1;
			}
		} else
		{
			if (mc.edgeYMax!=0 && mc.cacheToWorldY(mc.edgeYMax) < curLoc.y+RETREAT_RADIUS)
			{
//				we are near the SOUTH edge
				wall_in_dir[5] = wall_in_dir[4] = wall_in_dir[3] = 1;
			} else if (mc.edgeYMin!=0 && mc.cacheToWorldY(mc.edgeYMin) > curLoc.y-RETREAT_RADIUS)
			{
//				we are near the NORTH edge
				wall_in_dir[7] = wall_in_dir[0] = wall_in_dir[1] = 1;
			} else
			{
//				we are not near any wall or corner
			}
		}
		
//		dbg.setIndicatorString('y', 2, ""	+wall_in_dir[0]+wall_in_dir[1]+wall_in_dir[2]+wall_in_dir[3]
//											+wall_in_dir[4]+wall_in_dir[5]+wall_in_dir[6]+wall_in_dir[7]
//											+" "+mc.edgeXMax+" "+mc.edgeXMin+" "+mc.edgeYMax+" "+mc.edgeYMin+" "+mc.cacheToWorldX(mc.edgeXMax));
		
		Direction newdir = curLoc.directionTo(radar.closestAAEnemyWithFlux.location);
		wall_in_dir[newdir.ordinal()] = 1;
		
		String dir =  "".concat(wall_in_dir[0]==0?"o":"x")
						.concat(wall_in_dir[1]==0?"o":"x")
						.concat(wall_in_dir[2]==0?"o":"x")
						.concat(wall_in_dir[3]==0?"o":"x")
						.concat(wall_in_dir[4]==0?"o":"x")
						.concat(wall_in_dir[5]==0?"o":"x")
						.concat(wall_in_dir[6]==0?"o":"x")
						.concat(wall_in_dir[7]==0?"o":"x");
		dir = dir.concat(dir);
		int index;
		
		index = dir.indexOf("ooooooo");
		if (index>-1)
			return Constants.directions[(index+3)%8];
		
		index = dir.indexOf("oooooo");
		if (index>-1)
			return Constants.directions[(index+3)%8];
		
		index = dir.indexOf("ooooo");
		if (index>-1)
			return Constants.directions[(index+2)%8];
		
		index = dir.indexOf("oooo");
		if (index>-1)
			return Constants.directions[(index+2)%8];
		
		index = dir.indexOf("ooo");
		if (index>-1)
			return Constants.directions[(index+1)%8];
		
		index = dir.indexOf("oo");
		if (index>-1)
			return Constants.directions[(index+1)%8];
		
		index = dir.indexOf("o");
		if (index>-1)
			return Constants.directions[(index)%8];
		
		
		return newdir.opposite();
	}
}
