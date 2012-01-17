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
		
		// compute archon ID
		MapLocation[] alliedArchons = this.dc.getAlliedArchons();
		for(int i=alliedArchons.length; --i>=0; ) {
			if(alliedArchons[i].equals(this.curLoc)) {
				myArchonID = i;
				break;
			}
		}
		io.setAddresses(new String[] {"#e"});
		nav.setNavigationMode(NavigationMode.BUG);
		nav.setDestination(curLoc.add(-50, -50));
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
		
		if(!rc.isMovementActive()) {
			if(aboutToMove)  {
				if(rc.canMove(curDir)) {
				rc.moveForward();
				lastMoved = curDir;
				
				} 
				aboutToMove = false;
			} 
			if(!aboutToMove && !rc.isMovementActive()) {
				Direction dir = nav.navigateToDestination();
				if(dir!=null) {
					dir = nav.wiggleToMovableDirection(dir);
					if(dir!=null) {
					rc.setDirection(dir);
					aboutToMove = true;
					}
				}
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
