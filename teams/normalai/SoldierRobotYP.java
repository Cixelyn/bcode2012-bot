package normalai;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.PowerNode;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;

enum SoldierState {
	CHASE,
	SWARM,
	RETREAT,
}

class SoldierConstants {
	public static final int SWARM_ROUNDS_UNTIL_STALE = 20;
	public static final int CHASE_ROUNDS = 40;
	
	public static final int CHASE_CLOSE_DISTANCE = 2;
	
	public static final int RETREAT_ROUNDS = 20;
	
	public static final int RETREAT_THRESHOLD = -4;
	public static final int RETREAT_STABILIZE = -2;
	public static final int RETREAT_CHASE = -1;
	
	public static int SWARM_TOO_CLOSE_DISTANCE = 3;
	public static int SWARM_TOO_FAR_DISTANCE = 30;
	
	public static double SHUTDOWN_THRESHOLD = 1.0;
	
	public static double ATTACK = RobotType.SOLDIER.attackPower;
}

public class SoldierRobotYP extends BaseRobot {
	
	private SoldierState curstate;
	
	private RobotInfo chaseTarget;
	private Direction chaseDir;
	private int chaseRounds;
	
	private int retreatRounds;
	private Direction retreatDir;
	private MapLocation retreatLoc;
	
//	private MapLocation closestArchon;
	private MapLocation swarmLoc;
	private Direction swarmDir;
	private int swarmUpdateRounds;
	private int closestmsg;
	
	
	MapLocation movetarget;
	Direction movedirection;
	
	public SoldierRobotYP(RobotController myRC) throws GameActionException {
		super(myRC);
		
		curstate = SoldierState.SWARM;
		
		io.setChannels(new BroadcastChannel[] {
				BroadcastChannel.ALL,
				BroadcastChannel.SOLDIERS,
		});
	}

	@Override
	public void run() throws GameActionException
	{
		radar.scan(true, true);
		
		swarmUpdateRounds++;
		
		if(!rc.isAttackActive()) 
			attack();
		
		execute(curstate);
		
		switch (curstate)
		{
		case SWARM:
			rc.setIndicatorString(0, "state:"+curstate+" "+swarmLoc+" "+swarmDir+" "+swarmUpdateRounds);
			break;
		case CHASE:
			rc.setIndicatorString(0, "state:"+curstate+" "+chaseTarget+" "+chaseDir+" "+chaseRounds);
			break;
		case RETREAT:
			rc.setIndicatorString(0, "state:"+curstate+" "+retreatRounds+" "+retreatDir);
			break;
		}
		
		closestmsg = 999;
		
		eakc.broadcastDeadEnemyArchonIDs();
		
		fbs.manageFlux();
	}
	
