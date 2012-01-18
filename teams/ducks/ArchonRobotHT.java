package ducks;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class ArchonRobotHT extends BaseRobot{
	boolean aboutToMove = false;
	Direction computedMoveDir = null;
	Direction dirToSense = null;
	int turnsStuck = 0;
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
		fbs.setBattleMode();
		nav.setNavigationMode(NavigationMode.TANGENT_BUG);
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
		
		MapLocation target = mc.guessBestPowerNodeToCapture();
		rc.setIndicatorString(0, "Target: <"+(target.x-curLoc.x)+","+(target.y-curLoc.y)+")");
		rc.setIndicatorString(1, "aboutToMove: "+aboutToMove+", computedMoveDir: "+computedMoveDir);
		nav.setDestination(target);
		
		if(dirToSense!=null) {
			mc.senseAfterMove(dirToSense);
			dirToSense = null;
		}
		
		fbs.manageFlux();
		
		radar.scan(true, true);
		
		if(Clock.getBytecodeNum()<7000)
			nav.prepare();
		
		if(!rc.isMovementActive()) {
			if(aboutToMove)  {
				if(rc.canMove(curDir)) {
					if(mc.isPowerNode(curLocInFront)) {
						if(rc.getFlux()>200) {
							rc.spawn(RobotType.TOWER);
						}
					} else {
						rc.moveForward();
						dirToSense = curDir;
						aboutToMove = false;
					}
				} else {
					Direction dir = nav.wiggleToMovableDirection(computedMoveDir);
					if(dir!=null) {
						rc.setDirection(dir);
					} else {
						turnsStuck++;
						if(turnsStuck>10) {
							rc.setDirection(nav.navigateCompletelyRandomly());
						}
					}
				}
			} else if(rc.getFlux()>280) {
				if(rc.canMove(curDir)) {
					rc.spawn(RobotType.SOLDIER);
				} else {
					rc.setDirection(curDir.rotateLeft());
				}
			} else {
				computedMoveDir = nav.navigateToDestination();
				if(computedMoveDir!=null) {
					Direction dir = nav.wiggleToMovableDirection(computedMoveDir);
					if(dir!=null) {
						rc.setDirection(dir);
						turnsStuck = 0;
						aboutToMove = true;
					}
				}
			}
		}
		
		
		if(Clock.getBytecodeNum()<5000 && Clock.getRoundNum()%6==myArchonID) {
			ses.broadcastPowerNodeFragment();
			ses.broadcastMapFragment();
			ses.broadcastMapEdges();
			mc.extractUpdatedPackedDataStep();
		}
		
		
		
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
