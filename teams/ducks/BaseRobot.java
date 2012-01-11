package ducks;

import battlecode.common.*;

public abstract class BaseRobot {
	
	final RobotController rc;
	final DataCache dc;
	
	
	// Robot Stats
	final RobotType myType;
	final Team myTeam;
	final double myMaxEnergon;
	final double myMaxFlux;

	
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
		this.dc = new DataCache(rc);
		
		myType = this.rc.getType();
		myTeam = this.rc.getTeam();
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
			
			// power down if not enough flux
			if (rc.getFlux() <= POWER_DOWN_FLUX) {
				this.rc.setIndicatorString(0, "POWERING DOWN");
			} else {
				this.rc.setIndicatorString(0, "" + this.myType + " - " + this.currState);
				try{
					run();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			rc.yield();
			
		}
	}
	

}
