package ypstrategyframework;

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
	private boolean bugFlipped;
	private Direction bugObs;
	private int bugRoundStart;
	private int bugStopThreshold;
	private int bugDist;
	private Direction bugDirection;
	
	private static final int BUG_ROUNDS_BEFORE_STOPPING_INITIAL = 3000;
	
	public BlindBug(BaseRobot myBR) {
		super(myBR);
	}
	
	@Override
	public Direction navigateTo(MapLocation destination) throws GameActionException {
		return navigateToFull(destination);
	}
	
	/**
	 * resets the bug to an unbugging state. Clears target and start and sets appropriate variables
	 */
	public void resetBug()
	{
		bugTarget = null;
		bugStart = null;
		bugGing = false;
		bugRoundStart = 0;
		bugStopThreshold = BUG_ROUNDS_BEFORE_STOPPING_INITIAL;
		bugDist = Integer.MAX_VALUE;
		bugFlipped = false;
	}
	
	/**
	 * Bug nav, only using terrain, does not take into account obstacles like other robots
	 */
	public Direction navigateToIgnoreBots(MapLocation target)
	{
		if (rc.isMovementActive()) {
			return Direction.NONE;
		}
		
		bugDirection = null;
		
		if (!target.equals(bugTarget))
		{
			bugTarget = target;
			bugStart = null;
			bugGing = false;
			bugRoundStart = 0;
			bugStopThreshold = BUG_ROUNDS_BEFORE_STOPPING_INITIAL;
			bugDist = Integer.MAX_VALUE;
			bugFlipped = false;
		}
		
		if (br.currLoc.equals(target)) return Direction.NONE;
		
		Direction toTarget = br.currLoc.directionTo(target);
		
		if (br.currLoc.isAdjacentTo(target))
		{
			return toTarget;
		}
		
//		boolean[] moveable = br.dc.getMovableDirections();
		

		//rc.setIndicatorString(1, "start:"+bugStart+" end:"+bugTarget+" cw:"+bugCW+" cur:"+br.currLoc+" obs:"+bugObs+" bugging:"+bugGing);
		//rc.setIndicatorString(2, "rstart:"+bugRoundStart+" stopthreshold:"+bugStopThreshold);
		
		int x=0;
		boolean[] moveableland = br.dc.getMovableLand();
		while (true)
		{
			if (x++>3)
			{
				return br.currDir.opposite();
			}
			if (bugGing)
			{
				if (!bugFlipped)
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
							bugCW = !bugCW;
							bugRoundStart = br.currRound;
							bugStopThreshold = 99999;
							bugFlipped = true;
							continue;
						}
						
					}
					if (stopbugging || moveableland[bugObs.ordinal()])
					{
						bugGing = false;
						bugCW = !bugCW;
						bugStopThreshold = BUG_ROUNDS_BEFORE_STOPPING_INITIAL;
						bugFlipped = true;
						continue;
					}
				}
				
