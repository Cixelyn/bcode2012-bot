package ducks;


public class TangentBug{
	static int[][] d = new int[][] {{-1,-1}, {0,-1}, {1,-1}, {1,0}, 
		{1,1}, {0,1}, {-1,1}, {-1,0}};
	
	/** Scan this many times every turn to see if there is a wall in our way. */
	final int INITIAL_SCAN_RANGE = 20;
	/** Trace down the obstructing wall this many times in one turn. */
	final int WALL_SCAN_STEPS_PER_TURN = 15;
	/** After guessing which way to trace, go the opposite way with this probability. */
	final double CHANCE_OF_GOING_SLOW_WAY = 0.2;
	/** We approximate that it will take an average of this*d squares to navigate a distance of d. */
	final double HEURISTIC_WEIGHT_MAP_UGLINESS = 1.5;
	/** The direction that we traced last turn.
	 * This should be reset if something happens, 
	 * like we get new map information or the destination changes.
	 */
	int traceDirectionLastTurn = -1;
	
	int oldtx = -1;
	int oldty = -1;
	
	boolean tracing = false;
	final int BUFFER_LENGTH = 4096;
	final int BUFFER_START = BUFFER_LENGTH;
	int bufferLeft = BUFFER_START; //buffer side for clockwise tracing
	int bufferRight = BUFFER_START; //for counterclockwise tracing
	int bufferTracePos = BUFFER_START;
	int leftWallDir = -1;
	int rightWallDir = -1; 
	int[][] buffer = new int[BUFFER_LENGTH*2][2];
	boolean[][] wallCache = null;
	
