package tangentbugplayer;

import battlecode.common.*;

public class RobotPlayer {
	static int startX=0, startY=0;
    public static void run(RobotController myRC) {
    	boolean[][] isWall;
    	boolean[][] sensed;
    	isWall = new boolean[256][256];
    	sensed = new boolean[256][256];
		for(int a=0; a<256; a++) for(int b=0; b<256; b++) {
			isWall[a][b] = a==0||a==255||b==0||b==255;
			sensed[a][b] = false;
		}
		RobotType myType = myRC.getType();
		int senseDist = myType==RobotType.ARCHON?5:myType==RobotType.SCOUT?4:1;
		TangentBug pathfinder = new TangentBug();
		startX = myRC.getLocation().x;
		startY = myRC.getLocation().y;
		while (true) {
        	try {
        		if(Clock.getRoundNum()%100==33) pathfinder.reset();
                MapLocation myLoc = myRC.getLocation();
                int myX = myLoc.x;
                int myY = myLoc.y;
                Direction myDir = myRC.getDirection();
                MapLocation locInFront = myLoc.add(myDir);
                double myFlux = myRC.getFlux();
                Team myTeam = myRC.getTeam();
        		for(int dx=-senseDist; dx<=senseDist; dx++) for(int dy=-senseDist; dy<=senseDist; dy++) {
        			if(sensed[fx(myX+dx)][fy(myY+dy)]) continue;
        			MapLocation loc = myLoc.add(dx, dy);
        			TerrainTile tt = myRC.senseTerrainTile(loc);
        			if(tt!=null) {
        				isWall[fx(loc.x)][fy(loc.y)] = myRC.senseTerrainTile(loc)!=TerrainTile.LAND;
        				sensed[fx(loc.x)][fy(loc.y)] = true;
        			}
        		}
        		MapLocation[] locs = myRC.senseCapturablePowerNodes();
        		int targetX = locs[0].x;
        		int targetY = locs[0].y;
        		if(!myRC.isMovementActive()) {
	        		int[] d = pathfinder.computeMove(isWall, fx(myX), fy(myY), fx(targetX), fy(targetY));
	        		Direction dir = myLoc.directionTo(myLoc.add(d[0], d[1]));
	        		if(dir!=Direction.OMNI && !myRC.canMove(dir) && Math.random()<0.5) {
	        			if(Math.random()<0.5)
	        				dir = dir.rotateLeft();
	        			else
	        				dir = dir.rotateRight();
	        		}
	        		if(dir!=Direction.OMNI && myRC.canMove(dir)) {
		        		myRC.setDirection(dir);
		        		myRC.yield();
		        		GameObject obj = myRC.senseObjectAtLocation(myLoc.add(myRC.getDirection()), RobotLevel.POWER_NODE);
		        		if(obj!=null && myRC.senseConnected((PowerNode)obj)) {
		        			if(myRC.getFlux()>200 && myRC.canMove(myRC.getDirection())) myRC.spawn(RobotType.TOWER);
		        		} else if(myRC.canMove(myRC.getDirection()))
		        			myRC.moveForward();
	        		}
        		}
        		myRC.yield();
            } catch (Exception e) {
                System.out.println("caught exception:");
                e.printStackTrace();
            }
        }
    }
    public static int fx(int x) {
    	return (x-startX+128)%256;
    }
    public static int fy(int y) {
    	return (y-startY+128)%256;
    }
}
