package firstdayplayer;

import battlecode.common.Direction;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.PowerNode;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class RobotPlayer {
    public static void run(RobotController myRC) {
    	int[][] map = new int[150][150];
		boolean[][] sensed = new boolean[150][150];
		for(int a=0; a<150; a++) for(int b=0; b<150; b++) {
			map[a][b] = 1;
			sensed[a][b] = false;
		}
		RobotType myType = myRC.getType();
		int senseDist = myType==RobotType.ARCHON?5:myType==RobotType.SCOUT?4:3;
		MapLocation target = null;
		Direction targetDir = null;
    	while (true) {
    		try {
        		MapLocation myLoc = myRC.getLocation();
        		if(target==null) target = myLoc.add(10, 40);
        		if(targetDir==null) targetDir = Direction.SOUTH;
                int myX = myLoc.x;
                int myY = myLoc.y;
        		for(int dx=-senseDist; dx<=senseDist; dx++) for(int dy=-senseDist; dy<=senseDist; dy++) {
        			if(sensed[(myX+dx)%150][(myY+dy)%150]) continue;
        			MapLocation loc = myLoc.add(dx, dy);
        			TerrainTile tt = myRC.senseTerrainTile(loc);
        			if(tt!=null) {
        				map[loc.x%150][loc.y%150] = myRC.senseTerrainTile(loc)==TerrainTile.LAND?1:0;
        				sensed[loc.x%150][loc.y%150] = true;
        			}
        		}
        		
        		if(myType==RobotType.ARCHON) {
        			if(target==null || Math.random()<0.00) {
        				target = null;
        				MapLocation[] nodes = myRC.senseCapturablePowerNodes();
    					for(MapLocation node: nodes) {
    						if(target==null || (node.distanceSquaredTo(myLoc)<target.distanceSquaredTo(myLoc) && Math.random()<0.8) )
    							target = node;
    					}
        			}
        			if(myRC.getType()==RobotType.ARCHON && 
            				myRC.getFlux()>30 && 
        					myRC.senseObjectAtLocation(myLoc.add(myRC.getDirection()), RobotLevel.ON_GROUND)!=null) {
        				GameObject obj = myRC.senseObjectAtLocation(myLoc.add(myRC.getDirection()), RobotLevel.ON_GROUND);
        				//System.out.println("TRANSFER!!");
        				if((obj instanceof Robot) && myRC.senseRobotInfo((Robot)obj).type==RobotType.SOLDIER) {
        					double flux = myRC.senseRobotInfo((Robot)obj).flux;
        					if(flux<20)
        						myRC.transferFlux(myLoc.add(myRC.getDirection()), RobotLevel.ON_GROUND, 30);
        				}
            			
            		}
        			if(!myRC.isMovementActive()) {
        				if(myRC.getFlux()>210 && 
                				myRC.senseObjectAtLocation(myLoc.add(myRC.getDirection()), RobotLevel.ON_GROUND)==null &&
                				myRC.senseObjectAtLocation(myLoc.add(myRC.getDirection()), RobotLevel.POWER_NODE)!=null) {
                			PowerNode node = (PowerNode)myRC.senseObjectAtLocation(myLoc.add(myRC.getDirection()), RobotLevel.POWER_NODE);
                			MapLocation[] capturable = myRC.senseCapturablePowerNodes();
                			boolean c = false;
                			for(MapLocation pn: capturable) {
                				if(pn.equals(node.getLocation()))
                					c = true;
                			}
                			if(c) {
                				myRC.spawn(RobotType.TOWER);
                				//target = null;
                			}
                		} else if(myRC.getFlux()>150 && 
                				myRC.senseTerrainTile(myLoc.add(myRC.getDirection()))==TerrainTile.LAND &&
                				myRC.senseObjectAtLocation(myLoc.add(myRC.getDirection()), RobotLevel.ON_GROUND)==null && 
                				Math.random()<0.5) {
                			myRC.spawn(RobotType.SOLDIER);
                		} else {
	        				Direction dir = myLoc.directionTo(target);
	        				
	        				if(myRC.canMove(dir))
	    						myRC.moveForward(); 
	        				else {
	        					if(Math.random()<0.5) {
		        					while(Math.random()<0.4) 
		        						dir = dir.rotateLeft();
		        				} else {
		        					while(Math.random()<0.4) 
		        						dir = dir.rotateRight();
		        				}
	        					myRC.setDirection(dir);
		    				}
                		}
            		}
        		} else if(myType==RobotType.SOLDIER) {
        			if(myRC.getFlux()>1) {
        				GameObject o = null;
        				GameObject[] objs = myRC.senseNearbyGameObjects(GameObject.class);
        				for(GameObject obj : objs) {
        					if(!obj.getTeam().equals(myRC.getTeam())) {
        						if(o==null || Math.random()<0.5) o = obj;
        					}
        				}
        			if(!myRC.isAttackActive()) {
        				
        				if(o!=null && o.getRobotLevel()!=RobotLevel.POWER_NODE && myRC.canAttackSquare(myRC.senseLocationOf(o)))
        					myRC.attackSquare(myRC.senseLocationOf(o), o.getRobotLevel());
        			}
        			if(targetDir==null || Math.random()<0.00) {
        				int dx = (int)(Math.random()*100-50);
        				int dy = (int)(Math.random()*100-50);
        				if(dx==0 && dy==0)dy++;
        				targetDir = myLoc.directionTo(myLoc.add(dx, dy));
        			}
        			if(o==null && !myRC.isMovementActive()) {
        				Direction dir = targetDir;
        				if(Math.random()<0.5) {
        					while(Math.random()<0.2) 
        						dir = dir.rotateLeft();
        				} else {
        					while(Math.random()<0.2) 
        						dir = dir.rotateRight();
        				}
        				if(dir!=myRC.getDirection()) {
	    					if(dir!=Direction.NONE && dir!=Direction.OMNI) 
	    						myRC.setDirection(dir);
	    				} else {
	    					if(myRC.canMove(myRC.getDirection()) &&
	    							myRC.senseObjectAtLocation(myLoc.add(myRC.getDirection()), RobotLevel.POWER_NODE)==null)
	    						myRC.moveForward();
	    				}
            		}
        		}}
        		
        		
                myRC.yield();
            } catch (Exception e) {
                System.out.println("caught exception:");
                e.printStackTrace();
            }
        }
    }
}