	public TangentBug() {
		reset();
	}
	public void reset() {
		tracing = false;
	}
	public int[] computeMove(boolean[][] map, int sx, int sy, int tx, int ty) {
		int xmax = map.length;
		int ymax = map[0].length;
		if(Math.abs(sx-tx)<=1 && Math.abs(sy-ty)<=1) return new int[] {tx-sx, ty-sy};
		
		if(tx!=oldtx || ty!=oldty) reset();
		oldtx = tx;
		oldty = ty;
		
		int[] firstMove = null;
		boolean hitWallCache = false;
		
		int firstWallHitX = -1;
		int firstWallHitY = -1;
		int firstWallHitDir = -1;
		
		int cx = sx; // current x,y position of virtual trace
		int cy = sy;
		
		// if(tracing) for(int y=0; y<ymax; y++) { for(int x=0; x<xmax; x++) //System.out.print(wallCache[x][y]?'#':'.'); //System.out.println(); }
		
		for(int step=0; step<INITIAL_SCAN_RANGE; step++) {
			if(cx==tx && cy==ty) break;
			
			// go towards dest until we hit a wall
			int dirTowards = getDirTowards(cx, cy, tx, ty);
			int x = cx+d[dirTowards][0];
			int y = cy+d[dirTowards][1];
			if(tracing && !hitWallCache && wallCache[x][y]) {
				// we've hit our wall cache! we're still in trace mode
				//System.out.println("  hit wall cache at: "+x+","+y);
				hitWallCache = true;
			}
			
			if(!map[x][y]) {
				cx += d[dirTowards][0];
				cy += d[dirTowards][1];
				if(firstMove==null) 
					firstMove = d[dirTowards];
			} else {
				if(firstWallHitX==-1) {
					// we've hit a wall! store it
					firstWallHitX = cx;
					firstWallHitY = cy;
					firstWallHitDir = dirTowards;
					break;
				}
			}
		}
		if(!hitWallCache) {
			// we didn't manage to hit our wall cache. leave tracing mode with some probability
			if(tracing) //System.out.println("LEAVING CONDITION DETECTED");
			if(Math.random()<0.3)
				tracing = false;
		}
		if(!tracing) {
			if(firstWallHitX==-1) {
				return (firstMove==null) ? (new int[] {0,0}) : firstMove;
			} else {
				tracing = true;
				wallCache = new boolean[xmax][ymax];
				buffer[BUFFER_START][0] = firstWallHitX;
				buffer[BUFFER_START][1] = firstWallHitY;
				bufferLeft = BUFFER_START-1;
				bufferRight = BUFFER_START+1;
				bufferTracePos = BUFFER_START;
				leftWallDir = firstWallHitDir;
				rightWallDir = firstWallHitDir;
				traceDirectionLastTurn = -1;
			}
		}
		
		
		
		
		//trace clockwise and cache 
		for(int n=0; n<WALL_SCAN_STEPS_PER_TURN; n++) {
			if(bufferLeft+1!=BUFFER_START && 
					buffer[bufferLeft+1][0]==buffer[BUFFER_START][0] && 
					buffer[bufferLeft+1][1]==buffer[BUFFER_START][1]) {
				break;
			}
			if(bufferLeft<BUFFER_START-2) {
				int Ax = buffer[BUFFER_START][0];
				int Ay = buffer[BUFFER_START][1];
				int Bx = tx;
				int By = ty;
				int Cx = buffer[bufferLeft+1][0];
				int Cy = buffer[bufferLeft+1][1];
				int Dx = buffer[bufferLeft+2][0];
				int Dy = buffer[bufferLeft+2][1];
				int distSquaredAtoB = (Ax-Bx)*(Ax-Bx)+(Ay-By)*(Ay-By);
				int distSquaredBtoC = (Bx-Cx)*(Bx-Cx)+(By-Cy)*(By-Cy);
				int ABcrossAC = (Ax-Bx)*(Ay-Cy)-(Ax-Cx)*(Ay-By);
				int ABcrossAD = (Ax-Bx)*(Ay-Dy)-(Ax-Dx)*(Ay-By);
				if(distSquaredBtoC < distSquaredAtoB && ABcrossAC*ABcrossAD<=0) {
					break;
				}
			}
			for(int wx=-1, wy=-1, ti=0; ti<d.length; ti++) {
				int i = (-1*ti + leftWallDir + d.length) % d.length;
				int x = buffer[bufferLeft+1][0]+d[i][0];
				int y = buffer[bufferLeft+1][1]+d[i][1];
				if(map[x][y]) {
					wx = x; 
					wy = y;
					wallCache[wx][wy] = true;
				} else {
					buffer[bufferLeft][0] = x;
					buffer[bufferLeft][1] = y;
					bufferLeft--;
					for(int j=0; j<d.length; j++) {
						if(x+d[j][0]==wx && y+d[j][1]==wy) {
							leftWallDir = j;
							break;
						}	
					}
					break;
				}
			}
		}
		
		//trace counterclockwise and cache 
		for(int n=0; n<WALL_SCAN_STEPS_PER_TURN; n++) {
			if(bufferRight-1!=BUFFER_START && 
					buffer[bufferRight-1][0]==buffer[BUFFER_START][0] && 
					buffer[bufferRight-1][1]==buffer[BUFFER_START][1]) {
				break;
			}
			if(bufferRight>BUFFER_START+2) {
				int Ax = buffer[BUFFER_START][0];
				int Ay = buffer[BUFFER_START][1];
				int Bx = tx;
				int By = ty;
				int Cx = buffer[bufferRight-1][0];
				int Cy = buffer[bufferRight-1][1];
				int Dx = buffer[bufferRight-2][0];
				int Dy = buffer[bufferRight-2][1];
				int distSquaredAtoB = (Ax-Bx)*(Ax-Bx)+(Ay-By)*(Ay-By);
				int distSquaredBtoC = (Bx-Cx)*(Bx-Cx)+(By-Cy)*(By-Cy);
				int ABcrossAC = (Ax-Bx)*(Ay-Cy)-(Ax-Cx)*(Ay-By);
				int ABcrossAD = (Ax-Bx)*(Ay-Dy)-(Ax-Dx)*(Ay-By);
				if(distSquaredBtoC < distSquaredAtoB && ABcrossAC*ABcrossAD<=0) {
					break;
				}
			}
			for(int wx=-1, wy=-1, ti=0; ti<d.length; ti++) {
				int i = (1*ti + rightWallDir + d.length) % d.length;
				int x = buffer[bufferRight-1][0]+d[i][0];
				int y = buffer[bufferRight-1][1]+d[i][1];
				if(map[x][y]) {
					wx = x; 
					wy = y;
					wallCache[wx][wy] = true;
				} else {
					buffer[bufferRight][0] = x;
					buffer[bufferRight][1] = y;
					bufferRight++;
					for(int j=0; j<d.length; j++) {
						if(x+d[j][0]==wx && y+d[j][1]==wy) {
							rightWallDir = j;
							break;
						}	
					}
					break;
				}
			}
		}
		
		//for(int i=BUFFER_START-30; i<=BUFFER_START+30; i++) //System.out.println("  "+i+" "+buffer[i][0]+","+buffer[i][1]);
		
		int finalDir = -1;
		if(traceDirectionLastTurn==-1) {
			int[] bpos = new int[2];
			int[] bdir = new int[2];
			double[] heuristicValue = new double[2];
			for(int traceDir = 0; traceDir<=1; traceDir++) {
				bpos[traceDir] = BUFFER_START;
				bdir[traceDir] = -1;
				heuristicValue[traceDir] = 0;
				boolean crossedMLine = false;
				int dirStoT = (traceDir==0) ? getDirCounterclockwiseOf(sx, sy, tx, ty) : 
					getDirClockwiseOf(sx, sy, tx, ty);
				int lastdDir = 1000;
				int bestdDir = -1;
				for(int pos=BUFFER_START; 
						pos>bufferLeft && pos<bufferRight; 
						pos+=(traceDir==0) ? -1 : 1) {
					
					cx = buffer[pos][0];
					cy = buffer[pos][1];
					if(sx==cx && sy==cy) {
						continue;
					}
					int dirStoC = (traceDir==0) ? getDirClockwiseOf(sx, sy, cx, cy) : 
						getDirCounterclockwiseOf(sx, sy, cx, cy);
					int dDir = (((traceDir==0)?(dirStoT-dirStoC):(dirStoC-dirStoT))+8) % 8;
					if(lastdDir>8) {
						crossedMLine = dDir>5;
					} else {
						if(dDir-lastdDir>4) crossedMLine = true;
						if(lastdDir-dDir>4) crossedMLine = false;
					}
					//System.out.println("  "+pos+" "+buffer[pos][0]+","+buffer[pos][1]+" "+dirStoC+" "+crossedMLine);
					if(!crossedMLine && dDir>bestdDir) {
						bestdDir = dDir;
						bpos[traceDir] = pos;
						bdir[traceDir] = dirStoC;
					}
					if(!crossedMLine) {
						heuristicValue[traceDir] = Math.max(heuristicValue[traceDir],
								Math.sqrt((sx-cx)*(sx-cx)+(sy-cy)*(sy-cy))+
								Math.sqrt((tx-cx)*(tx-cx)+(ty-cy)*(ty-cy))*HEURISTIC_WEIGHT_MAP_UGLINESS);
					}
					lastdDir = dDir;
				}
				//System.out.println(" heuristic "+traceDir+": "+heuristicValue[traceDir]);
			}
			//find better direction by taking smaller heuristic value
			int bestTraceDir =  heuristicValue[0]<heuristicValue[1]?0:1;
			//with small probability, go the wrong way
			if(Math.random()<CHANCE_OF_GOING_SLOW_WAY) bestTraceDir = 1-bestTraceDir;
			bufferTracePos = bpos[bestTraceDir];
			traceDirectionLastTurn = bestTraceDir;
			finalDir = bdir[bestTraceDir];
		} else {
			int bpos = bufferTracePos;
			int bdir = -1;
			int traceDir = traceDirectionLastTurn;
			boolean crossedMLine = false;
			int dirStoT = (traceDir==0) ? getDirCounterclockwiseOf(sx, sy, tx, ty) : 
				getDirClockwiseOf(sx, sy, tx, ty);
			int lastdDir = 1000;
			int bestdDir = -1;
			for(int pos=bufferTracePos; 
					pos>bufferLeft && pos<bufferRight; 
					pos+=(traceDir==0) ? -1 : 1) {
				
				cx = buffer[pos][0];
				cy = buffer[pos][1];
				if(sx==cx && sy==cy) {
					continue;
				}
				int dirStoC = (traceDir==0) ? getDirClockwiseOf(sx, sy, cx, cy) : 
					getDirCounterclockwiseOf(sx, sy, cx, cy);
				int dDir = (((traceDir==0)?(dirStoT-dirStoC):(dirStoC-dirStoT))+8) % 8;
				if(lastdDir>8) {
					crossedMLine = dDir>5;
				} else {
					if(dDir-lastdDir>4) crossedMLine = true;
					if(lastdDir-dDir>4) crossedMLine = false;
				}
				//System.out.println("  "+pos+" "+buffer[pos][0]+","+buffer[pos][1]+" "+dirStoC+" "+crossedMLine);
				if(!crossedMLine && dDir>bestdDir) {
					bestdDir = dDir;
					bpos = pos;
					bdir = dirStoC;
				}
				lastdDir = dDir;
			}
			
			bufferTracePos = bpos;
			finalDir = bdir;
		}
		
		//System.out.println(" currently at: "+sx+","+sy);
		//System.out.println(" finalDir: "+finalDir);
		if(finalDir==-1) {
			tracing = false;
			return new int[] {0,0};
		} else while(true) {
			
			//TODO Replace this hack to get around obstacles with a directional bug system. 
			// (This will prevent getting stuck in cases where an intermediate wall blocks you from the waypoint.)
			int x = sx+d[finalDir][0];
			int y = sy+d[finalDir][1];
			if(map[x][y]) {
				finalDir = (finalDir+(traceDirectionLastTurn==0?1:-1)+8)%8;
			} else {
				break;
			}
		}
		
		return d[finalDir];
		
	}
		
