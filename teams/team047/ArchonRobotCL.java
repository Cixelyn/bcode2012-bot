package team047;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class ArchonRobotCL extends BaseRobot {
	
	private enum BehaviorState{SPLIT,DEFEND,RUSH,CAP}
	BehaviorState behavior;
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
		
		behavior = BehaviorState.SPLIT;
		
		io.addChannel(BroadcastChannel.ALL);
		io.addChannel(BroadcastChannel.ARCHONS);
	}

	@Override
	public void run() throws GameActionException {
		rc.setIndicatorString(0,behavior.toString());
		

	}
	
	
	@Override
	public void useExtraBytecodes() {
		if(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>3000)
			nav.prepare(); 
		if(Clock.getRoundNum()%6==myArchonID) {
			if(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>4000)
				ses.broadcastMapFragment();
			if(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>2000)
				ses.broadcastPowerNodeFragment();
			if(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>1000) 
				ses.broadcastMapEdges();
		}
		super.useExtraBytecodes();
		while(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>3000) 
			nav.prepare();
		while(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>1000) 
			mc.extractUpdatedPackedDataStep();
	}

}
