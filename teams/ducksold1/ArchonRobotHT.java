package ducksold1;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class ArchonRobotHT extends BaseRobot{
	boolean aboutToMove = false;
	Direction lastMoved = null;
	int myArchonID;
	public ArchonRobotHT(RobotController myRC) {
		super(myRC);
		nav.setNavigationMode(NavigationMode.TANGENT_BUG);
		nav.setNavigationMode(NavigationMode.RANDOM);
		
		// compute archon ID
		MapLocation[] alliedArchons = this.dc.getAlliedArchons();
		for(int i=alliedArchons.length; --i>=0; ) {
			if(alliedArchons[i].equals(this.currLoc)) {
				myArchonID = i;
				break;
			}
		}
		io.setAddresses(new String[] {"#e"});
	}
	
	@Override
	public void run() throws GameActionException {
		//if(!currLoc.equals(rc.senseAlliedArchons()[0])) return;
		if(myArchonID==5 && Clock.getRoundNum()%500==5)
			System.out.println(mc);
		mc.senseAfterMove(lastMoved);
		MapLocation target = rc.senseCapturablePowerNodes()[0];
		rc.setIndicatorString(0, "dxdy: "+(target.x-currLoc.x)+","+(target.y-currLoc.y));
		rc.setIndicatorString(1, "abouttomove: "+aboutToMove);
		nav.setDestination(target);
		nav.prepare();
		
		if(!rc.isMovementActive()) {
			Direction dir;
			if(aboutToMove) {
				dir = currDir;
				aboutToMove = false;
			} else {
				dir = nav.navigateToDestination();
				rc.setIndicatorString(2, dir.toString());
			}
			if(!(dir==Direction.NONE || dir==Direction.OMNI)) {
				for(int i=0; i<30 && !rc.canMove(dir); i++) {
					if(Math.random()<0.5)
						dir = dir.rotateLeft();
					else
						dir = dir.rotateRight();
				}
				if(rc.canMove(dir)) {
					if(dir==currDir) {
						if(currLocInFront.equals(target)) {
							if(currFlux>210 && rc.canMove(currDir)) {
								rc.spawn(RobotType.TOWER);
							}
						} else {
							lastMoved = currDir;
							rc.moveForward();
						}
					} else {
						rc.setDirection(dir);
						aboutToMove = true;
					}
				}
			}
		
		}
		
		if(Math.random()<0.3) {
		ses.broadcastPowerNodeGraph();
		ses.broadcastMapFragment();
		ses.broadcastMapEdges();
		}
		mc.extractUpdatedPackedDataStep();

	}
	@Override
	public void processMessage(char msgType, StringBuilder sb) {
		int[] data = null;
		if(msgType=='e') {
			data = Radio.decodeShorts(sb);
			ses.receiveMapEdges(data);
		} else if(msgType=='m') {
			data = Radio.decodeInts(sb);
			ses.receiveMapFragment(data);
		} else if(msgType=='p') {
			data = Radio.decodeInts(sb);
			ses.receivePowerNodeGraph(data);
		} 
	}
}