	private int getDirClockwiseOf(int sx, int sy, int tx, int ty) {
		if(tx==sx) {
			if(ty>sy) return 5;
			else return 1;
		}
		double slope = (ty-sy)/(tx-sx);
		if(tx>sx) {
			if(slope>=1) return 4;
			else if(slope>=0) return 3;
			else if(slope>=-1) return 2;
			else return 1;
		} else {
			if(slope>=1) return 0;
			else if(slope>=0) return 7;
			else if(slope>=-1) return 6;
			else return 5;
		}
	}
	private int getDirCounterclockwiseOf(int sx, int sy, int tx, int ty) {
		if(tx==sx) {
			if(ty>sy) return 5;
			else return 1;
		}
		double slope = (ty-sy)/(tx-sx);
		if(tx>sx) {
			if(slope>1) return 5;
			else if(slope>0) return 4;
			else if(slope>-1) return 3;
			else return 2;
		} else {
			if(slope>1) return 1;
			else if(slope>0) return 0;
			else if(slope>-1) return 7;
			else return 6; 
		}
	}
	private int getDirTowards(int sx, int sy, int tx, int ty) {
		if(tx==sx) {
			if(ty>sy) return 5;
			else return 1;
		}
		double slope = (ty-sy)/(tx-sx);
		if(tx>sx) {
			if(slope>2.414) return 5;
			else if(slope>0.414) return 4;
			else if(slope>-0.414) return 3;
			else if(slope>-2.414) return 2;
			else return 1;
		} else {
			if(slope>2.414) return 1;
			else if(slope>0.414) return 0;
			else if(slope>-0.414) return 7;
			else if(slope>-2.414) return 6;
			else return 5;
		}
	}
	
	private int[] getDirsTowards(int sx, int sy, int tx, int ty) {
		if(tx==sx) {
			if(ty>sy) return new int[] {5};
			else return new int[] {1};
		}
		double slope = ((double)(ty-sy))/(tx-sx);
		if(tx>sx) {
			if(slope>2.414) return new int[] {5,4};
			else if(slope>1) return new int[] {4,5};
			else if(slope>0.414) return new int[] {4,3};
			else if(slope>0) return new int[] {3,4};
			else if(slope>-0.414) return new int[] {3,2};
			else if(slope>-1) return new int[] {2,3};
			else if(slope>-2.414) return new int[] {2,1};
			else return new int[] {1,2};
		} else {
			if(slope>2.414) return new int[] {1,0};
			else if(slope>1) return new int[] {0,1};
			else if(slope>0.414) return new int[] {0,7};
			else if(slope>0) return new int[] {7,0};
			else if(slope>-0.414) return new int[] {7,6};
			else if(slope>-1) return new int[] {6,7};
			else if(slope>-2.414) return new int[] {6,5};
			else return new int[] {5,6};
		}
	}
	
}
