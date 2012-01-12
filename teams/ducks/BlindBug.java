package ducks;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

public class BlindBug extends Navigation {

	private MapLocation bugTarget;
	private MapLocation bugStart;
	private double bugSlope;
	private boolean bugGing;
	private boolean bugCW;
	private Direction bugObs;
	private int bugRoundStart;
	
	private static final int BUG_ROUNDS_BEFORE_STOPPING = 300;
	
	public BlindBug(BaseRobot myBR) {
		super(myBR);
	}
	
	public void navigateTo(MapLocation target) throws GameActionException
	{
		if (rc.isMovementActive()) {
			return;
		}
		
		if (!target.equals(bugTarget))
		{
			bugTarget = target;
			bugStart = null;
			bugGing = false;
			bugRoundStart = 0;
		}
		
		if (br.currLoc.equals(target)) return;
		
		Direction toTarget = br.currLoc.directionTo(target);
		boolean[] moveable = br.dc.getMovableDirections();
		
		if (br.currLoc.isAdjacentTo(target))
		{
			if (br.currDir.equals(toTarget))
				if (moveable[toTarget.ordinal()])
					rc.moveForward();
			else rc.setDirection(toTarget);
			return;
		}
		
		boolean[] moveableland = br.dc.getMovableLand();
		while (true)
		{
			if (bugGing)
			{
				boolean stopbugging = false;
				if (br.currRound-bugRoundStart > BUG_ROUNDS_BEFORE_STOPPING)
				{
					stopbugging = true;
				} else if (bugTarget.x == bugStart.x)
				{
					if (br.currLoc.x == bugStart.x)
					{
						if (br.currLoc.y>bugStart.y)
						{
							if (br.currLoc.y<=bugTarget.y)
							{
								stopbugging = true;
							}
						} else if (br.currLoc.y<bugStart.y)
						{
							if (br.currLoc.y>=bugTarget.y)
							{
								stopbugging = true;
							}
						}
					}
				} else if (bugTarget.y == bugStart.y)
				{
					if (br.currLoc.y == bugStart.y)
					{
						if (br.currLoc.x>bugStart.x)
						{
							if (br.currLoc.x<=bugTarget.x)
							{
								stopbugging = true;
							}
						} else if (br.currLoc.x<bugStart.x)
						{
							if (br.currLoc.x>=bugTarget.x)
							{
								stopbugging = true;
							}
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
								if (br.currLoc.y>bugStart.y)
								{
									double approxX = (br.currLoc.y-bugStart.y)/bugSlope+bugStart.x;
									int a = (int)approxX;
									if (br.currLoc.x == a || br.currLoc.x == a+1)
									{
										stopbugging = true;
									} 
								}
							} else
							{
								if (br.currLoc.y<bugStart.y)
								{
									double approxX = (br.currLoc.y-bugStart.y)/bugSlope+bugStart.x;
									int a = (int)approxX;
									if (br.currLoc.x == a || br.currLoc.x == a+1)
									{
										stopbugging = true;
									} 
								}
							}
						} else //bugSlope<1
						{
							if (bugTarget.x>bugStart.x)
							{
								if (br.currLoc.x>bugStart.x)
								{
									double approxY = bugSlope*(br.currLoc.x-bugStart.x)+bugStart.y;
									int a = (int)approxY;
									if (br.currLoc.y == a || br.currLoc.y == a+1)
									{
										stopbugging = true;
									} 
								}
							} else
							{
								if (br.currLoc.x<bugStart.x)
								{
									double approxY = bugSlope*(br.currLoc.x-bugStart.x)+bugStart.y;
									int a = (int)approxY;
									if (br.currLoc.y == a || br.currLoc.y == a+1)
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
								if (br.currLoc.x>bugStart.x)
								{
									double approxY = bugSlope*(br.currLoc.x-bugStart.x)+bugStart.y;
									int a = (int)approxY;
									if (br.currLoc.y == a || br.currLoc.y == a+1)
									{
										stopbugging = true;
									} 
								}
							} else
							{
								if (br.currLoc.x<bugStart.x)
								{
									double approxY = bugSlope*(br.currLoc.x-bugStart.x)+bugStart.y;
									int a = (int)approxY;
									if (br.currLoc.y == a || br.currLoc.y == a+1)
									{
										stopbugging = true;
									} 
								}
							}
						} else //bugSlope<-1
						{
							if (bugTarget.y>bugStart.y)
							{
								if (br.currLoc.y>bugStart.y)
								{
									double approxX = (br.currLoc.y-bugStart.y)/bugSlope+bugStart.x;
									int a = (int)approxX;
									if (br.currLoc.x == a || br.currLoc.x == a+1)
									{
										stopbugging = true;
									} 
								}
							} else
							{
								if (br.currLoc.y<bugStart.y)
								{
									double approxX = (br.currLoc.y-bugStart.y)/bugSlope+bugStart.x;
									int a = (int)approxX;
									if (br.currLoc.x == a || br.currLoc.x == a+1)
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
						if (br.currDir.equals(Constants.directions[dir]))
						{
							rc.moveForward();
							bugObs = br.currLoc.add(br.currDir).directionTo(br.currLoc.add(br.currDir.rotateRight()));
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
						if (br.currDir.equals(Constants.directions[dir]))
						{
							rc.moveForward();
							bugObs = br.currLoc.add(br.currDir).directionTo(br.currLoc.add(br.currDir.rotateLeft()));
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
				if (br.currDir == toTarget)
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
						bugStart = br.currLoc;
						bugRoundStart = br.currRound;
						if (bugTarget.x!=bugStart.x)
							bugSlope = (bugTarget.y-bugStart.y)/(bugTarget.x-bugStart.x);
					}
				} else if (br.currDir == toTarget.rotateLeft())
				{
					if (moveable[br.currDir.ordinal()])
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
						bugStart = br.currLoc;
						bugRoundStart = br.currRound;
						if (bugTarget.x!=bugStart.x)
							bugSlope = (bugTarget.y-bugStart.y)/(bugTarget.x-bugStart.x);
					}
				} else if (br.currDir == toTarget.rotateRight())
				{
					if (moveable[br.currDir.ordinal()])
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
						bugStart = br.currLoc;
						bugRoundStart = br.currRound;
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
						bugStart = br.currLoc;
						bugRoundStart = br.currRound;
						if (bugTarget.x!=bugStart.x)
							bugSlope = (bugTarget.y-bugStart.y)/(bugTarget.x-bugStart.x);
					}
				}
			}
		}
	}
}
