package ypstrategyframework;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotLevel;

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
	public static final int CHASE_ROUNDS = 40;
	
	public static final int SAFE_THRESHOLD = 5;
	
	public static final int RETREAT_THRESHOLD = -10;
	public static final int RETREAT_ROUNDS = 40;
	
}

public class ArchonRobot6 extends BaseRobot {
	
	ArchonState curState;
	final ArchonBehavior ab;
	
	private int roundsSplit;
	private int roundsBuild;
	
	@SuppressWarnings("unused")
	private int[] evaluations;
	@SuppressWarnings("unused")
	private MapLocation[] evaluationOrigins;
	
	private int retreat_rounds;
	
	private Direction chaseDirection;
	private int roundsLastSeenEnemy;
	
	
	public ArchonRobot6(RobotController myRC) {
		super(myRC);
		ab = new ArchonBehavior(this);
		evaluations = new int[6];
		evaluationOrigins = new MapLocation[6];
	}

	@Override
	public void run() throws GameActionException {
		
		initializeForRound();
		
		execute(curState);
		
		cleanupForRound();
	}
	
	private void execute(ArchonState curState) throws GameActionException
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
	
	private void gotoState(ArchonState newstate) throws GameActionException
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
		} break;
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
	
	private void initialScout() throws GameActionException {
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
	private void initialReturn() throws GameActionException {
		if (currLoc.distanceSquaredTo(rc.sensePowerCore().getLocation()) 
				<= ArchonConstants.INITIAL_RETURN_DISTANCE ||
				currRound >= ArchonConstants.MAX_INITIAL_RETURN_ROUNDS)
		{
			gotoState(ArchonState.INITIALSPLIT);
			return;
		}
		ab.initialReturn();
		
	}
	private void initialSplit() throws GameActionException {
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
	private void initialBuild() throws GameActionException {
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
	private void swarm() throws GameActionException {
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
				if (rc.senseObjectAtLocation(currLoc, RobotLevel.POWER_NODE)!=null)
					ab.getOutOfSquare();
				else
				{
					ab.swarmAround(leader);
				}
			} else
			{
				ab.returnToSwarm(leader);
			}
		}
	}
	private void engage() throws GameActionException {
		int eval = ab.evaluatePosition();
		
		if (!ab.seeEnemies())
		{
			gotoState(ArchonState.CHASE);
			return;
		}
		
		if (eval > ArchonConstants.CHASE_THRESHOLD)
		{
			gotoState(ArchonState.CHASE);
			return;
		}
		
		if (eval < ArchonConstants.RETREAT_ROUNDS)
		{
			gotoState(ArchonState.CHASE);
			return;
		}
		
		ab.checkSoldiers();
		
		ab.keepDistance();
		
	}
	private void chase() throws GameActionException {
		
		int eval = ab.evaluatePosition();
		
		if (eval < ArchonConstants.RETREAT_THRESHOLD)
		{
			gotoState(ArchonState.RETREAT);
			return;
		}
		if (eval < ArchonConstants.CHASE_THRESHOLD)
		{
			gotoState(ArchonState.ENGAGE);
			return;
		}
		if (!ab.hasTarget() || !ab.seeEnemies())
		{
			if (roundsLastSeenEnemy++ > ArchonConstants.CHASE_ROUNDS)
			{
				gotoState(ArchonState.SWARM);
				return;
			}
		} else
		{
			chaseDirection = currLoc.directionTo(ab.getTarget());
			roundsLastSeenEnemy = 0;
		}
		if (chaseDirection==null)
		{
			gotoState(ArchonState.SWARM);
			return;
		}
		ab.chase(chaseDirection);
	}
	private void retreat() throws GameActionException {
		int eval = ab.evaluatePosition();
		
		if (eval > ArchonConstants.RETREAT_THRESHOLD)
		{
			if (retreat_rounds>ArchonConstants.RETREAT_ROUNDS)
			{
				gotoState(ArchonState.REGROUP);
				return;
			}
		}
		
		ab.retreatFromEnemy();
	}
	private void regroup() throws GameActionException {
		int eval = ab.evaluatePosition();
		
		if (eval > ArchonConstants.RETREAT_THRESHOLD)
		{
			gotoState(ArchonState.RETREAT);
			return;
		}
		
		if (eval > ArchonConstants.SAFE_THRESHOLD)
		{
			gotoState(ArchonState.SWARM);
			return;
		}
		
		ab.regroup();
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
