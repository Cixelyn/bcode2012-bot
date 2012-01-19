package ducks;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class ArchonRobotCL extends BaseRobot {
	
	int myArchonID;
	

	public ArchonRobotCL(RobotController myRC) throws GameActionException {
		super(myRC);
		MapLocation[] alliedArchons = this.dc.getAlliedArchons();
		for(int i=alliedArchons.length; --i>=0; ) {
			if(alliedArchons[i].equals(this.curLoc)) {
				myArchonID = i;
				break;
			}
		}
	}

	@Override
	public void run() throws GameActionException {

	}

}
