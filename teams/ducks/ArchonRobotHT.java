package ducks;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class ArchonRobotHT extends BaseRobot{
	boolean aboutToMove = false;
	Direction lastMoved = null;
	int myArchonID;
	public ArchonRobotHT(RobotController myRC) {
		super(myRC);
		nav.setNavigationMode(NavigationMode.RANDOM);
		
		// compute archon ID
		MapLocation[] alliedArchons = this.dc.getAlliedArchons();
		for(int i=alliedArchons.length; --i>=0; ) {
			if(alliedArchons[i].equals(this.curLoc)) {
				myArchonID = i;
				break;
			}
		}
		io.setAddresses(new String[] {"#e"});
	}
	
	@Override
	public void run() throws GameActionException {
//		if(!currLoc.equals(rc.senseAlliedArchons()[0])) return;
//		if(myArchonID==0 && Clock.getRoundNum()%500==5) {
//			System.out.println(mc);
//			System.out.println(mc.guessEnemyPowerCoreLocation());
//			System.out.println(mc.guessBestPowerNodeToCapture());
//		}
		mc.senseAfterMove(lastMoved);
		
		rc.setIndicatorString(0, ""+aboutToMove);
		if(!rc.isMovementActive()) {
			if(aboutToMove && rc.canMove(curDir))  {
				rc.moveForward();
				lastMoved = curDir;
				aboutToMove = false;
			} else {
				Direction dir = curDir;
				if(Math.random()<0.3)  {
					if(Math.random()<0.5) 
						dir = dir.rotateLeft();
					else
						dir = dir.rotateRight();
				}
				int i=0;
				while(!rc.canMove(dir)) {
					i++;
					if(i>3) break;
					if(Math.random()<0.5) 
						dir = dir.rotateLeft();
					else
						dir = dir.rotateRight();
				}
				rc.setDirection(dir);
				aboutToMove = true;
			}
		}
		
		
		if(Math.random()<0.2) {
			ses.broadcastPowerNodeFragment();
			ses.broadcastMapFragment();
			ses.broadcastMapEdges();
		}
		mc.extractUpdatedPackedDataStep();
		
		
	}
	@Override
	public void processMessage(char msgType, StringBuilder sb) {
		int[] data = null;
		if(msgType=='e') {
			data = BroadcastSystem.decodeShorts(sb);
			ses.receiveMapEdges(data);
		} else if(msgType=='m') {
			data = BroadcastSystem.decodeInts(sb);
			ses.receiveMapFragment(data);
		} else if(msgType=='p') {
			data = BroadcastSystem.decodeInts(sb);
			ses.receivePowerNodeFragment(data);
		} 
	}
}
