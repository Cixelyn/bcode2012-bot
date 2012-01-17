package ducks;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class BlindBug {

	private final BaseRobot br;
	private final RobotController rc;
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
		br = myBR;
		rc = myBR.rc;
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
		
		if (br.curLoc.equals(target)) return Direction.NONE;
		
		Direction toTarget = br.curLoc.directionTo(target);
		
		if (br.curLoc.isAdjacentTo(target))
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
				return br.curDir.opposite();
			}
			if (bugGing)
			{
				if (!bugFlipped)
				{
					boolean stopbugging = false;
					if (br.curRound-bugRoundStart > bugStopThreshold)
					{
						stopbugging = true;
						bugCW = !bugCW;
						bugRoundStart = br.curRound;
						bugStopThreshold = bugStopThreshold*2;
						continue;
					} else if (bugTarget.x == bugStart.x)
					{
						if (br.curLoc.x == bugStart.x)
						{
							if (br.curLoc.y>bugStart.y)
							{
								if (br.curLoc.y<=bugTarget.y)
								{
									stopbugging = true;
								}
							} else if (br.curLoc.y<bugStart.y)
							{
								if (br.curLoc.y>=bugTarget.y)
								{
									stopbugging = true;
								}
							}
						}
					} else if (bugTarget.y == bugStart.y)
					{
						if (br.curLoc.y == bugStart.y)
						{
							if (br.curLoc.x>bugStart.x)
							{
								if (br.curLoc.x<=bugTarget.x)
								{
									stopbugging = true;
								}
							} else if (br.curLoc.x<bugStart.x)
							{
								if (br.curLoc.x>=bugTarget.x)
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
									if (br.curLoc.y>bugStart.y)
									{
										double approxX = (br.curLoc.y-bugStart.y)/bugSlope+bugStart.x;
										int a = (int)approxX;
										if (br.curLoc.x == a || br.curLoc.x == a+1)
										{
											stopbugging = true;
										} 
									}
								} else
								{
									if (br.curLoc.y<bugStart.y)
									{
										double approxX = (br.curLoc.y-bugStart.y)/bugSlope+bugStart.x;
										int a = (int)approxX;
										if (br.curLoc.x == a || br.curLoc.x == a+1)
										{
											stopbugging = true;
										} 
									}
								}
							} else //bugSlope<1
							{
								if (bugTarget.x>bugStart.x)
								{
									if (br.curLoc.x>bugStart.x)
									{
										double approxY = bugSlope*(br.curLoc.x-bugStart.x)+bugStart.y;
										int a = (int)approxY;
										if (br.curLoc.y == a || br.curLoc.y == a+1)
										{
											stopbugging = true;
										} 
									}
								} else
								{
									if (br.curLoc.x<bugStart.x)
									{
										double approxY = bugSlope*(br.curLoc.x-bugStart.x)+bugStart.y;
										int a = (int)approxY;
										if (br.curLoc.y == a || br.curLoc.y == a+1)
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
									if (br.curLoc.x>bugStart.x)
									{
										double approxY = bugSlope*(br.curLoc.x-bugStart.x)+bugStart.y;
										int a = (int)approxY;
										if (br.curLoc.y == a || br.curLoc.y == a+1)
										{
											stopbugging = true;
										} 
									}
								} else
								{
									if (br.curLoc.x<bugStart.x)
									{
										double approxY = bugSlope*(br.curLoc.x-bugStart.x)+bugStart.y;
										int a = (int)approxY;
										if (br.curLoc.y == a || br.curLoc.y == a+1)
										{
											stopbugging = true;
										} 
									}
								}
							} else //bugSlope<-1
							{
								if (bugTarget.y>bugStart.y)
								{
									if (br.curLoc.y>bugStart.y)
									{
										double approxX = (br.curLoc.y-bugStart.y)/bugSlope+bugStart.x;
										int a = (int)approxX;
										if (br.curLoc.x == a || br.curLoc.x == a+1)
										{
											stopbugging = true;
										} 
									}
								} else
								{
									if (br.curLoc.y<bugStart.y)
									{
										double approxX = (br.curLoc.y-bugStart.y)/bugSlope+bugStart.x;
										int a = (int)approxX;
										if (br.curLoc.x == a || br.curLoc.x == a+1)
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
							int dist = br.curLoc.distanceSquaredTo(bugTarget);
							if (dist<bugDist)
							{
								bugDist = dist;
								stopbugging = true;
							}
						} else if (rc.senseTerrainTile(br.curLoc.add(bugObs))==TerrainTile.OFF_MAP)
						{
							bugCW = !bugCW;
							bugRoundStart = br.curRound;
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
					if (br.curDir.equals(Constants.directions[dir]))
					{
//							rc.moveForward();
						bugObs = br.curLoc.add(br.curDir).directionTo(br.curLoc.add(br.curDir.rotateRight()));
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
					if (br.curDir.equals(Constants.directions[dir]))
					{
//							rc.moveForward();
						bugObs = br.curLoc.add(br.curDir).directionTo(br.curLoc.add(br.curDir.rotateLeft()));
						return Constants.directions[dir];
					} else
					{
//							rc.setDirection(Constants.directions[dir]);
						return Constants.directions[dir];
					}
				}
			} else //if (!bugGing)
			{
				if (br.curDir == toTarget)
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
						bugStart = br.curLoc;
						bugRoundStart = br.curRound;
						
						bugStopThreshold = BUG_ROUNDS_BEFORE_STOPPING_INITIAL;
						bugDist = br.curLoc.distanceSquaredTo(bugTarget);
						bugFlipped = false;
						
						if (bugTarget.x!=bugStart.x)
							bugSlope = (bugTarget.y-bugStart.y)/(bugTarget.x-bugStart.x);
					}
				} else if (br.curDir == toTarget.rotateLeft())
				{
					if (moveableland[br.curDir.ordinal()])
					{
//						rc.moveForward();
						return br.curDir;
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
						bugStart = br.curLoc;
						bugRoundStart = br.curRound;
						
						bugStopThreshold = BUG_ROUNDS_BEFORE_STOPPING_INITIAL;
						bugDist = br.curLoc.distanceSquaredTo(bugTarget);
						bugFlipped = false;
						
						if (bugTarget.x!=bugStart.x)
							bugSlope = (bugTarget.y-bugStart.y)/(bugTarget.x-bugStart.x);
					}
				} else if (br.curDir == toTarget.rotateRight())
				{
					if (moveableland[br.curDir.ordinal()])
					{
//						rc.moveForward();
						return br.curDir;
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
						bugStart = br.curLoc;
						bugRoundStart = br.curRound;
						
						bugStopThreshold = BUG_ROUNDS_BEFORE_STOPPING_INITIAL;
						bugDist = br.curLoc.distanceSquaredTo(bugTarget);
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
						bugStart = br.curLoc;
						bugRoundStart = br.curRound;
						
						bugStopThreshold = BUG_ROUNDS_BEFORE_STOPPING_INITIAL;
						bugDist = br.curLoc.distanceSquaredTo(bugTarget);
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
		
		if (br.curLoc.equals(target)) return Direction.NONE;
		
		Direction toTarget = br.curLoc.directionTo(target);
		
		if (br.curLoc.isAdjacentTo(target))
		{
			return toTarget;
		}
		
		boolean[] moveable = br.dc.getMovableDirections();
		

		rc.setIndicatorString(1, "start:"+bugStart+" end:"+bugTarget+" cw:"+bugCW+" cur:"+br.curLoc+" obs:"+bugObs+" bugging:"+bugGing);
		rc.setIndicatorString(2, "rstart:"+bugRoundStart+" stopthreshold:"+bugStopThreshold);
		
		int x=0;
		boolean[] moveableland = br.dc.getMovableLand();
		while (true)
		{
			if (x++>3)
			{
				return br.curDir.opposite();
			}
			if (bugGing)
			{
				if (!bugFlipped)
				{
					boolean stopbugging = false;
					if (br.curRound-bugRoundStart > bugStopThreshold)
					{
						stopbugging = true;
						bugCW = !bugCW;
						bugRoundStart = br.curRound;
						bugStopThreshold = bugStopThreshold*2;
						continue;
					} else if (bugTarget.x == bugStart.x)
					{
						if (br.curLoc.x == bugStart.x)
						{
							if (br.curLoc.y>bugStart.y)
							{
								if (br.curLoc.y<=bugTarget.y)
								{
									stopbugging = true;
								}
							} else if (br.curLoc.y<bugStart.y)
							{
								if (br.curLoc.y>=bugTarget.y)
								{
									stopbugging = true;
								}
							}
						}
					} else if (bugTarget.y == bugStart.y)
					{
						if (br.curLoc.y == bugStart.y)
						{
							if (br.curLoc.x>bugStart.x)
							{
								if (br.curLoc.x<=bugTarget.x)
								{
									stopbugging = true;
								}
							} else if (br.curLoc.x<bugStart.x)
							{
								if (br.curLoc.x>=bugTarget.x)
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
									if (br.curLoc.y>bugStart.y)
									{
										double approxX = (br.curLoc.y-bugStart.y)/bugSlope+bugStart.x;
										int a = (int)approxX;
										if (br.curLoc.x == a || br.curLoc.x == a+1)
										{
											stopbugging = true;
										} 
									}
								} else
								{
									if (br.curLoc.y<bugStart.y)
									{
										double approxX = (br.curLoc.y-bugStart.y)/bugSlope+bugStart.x;
										int a = (int)approxX;
										if (br.curLoc.x == a || br.curLoc.x == a+1)
										{
											stopbugging = true;
										} 
									}
								}
							} else //bugSlope<1
							{
								if (bugTarget.x>bugStart.x)
								{
									if (br.curLoc.x>bugStart.x)
									{
										double approxY = bugSlope*(br.curLoc.x-bugStart.x)+bugStart.y;
										int a = (int)approxY;
										if (br.curLoc.y == a || br.curLoc.y == a+1)
										{
											stopbugging = true;
										} 
									}
								} else
								{
									if (br.curLoc.x<bugStart.x)
									{
										double approxY = bugSlope*(br.curLoc.x-bugStart.x)+bugStart.y;
										int a = (int)approxY;
										if (br.curLoc.y == a || br.curLoc.y == a+1)
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
									if (br.curLoc.x>bugStart.x)
									{
										double approxY = bugSlope*(br.curLoc.x-bugStart.x)+bugStart.y;
										int a = (int)approxY;
										if (br.curLoc.y == a || br.curLoc.y == a+1)
										{
											stopbugging = true;
										} 
									}
								} else
								{
									if (br.curLoc.x<bugStart.x)
									{
										double approxY = bugSlope*(br.curLoc.x-bugStart.x)+bugStart.y;
										int a = (int)approxY;
										if (br.curLoc.y == a || br.curLoc.y == a+1)
										{
											stopbugging = true;
										} 
									}
								}
							} else //bugSlope<-1
							{
								if (bugTarget.y>bugStart.y)
								{
									if (br.curLoc.y>bugStart.y)
									{
										double approxX = (br.curLoc.y-bugStart.y)/bugSlope+bugStart.x;
										int a = (int)approxX;
										if (br.curLoc.x == a || br.curLoc.x == a+1)
										{
											stopbugging = true;
										} 
									}
								} else
								{
									if (br.curLoc.y<bugStart.y)
									{
										double approxX = (br.curLoc.y-bugStart.y)/bugSlope+bugStart.x;
										int a = (int)approxX;
										if (br.curLoc.x == a || br.curLoc.x == a+1)
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
							int dist = br.curLoc.distanceSquaredTo(bugTarget);
							if (dist<bugDist)
							{
								bugDist = dist;
								stopbugging = true;
							}
						} else if (rc.senseTerrainTile(br.curLoc.add(bugObs))==TerrainTile.OFF_MAP)
						{
							bugCW = !bugCW;
							bugRoundStart = br.curRound;
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
						if (br.curDir.equals(Constants.directions[dir]))
						{
//							rc.moveForward();
							bugObs = br.curLoc.add(br.curDir).directionTo(br.curLoc.add(br.curDir.rotateRight()));
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
						if (br.curDir.equals(Constants.directions[dir]))
						{
//							rc.moveForward();
							bugObs = br.curLoc.add(br.curDir).directionTo(br.curLoc.add(br.curDir.rotateLeft()));
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
				if (br.curDir == toTarget)
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
						bugStart = br.curLoc;
						bugRoundStart = br.curRound;
						
						bugStopThreshold = BUG_ROUNDS_BEFORE_STOPPING_INITIAL;
						bugDist = br.curLoc.distanceSquaredTo(bugTarget);
						bugFlipped = false;
						
						if (bugTarget.x!=bugStart.x)
							bugSlope = (bugTarget.y-bugStart.y)/(bugTarget.x-bugStart.x);
					}
				} else if (br.curDir == toTarget.rotateLeft())
				{
					if (moveable[br.curDir.ordinal()])
					{
//						rc.moveForward();
						return br.curDir;
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
						bugStart = br.curLoc;
						bugRoundStart = br.curRound;
						
						bugStopThreshold = BUG_ROUNDS_BEFORE_STOPPING_INITIAL;
						bugDist = br.curLoc.distanceSquaredTo(bugTarget);
						bugFlipped = false;
						
						if (bugTarget.x!=bugStart.x)
							bugSlope = (bugTarget.y-bugStart.y)/(bugTarget.x-bugStart.x);
					}
				} else if (br.curDir == toTarget.rotateRight())
				{
					if (moveable[br.curDir.ordinal()])
					{
//						rc.moveForward();
						return br.curDir;
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
						bugStart = br.curLoc;
						bugRoundStart = br.curRound;
						
						bugStopThreshold = BUG_ROUNDS_BEFORE_STOPPING_INITIAL;
						bugDist = br.curLoc.distanceSquaredTo(bugTarget);
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
						bugStart = br.curLoc;
						bugRoundStart = br.curRound;
						
						bugStopThreshold = BUG_ROUNDS_BEFORE_STOPPING_INITIAL;
						bugDist = br.curLoc.distanceSquaredTo(bugTarget);
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
		
		if (br.rc.senseTerrainTile(br.curLoc.add(bugDirection))==TerrainTile.OFF_MAP) return Direction.NONE;
		
		Direction toTarget = targetdir;
		
		boolean[] moveable = br.dc.getMovableDirections();
		

		rc.setIndicatorString(1, "start:"+bugStart+" end:"+bugDirection+" cw:"+bugCW+" cur:"+br.curLoc+" obs:"+bugObs+" bugging:"+bugGing);
		rc.setIndicatorString(2, "rstart:"+bugRoundStart+" stopthreshold:"+bugStopThreshold);
		
		int x=0;
		boolean[] moveableland = br.dc.getMovableLand();
		while (true)
		{
			if (x++>3)
			{
				return br.curDir.opposite();
			}
			if (bugGing)
			{
				if (!bugFlipped)
				{
					boolean stopbugging = false;
					if (br.curRound-bugRoundStart > bugStopThreshold)
					{
						stopbugging = true;
						bugCW = !bugCW;
						bugRoundStart = br.curRound;
						bugStopThreshold = bugStopThreshold*2;
						continue;
					} else if (bugTarget.x == bugStart.x)
					{
						if (br.curLoc.x == bugStart.x)
						{
							if (br.curLoc.y>bugStart.y)
							{
								if (br.curLoc.y<=bugTarget.y)
								{
									stopbugging = true;
								}
							} else if (br.curLoc.y<bugStart.y)
							{
								if (br.curLoc.y>=bugTarget.y)
								{
									stopbugging = true;
								}
							}
						}
					} else if (bugTarget.y == bugStart.y)
					{
						if (br.curLoc.y == bugStart.y)
						{
							if (br.curLoc.x>bugStart.x)
							{
								if (br.curLoc.x<=bugTarget.x)
								{
									stopbugging = true;
								}
							} else if (br.curLoc.x<bugStart.x)
							{
								if (br.curLoc.x>=bugTarget.x)
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
									if (br.curLoc.y>bugStart.y)
									{
										double approxX = (br.curLoc.y-bugStart.y)/bugSlope+bugStart.x;
										int a = (int)approxX;
										if (br.curLoc.x == a || br.curLoc.x == a+1)
										{
											stopbugging = true;
										} 
									}
								} else
								{
									if (br.curLoc.y<bugStart.y)
									{
										double approxX = (br.curLoc.y-bugStart.y)/bugSlope+bugStart.x;
										int a = (int)approxX;
										if (br.curLoc.x == a || br.curLoc.x == a+1)
										{
											stopbugging = true;
										} 
									}
								}
							} else //bugSlope<1
							{
								if (bugTarget.x>bugStart.x)
								{
									if (br.curLoc.x>bugStart.x)
									{
										double approxY = bugSlope*(br.curLoc.x-bugStart.x)+bugStart.y;
										int a = (int)approxY;
										if (br.curLoc.y == a || br.curLoc.y == a+1)
										{
											stopbugging = true;
										} 
									}
								} else
								{
									if (br.curLoc.x<bugStart.x)
									{
										double approxY = bugSlope*(br.curLoc.x-bugStart.x)+bugStart.y;
										int a = (int)approxY;
										if (br.curLoc.y == a || br.curLoc.y == a+1)
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
									if (br.curLoc.x>bugStart.x)
									{
										double approxY = bugSlope*(br.curLoc.x-bugStart.x)+bugStart.y;
										int a = (int)approxY;
										if (br.curLoc.y == a || br.curLoc.y == a+1)
										{
											stopbugging = true;
										} 
									}
								} else
								{
									if (br.curLoc.x<bugStart.x)
									{
										double approxY = bugSlope*(br.curLoc.x-bugStart.x)+bugStart.y;
										int a = (int)approxY;
										if (br.curLoc.y == a || br.curLoc.y == a+1)
										{
											stopbugging = true;
										} 
									}
								}
							} else //bugSlope<-1
							{
								if (bugTarget.y>bugStart.y)
								{
									if (br.curLoc.y>bugStart.y)
									{
										double approxX = (br.curLoc.y-bugStart.y)/bugSlope+bugStart.x;
										int a = (int)approxX;
										if (br.curLoc.x == a || br.curLoc.x == a+1)
										{
											stopbugging = true;
										} 
									}
								} else
								{
									if (br.curLoc.y<bugStart.y)
									{
										double approxX = (br.curLoc.y-bugStart.y)/bugSlope+bugStart.x;
										int a = (int)approxX;
										if (br.curLoc.x == a || br.curLoc.x == a+1)
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
							int dist = calcDist(targetdir, br.curLoc);
							if (dist<bugDist)
							{
								bugDist = dist;
								stopbugging = true;
							}
						} else if (rc.senseTerrainTile(br.curLoc.add(bugObs))==TerrainTile.OFF_MAP)
						{
							bugCW = !bugCW;
							bugRoundStart = br.curRound;
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
						if (br.curDir.equals(Constants.directions[dir]))
						{
//							rc.moveForward();
							bugObs = br.curLoc.add(br.curDir).directionTo(br.curLoc.add(br.curDir.rotateRight()));
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
						if (br.curDir.equals(Constants.directions[dir]))
						{
//							rc.moveForward();
							bugObs = br.curLoc.add(br.curDir).directionTo(br.curLoc.add(br.curDir.rotateLeft()));
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
				if (br.curDir == toTarget)
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
						bugStart = br.curLoc;
						bugRoundStart = br.curRound;
						
						bugStopThreshold = BUG_ROUNDS_BEFORE_STOPPING_INITIAL;
						bugDist = calcDist(targetdir, br.curLoc);
						bugFlipped = false;
						
						setBugTarget(bugStart,toTarget);
					}
				} else if (br.curDir == toTarget.rotateLeft())
				{
					if (moveable[br.curDir.ordinal()])
					{
//						rc.moveForward();
						return br.curDir;
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
						bugStart = br.curLoc;
						bugRoundStart = br.curRound;
						
						bugStopThreshold = BUG_ROUNDS_BEFORE_STOPPING_INITIAL;
						bugDist = calcDist(targetdir, br.curLoc);
						bugFlipped = false;

						setBugTarget(bugStart,toTarget);
					}
				} else if (br.curDir == toTarget.rotateRight())
				{
					if (moveable[br.curDir.ordinal()])
					{
//						rc.moveForward();
						return br.curDir;
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
						bugStart = br.curLoc;
						bugRoundStart = br.curRound;
						
						bugStopThreshold = BUG_ROUNDS_BEFORE_STOPPING_INITIAL;
						bugDist = calcDist(targetdir, br.curLoc);
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
						bugStart = br.curLoc;
						bugRoundStart = br.curRound;
						
						bugStopThreshold = BUG_ROUNDS_BEFORE_STOPPING_INITIAL;
						bugDist = calcDist(targetdir, br.curLoc);
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
