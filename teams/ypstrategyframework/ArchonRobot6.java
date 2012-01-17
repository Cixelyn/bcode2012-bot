package ypstrategyframework;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

enum ArchonState {
	INITIALIZE,
	INITIALSCOUT,
	INITIALRETURN,
	INITIALSPLIT,
	INITIALBUILD,
	EXPLORE,
	SWARM,
	ENGAGE,
	CHASE,
	RETREAT,
	REGROUP,
}

class ArchonConstants {
	public static final int INITIAL_SCOUT_ROUNDS = 100;
	public static final int MAX_INITIAL_RETURN_ROUNDS = 200;
	public static final int INITIAL_RETURN_DISTANCE = 16;
	public static final int MAX_INITIAL_SPLIT_ROUNDS = 40;
	public static final int MAX_INITIAL_BUILD_ROUNDS = 300;
	
	public static final int CHASE_THRESHOLD = 10;
	
	public static final int RETREAT_THRESHOLD = -10;
	
}

public class ArchonRobot6 extends BaseRobot {
	
	ArchonState curState;
	final ArchonBehavior ab;
	
	private int roundsSplit;
	private int roundsBuild;
	
	
	public ArchonRobot6(RobotController myRC) {
		super(myRC);
		ab = new ArchonBehavior(this);
	}

	@Override
	public void run() throws GameActionException {
		
		initializeForRound();
		
		execute(curState);
		
		cleanupForRound();
	}
	
	private void execute(ArchonState curState)
	{
		switch(curState)
		{
		case INITIALIZE:
			initialize();
			break;
		case INITIALSCOUT:
			initialScout();
			break;
		case INITIALRETURN:
			initialReturn();
			break;
		case INITIALSPLIT:
			initialSplit();
			break;
		case INITIALBUILD:
			initialBuild();
			break;
		case SWARM:
			swarm();
			break;
		case ENGAGE:
			engage();
			break;
		case CHASE:
			chase();
			break;
		case RETREAT:
			retreat();
			break;
		case REGROUP:
			regroup();
			break;
		}
	}
	
	private void gotoState(ArchonState newstate)
	{
		switch (newstate)
		{
		case INITIALSPLIT:
		{
			roundsSplit = 0;
		} break;
		case INITIALBUILD:
		{
			roundsBuild = 0;
		}
		}
		
		curState = newstate;
		execute(curState);
	}

	private void initializeForRound() {
		ab.initializeForRound();
	}
	
	private void cleanupForRound() {
		ab.manageFlux();
		ab.cleanupForRound();
	}
	
	private void initialize() {
		ab.initialize();
		curState = ArchonState.INITIALSCOUT;
	}
	
	private void initialScout() {
		if (currRound > ArchonConstants.INITIAL_SCOUT_ROUNDS)
		{
			gotoState(ArchonState.INITIALRETURN);
			return;
		} else if (ab.seeEnemies())
		{
			gotoState(ArchonState.INITIALRETURN);
			return;
		}
		ab.initialScout();
	}
	private void initialReturn() {
		if (currLoc.distanceSquaredTo(rc.sensePowerCore().getLocation()) 
				<= ArchonConstants.INITIAL_RETURN_DISTANCE ||
				currRound >= ArchonConstants.MAX_INITIAL_RETURN_ROUNDS)
		{
			gotoState(ArchonState.INITIALSPLIT);
			return;
		}
		ab.initialReturn();
		
	}
	private void initialSplit() {
		if (roundsSplit++ > ArchonConstants.MAX_INITIAL_SPLIT_ROUNDS)
		{
			gotoState(ArchonState.INITIALBUILD);
			return;
		}
		if (dc.getClosestArchon().distanceSquaredTo(currLoc) >= GameConstants.PRODUCTION_PENALTY_R2)
		{
			gotoState(ArchonState.INITIALBUILD);
			return;
		} else
		{
			ab.initialSplit();
		}
	}
	private void initialBuild() {
		if (roundsBuild++ > ArchonConstants.MAX_INITIAL_BUILD_ROUNDS)
		{
			gotoState(ArchonState.SWARM);
			return;
		}
		if (ab.finishedBuild())
		{
			gotoState(ArchonState.SWARM);
			return;
		}
		ab.initialBuild();
		
	}
	private void swarm() {
		if (ab.isLeader())
		{
			if (ab.seeEnemies() || ab.hasTarget())
			{
				gotoState(ArchonState.ENGAGE);
			} else
			{
				MapLocation powernode = ab.selectNextPowernode();
				
				if (rc.canSenseSquare(powernode))
				{
					ab.buildPowerNode(powernode);
				} else
				{
					ab.swarmMoveTo(powernode);
				}
			}
		} else
		{
			ab.announceEnemies();
			
			MapLocation leader = ab.getLeaderLocation();
			
			if (rc.canSenseSquare(leader))
			{
				ab.keepOff(powernode);
				ab.giveFlux()
			} else
			{
				ab.swarmMoveTo(powernode);
			}
		}
	}
	private void engage() {
		// TODO Auto-generated method stub
		
	}
	private void chase() {
		// TODO Auto-generated method stub
		
	}
	private void retreat() {
		// TODO Auto-generated method stub
		
	}
	private void regroup() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void processMessage(char msgType, StringBuilder sb)
			throws GameActionException {
		
		switch (msgType)
		{
		
		default:
			super.processMessage(msgType, sb);
		}
	}
	
	
	

}
