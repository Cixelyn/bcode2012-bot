package ducks;

import battlecode.common.*;

public abstract class BaseRobot {
	
	final RobotController rc;
	final DataCache dc;
	
	
	// Robot Stats
	final RobotType myType;
	final double myMaxEnergon;
	final double myMaxFlux;

	
	public double currEnergon;
	public double currFlux;
	public MapLocation currLoc, currLocInFront, currLocInBack;
	public Direction currDir;

	public int spawnRound;
	public int currRound;
	
	public RobotState currState;
	
	
	public BaseRobot(RobotController myRC) {
		this.rc = myRC;
		this.dc = new DataCache(rc);
		
		myType = this.rc.getType();
		myMaxEnergon = this.myType.maxEnergon;
		myMaxFlux = this.myType.maxFlux;
		
		spawnRound = Clock.getRoundNum();
		
	}
	
	public abstract void run() throws GameActionException;
	
	public void loop() {
		while(true) {
		
			currEnergon = rc.getEnergon();
			currFlux = rc.getFlux();
			currLoc = rc.getLocation();
			currDir = rc.getDirection();
			
			currLocInFront = currLoc.add(currDir);
			currLocInBack = currLoc.add(currDir.opposite());
			
			currRound = Clock.getRoundNum();
			
			
			try{
				run();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			rc.yield();
			
		}
	}
	

}
