package ducks;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

enum ArchonState {
	ATTACKBASE,
	CAPTURE,
	CHASE,
	RETREAT,
}

class ArchonConstants {
	public static final int ROUNDS_TO_ATACK_BASE = 1500;
	
	public static final int CHASE_ROUNDS = 40;
	public static final int RETREAT_ROUNDS = 40;
	
	public static final int RETREAT_THRESHOLD = -4;
	public static final int RETREAT_STABILIIZE = 0;
	public static final int ROUND_ALLOWANCE = -2;
	
	public static final int ATTACK_CLOSEST_DIST = 18;
	public static final int CHASE_CLOSEST_DIST = 20;
	public static final int RETREAT_CLOSEST_DIST = 15;
	
	public static final double ATTACK_SPAWN_SOLDIER = 120+30;
	public static final double CAPTURE_SPAWN_SOLDIER = 215;
	public static final double CHASE_SPAWN_SOLDIER = 120+10;
	public static final double RETREAT_SPAWN_SOLDIER = 120+5;

	public static final double ATTACK_SPAWN_SCOUT = 80+30;
	public static final double CAPTURE_SPAWN_SCOUT = 215;
	public static final double CHASE_SPAWN_SCOUT = 80+10;
	public static final double RETREAT_SPAWN_SCOUT = 80+5;
	
	public static final double ATTACK_SPAWN_SOLDIER_NUM = 399;
	public static final double CAPTURE_SPAWN_SOLDIER_NUM = 499;
	public static final double CHASE_SPAWN_SOLDIER_NUM = 499;
	public static final double RETREAT_SPAWN_SOLDIER_NUM = 399;

	public static final double ATTACK_SPAWN_SCOUT_NUM = 1;
	public static final double CAPTURE_SPAWN_SCOUT_NUM = 1;
	public static final double CHASE_SPAWN_SCOUT_NUM = 1;
	public static final double RETREAT_SPAWN_SCOUT_NUM = 1;
}

public class ArchonRobotYP extends BaseRobot {

	private ArchonState curstate;
	private ArchonState prevstate;
	
	private int origAID;
	private int archonIndex;
	
	private RobotInfo chaseTarget;
	private Direction chaseDir;
	private int chaseRounds;
	
	private int retreatRounds;
	private Direction retreatDir;
	private MapLocation retreatLoc;
	
	MapLocation movetarget;
	Direction movedirection;

	private RobotType spawntype;
	private int spawned;
	
	public ArchonRobotYP(RobotController myRC) throws GameActionException 
	{
		super(myRC);
		
		io.setChannels(new BroadcastChannel[] {
				BroadcastChannel.ALL,
				BroadcastChannel.ARCHONS,
				BroadcastChannel.EXPLORERS
		});
		
		MapLocation[] archons = dc.getAlliedArchons();
		for (int x=0; x<6; x++)
		{
			if (curLoc.equals(archons[x]))
			{
				origAID = x;
				break;
			}
		}
		
		archonIndex = origAID;
		
		curstate = ArchonState.ATTACKBASE;
		prevstate = ArchonState.ATTACKBASE;
		
		chaseDir = null;
		chaseRounds = ArchonConstants.CHASE_ROUNDS;
		
		spawned = 0;
		spawntype = RobotType.SOLDIER;
		
		nav.setNavigationMode(NavigationMode.TANGENT_BUG);
		fbs.setBattleMode();
	}

	private void calculateArchonPosition() throws GameActionException
	{
		if (archonIndex != 0)
		{
			MapLocation[] archons = dc.getAlliedArchons();
			if (archonIndex >= archons.length) archonIndex = archons.length;
			while (!archons[archonIndex].equals(curLoc)) archonIndex--;
		}
	}
	
