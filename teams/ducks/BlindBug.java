package ducks;

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
	
	public Direction navigateTo(MapLocation target) throws GameActionException
	{
		if (rc.isMovementActive()) {
			return Direction.NONE;
		}
		
		if (!target.equals(bugTarget))
		{
			bugTarget = target;
			bugStart = null;
			bugGing = false;
			bugRoundStart = 0;
		}
		
		if (br.currLoc.equals(target)) return Direction.NONE;
		
		Direction toTarget = br.currLoc.directionTo(target);
		boolean[] moveable = br.dc.getMovableDirections();
		
		if (br.currLoc.isAdjacentTo(target))
		{
			return toTarget;
		}
		
		boolean[] moveableland = br.dc.getMovableLand();
		int x = 0;
		while (true)
		{
			if (x++ > 100) {
				System.out.println(x);
			}
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
//							rc.moveForward();
							bugObs = br.currLoc.add(br.currDir).directionTo(br.currLoc.add(br.currDir.rotateRight()));
							return Constants.directions[dir];
						} else
						{
//							rc.setDirection(Constants.directions[dir]);
							return Constants.directions[dir];
						}
					} else
					{
//						int dir = toTarget.ordinal();
						int odir = bugObs.ordinal();
						int dir = odir;
						while (!moveable[dir]) dir = (dir+1)%8;
						if (br.currDir.equals(Constants.directions[dir]))
						{
//							rc.moveForward();
							bugObs = br.currLoc.add(br.currDir).directionTo(br.currLoc.add(br.currDir.rotateLeft()));
							return Constants.directions[dir];
						} else
						{
//							rc.setDirection(Constants.directions[dir]);
							return Constants.directions[dir];
						}
					}
				} else return Direction.NONE;
			} else //if (!bugGing)
			{
				if (br.currDir == toTarget)
				{
					if (moveable[toTarget.ordinal()])
					{
//						rc.moveForward();
						return toTarget;
					} else if (moveableland[toTarget.rotateLeft().ordinal()])
					{
//						rc.setDirection(toTarget.rotateLeft());
						return toTarget.rotateLeft();
					} else if (moveableland[toTarget.rotateRight().ordinal()])
					{
//						rc.setDirection(toTarget.rotateRight());
						return toTarget.rotateRight();
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
//						rc.moveForward();
						return br.currDir;
					} else if (moveableland[toTarget.ordinal()])
					{
//						rc.setDirection(toTarget);
						return toTarget;
					} else if (moveableland[toTarget.rotateRight().ordinal()])
					{
//						rc.setDirection(toTarget.rotateRight());
						return toTarget.rotateRight();
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
//						rc.moveForward();
						return br.currDir;
					} else if (moveableland[toTarget.ordinal()])
					{
//						rc.setDirection(toTarget);
						return toTarget;
					} else if (moveableland[toTarget.rotateLeft().ordinal()])
					{
//						rc.setDirection(toTarget.rotateLeft());
						return toTarget.rotateLeft();
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
//						rc.setDirection(toTarget);
						return toTarget;
					} else if (moveableland[toTarget.rotateLeft().ordinal()])
					{
//						rc.setDirection(toTarget.rotateLeft());
						return toTarget.rotateLeft();
					} else if (moveableland[toTarget.rotateRight().ordinal()])
					{
//						rc.setDirection(toTarget.rotateRight());
						return toTarget.rotateRight();
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