//				if (	moveableland[0] || moveableland[1] || moveableland[2] || moveableland[3] || 
//						moveableland[4] || moveableland[5] || moveableland[6] || moveableland[7])
				
				if (bugCW)
				{
//						int dir = toTarget.ordinal();
					int odir = bugObs.ordinal();
					int dir = odir;
					while (!moveableland[dir]) dir = (dir+7)%8;
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
					while (!moveableland[dir]) dir = (dir+1)%8;
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
			} else //if (!bugGing)
			{
				if (br.currDir == toTarget)
				{
					if (moveableland[toTarget.ordinal()])
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
						bugFlipped = false;
						
						if (bugTarget.x!=bugStart.x)
							bugSlope = (bugTarget.y-bugStart.y)/(bugTarget.x-bugStart.x);
					}
				} else if (br.currDir == toTarget.rotateLeft())
				{
					if (moveableland[br.currDir.ordinal()])
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
						bugFlipped = false;
						
						if (bugTarget.x!=bugStart.x)
							bugSlope = (bugTarget.y-bugStart.y)/(bugTarget.x-bugStart.x);
					}
				} else if (br.currDir == toTarget.rotateRight())
				{
					if (moveableland[br.currDir.ordinal()])
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
						bugFlipped = false;
						
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
						bugFlipped = false;
						
						if (bugTarget.x!=bugStart.x)
							bugSlope = (bugTarget.y-bugStart.y)/(bugTarget.x-bugStart.x);
					}
				}
			}
		}
	}
	
	/**
	 * bug nav that takes into account other robots
	 */
	public Direction navigateToFull(MapLocation target)
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
			bugDist = Integer.MAX_VALUE;
			bugFlipped = false;
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
				return br.currDir.opposite();
			}
			if (bugGing)
			{
				if (!bugFlipped)
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
							bugCW = !bugCW;
							bugRoundStart = br.currRound;
							bugStopThreshold = 99999;
							bugFlipped = true;
							continue;
						}
						
					}
					if (stopbugging || moveable[bugObs.ordinal()])
					{
						bugGing = false;
						bugCW = !bugCW;
						bugStopThreshold = BUG_ROUNDS_BEFORE_STOPPING_INITIAL;
						bugFlipped = true;
						continue;
					}
				}
				
				if (	moveable[0] || moveable[1] || moveable[2] || moveable[3] || 
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
						bugFlipped = false;
						
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
						bugFlipped = false;
						
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
						bugFlipped = false;
						
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
						bugFlipped = false;
						
						if (bugTarget.x!=bugStart.x)
							bugSlope = (bugTarget.y-bugStart.y)/(bugTarget.x-bugStart.x);
					}
				}
			}
		}
	}
	
	/**
	 * bug nav that takes into account other robots
	 */
	public Direction navigateToDirection(Direction targetdir)
	{
		if (rc.isMovementActive()) {
			return Direction.NONE;
		}
		
//		bugTarget = null;
		
		if (!targetdir.equals(bugDirection))
		{
			bugDirection = targetdir;
			bugStart = null;
			bugGing = false;
			bugRoundStart = 0;
			bugStopThreshold = BUG_ROUNDS_BEFORE_STOPPING_INITIAL;
			bugDist = 9999;
			bugFlipped = false;
		}
		
		if (br.rc.senseTerrainTile(br.currLoc.add(bugDirection))==TerrainTile.OFF_MAP) return Direction.NONE;
		
		Direction toTarget = targetdir;
		
		boolean[] moveable = br.dc.getMovableDirections();
		

		rc.setIndicatorString(1, "start:"+bugStart+" end:"+bugDirection+" cw:"+bugCW+" cur:"+br.currLoc+" obs:"+bugObs+" bugging:"+bugGing);
		rc.setIndicatorString(2, "rstart:"+bugRoundStart+" stopthreshold:"+bugStopThreshold);
		
		int x=0;
		boolean[] moveableland = br.dc.getMovableLand();
		while (true)
		{
			if (x++>3)
			{
				return br.currDir.opposite();
			}
			if (bugGing)
			{
				if (!bugFlipped)
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
							int dist = calcDist(targetdir, br.currLoc);
							if (dist<bugDist)
							{
								bugDist = dist;
								stopbugging = true;
							}
						} else if (rc.senseTerrainTile(br.currLoc.add(bugObs))==TerrainTile.OFF_MAP)
						{
							bugCW = !bugCW;
							bugRoundStart = br.currRound;
							bugStopThreshold = 99999;
							bugFlipped = true;
							continue;
						}
						
					}
					if (stopbugging || moveable[bugObs.ordinal()])
					{
						bugGing = false;
						bugCW = !bugCW;
						bugStopThreshold = BUG_ROUNDS_BEFORE_STOPPING_INITIAL;
						bugFlipped = true;
						continue;
					}
				}
				
				if (	moveable[0] || moveable[1] || moveable[2] || moveable[3] || 
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
						bugDist = calcDist(targetdir, br.currLoc);
						bugFlipped = false;
						
						setBugTarget(bugStart,toTarget);
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
						bugDist = calcDist(targetdir, br.currLoc);
						bugFlipped = false;

						setBugTarget(bugStart,toTarget);
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
						bugDist = calcDist(targetdir, br.currLoc);
						bugFlipped = false;

						setBugTarget(bugStart,toTarget);
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
						bugDist = calcDist(targetdir, br.currLoc);
						bugFlipped = false;

						setBugTarget(bugStart,toTarget);
					}
				}
			}
		}
	}
	
	/**
	 * distance metric for navigating to a direction.
	 * note: - DOES NOT MEAN YOU ARE AT TARGET,
	 * THE SACLES ARE ARBITRARY
	 */
	public int calcDist(Direction dir, MapLocation loc)
	{
		switch (dir)
		{
		case NORTH: return loc.y;
		case NORTH_EAST: return loc.y-loc.x;
		case EAST: return -loc.x;
		case SOUTH_EAST: return -loc.x-loc.y;
		case SOUTH: return -loc.y;
		case SOUTH_WEST: return loc.x-loc.y;
		case WEST: return loc.x;
		case NORTH_WEST: return loc.x+loc.y;
		default: return 0;
		}
	}
	
	/** distance to project false bug target when bugging in a direction */
	private static final int GET_BUG_TARGET_DIST = 10000;
	/**
	 * returns a fake target for bugging in a direction
	 */
	public MapLocation getBugTarget(MapLocation loc, Direction dir)
	{
		switch (dir)
		{
		case NORTH: return loc.add(0, -GET_BUG_TARGET_DIST);
		case NORTH_EAST: return loc.add(GET_BUG_TARGET_DIST,-GET_BUG_TARGET_DIST);
		case EAST: return loc.add(GET_BUG_TARGET_DIST,0);
		case SOUTH_EAST: return loc.add(GET_BUG_TARGET_DIST,GET_BUG_TARGET_DIST);
		case SOUTH: return loc.add(0,GET_BUG_TARGET_DIST);
		case SOUTH_WEST: return loc.add(-GET_BUG_TARGET_DIST,GET_BUG_TARGET_DIST);
		case WEST: return loc.add(-GET_BUG_TARGET_DIST,0);
		case NORTH_WEST: return loc.add(-GET_BUG_TARGET_DIST,-GET_BUG_TARGET_DIST);
		default: return loc;
		}
	}
	
	/**
	 * sets the fake target and bug slope for bugging in a direction
	 */
	public void setBugTarget(MapLocation loc, Direction dir)
	{
		switch (dir)
		{
		case NORTH: bugSlope = Double.NEGATIVE_INFINITY; bugTarget = loc.add(0, -GET_BUG_TARGET_DIST); return;
		case NORTH_EAST: bugSlope = -1.0; bugTarget = loc.add(GET_BUG_TARGET_DIST,-GET_BUG_TARGET_DIST); return;
		case EAST: bugSlope = 0.0; bugTarget = loc.add(GET_BUG_TARGET_DIST,0); return;
		case SOUTH_EAST: bugSlope = 1.0; bugTarget = loc.add(GET_BUG_TARGET_DIST,GET_BUG_TARGET_DIST); return;
		case SOUTH: bugSlope = Double.POSITIVE_INFINITY; bugTarget = loc.add(0,GET_BUG_TARGET_DIST); return;
		case SOUTH_WEST: bugSlope = -1.0; bugTarget = loc.add(-GET_BUG_TARGET_DIST,GET_BUG_TARGET_DIST); return;
		case WEST: bugSlope = 0.0; bugTarget = loc.add(-GET_BUG_TARGET_DIST,0); return;
		case NORTH_WEST: bugSlope = 1.0; bugTarget = loc.add(-GET_BUG_TARGET_DIST,-GET_BUG_TARGET_DIST); return;
		default: bugSlope = 0.0; bugTarget = loc; return;
		}
	}
}
