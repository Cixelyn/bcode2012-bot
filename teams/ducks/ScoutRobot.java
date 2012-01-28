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
	boolean doneWithInitialScout;
	Direction lastRetreatDir;
	
	private final static int RETREAT_RADIUS = 3;
	private final static int ROUNDS_TO_EXPLORE = 500;
	private final static double REGENRATION_FLUX_THRESHOLD = 3.0;
	
	private final static double THRESHOLD_TO_HEAL_ALLIES = 0.9;
	private final static double THRESHOLD_TO_REFUEL_ALLIES = 30;
	
	
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
		doneWithInitialScout = false;
		lastRetreatDir = null;
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
		if(!doneWithInitialScout && Clock.getRoundNum() < ROUNDS_TO_EXPLORE) 
		{
			strategy = StrategyState.INITIAL_EXPLORE;
		} else if(radar.numEnemyRobots > 0 || radar.numEnemyScouts < radar.numEnemyRobots) {
			strategy = StrategyState.BATTLE;
		} else {
			strategy = StrategyState.SUPPORT;
		}
		if(lastStrategy != strategy) {
			resetBehavior();
		}
		lastStrategy = strategy;
		
	
		// report enemy info in all conditions
//		MapLocation closestEnemyLocation = radar.closestEnemy==null ? null : radar.closestEnemy.location;
		if(curRound%5 == myID%5)
			radar.broadcastEnemyInfo(false);
		
		
		// logic for initial explore
		if(strategy == StrategyState.INITIAL_EXPLORE) {
			
			if(behavior == BehaviorState.SCOUT_FOR_ENEMIES) {
				if(radar.closestEnemy != null && radar.closestEnemy.type != RobotType.SCOUT) {
					doneWithInitialScout = true;
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
			behavior = BehaviorState.REPORT_TO_ARCHON;
		} else if (curLoc.distanceSquaredTo(dc.getClosestArchon())<2) {
			resetBehavior();
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
				objective = dc.getClosestArchon();
				break;
			case PET:
//				objective = dc.getClosestArchon();
//				petSupport();
				supportFrontline();
				break;
			case SUPPORT_FRONT_LINES:
				supportFrontline();
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
		if (rc.getFlux() > REGENRATION_FLUX_THRESHOLD &&
				((curEnergon < myMaxEnergon - 0.2) || radar.numAllyToRegenerate > 0)) {
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
		dbg.setIndicatorString('y', 2, "flux:"+radar.lowestFlux+" "+(radar.lowestFluxAllied!=null?radar.lowestFluxAllied.location:null)
				+" energon:"+radar.lowestEnergonRatio+" "+(radar.lowestEnergonAllied!=null?radar.lowestEnergonAllied.location:null));
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
	
	private void petSupport()
	{
//		old logic
//		if(closestEnemyLocation != null)
//			objective = closestEnemyLocation;
//		else if(enemySpottedTarget != null)
//			objective = enemySpottedTarget;
//		else
//			objective = dc.getClosestArchon();
		
//		1. find unit with lowest flux and go there
//		(code done in radar)
		if (radar.lowestFlux < THRESHOLD_TO_REFUEL_ALLIES)
		{
			objective = radar.lowestFluxAllied.location;
		} else if (radar.lowestEnergonRatio < THRESHOLD_TO_HEAL_ALLIES)
		{
			objective = radar.lowestEnergonAllied.location;
		} else if(closestEnemyLocation != null)
		{
			objective = closestEnemyLocation;
		} else if (enemySpottedTarget != null)
		{
			objective = enemySpottedTarget;
		} else 
		{
			objective= dc.getClosestArchon();
		}
	}
	
	private void supportFrontline()
	{
//		old logic
//		if(closestEnemyLocation != null)
//			objective = closestEnemyLocation;
//		else if(enemySpottedTarget != null)
//			objective = enemySpottedTarget;
//		else
//			objective = dc.getClosestArchon();
		
//		1. find unit with lowest flux and go there
//		(code done in radar)
		if (radar.lowestEnergonRatio < THRESHOLD_TO_HEAL_ALLIES)
		{
			objective = radar.lowestEnergonAllied.location;
		} else if (radar.lowestFlux < THRESHOLD_TO_REFUEL_ALLIES)
		{
			objective = radar.lowestFluxAllied.location;
		} else if(closestEnemyLocation != null)
		{
			objective = closestEnemyLocation;
		} else if (enemySpottedTarget != null)
		{
			objective = enemySpottedTarget;
		} else 
		{
			objective= dc.getClosestArchon();
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
//		default:
//			super.processMessage(msgType, sb);
		}
	}
	
	@Override
	public MoveInfo computeNextMove() throws GameActionException {
		if(rc.getFlux() < 0.5) 
			return null;
	
		// ALWAYS RETREAT FROM ENEMEY if in range
		if (radar.closestEnemyWithFlux != null)
		{
			if (behavior == BehaviorState.LOOK_FOR_MAP_EDGE ||
				behavior == BehaviorState.REPORT_TO_ARCHON)
			{
				if (radar.closestEnemyDist  <= 23)
				{
//					flee code
					Direction dir = getFullRetreatDir();
//					Direction dir = curLoc.directionTo(radar.closestEnemyWithFlux.location).opposite();
//					Direction dir = curLoc.directionTo(radar.getEnemySwarmCenter()).opposite();
					
					lastRetreatDir = dir;
					return new MoveInfo(dir, true);
				}
			}
			
			lastRetreatDir = null;
			int fleedist = 0;
			switch (radar.closestEnemyWithFlux.type)
			{
			case SOLDIER:
				if (radar.closestEnemyWithFlux.roundsUntilAttackIdle >= 4)
					fleedist = 10;
				else
					fleedist = 13;
				break;
			case DISRUPTER:
				if (radar.closestEnemyWithFlux.roundsUntilAttackIdle >= 4)
					fleedist = 16;
				else
					fleedist = 19;
				break;
				
			}
			
			if (radar.closestEnemyWithFluxDist <= fleedist)
			{
//				flee code
				Direction dir = getRetreatDir();
//				Direction dir = curLoc.directionTo(radar.closestEnemyWithFlux.location).opposite();
//				Direction dir = curLoc.directionTo(radar.getEnemySwarmCenter()).opposite();
				
				lastRetreatDir = dir;
				return new MoveInfo(dir, true);
			} else
			{
				lastRetreatDir = null;
				Direction target = null;
				// INITIAL_EXPLORE STATES
				if(behavior == BehaviorState.LOOK_FOR_MAP_EDGE)
					target = mapEdgeToSeek;
				else if(behavior == BehaviorState.SCOUT_FOR_ENEMIES) {
					if(radar.closestAllyScoutDist < 25)
						target = curLoc.directionTo(radar.closestAllyScout.location);
					else
						target = nav.navigateRandomly(objective);
				} else
				{
					target = curLoc.directionTo(objective);
				}
				
				if (curLoc.add(target).distanceSquaredTo(radar.closestEnemyWithFlux.location) <= fleedist)
					return null;
				else return new MoveInfo(target, false);
			}
		}
		
		lastRetreatDir = null;

		// INITIAL_EXPLORE STATES
		if(behavior == BehaviorState.LOOK_FOR_MAP_EDGE)
			return new MoveInfo(mapEdgeToSeek, false);
		else if(behavior == BehaviorState.SCOUT_FOR_ENEMIES) {
			if(radar.closestAllyScoutDist < 25)
				return new MoveInfo(curLoc.directionTo(radar.closestAllyScout.location).opposite(), false);
			else
				return new MoveInfo(nav.navigateRandomly(objective), false);
		}
		
		//	keep away from allied scouts
		if(behavior != BehaviorState.REPORT_TO_ARCHON && radar.closestAllyScoutDist < 16)
			return new MoveInfo(curLoc.directionTo(radar.closestAllyScout.location).opposite(), false);
		
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
	
	private Direction getFullRetreatDir()
	{
//		7 0 1
//		6   2
//		5 4 3
		int[] wall_in_dir = new int[8];
		int[] closest_in_dir = radar.closestInDir;
		
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
		
//		Direction newdir = curLoc.directionTo(radar.closestEnemyWithFlux.location);
		if (lastRetreatDir != null)
			wall_in_dir[lastRetreatDir.opposite().ordinal()] = 1;
		
//		String dir =  "".concat(wall_in_dir[0]==0?"o":"x")
//						.concat(wall_in_dir[1]==0?"o":"x")
//						.concat(wall_in_dir[2]==0?"o":"x")
//						.concat(wall_in_dir[3]==0?"o":"x")
//						.concat(wall_in_dir[4]==0?"o":"x")
//						.concat(wall_in_dir[5]==0?"o":"x")
//						.concat(wall_in_dir[6]==0?"o":"x")
//						.concat(wall_in_dir[7]==0?"o":"x");
//		dir = dir.concat(dir);
		String dir =  "".concat(closest_in_dir[0]==99?(wall_in_dir[0]==0?"o":"x"):"x")
				.concat(closest_in_dir[1]==99?(wall_in_dir[1]==0?"o":"x"):"x")
				.concat(closest_in_dir[2]==99?(wall_in_dir[2]==0?"o":"x"):"x")
				.concat(closest_in_dir[3]==99?(wall_in_dir[3]==0?"o":"x"):"x")
				.concat(closest_in_dir[4]==99?(wall_in_dir[4]==0?"o":"x"):"x")
				.concat(closest_in_dir[5]==99?(wall_in_dir[5]==0?"o":"x"):"x")
				.concat(closest_in_dir[6]==99?(wall_in_dir[6]==0?"o":"x"):"x")
				.concat(closest_in_dir[7]==99?(wall_in_dir[7]==0?"o":"x"):"x");
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
		
		int lowest = closest_in_dir[0];
		int lowesti = 0;
		for (int x=1; x<8; x++)
			if (closest_in_dir[x]>lowest)
			{
				lowesti = x;
				lowest = closest_in_dir[x];
			}
		return Constants.directions[lowesti];
	}
}