	@Override
	public void run() throws GameActionException
	{
		radar.scan(true, true);
		
		calculateArchonPosition();
		
		execute(curstate);
		
		if ((Clock.getRoundNum()%3) == 0)
		{
			switch (curstate)
			{
			case ATTACKBASE:
				sendSwarmInfo(curLoc, curLoc.directionTo(movetarget));
				break;
			case CAPTURE:
				sendSwarmInfo(curLoc, curLoc.directionTo(movetarget));
				break;
			case CHASE:
				sendSwarmInfo(curLoc, chaseDir);
				break;
			case RETREAT:
				sendSwarmInfo(curLoc, retreatDir.opposite());
				break;
			}
		}
		
		switch (spawntype)
		{
		case SOLDIER:
			switch (curstate)
			{
			case ATTACKBASE:
				if (spawned >= ArchonConstants.ATTACK_SPAWN_SOLDIER_NUM)
				{ spawned = 0; spawntype = RobotType.SCOUT; }
				break;
			case CAPTURE:
				if (spawned >= ArchonConstants.CAPTURE_SPAWN_SOLDIER_NUM)
				{ spawned = 0; spawntype = RobotType.SCOUT; }
				break;
			case CHASE:
				if (spawned >= ArchonConstants.CHASE_SPAWN_SOLDIER_NUM)
				{ spawned = 0; spawntype = RobotType.SCOUT; }
				break;
			case RETREAT:
				if (spawned >= ArchonConstants.RETREAT_SPAWN_SOLDIER_NUM)
				{ spawned = 0; spawntype = RobotType.SCOUT; }
				break;
			} break;
		case SCOUT:
			switch (curstate)
			{
			case ATTACKBASE:
				if (spawned >= ArchonConstants.ATTACK_SPAWN_SCOUT_NUM)
				{ spawned = 0; spawntype = RobotType.SOLDIER; }
				break;
			case CAPTURE:
				if (spawned >= ArchonConstants.CAPTURE_SPAWN_SCOUT_NUM)
				{ spawned = 0; spawntype = RobotType.SOLDIER; }
				break;
			case CHASE:
				if (spawned >= ArchonConstants.CHASE_SPAWN_SCOUT_NUM)
				{ spawned = 0; spawntype = RobotType.SOLDIER; }
				break;
			case RETREAT:
				if (spawned >= ArchonConstants.RETREAT_SPAWN_SCOUT_NUM)
				{ spawned = 0; spawntype = RobotType.SOLDIER; }
				break;
			} break;
		}
		
		switch (curstate)
		{
		case ATTACKBASE:
			rc.setIndicatorString(0, "state:"+curstate+" "+movetarget);
			break;
		case CAPTURE:
			rc.setIndicatorString(0, "state:"+curstate+" "+movetarget);
			break;
		case CHASE:
			rc.setIndicatorString(0, "state:"+curstate+" "+chaseTarget+" "+chaseDir+" "+chaseRounds);
			break;
		case RETREAT:
			rc.setIndicatorString(0, "state:"+curstate+" "+retreatRounds+" "+retreatDir);
			break;
		}
		
		
		
		fbs.manageFlux();
	}
	
	public void execute(ArchonState state) throws GameActionException
	{
		switch (state)
		{
		case ATTACKBASE:
			attackbase();
			break;
		case CAPTURE:
			capture();
			break;
		case CHASE:
			chase();
			break;
		case RETREAT:
			retreat();
			break;
		}
	}
	
	@Override
	public void processMessage(BroadcastType msgType, StringBuilder sb)
			throws GameActionException {
		switch(msgType) {
		case ENEMY_ARCHON_KILL:
			eakc.reportEnemyArchonKills(BroadcastSystem.decodeUShorts(sb));
			break;
		case MAP_EDGES:
			ses.receiveMapEdges(BroadcastSystem.decodeUShorts(sb));
			break;
		case MAP_FRAGMENTS:
			ses.receiveMapFragment(BroadcastSystem.decodeInts(sb));
			break;
		case POWERNODE_FRAGMENTS:
			ses.receivePowerNodeFragment(BroadcastSystem.decodeInts(sb));
			break;
		default:
//			super.processMessage(msgType, sb);
			break;
				
		} 
	}
	
	private void gotoStateAndExecute(ArchonState state) throws GameActionException
	{
		if (curstate != state)
		{
			switch (curstate)
			{
			case CHASE:
			case RETREAT:
				curstate = state;
				execute(state);
				return;
			default:
				break;
			}
			prevstate = curstate;
			curstate = state;
			execute(state);
		} else
		{
			System.out.println("transitioning from "+state+" to "+curstate);
		}
	}
	
