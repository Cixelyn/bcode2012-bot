package shittyplayer;

import battlecode.common.*;

public class RobotPlayer {
    public static void run(RobotController myRC) {
    	boolean[][] isWall;
    	boolean[][] sensed;
    	isWall = new boolean[100][100];
    	sensed = new boolean[100][100];
		for(int a=0; a<100; a++) for(int b=0; b<100; b++) {
			isWall[a][b] = false;
			sensed[a][b] = false;
		}
		RobotType myType = myRC.getType();
		int senseDist = myType==RobotType.ARCHON?5:myType==RobotType.SCOUT?4:1;
		MapLocation parentArchonLoc = null;
        while (true) {
        	try {
                MapLocation myLoc = myRC.getLocation();
                int myX = myLoc.x;
                int myY = myLoc.y;
                Direction myDir = myRC.getDirection();
                MapLocation locInFront = myLoc.add(myDir);
                double myFlux = myRC.getFlux();
                Team myTeam = myRC.getTeam();
        		for(int dx=-senseDist; dx<=senseDist; dx++) for(int dy=-senseDist; dy<=senseDist; dy++) {
        			if(sensed[(myX+dx)%100][(myY+dy)%100]) continue;
        			MapLocation loc = myLoc.add(dx, dy);
        			TerrainTile tt = myRC.senseTerrainTile(loc);
        			if(tt!=null) {
        				isWall[loc.x%100][loc.y%100] = myRC.senseTerrainTile(loc)!=TerrainTile.LAND;
        				sensed[loc.x%100][loc.y%100] = true;
        			}
        		}
        		
        		
        		if(myType==RobotType.ARCHON) {
        			if(myFlux>100) {
        				GameObject obj = myRC.senseObjectAtLocation(locInFront, RobotLevel.ON_GROUND);
        				if(obj!=null && obj.getTeam()==myTeam && obj instanceof Robot) {
        					Robot robot = (Robot)obj;
        					
            				double flux = myRC.senseRobotInfo(robot).flux;
        					double transfer = Math.min(30, 50-flux);
        					if(transfer>0)
        						myRC.transferFlux(locInFront, RobotLevel.ON_GROUND, transfer);
        				}
            		}
        			
        			int numEnemies = 0;
        			MapLocation[] enemyLocs = null;
        			if(myFlux>1) {
    	        		Robot[] robots = myRC.senseNearbyGameObjects(Robot.class);
    	        		for(Robot robot : robots) {
    	        			if(robot.getTeam()!=myTeam)
    	        				numEnemies++;
    	        		}
    	        		if(numEnemies>0) {
    	        			double[] hp = new double[robots.length];
    	        			enemyLocs = new MapLocation[numEnemies];
    	        			for(int i=0; i<robots.length; i++) {
    	        				hp[i] = robots[i].getTeam()==myTeam ? 1000 : myRC.senseRobotInfo(robots[i]).energon;
    	        			}
    	        			for(int e=0; e<numEnemies; e++) {
    	        				int besti = 0;
    	        				double lowestHP = 999;
	    	        			for(int i=0; i<robots.length; i++) {
	    	        				if(hp[i]<lowestHP) {
	    	        					lowestHP = hp[i];
	    	        					besti = i;
	    	        				}
	    	        			}
	    	        			enemyLocs[e] = myRC.senseRobotInfo(robots[besti]).location;
	    	        			hp[besti] = 1000;
    	        			}
    	        			int[] keys = new int[4];
    	        			keys[0] = Clock.getRoundNum();
    	        			keys[1] = 55555;
    	        			keys[2] = myX;
    	        			keys[3] = myY;
        	        		Message message = new Message();
        	        		message.ints = keys;
        	        		message.locations = enemyLocs;
        	        		myRC.setIndicatorString(0, ""+numEnemies);
    		        		myRC.broadcast(message);
    	        		}
            		}
        			
            		if(!myRC.isMovementActive()) {
            			if(myRC.getType()==RobotType.ARCHON && 
                				myRC.getFlux()>200 && 
                				myRC.senseObjectAtLocation(myLoc.add(myRC.getDirection()), RobotLevel.ON_GROUND)==null &&
                				myRC.senseObjectAtLocation(myLoc.add(myRC.getDirection()), RobotLevel.POWER_NODE)!=null) {
                			PowerNode node = (PowerNode)myRC.senseObjectAtLocation(myLoc.add(myRC.getDirection()), RobotLevel.POWER_NODE);
                			if(myRC.senseConnected(node) && !myRC.senseOwned(node))
                				myRC.spawn(RobotType.TOWER);
                		} else if(numEnemies>0 && 
                				myRC.getFlux()>220 &&
        						myRC.canMove(myRC.getDirection())) {
                			myRC.spawn(RobotType.SOLDIER);
            			} else if(Math.random()<0.5 &&
        						myRC.canMove(myRC.getDirection())) {
        					myRC.moveForward();
            			} else {
            				if(Math.random()<0.33) {
            					if(numEnemies==0) {
	            					MapLocation loc = null;
	            					MapLocation[] nodes = myRC.senseCapturablePowerNodes();
	            					for(MapLocation node: nodes) {
	            						if(loc==null || node.distanceSquaredTo(myLoc)<loc.distanceSquaredTo(myLoc) )
	            							loc = node;
	            					}
	            					Direction dir = myLoc.directionTo(loc);
	            					if(dir!=Direction.NONE && dir!=Direction.OMNI)
	            						myRC.setDirection(dir);
            					} else {
            						MapLocation closestEnemy = null;
                					int dist = Integer.MAX_VALUE;
                					for(int i=0; i<enemyLocs.length; i++) {
                    					if(myLoc.distanceSquaredTo(enemyLocs[i]) < dist) {
                    						dist = myLoc.distanceSquaredTo(enemyLocs[i]);
                    						closestEnemy = enemyLocs[i];
                    					}
                    				}
                					Direction dir = myLoc.directionTo(closestEnemy).opposite();
                					if(dir!=myDir) {
                						myRC.setDirection(dir);
                					} else if(myRC.canMove(myDir)) {
                						myRC.moveForward();
                					}
            					}
            				} else if(Math.random()<0.5) {
            					myRC.setDirection(myRC.getDirection().rotateLeft());
            				} else {
            					myRC.setDirection(myRC.getDirection().rotateRight());
            				}
            			}
            		}
        		} else if(myType == RobotType.SOLDIER) {
        			parentArchonLoc = null;
        			int lowestDistance = Integer.MAX_VALUE;
        			MapLocation[] archonLocs = myRC.senseAlliedArchons();
        			for(MapLocation loc: archonLocs) {
        				if(parentArchonLoc==null || loc.distanceSquaredTo(myLoc)<lowestDistance) {
        					lowestDistance = loc.distanceSquaredTo(myLoc);
        					parentArchonLoc = loc;
        				}
        			}
        			lowestDistance = Integer.MAX_VALUE;
        			MapLocation[] enemies = null;
        			if(myFlux>1) {
            			Message[] messages = myRC.getAllMessages();
            			for(Message message: messages) {
            				if(message.ints.length!=4 || message.ints[1]!=55555 || message.ints[0]<Clock.getRoundNum()-1)
            					break;
            				int senderDistance = new MapLocation(message.ints[2], message.ints[3]).distanceSquaredTo(myLoc);
            				if(senderDistance < lowestDistance) {
            					lowestDistance = senderDistance;
            					enemies = message.locations;
            				}
            			}
            		}
        			
        			if(enemies!=null && !myRC.isAttackActive()) {
        				for(int i=0; i<enemies.length; i++) {
        					if(!myRC.canAttackSquare(enemies[i])) continue;
        					GameObject groundObj = myRC.senseObjectAtLocation(enemies[i], RobotLevel.ON_GROUND);
        					GameObject airObj = myRC.senseObjectAtLocation(enemies[i], RobotLevel.ON_GROUND);
        					if(groundObj!=null && groundObj.getTeam()!=myTeam) {
        						myRC.attackSquare(enemies[i], RobotLevel.ON_GROUND);
        						break;
        					} else if(airObj!=null && airObj.getTeam()!=myTeam) {
        						myRC.attackSquare(enemies[i], RobotLevel.IN_AIR);
        						break;
        					}
        				}
        				
        			}
        			if(!myRC.isMovementActive()) {
        				if(enemies==null) {
        					Direction dir = myLoc.directionTo(parentArchonLoc);
        					if(dir!=Direction.NONE && dir!=Direction.OMNI && dir!=myDir) {
        						myRC.setDirection(dir);
        					} else if(myRC.canMove(myDir)) {
        						myRC.moveForward();
        					}
        				} else {
        					boolean enemyInAttackRadius = false;
        					for(int i=0; i<enemies.length; i++) {
            					if(myLoc.distanceSquaredTo(enemies[i]) < myType.attackRadiusMaxSquared) {
            						Direction dir = myLoc.directionTo(enemies[i]);
                					if(dir!=Direction.NONE && dir!=Direction.OMNI && dir!=myDir) {
                						myRC.setDirection(dir);
                					}
                					enemyInAttackRadius = true;
                					break;
            					}
            				}
            				if(!enemyInAttackRadius) {
            					MapLocation closestEnemy = null;
            					int dist = Integer.MAX_VALUE;
            					for(int i=0; i<enemies.length; i++) {
                					if(myLoc.distanceSquaredTo(enemies[i]) < dist) {
                						dist = myLoc.distanceSquaredTo(enemies[i]);
                						closestEnemy = enemies[i];
                					}
                				}
            					Direction dir = myLoc.directionTo(closestEnemy);
            					if(dir!=myDir) {
            						myRC.setDirection(dir);
            					} else if(myRC.canMove(myDir)) {
            						myRC.moveForward();
            					}
            				}
        				}
        			}
        		}
        		
        		
        		
                myRC.yield();
            } catch (Exception e) {
                System.out.println("caught exception:");
                e.printStackTrace();
            }
        }
    }
}