	public void execute(SoldierState state) throws GameActionException
	{
		switch (state)
		{
		case SWARM:
			swarm();
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
		switch (msgType)
		{
		case SWARM_DETAILS:
		{
			int[] msg = BroadcastSystem.decodeUShorts(sb);
			MapLocation loc = new MapLocation(msg[1], msg[2]);
			int dist = loc.distanceSquaredTo(curLoc);
			if (dist < closestmsg)
			{
				swarmLoc = loc;
				swarmDir = Constants.directions[msg[0]];
				swarmUpdateRounds = 0;
			}
		} break;
		}
	}
	
	private void gotoStateAndExecute(SoldierState state) throws GameActionException
	{
		if (curstate != state)
		{
			curstate = state;
			execute(state);
		} else
		{
			System.out.println("transitioning from "+state+" to "+curstate);
		}
	}
	
	public void attack() throws GameActionException
	{
		radar.scan(true, true);
		
		if (radar.numEnemyRobots == 0) return;
		
		RobotInfo best = null;
		boolean kill = false;
		int closest = 999;
		
		for (int x=0; x<radar.numEnemyRobots; x++)
		{
			RobotInfo ri = radar.enemyInfos[radar.enemyRobots[x]];
			if (!rc.canAttackSquare(ri.location)) continue;
			
			if (best == null)
			{
				best = ri;
				kill = best.energon <= SoldierConstants.ATTACK;
				closest = curLoc.distanceSquaredTo(best.location);
			} else if (kill)
			{
				if (ri.energon > SoldierConstants.ATTACK) continue;
				
				int dist = curLoc.distanceSquaredTo(ri.location);
				if (closest > dist)
				{
					best = ri;
					closest = curLoc.distanceSquaredTo(best.location);
				}
			} else
			{
				int dist = curLoc.distanceSquaredTo(ri.location);
				if (closest > dist)
				{
					best = ri;
					closest = curLoc.distanceSquaredTo(best.location);
					kill = best.energon <= SoldierConstants.ATTACK;
				}
			}
		}
		
		if (best != null)
			rc.attackSquare(best.location, best.robot.getRobotLevel());
	}
	
	public void swarm() throws GameActionException
	{
		radar.scan(true, true);
		
		if (radar.numEnemyRobots>0)
		{
			gotoStateAndExecute(SoldierState.CHASE);
			return;
		}
		
		
		if (swarmLoc == null || swarmUpdateRounds>SoldierConstants.SWARM_ROUNDS_UNTIL_STALE)
		{
			swarmLoc = dc.getClosestArchon();
			swarmDir = null;
		}
	}
	
	public void chase() throws GameActionException
	{
		radar.scan(true, true);
		
		if (radar.getArmyDifference() <= SoldierConstants.RETREAT_THRESHOLD)
		{
			gotoStateAndExecute(SoldierState.RETREAT);
			return;
		}
		
		if (radar.numEnemyRobots == 0)
		{
			if (chaseRounds--<=0 || chaseDir==null)
			{
				gotoStateAndExecute(SoldierState.SWARM);
				return;
			}
			return;
		} else if (radar.numEnemyRobots == radar.numEnemyTowers)
		{
			int closest = 999;
//			check that the towers are actually attackable
			for (int x=0; x<radar.numEnemyTowers; x++)
			{
				RobotInfo ri = radar.enemyInfos[radar.enemyTowers[x]];
				if (rc.senseConnected((PowerNode) rc.senseObjectAtLocation(ri.location, RobotLevel.POWER_NODE)))
				{
					int dist = curLoc.distanceSquaredTo(ri.location);
					if (dist < closest)
					{
						closest = dist;
						chaseTarget = ri;
					}
				}
			}
			if (chaseTarget==null)
			{
				if (chaseRounds--<=0 || chaseDir==null)
				{
					gotoStateAndExecute(SoldierState.SWARM);
					return;
				}
				return;
			}
		} else
		{
			chaseTarget = radar.closestEnemy;
		}
		
		chaseRounds = SoldierConstants.CHASE_ROUNDS;
		
		chaseDir = curLoc.directionTo(chaseTarget.location);
	}
	
	public void retreat() throws GameActionException
	{
		radar.scan(true, true);
		
		if (radar.getArmyDifference() >= SoldierConstants.RETREAT_CHASE)
		{
			gotoStateAndExecute(SoldierState.CHASE);
			return;
		} else if (radar.getArmyDifference() >= SoldierConstants.RETREAT_STABILIZE)
		{
			gotoStateAndExecute(SoldierState.SWARM);
			return;
		}
		
		if (radar.numEnemyRobots == 0)
		{
			if (retreatRounds-- <= 0 || retreatDir==null)
			{
				gotoStateAndExecute(SoldierState.SWARM);
				return;
			}
			return;
		} else retreatRounds = SoldierConstants.RETREAT_ROUNDS;
		
		retreatDir = radar.getEnemySwarmTarget().directionTo(curLoc);
		return;
	}
	
	@Override
	public MoveInfo computeNextMove() throws GameActionException
	{
		if (rc.getFlux() < SoldierConstants.SHUTDOWN_THRESHOLD)
		{
			//TODO turn around code
			return null;
		} else
		{
			switch (curstate)
			{
			case SWARM:
			{
				if (swarmDir==null)
				{
					nav.setDestination(swarmLoc);
					return new MoveInfo(nav.navigateToDestination(), false);
				}
				
				Direction toSwarm = curLoc.directionTo(swarmLoc);
				
//				if our direction to the swarm loc is equal or adjacent to the swarm dir
//				then this means we are behind the swarm and need to move through
				if ((toSwarm.ordinal()-swarmDir.ordinal()+9)%8 < 2)
				{
					return new MoveInfo(swarmDir, false);
				}
				
				if (curLoc.distanceSquaredTo(swarmLoc) < SoldierConstants.SWARM_TOO_CLOSE_DISTANCE)
				{
					return new MoveInfo(swarmDir, false);
				}
				
				if (curLoc.distanceSquaredTo(swarmLoc) > SoldierConstants.SWARM_TOO_FAR_DISTANCE)
				{
					return new MoveInfo(toSwarm, true);
				}
				
				if (curDir != swarmDir)
					return new MoveInfo(swarmDir);
				else new MoveInfo(swarmDir, false);
			}
			case CHASE:
			{
				if (radar.closestEnemyDist <= SoldierConstants.CHASE_CLOSE_DISTANCE)
				{
					chaseDir = curLoc.directionTo(radar.closestEnemy.location);
					return new MoveInfo(chaseDir.opposite(), true);
				} else
					return new MoveInfo(chaseDir, false);
			}
			case RETREAT:
			{
//				MASSIVE TODO calculate vectors of retreat and if any coincides 
//				with a direction to an archon
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
		}
		return null;
	}
}
