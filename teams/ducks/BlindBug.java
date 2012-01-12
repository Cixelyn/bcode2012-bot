package ducks;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.TerrainTile;

public class BlindBug extends Navigation {

	private MapLocation bugTarget;
	private MapLocation bugStart;
	private double bugSlope;
	private boolean bugGing;
	private boolean bugCW;
	private Direction bugObs;
	private int bugRoundStart;
	private int bugStopThreshold;
	private int bugDist;
	
	private static final int BUG_ROUNDS_BEFORE_STOPPING_INITIAL = 3000;
	
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
			bugStopThreshold = BUG_ROUNDS_BEFORE_STOPPING_INITIAL;
			bugDist = 9999;
		}
		
		if (br.currLoc.equals(target)) return Direction.NONE;
		
		Direction toTarget = br.currLoc.directionTo(target);
		
		if (br.currLoc.isAdjacentTo(target))
		{
			return toTarget;
		}
		
		boolean[] moveable = br.dc.getMovableDirections();
		

		rc.setIndicatorString(1, "start:"+bugStart+" end:"+bugTarget+" cw:"+bugCW+" cur:"+br.currLoc+" obs:"+bugObs+" bugging:"+bugGing);
		rc.setIndicatorString(2, "rstart:"+bugRoundStart+" stopthreshold:"+bugStopThreshold);
		
		int x=0;
		boolean[] moveableland = br.dc.getMovableLand();
		while (true)
		{
			if (x++>3)
			{
				x++;
				return br.currDir.opposite();
			}
			if (bugGing)
			{
				boolean stopbugging = false;
				if (br.currRound-bugRoundStart > bugStopThreshold)
				{
					stopbugging = true;
					bugCW = !bugCW;
					bugRoundStart = br.currRound;
					bugStopThreshold = bugStopThreshold*2;
					continue;
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
				if (!stopbugging )
				{
					if (moveableland[toTarget.ordinal()])
					{
						int dist = br.currLoc.distanceSquaredTo(bugTarget);
						if (dist<bugDist)
						{
							bugDist = dist;
							stopbugging = true;
						}
					} else if (rc.senseTerrainTile(br.currLoc.add(bugObs))==TerrainTile.OFF_MAP)
					{
						stopbugging = true;
					}
					
				}
				if (stopbugging || moveable[bugObs.ordinal()])
				{
					bugGing = false;
					bugCW = !bugCW;
					bugStopThreshold = BUG_ROUNDS_BEFORE_STOPPING_INITIAL;
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
						
						bugStopThreshold = BUG_ROUNDS_BEFORE_STOPPING_INITIAL;
						bugDist = br.currLoc.distanceSquaredTo(bugTarget);
						
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
						
						bugStopThreshold = BUG_ROUNDS_BEFORE_STOPPING_INITIAL;
						bugDist = br.currLoc.distanceSquaredTo(bugTarget);
						
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
						
						bugStopThreshold = BUG_ROUNDS_BEFORE_STOPPING_INITIAL;
						bugDist = br.currLoc.distanceSquaredTo(bugTarget);
						
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
						
						bugStopThreshold = BUG_ROUNDS_BEFORE_STOPPING_INITIAL;
						bugDist = br.currLoc.distanceSquaredTo(bugTarget);
						
						if (bugTarget.x!=bugStart.x)
							bugSlope = (bugTarget.y-bugStart.y)/(bugTarget.x-bugStart.x);
					}
				}
			}
		}
	}
}
