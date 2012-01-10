package ducks;

import battlecode.common.*;

public class DataCache {
	
	RobotController rc;
	MapLocation[] alliedArchons;
	int alliedArchonsTime = -1;

	
	public DataCache(RobotController myRC) {
		this.rc = myRC;
	}

	
	public MapLocation[] getAlliedArchons() {
		
		if(Clock.getRoundNum() > alliedArchonsTime) {
			alliedArchons = rc.senseAlliedArchons();
		}
		
		return alliedArchons;
		
	}
	
	
}