	public void attackbase() throws GameActionException 
	{
		radar.scan(true, true);
		
		if (radar.getArmyDifference() <= ArchonConstants.RETREAT_THRESHOLD)
		{
			gotoStateAndExecute(ArchonState.RETREAT);
			return;
		}
		
		if (radar.closestEnemy != null)
		{
			gotoStateAndExecute(ArchonState.CHASE);
			return;
		}
		
		if (curRound >= ArchonConstants.ROUNDS_TO_ATACK_BASE)
		{
			gotoStateAndExecute(ArchonState.CAPTURE);
			return;
		}
		
		if (eakc.getNumEnemyArchonsAlive() < dc.getAlliedArchons().length-2)
		{
			gotoStateAndExecute(ArchonState.CAPTURE);
			return;
		}
		
		MapLocation powercore = mc.getEnemyPowerCoreLocation();
		if (powercore != null)
		{
			gotoStateAndExecute(ArchonState.CAPTURE);
			return;
		}
		
		movetarget = mc.guessEnemyPowerCoreLocation();
	}
	
	public void capture() throws GameActionException 
	{
		radar.scan(true, true);
		
		if (radar.getArmyDifference() <= ArchonConstants.RETREAT_THRESHOLD)
		{
			gotoStateAndExecute(ArchonState.RETREAT);
			return;
		}
		
		if (radar.closestEnemy != null)
		{
			gotoStateAndExecute(ArchonState.CHASE);
			return;
		}
		
		movetarget = mc.guessBestPowerNodeToCapture();
	}
	
	public void chase() throws GameActionException 
	{
		radar.scan(true, true);
		
		if (radar.getArmyDifference() <= ArchonConstants.RETREAT_THRESHOLD)
		{
			gotoStateAndExecute(ArchonState.RETREAT);
			return;
		}
		
		if (radar.numEnemyRobots == 0)
		{
			if (chaseRounds-- <= 0 || chaseDir==null)
			{
				gotoStateAndExecute(prevstate);
				return;
			}
		}
		
		chaseRounds = ArchonConstants.CHASE_ROUNDS;
		
		if (chaseTarget != null)
		{
			if (curRound > radar.enemyTimes[chaseTarget.robot.getID()])
			{
				chaseTarget = null;
			}
		}
		
		
		if (chaseTarget == null)
		{
			if (radar.numEnemyArchons>0)
			{
				chaseTarget = radar.enemyInfos[radar.enemyArchons[0]];
			} else chaseTarget = radar.closestEnemy;
		} else if (chaseTarget.type == RobotType.ARCHON)
		{
			int closest = curLoc.distanceSquaredTo(chaseTarget.location);
			if (radar.numEnemyArchons>0)
			{
				for (int x=0; x<radar.numEnemyArchons; x++)
				{
					RobotInfo ri = radar.enemyInfos[radar.enemyArchons[0]];
					int dist = curLoc.distanceSquaredTo(ri.location);
					if (dist < closest)
					{
						chaseTarget = radar.enemyInfos[radar.enemyArchons[0]];
						closest = dist;
					}
				}
			}
		} else
		{
			if (radar.numEnemyArchons>0)
			{
				chaseTarget = radar.enemyInfos[radar.enemyArchons[0]];
			} else chaseTarget = radar.closestEnemy;
		}
		
		if (chaseTarget != null)
		{
			chaseDir = curLoc.directionTo(chaseTarget.location);
		} else gotoStateAndExecute(prevstate);
	}
	
	public void retreat() throws GameActionException
	{
		radar.scan(true, true);
		
		if (radar.getArmyDifference() >= ArchonConstants.RETREAT_STABILIIZE)
		{
			gotoStateAndExecute(prevstate);
			return;
		}
		
		if (radar.numEnemyRobots == 0)
		{
			if (retreatRounds-- <= 0 || retreatDir==null)
			{
				gotoStateAndExecute(prevstate);
				return;
			}
			return;
		} else retreatRounds = ArchonConstants.RETREAT_ROUNDS;
		
		retreatDir = radar.getEnemySwarmTarget().directionTo(curLoc);
		return;
	}
	
