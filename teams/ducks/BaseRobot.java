package ducks;

import battlecode.common.*;

public abstract class BaseRobot {
	
	final RobotController rc;
	final DataCache dc;
	final MapCache mc;
	final Navigator nav;
	final Radio io;
	Navigation nv;
	
	// Robot Stats
	final RobotType myType;
	final double myMaxEnergon, myMaxFlux;
	final Team myTeam;

	
	public double currEnergon, currFlux;
	public MapLocation currLoc, currLocInFront, currLocInBack;
	public Direction currDir;

	public final int spawnRound;
	public int currRound;
	
	public RobotState currState;
	
	public EnemyArchonInfo enemyArchonInfo;
	
	public BaseRobot(RobotController myRC) {
		rc = myRC;
		
		myType = rc.getType();
		myTeam = rc.getTeam();
		myMaxEnergon = myType.maxEnergon;
		myMaxFlux = myType.maxFlux;
		
		dc = new DataCache(this);
		mc = new MapCache(this);
		nav = new Navigator(this);
		io = new Radio(this);
		
		spawnRound = Clock.getRoundNum();
		
		enemyArchonInfo = new EnemyArchonInfo();
	}
	
	public abstract void run() throws GameActionException;
	
	public void loop() {
		while(true) {
	
			// Useful Statistics
			currEnergon = rc.getEnergon();
			currFlux = rc.getFlux();
			currLoc = rc.getLocation();
			currDir = rc.getDirection();
			
			currLocInFront = currLoc.add(currDir);
			currLocInBack = currLoc.add(currDir.opposite());
			
			currRound = Clock.getRoundNum();
			
			// show state of robot
			rc.setIndicatorString(0, "" + myType + " - " + currState);
			// show location of robot
			rc.setIndicatorString(1, "Location: " + currLoc);
			// show number of enemy archons
			rc.setIndicatorString(2, "Number of enemy archons: " +
					enemyArchonInfo.getNumEnemyArchons());
			
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
