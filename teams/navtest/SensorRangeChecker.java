package navtest;

import java.util.ArrayList;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class SensorRangeChecker {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		for (RobotType t : RobotType.values())
		{
			System.out.println("private static final int[][][] sensorRange"+t.name().toUpperCase()+" = new int[][][] { //"+t.name().toUpperCase());
			for (int iii=0; iii<8; iii++)
			{
				Direction d = Direction.values()[iii];
				
				ArrayList<int[]> tosense = new ArrayList<int[]>();
				int r = (int)(Math.sqrt(t.sensorRadiusSquared))+1;
				for (int x=-r; x<=r; x++)
					for (int y=-r; y<=r; y++)
					{
						MapLocation loc = new MapLocation(x, y);
						if (checkCanSense(loc, t, d) && !checkCanSense(loc.add(d), t, d))
							tosense.add(new int[]{x,y});
					}
				System.out.println("\t{ //"+d.name().toUpperCase());
				System.out.print("\t\t");
				for (int[] a : tosense)
				{
					System.out.print("{"+a[0]+","+a[1]+"},");
				}
				System.out.println();
				System.out.println("\t},");
			}
			System.out.println("};");
		}
		
	}
	
	public static boolean checkCanSense(MapLocation loc, RobotType type, Direction dir) {
		MapLocation myLoc = new MapLocation(0, 0);
		return myLoc.distanceSquaredTo(loc)<=type.sensorRadiusSquared
			&& inAngleRange(myLoc,dir,loc,type.sensorCosHalfTheta);
	}
	
	private static MapLocation origin = new MapLocation(0, 0);
	
	public static boolean inAngleRange(MapLocation sensor, Direction dir, MapLocation target, double cosHalfTheta) {
        MapLocation dirVec = origin.add(dir);
        double dx = target.x - sensor.x;
        double dy = target.y - sensor.y;
        int a = dirVec.x;
        int b = dirVec.y;
        double dotProduct = a * dx + b * dy;

        if (dotProduct < 0) {
            if (cosHalfTheta > 0)
                return false;
        } else if (cosHalfTheta < 0)
            return true;

        double rhs = cosHalfTheta * cosHalfTheta * (dx * dx + dy * dy) * (a * a + b * b);

        if (dotProduct < 0)
            return (dotProduct * dotProduct <= rhs + 0.00001d);
        else
            return (dotProduct * dotProduct >= rhs - 0.00001d);
    }
	
	public void senseAllTerrainTiles(RobotController rc)
	{
		MapLocation currLoc = rc.getLocation();
		RobotType currType = rc.getType();
		final int cx = currLoc.x;
		final int cy = currLoc.y;
		Direction d = rc.getDirection();
		
		boolean[][] map = new boolean[256][256];
		boolean[][] seen = new boolean[256][256];
		
		switch (currType)
		{
		case ARCHON:
		{
			for (int x=-5; x<=5; x++)
				for (int y=-5; y<=5; y++)
				{
					if (!seen[cx+x][cy+y])
						;
				}
		}
			break;
		case DISRUPTER:
		{
			
		}
			break;
		case SCORCHER:
		{
			
		}
			break;
		case SCOUT:
		{
			
		}
			break;
		case SOLDIER:
		{
			
		}
			break;
		}
	}
	
	private static final int[][][] sensorRangeARCHON = new int[][][] { //ARCHON
		{ //NORTH
			{-6,0},{-5,-3},{-4,-4},{-3,-5},{-2,-5},{-1,-5},{0,-6},{1,-5},{2,-5},{3,-5},{4,-4},{5,-3},{6,0},
		},
		{ //NORTH_EAST
			{-3,-5},{-2,-5},{0,-6},{0,-5},{1,-5},{2,-5},{3,-5},{3,-4},{4,-4},{4,-3},{5,-3},{5,-2},{5,-1},{5,0},{5,2},{5,3},{6,0},
		},
		{ //EAST
			{0,-6},{0,6},{3,-5},{3,5},{4,-4},{4,4},{5,-3},{5,-2},{5,-1},{5,1},{5,2},{5,3},{6,0},
		},
		{ //SOUTH_EAST
			{-3,5},{-2,5},{0,5},{0,6},{1,5},{2,5},{3,4},{3,5},{4,3},{4,4},{5,-3},{5,-2},{5,0},{5,1},{5,2},{5,3},{6,0},
		},
		{ //SOUTH
			{-6,0},{-5,3},{-4,4},{-3,5},{-2,5},{-1,5},{0,6},{1,5},{2,5},{3,5},{4,4},{5,3},{6,0},
		},
		{ //SOUTH_WEST
			{-6,0},{-5,-3},{-5,-2},{-5,0},{-5,1},{-5,2},{-5,3},{-4,3},{-4,4},{-3,4},{-3,5},{-2,5},{-1,5},{0,5},{0,6},{2,5},{3,5},
		},
		{ //WEST
			{-6,0},{-5,-3},{-5,-2},{-5,-1},{-5,1},{-5,2},{-5,3},{-4,-4},{-4,4},{-3,-5},{-3,5},{0,-6},{0,6},
		},
		{ //NORTH_WEST
			{-6,0},{-5,-3},{-5,-2},{-5,-1},{-5,0},{-5,2},{-5,3},{-4,-4},{-4,-3},{-3,-5},{-3,-4},{-2,-5},{-1,-5},{0,-6},{0,-5},{2,-5},{3,-5},
		},
	};
	private static final int[][][] sensorRangeSOLDIER = new int[][][] { //SOLDIER
		{ //NORTH
			{-3,-1},{-2,-2},{-1,-3},{0,-3},{1,-3},{2,-2},{3,-1},
		},
		{ //NORTH_EAST
			{-1,-3},{0,-3},{1,-3},{1,-2},{2,-2},{2,-1},{3,-1},{3,0},{3,1},
		},
		{ //EAST
			{1,-3},{1,3},{2,-2},{2,2},{3,-1},{3,0},{3,1},
		},
		{ //SOUTH_EAST
			{-1,3},{0,3},{1,2},{1,3},{2,1},{2,2},{3,-1},{3,0},{3,1},
		},
		{ //SOUTH
			{-3,1},{-2,2},{-1,3},{0,3},{1,3},{2,2},{3,1},
		},
		{ //SOUTH_WEST
			{-3,-1},{-3,0},{-3,1},{-2,1},{-2,2},{-1,2},{-1,3},{0,3},{1,3},
		},
		{ //WEST
			{-3,-1},{-3,0},{-3,1},{-2,-2},{-2,2},{-1,-3},{-1,3},
		},
		{ //NORTH_WEST
			{-3,-1},{-3,0},{-3,1},{-2,-2},{-2,-1},{-1,-3},{-1,-2},{0,-3},{1,-3},
		},
	};
	private static final int[][][] sensorRangeSCOUT = new int[][][] { //SCOUT
		{ //NORTH
			{-5,0},{-4,-3},{-3,-4},{-2,-4},{-1,-4},{0,-5},{1,-4},{2,-4},{3,-4},{4,-3},{5,0},
		},
		{ //NORTH_EAST
			{-3,-4},{-2,-4},{0,-5},{0,-4},{1,-4},{2,-4},{3,-4},{3,-3},{4,-3},{4,-2},{4,-1},{4,0},{4,2},{4,3},{5,0},
		},
		{ //EAST
			{0,-5},{0,5},{3,-4},{3,4},{4,-3},{4,-2},{4,-1},{4,1},{4,2},{4,3},{5,0},
		},
		{ //SOUTH_EAST
			{-3,4},{-2,4},{0,4},{0,5},{1,4},{2,4},{3,3},{3,4},{4,-3},{4,-2},{4,0},{4,1},{4,2},{4,3},{5,0},
		},
		{ //SOUTH
			{-5,0},{-4,3},{-3,4},{-2,4},{-1,4},{0,5},{1,4},{2,4},{3,4},{4,3},{5,0},
		},
		{ //SOUTH_WEST
			{-5,0},{-4,-3},{-4,-2},{-4,0},{-4,1},{-4,2},{-4,3},{-3,3},{-3,4},{-2,4},{-1,4},{0,4},{0,5},{2,4},{3,4},
		},
		{ //WEST
			{-5,0},{-4,-3},{-4,-2},{-4,-1},{-4,1},{-4,2},{-4,3},{-3,-4},{-3,4},{0,-5},{0,5},
		},
		{ //NORTH_WEST
			{-5,0},{-4,-3},{-4,-2},{-4,-1},{-4,0},{-4,2},{-4,3},{-3,-4},{-3,-3},{-2,-4},{-1,-4},{0,-5},{0,-4},{2,-4},{3,-4},
		},
	};
	private static final int[][][] sensorRangeDISRUPTER = new int[][][] { //DISRUPTER
		{ //NORTH
			{-4,0},{-3,-2},{-2,-3},{-1,-3},{0,-4},{1,-3},{2,-3},{3,-2},{4,0},
		},
		{ //NORTH_EAST
			{-2,-3},{0,-4},{0,-3},{1,-3},{2,-3},{2,-2},{3,-2},{3,-1},{3,0},{3,2},{4,0},
		},
		{ //EAST
			{0,-4},{0,4},{2,-3},{2,3},{3,-2},{3,-1},{3,1},{3,2},{4,0},
		},
		{ //SOUTH_EAST
			{-2,3},{0,3},{0,4},{1,3},{2,2},{2,3},{3,-2},{3,0},{3,1},{3,2},{4,0},
		},
		{ //SOUTH
			{-4,0},{-3,2},{-2,3},{-1,3},{0,4},{1,3},{2,3},{3,2},{4,0},
		},
		{ //SOUTH_WEST
			{-4,0},{-3,-2},{-3,0},{-3,1},{-3,2},{-2,2},{-2,3},{-1,3},{0,3},{0,4},{2,3},
		},
		{ //WEST
			{-4,0},{-3,-2},{-3,-1},{-3,1},{-3,2},{-2,-3},{-2,3},{0,-4},{0,4},
		},
		{ //NORTH_WEST
			{-4,0},{-3,-2},{-3,-1},{-3,0},{-3,2},{-2,-3},{-2,-2},{-1,-3},{0,-4},{0,-3},{2,-3},
		},
	};
	private static final int[][][] sensorRangeSCORCHER = new int[][][] { //SCORCHER
		{ //NORTH
			{-2,-2},{-1,-3},{0,-3},{1,-3},{2,-2},
		},
		{ //NORTH_EAST
			{-1,-3},{0,-3},{1,-3},{1,-2},{2,-2},{2,-1},{3,-1},{3,0},{3,1},
		},
		{ //EAST
			{2,-2},{2,2},{3,-1},{3,0},{3,1},
		},
		{ //SOUTH_EAST
			{-1,3},{0,3},{1,2},{1,3},{2,1},{2,2},{3,-1},{3,0},{3,1},
		},
		{ //SOUTH
			{-2,2},{-1,3},{0,3},{1,3},{2,2},
		},
		{ //SOUTH_WEST
			{-3,-1},{-3,0},{-3,1},{-2,1},{-2,2},{-1,2},{-1,3},{0,3},{1,3},
		},
		{ //WEST
			{-3,-1},{-3,0},{-3,1},{-2,-2},{-2,2},
		},
		{ //NORTH_WEST
			{-3,-1},{-3,0},{-3,1},{-2,-2},{-2,-1},{-1,-3},{-1,-2},{0,-3},{1,-3},
		},
	};
	private static final int[][][] sensorRangeTOWER = new int[][][] { //TOWER
		{ //NORTH
			
		},
		{ //NORTH_EAST
			
		},
		{ //EAST
			
		},
		{ //SOUTH_EAST
			
		},
		{ //SOUTH
			
		},
		{ //SOUTH_WEST
			
		},
		{ //WEST
			
		},
		{ //NORTH_WEST
			
		},
	};

}