	@Override
	public MoveInfo computeNextMove() throws GameActionException
	{
		if(dc.getClosestArchon()!=null) {
			int distToNearestArchon = curLoc.distanceSquaredTo(dc.getClosestArchon());
			if(distToNearestArchon <= 25 && Math.random() < 0.6-Math.sqrt(distToNearestArchon)/10) 
				return new MoveInfo(curLoc.directionTo(dc.getClosestArchon()).opposite(), false);
		}
		
		switch (curstate)
		{
		case ATTACKBASE:
		{
			if (rc.canMove(curDir))
			{
				switch (spawntype)
				{
				case SOLDIER:
					if (rc.getFlux() > ArchonConstants.ATTACK_SPAWN_SOLDIER)
					{
						spawned++;
						return new MoveInfo(RobotType.SOLDIER, curDir);
					}
					break;
				case SCOUT:
					if (rc.getFlux() > ArchonConstants.ATTACK_SPAWN_SCOUT)
					{
						spawned++;
						return new MoveInfo(RobotType.SCOUT, curDir);
					}
				}
			}
			
			
			nav.setDestination(movetarget);
			return new MoveInfo(nav.navigateToDestination(), false);
		}
		case CAPTURE:
		{
			if (curLoc.isAdjacentTo(movetarget))
			{
				Direction dir = curLoc.directionTo(movetarget);
				
				if (rc.getFlux() < RobotType.TOWER.spawnCost) return null;
				if (rc.canMove(dir))
				{
					return new MoveInfo(RobotType.TOWER, dir);
				} else return null;
			} else if (curLoc.equals(movetarget))
			{
				return new MoveInfo(nav.navigateCompletelyRandomly(),true);
			} else
			{
				if (rc.canMove(curDir))
				{
					switch (spawntype)
					{
					case SOLDIER:
						if (rc.getFlux() > ArchonConstants.CAPTURE_SPAWN_SOLDIER)
						{
							spawned++;
							return new MoveInfo(RobotType.SOLDIER, curDir);
						}
						break;
					case SCOUT:
						if (rc.getFlux() > ArchonConstants.CAPTURE_SPAWN_SCOUT)
						{
							spawned++;
							return new MoveInfo(RobotType.SCOUT, curDir);
						}
					}
				}
				
				nav.setDestination(movetarget);
				return new MoveInfo(nav.navigateToDestination(),true);
			}
		}
		case CHASE:
		{
			if (rc.canMove(curDir))
			{
				switch (spawntype)
				{
				case SOLDIER:
					if (rc.getFlux() > ArchonConstants.CHASE_SPAWN_SOLDIER)
					{
						spawned++;
						return new MoveInfo(RobotType.SOLDIER, curDir);
					}
					break;
				case SCOUT:
					if (rc.getFlux() > ArchonConstants.CHASE_SPAWN_SCOUT)
					{
						spawned++;
						return new MoveInfo(RobotType.SCOUT, curDir);
					}
				}
			}
			
			if (radar.closestEnemyDist < ArchonConstants.CHASE_CLOSEST_DIST)
				return new MoveInfo(radar.getEnemySwarmTarget().directionTo(curLoc),true);
			
			return new MoveInfo(chaseDir, false);
		}
		case RETREAT:
		{
			if (rc.canMove(curDir))
			{
				switch (spawntype)
				{
				case SOLDIER:
					if (rc.getFlux() > ArchonConstants.RETREAT_SPAWN_SOLDIER)
					{
						spawned++;
						return new MoveInfo(RobotType.SOLDIER, curDir);
					}
					break;
				case SCOUT:
					if (rc.getFlux() > ArchonConstants.RETREAT_SPAWN_SCOUT)
					{
						spawned++;
						return new MoveInfo(RobotType.SCOUT, curDir);
					}
				}
			}
			
			if (retreatLoc == null || 
					curLoc.directionTo(retreatLoc) != retreatDir ||
					curLoc.distanceSquaredTo(retreatLoc) < 5)
			{
				retreatLoc = curLoc.add(retreatDir,9);
				nav.setDestination(retreatLoc);
			}
			
			return new MoveInfo(nav.navigateGreedy(retreatLoc), true);
		}
		}
		return null;
	}
	
	@Override
	public void useExtraBytecodes() {
		if(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>2500)
			nav.prepare(); 
		if(Clock.getRoundNum()%8==origAID) {
			if(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>3700)
				ses.broadcastMapFragment();
			if(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>1700)
				ses.broadcastPowerNodeFragment();
			if(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>450) 
				ses.broadcastMapEdges();
		}
		
		if(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>1200) 
			io.sendAll();
		
		if(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>800)
			fbs.manageFlux();
		
		while(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>2500) 
			nav.prepare();
		while(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>1050) 
			mc.extractUpdatedPackedDataStep();
	}
	
	public void sendSwarmInfo(MapLocation swarmloc, Direction swarmdir)
	{
		io.sendUShorts(BroadcastChannel.SOLDIERS, BroadcastType.SWARM_DETAILS, new int[]
				{swarmdir.ordinal(), swarmloc.x, swarmloc.y}
		);
	}
	
	
}
