package ducks;

import battlecode.common.*;

public abstract class BaseRobot {
	
	final RobotController rc;
	final DataCache dc;
	final Radio io;
	
	
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

	public void processMessage(StringBuilder sb) {
		
	}
	
	public MapLocation bugTarget;
	public MapLocation bugStart;
	public double bugSlope;
	public boolean bugGing;
	public boolean bugCW;
	public Direction bugObs;
	
	public void blindBug(MapLocation target) throws GameActionException
	{
		if (!target.equals(bugTarget))
		{
			bugTarget = target;
			bugStart = null;
			bugGing = false;
		}
		
		if (currLoc.equals(target)) return;
		
		Direction toTarget = currLoc.directionTo(target);
		boolean[] moveable = dc.getMovableDirections();
		
		if (currLoc.isAdjacentTo(target))
		{
			if (currDir.equals(toTarget))
				if (moveable[toTarget.ordinal()])
					rc.moveForward();
			else rc.setDirection(toTarget);
			return;
		}
		
		boolean[] moveableland = dc.getMovableLand();
		
		
		while (true)
		{
			if (bugGing)
			{
				boolean stopbugging = false;
				if (bugTarget.x == bugStart.x)
				{
					if (currLoc.x == bugStart.x)
					{
						if (currLoc.y>bugStart.y)
						{
							if (currLoc.y<=bugTarget.y)
							{
								stopbugging = true;
							}
						} else if (currLoc.y>=bugTarget.y)
						{
							stopbugging = true;
						}
					}
				} else if (bugTarget.y == bugStart.y)
				{
					if (currLoc.y == bugStart.y)
					{
						if (currLoc.x>bugStart.x)
						{
							if (currLoc.x<=bugTarget.x)
							{
								stopbugging = true;
							}
						} else if (currLoc.x>=bugTarget.x)
						{
							stopbugging = true;
						}
					}
				} else
				{
					if (bugSlope>0)
					{
						if (bugSlope>1)
						{
							if (bugTarget.y>bugStart.y)
							{
								if (currLoc.y>bugStart.y)
								{
									double approxX = (currLoc.y-bugStart.y)/bugSlope+bugStart.x;
									int a = (int)approxX;
									if (currLoc.x == a || currLoc.x == a+1)
									{
										stopbugging = true;
									} 
								}
							} else
							{
								if (currLoc.y<bugStart.y)
								{
									double approxX = (currLoc.y-bugStart.y)/bugSlope+bugStart.x;
									int a = (int)approxX;
									if (currLoc.x == a || currLoc.x == a+1)
									{
										stopbugging = true;
									} 
								}
							}
						} else //bugSlope<1
						{
							if (bugTarget.x>bugStart.x)
							{
								if (currLoc.x>bugStart.x)
								{
									double approxY = bugSlope*(currLoc.x-bugStart.x)+bugStart.y;
									int a = (int)approxY;
									if (currLoc.y == a || currLoc.y == a+1)
									{
										stopbugging = true;
									} 
								}
							} else
							{
								if (currLoc.x<bugStart.x)
								{
									double approxY = bugSlope*(currLoc.x-bugStart.x)+bugStart.y;
									int a = (int)approxY;
									if (currLoc.y == a || currLoc.y == a+1)
									{
										stopbugging = true;
									} 
								}
							}
						}
					} else // bugSlope<0
					{
						if (bugSlope>-1)
						{
							if (bugTarget.x>bugStart.x)
							{
								if (currLoc.x>bugStart.x)
								{
									double approxY = bugSlope*(currLoc.x-bugStart.x)+bugStart.y;
									int a = (int)approxY;
									if (currLoc.y == a || currLoc.y == a+1)
									{
										stopbugging = true;
									} 
								}
							} else
							{
								if (currLoc.x<bugStart.x)
								{
									double approxY = bugSlope*(currLoc.x-bugStart.x)+bugStart.y;
									int a = (int)approxY;
									if (currLoc.y == a || currLoc.y == a+1)
									{
										stopbugging = true;
									} 
								}
							}
						} else //bugSlope<-1
						{
							if (bugTarget.y>bugStart.y)
							{
								if (currLoc.y>bugStart.y)
								{
									double approxX = (currLoc.y-bugStart.y)/bugSlope+bugStart.x;
									int a = (int)approxX;
									if (currLoc.x == a || currLoc.x == a+1)
									{
										stopbugging = true;
									} 
								}
							} else
							{
								if (currLoc.y<bugStart.y)
								{
									double approxX = (currLoc.y-bugStart.y)/bugSlope+bugStart.x;
									int a = (int)approxX;
									if (currLoc.x == a || currLoc.x == a+1)
									{
										stopbugging = true;
									} 
								}
							}
						}
					}
				}
				if (stopbugging || moveable[bugObs.ordinal()])
				{
					bugGing = false;
					bugCW = !bugCW;
					continue;
				} else if (	moveable[0] || moveable[1] || moveable[2] || moveable[3] || 
							moveable[4] || moveable[5] || moveable[6] || moveable[7])
				{
					if (bugCW)
					{
//						int dir = toTarget.ordinal();
						int odir = bugObs.ordinal();
						int dir = odir;
						while (!moveable[dir]) dir = (dir+7)%8;
						if (currDir.equals(Constants.directions[dir]))
						{
							rc.moveForward();
							bugObs = currLoc.add(currDir).directionTo(currLoc.add(currDir.rotateRight()));
							return;
						} else
						{
							rc.setDirection(Constants.directions[dir]);
							return;
						}
					} else
					{
//						int dir = toTarget.ordinal();
						int odir = bugObs.ordinal();
						int dir = odir;
						while (!moveable[dir]) dir = (dir+1)%8;
						if (currDir.equals(Constants.directions[dir]))
						{
							rc.moveForward();
							bugObs = currLoc.add(currDir).directionTo(currLoc.add(currDir.rotateLeft()));
							return;
						} else
						{
							rc.setDirection(Constants.directions[dir]);
							return;
						}
					}
				} else return;
			} else //if (!bugGing)
			{
				if (currDir == toTarget)
				{
					if (moveable[toTarget.ordinal()])
					{
						rc.moveForward();
						return;
					} else if (moveableland[toTarget.rotateLeft().ordinal()])
					{
						rc.setDirection(toTarget.rotateLeft());
						return;
					} else if (moveableland[toTarget.rotateRight().ordinal()])
					{
						rc.setDirection(toTarget.rotateRight());
						return;
					} else
					{
						bugGing = true;
						bugObs = toTarget;
						bugStart = currLoc;
						if (bugTarget.x!=bugStart.x)
							bugSlope = (bugTarget.y-bugStart.y)/(bugTarget.x-bugStart.x);
					}
				} else if (currDir == toTarget.rotateLeft())
				{
					if (moveable[currDir.ordinal()])
					{
						rc.moveForward();
						return;
					} else if (moveableland[toTarget.ordinal()])
					{
						rc.setDirection(toTarget);
						return;
					} else if (moveableland[toTarget.rotateRight().ordinal()])
					{
						rc.setDirection(toTarget.rotateRight());
						return;
					} else
					{
						bugGing = true;
						bugObs = toTarget;
						bugStart = currLoc;
						if (bugTarget.x!=bugStart.x)
							bugSlope = (bugTarget.y-bugStart.y)/(bugTarget.x-bugStart.x);
					}
				} else if (currDir == toTarget.rotateRight())
				{
					if (moveable[currDir.ordinal()])
					{
						rc.moveForward();
						return;
					} else if (moveableland[toTarget.ordinal()])
					{
						rc.setDirection(toTarget);
						return;
					} else if (moveableland[toTarget.rotateLeft().ordinal()])
					{
						rc.setDirection(toTarget.rotateLeft());
						return;
					} else
					{
						bugGing = true;
						bugObs = toTarget;
						bugStart = currLoc;
						if (bugTarget.x!=bugStart.x)
							bugSlope = (bugTarget.y-bugStart.y)/(bugTarget.x-bugStart.x);
					}
				} else
				{
					if (moveableland[toTarget.ordinal()])
					{
						rc.setDirection(toTarget);
						return;
					} else if (moveableland[toTarget.rotateLeft().ordinal()])
					{
						rc.setDirection(toTarget.rotateLeft());
						return;
					} else if (moveableland[toTarget.rotateRight().ordinal()])
					{
						rc.setDirection(toTarget.rotateRight());
						return;
					} else
					{
						bugGing = true;
						bugObs = toTarget;
						bugStart = currLoc;
						if (bugTarget.x!=bugStart.x)
							bugSlope = (bugTarget.y-bugStart.y)/(bugTarget.x-bugStart.x);
					}
				}
			}
		}
	}
}
