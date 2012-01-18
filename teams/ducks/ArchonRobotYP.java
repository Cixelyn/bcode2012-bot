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
	public static final int ROUNDS_TO_CHASE = 40;
	public static final int ROUNDS_TO_ATACK_BASE = 1500;
	
	public static final int RETREAT_THRESHOLD = -4;
	public static final int ROUND_ALLOWANCE = -2;
}

public class ArchonRobotYP extends BaseRobot {

	private ArchonState curstate;
	private ArchonState prevstate;
	
	private int origAID;
	private int archonIndex;
	
	private RobotInfo chaseTarget;
	private Direction chaseDir;
	private int chaseRounds;
	
	MapLocation movetarget;
	Direction movedirection;
	
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
		
		curstate = ArchonState.ATTACKBASE;
		
		chaseDir = null;
		chaseRounds = ArchonConstants.ROUNDS_TO_CHASE;
		
		nav.setNavigationMode(NavigationMode.TANGENT_BUG);
		fbs.setBattleMode();
	}

	@Override
	public void run() throws GameActionException
	{
		radar.scan(true, true);
		
		execute(curstate);
		
		sendMapInfo();
		
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
		}
	}
	
	@Override
	public void processMessage(BroadcastType msgType, StringBuilder sb)
			throws GameActionException {
		switch(msgType) {
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
			prevstate = curstate;
			curstate = state;
			execute(state);
		}
	}
	
	public void attackbase() throws GameActionException 
	{
		if (radar.closestEnemy != null)
		{
			gotoStateAndExecute(ArchonState.CHASE);
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
		if (radar.closestEnemy != null)
		{
			gotoStateAndExecute(ArchonState.CHASE);
			return;
		}
		
		movetarget = mc.guessBestPowerNodeToCapture();
	}
	
	public void chase() throws GameActionException 
	{
		if (radar.numEnemyRobots == 0)
		{
			if (chaseRounds-- <= 0 || chaseDir==null)
			{
				gotoStateAndExecute(prevstate);
				return;
			}
		}
		
		if (radar.getArmyDifference() < ArchonConstants.RETREAT_THRESHOLD)
		{
			gotoStateAndExecute(ArchonState.RETREAT);
			return;
		}
		
		chaseRounds = ArchonConstants.ROUNDS_TO_CHASE;
		
		if (curRound < radar.enemyTimes[chaseTarget.robot.getID()]+ArchonConstants.ROUND_ALLOWANCE)
		{
			chaseTarget = null;
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
	}
	
	@Override
	public MoveInfo computeNextMove() throws GameActionException
	{
		
		
		
		return null;
	}

	
	private void sendMapInfo()
	{
		if(Clock.getBytecodeNum()<5000 && Clock.getRoundNum()%6==origAID) {
			ses.broadcastPowerNodeFragment();
			ses.broadcastMapFragment();
			ses.broadcastMapEdges();
			mc.extractUpdatedPackedDataStep();
		}
	}

}
