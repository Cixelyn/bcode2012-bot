package ducks;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotLevel;

public class ScorcherRobotYPCamp extends BaseRobot {

	public ScorcherRobotYPCamp(RobotController myRC) throws GameActionException {
		super(myRC);
	}

	@Override
	public void run() throws GameActionException {
		if (!curDir.equals(curLoc.directionTo(myHome).opposite()))
			return;
		else
		{
			while (true)
			{
				rc.yield();
				rc.yield();
				rc.yield();
				rc.yield();
				rc.yield();
				rc.attackSquare(curLocInFront, RobotLevel.ON_GROUND);
			}
		}
		
	}
	
	@Override
	public MoveInfo computeNextMove() throws GameActionException {
		return new MoveInfo(curLoc.directionTo(myHome).opposite());
	}
}
