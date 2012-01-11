package ducks;

import battlecode.common.*;

public abstract class BaseRobot {
	
	final RobotController rc;
	final DataCache dc;
	final Radio io;
	Navigation nv;
	
	// Robot Stats
	final RobotType myType;
	final double myMaxEnergon;
	final double myMaxFlux;
	final Team myTeam;

	
	public double currEnergon;
	public double currFlux;
	public MapLocation currLoc, currLocInFront, currLocInBack;
	public Direction currDir;

	public final int spawnRound;
	public int currRound;
	
	public RobotState currState;
	
	// TODO(jven): put me in constants
	final double MIN_ARCHON_FLUX = 0.2;
	final double MIN_UNIT_FLUX = 20;
	final double POWER_DOWN_FLUX = 1;
	final int BROADCAST_FREQUENCY = 5;
	final int MIN_DAMAGED_UNITS_TO_REGEN = 1;
	
	
	
	public BaseRobot(RobotController myRC) {
		this.rc = myRC;
		this.dc = new DataCache(this);
		this.io = new Radio(this);
		
		myType = this.rc.getType();
		myTeam = this.rc.getTeam();
		myMaxEnergon = this.myType.maxEnergon;
		myMaxFlux = this.myType.maxFlux;
		
		spawnRound = Clock.getRoundNum();
		
	}
	
	public abstract void run() throws GameActionException;
	
	public void loop() {
		while(true) {
			
			// print out state of robot
			rc.setIndicatorString(0, "" + myType + " - " + currState);
	
			// Useful Statistics
			currEnergon = rc.getEnergon();
			currFlux = rc.getFlux();
			currLoc = rc.getLocation();
			currDir = rc.getDirection();
			
			currLocInFront = currLoc.add(currDir);
			currLocInBack = currLoc.add(currDir.opposite());
			
			currRound = Clock.getRoundNum();
			
			// Main Radio Receive Call
			try {
				io.receive();
			} catch(Exception e) {
				e.printStackTrace();
				rc.addMatchObservation(e.toString());
			}
			
		
			// Main Run Call
			try{
				run();
			} catch (Exception e) {
				e.printStackTrace();
				rc.addMatchObservation(e.toString());
			}
				
			
			// Broadcast Queued Messages
			try {
				io.sendAll();
			} catch (Exception e) {
				e.printStackTrace();
				rc.addMatchObservation(e.toString());
			}
			
		
			// End of Turn
			rc.yield();
			
		}
	}

	public void processMessage(char msgType, StringBuilder sb) {}
	

}
