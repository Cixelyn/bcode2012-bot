package hardai;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.PowerNode;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;




public class ScoutRobotTheBiggest extends BaseRobot {
	
	public static final int SWARM_ROUNDS_UNTIL_STALE = 20;
	public static final int CHASE_ROUNDS = 40;
	
	public static final int CHASE_CLOSE_DISTANCE = 4;
	
	public static final int RETREAT_ROUNDS = 20;
	
	public static final int RETREAT_THRESHOLD = -4;
	public static final int RETREAT_STABILIZE = -2;
	public static final int RETREAT_CHASE = -1;
	
	public static final int SWARM_TOO_CLOSE_DISTANCE = 3;
	public static final int SWARM_TOO_FAR_DISTANCE = 30;
	
	public static final double SHUTDOWN_THRESHOLD = 1.0;
	
	public static final double HEAL_THRESHOLD_FLUX = 4.0;
	public static final double HEAL_THRESHOLD_NUM = 2;
	
	public static final double ATTACK = RobotType.SCOUT.attackPower+0.1;
	
	private enum ScoutState {
		CHASE,
		SWARM,
		RETREAT,
	};
	
	private ScoutState curstate;
	
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
	
	public ScoutRobotTheBiggest(RobotController myRC) throws GameActionException {
		super(myRC);
		
		curstate = ScoutState.SWARM;
	}

	@Override
	public void run() throws GameActionException
	{
		radar.scan(true, true);
		
		swarmUpdateRounds++;
		
		if(!rc.isAttackActive()) 
			attack();
		
		execute(curstate);
		
		int numdmg = radar.numAllyToRegenerate+(curEnergon<myMaxEnergon?1:0);
		
		if (rc.getFlux() > HEAL_THRESHOLD_FLUX
				&& numdmg > HEAL_THRESHOLD_NUM)
			rc.regenerate();
		
		switch (curstate)
		{
		case CHASE:
			rc.setIndicatorString(0, "state:"+curstate+" "+chaseTarget+" "+chaseDir+" "+chaseRounds);
			break;
		case RETREAT:
			rc.setIndicatorString(0, "state:"+curstate+" "+retreatRounds+" "+retreatDir);
			break;
		}
		
		closestmsg = 999;
		
	}
	
	public void execute(ScoutState state) throws GameActionException
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
	
	private void gotoStateAndExecute(ScoutState state) throws GameActionException
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
				kill = best.flux <= ATTACK;
				closest = curLoc.distanceSquaredTo(best.location);
			} else if (kill)
			{
				if (ri.flux > ATTACK) continue;
				
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
					kill = best.flux <= ATTACK;
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
			gotoStateAndExecute(ScoutState.CHASE);
			return;
		}
		
		
		
		if (swarmLoc == null || swarmUpdateRounds>SWARM_ROUNDS_UNTIL_STALE)
		{
			swarmLoc = dc.getClosestArchon();
			swarmDir = null;
		}
	}
	
	public void chase() throws GameActionException
	{
		radar.scan(true, true);
		
		if (radar.getArmyDifference() <= RETREAT_THRESHOLD)
		{
			gotoStateAndExecute(ScoutState.RETREAT);
			return;
		}
		
		if (radar.numEnemyRobots == 0)
		{
			if (chaseRounds--<=0 || chaseDir==null)
			{
				gotoStateAndExecute(ScoutState.SWARM);
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
					gotoStateAndExecute(ScoutState.SWARM);
					return;
				}
				return;
			}
		} else
		{
			chaseTarget = radar.closestEnemy;
		}
		
		chaseRounds = CHASE_ROUNDS;
		
		chaseDir = curLoc.directionTo(chaseTarget.location);
	}
	
	public void retreat() throws GameActionException
	{
		radar.scan(true, true);
		
		if (radar.getArmyDifference() >= RETREAT_CHASE)
		{
			gotoStateAndExecute(ScoutState.CHASE);
			return;
		} else if (radar.getArmyDifference() >= RETREAT_STABILIZE)
		{
			gotoStateAndExecute(ScoutState.SWARM);
			return;
		}
		
		if (radar.numEnemyRobots == 0)
		{
			if (retreatRounds-- <= 0 || retreatDir==null)
			{
				gotoStateAndExecute(ScoutState.SWARM);
				return;
			}
			return;
		} else retreatRounds = RETREAT_ROUNDS;
		
		retreatDir = radar.getEnemySwarmTarget().directionTo(curLoc);
		return;
	}
	
	@Override
	public MoveInfo computeNextMove() throws GameActionException
	{
		if (rc.getFlux() < SHUTDOWN_THRESHOLD)
		{
			//TODO turn around code
			return null;
		} else
		{
			MapLocation closest = dc.getClosestArchon();
			if (closest != null)
			{
				return new MoveInfo(curLoc.directionTo(closest),false);
			}
			
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
				
				if (curLoc.distanceSquaredTo(swarmLoc) < SWARM_TOO_CLOSE_DISTANCE)
				{
					return new MoveInfo(swarmDir, false);
				}
				
				if (curLoc.distanceSquaredTo(swarmLoc) > SWARM_TOO_FAR_DISTANCE)
				{
					return new MoveInfo(toSwarm, true);
				}
				
				if (curDir != swarmDir)
					return new MoveInfo(swarmDir);
				else return null;
			}
			case CHASE:
			{
				if (curLoc.distanceSquaredTo(chaseTarget.location) <= CHASE_CLOSE_DISTANCE)
				{
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
				
//				return new MoveInfo(nav.navigateGreedy(retreatLoc), true);
				return new MoveInfo(retreatDir, true);
			}
			}
		}
		return null;
	}
}
