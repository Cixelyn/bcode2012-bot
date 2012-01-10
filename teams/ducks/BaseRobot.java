package ducks;

import battlecode.common.*;

public abstract class BaseRobot {
	
	final RobotController rc;
	
	// Robot Stats
	final RobotType myType;
	final double myMaxEnergon;
	final double myMaxFlux;

	
	public double currEnergon;
	public double currFlux;
	public MapLocation currLoc, currLocInFront, currLocInBack;
	public Direction currDir;
	
	
	
	public BaseRobot(RobotController myRC) {
		this.rc = myRC;
		
		myType = this.rc.getType();
		myMaxEnergon = this.myType.maxEnergon;
		myMaxFlux = this.myType.maxFlux;
		
	}
	
	public abstract void run();
	
	public void loop() {
		while(true) {
			
		
			currEnergon = rc.getEnergon();
			currFlux = rc.getFlux();
			currLoc = rc.getLocation();
			currDir = rc.getDirection();
			
			currLocInFront = currLoc.add(currDir);
			currLocInBack = currLoc.add(currDir.opposite());
			
			
			try{
				run();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			
		}
	}
	

}
