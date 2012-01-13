package ducks;

import ducks.Navigator;
import ducks.NavigatorUtilities;


public class TangentBug{
	final static int[][] d = new int[][] {{-1,-1}, {0,-1}, {1,-1}, {1,0}, 
		{1,1}, {0,1}, {-1,1}, {-1,0}};
	
	/** Scan this many times every turn to see if there is a wall in our way. */
	final static int MLINE_SCAN_STEPS_PER_TURN_TRACING = 3, MLINE_SCAN_STEPS_PER_TURN_NOT_TRACING = 5;
	/** Trace down the obstructing wall this many times in one turn. */
	final static int WALL_SCAN_STEPS_PER_TURN = 5;
	/** Look this many steps in each way to find the tangent in one turn. */
	final static int FIND_TANGENT_STEPS_PER_TURN = 10;
	
	/** We approximate that it will take an average of this*d squares to navigate a distance of d. */
	final static double HEURISTIC_WEIGHT_MAP_UGLINESS = 1.5;
	
	final static int BUFFER_START = 4096;
	final static int BUFFER_LENGTH = BUFFER_START*2;
	
	final boolean[][] map;
	final int xmax;
	final int ymax;
	
	int tx = -1;
	int ty = -1;
	
	
	// Wall variables - cleared every time we start tracing a new wall
	final int[][] buffer = new int[BUFFER_LENGTH][2];
	final int[][] wallCache; // fields are curWallCacheID*BUFFER_LENGTH+x, where x is a buffer position adjacent to the wall
	int leftWallDir = -1;
	int rightWallDir = -1; 
	int curWallCacheID = 1;
	int bufferLeft = BUFFER_START; //buffer side for clockwise tracing
	int bufferRight = BUFFER_START; //for counterclockwise tracing
	boolean tracing = false;
	int traceDirLastTurn = -1;
	boolean doneTracingClockwise = false;
	boolean doneTracingCounterclockwise = false;
	boolean traceDirLocked = false;
	int findTangentProgress = 0;
	
	// Preparatory variables - cleared every time robot moves
	boolean startedTracingDuringCurrentPrepCycle = false;
	boolean hitWallCache = false;
	int hitWallPos = -1;
	int firstWallHitX = -1;
	int firstWallHitY = -1;
	int firstWallHitDir = -1;
	int scanx = -1;
	int scany = -1;
	int[] bpos = new int[2];
	int[] bdir = new int[2];
	double[] heuristicValue = new double[2];
	boolean[] crossedMLine = new boolean[2];
	int[] lastdDir = new int[2];
	int[] bestdDir = new int[2];
	
	public TangentBug(boolean[][] map) {
		this.map = map;
		xmax = map.length;
		ymax = map[0].length;
		wallCache = new int[xmax][ymax];
		reset();
	}
	public void reset() {
		System.out.println("reset");
		tracing = false;
		curWallCacheID++;
	}
	public void setTarget(int tx, int ty) {
		if(this.tx==tx && this.ty==ty) return;
		reset();
		this.tx = tx; 
		this.ty = ty;
	}
	public void clearPreparatoryVariables() {
		startedTracingDuringCurrentPrepCycle = false;
		hitWallCache = false;
		hitWallPos = -1;
		firstWallHitX = -1;
		firstWallHitY = -1;
		firstWallHitDir = -1;
		scanx = -1;
		scany = -1;
		for(int traceDir=0; traceDir<=1; traceDir++) {
			crossedMLine[traceDir] = false;
			lastdDir[traceDir] = 1000;
			bestdDir[traceDir] = -1;
			bpos[traceDir] = BUFFER_START;
			bdir[traceDir] = -1;
			heuristicValue[traceDir] = (traceDirLocked && traceDir!=traceDirLastTurn) ? 1 : 0;
		}
		
	}
	public void prepare(int sx, int sy) {
		if(tracing) {
			if(!hitWallCache && !startedTracingDuringCurrentPrepCycle) {
				if(scanx==-1) {
					scanx = sx;
					scany = sy;
				}
				for(int step=0; step<MLINE_SCAN_STEPS_PER_TURN_TRACING; step++) {
					if(scanx==tx && scany==ty) break;
					
					// go towards dest until we hit a wall
					int dirTowards = NavigatorUtilities.getDirTowards(tx-scanx, ty-scany);
					scanx += d[dirTowards][0];
					scany += d[dirTowards][1];
					if(tracing && !hitWallCache && wallCache[scanx][scany]/BUFFER_LENGTH==curWallCacheID) {
						// we've hit our wall cache! we're still in trace mode
						hitWallCache = true;
						hitWallPos = wallCache[scanx][scany]%BUFFER_LENGTH;
					}
				}
			}
			
			//trace clockwise and cache 
			for(int n=0; n<WALL_SCAN_STEPS_PER_TURN && !doneTracingClockwise; n++) {
				traceClockwiseHelper();
			}
			
			//trace counterclockwise and cache 
			for(int n=0; n<WALL_SCAN_STEPS_PER_TURN && !doneTracingCounterclockwise; n++) {
				traceCounterclockwiseHelper();
				
			}
			
			int i;
			for(i=findTangentProgress; i<findTangentProgress+FIND_TANGENT_STEPS_PER_TURN; i++) {
				boolean flag = false;
				for(int traceDir = 0; traceDir<=1; traceDir++) {
					int pos = ((hitWallPos==-1)?BUFFER_START:hitWallPos) + ((traceDir==0) ? -i : i);
					if(pos<=bufferLeft || pos>=bufferRight) continue;
					flag = true;
					int cx = buffer[pos][0];
					int cy = buffer[pos][1];
					if(sx==cx && sy==cy) {
						continue;
					}
					if(traceDirLocked && traceDir!=traceDirLastTurn) continue;
					int dirStoT = (traceDir==0) ? NavigatorUtilities.getDirCounterclockwiseOf(tx-sx, ty-sy) : 
						NavigatorUtilities.getDirClockwiseOf(tx-sx, ty-sy);
					int dirStoC = (traceDir==0) ? NavigatorUtilities.getDirClockwiseOf(cx-sx, cy-sy) : 
						NavigatorUtilities.getDirCounterclockwiseOf(cx-sx, cy-sy);
					int dDir = (((traceDir==0)?(dirStoT-dirStoC):(dirStoC-dirStoT))+8) % 8;
					if(lastdDir[traceDir]>8) {
						crossedMLine[traceDir] = dDir>5;
					} else {
						if(dDir-lastdDir[traceDir]>4) crossedMLine[traceDir] = true;
						if(lastdDir[traceDir]-dDir>4) crossedMLine[traceDir] = false;
					}
					System.out.println("  "+pos+" "+buffer[pos][0]+","+buffer[pos][1]+" "+dirStoC+" "+crossedMLine[traceDir]);
					if(!crossedMLine[traceDir] && dDir>bestdDir[traceDir]) {
						bestdDir[traceDir] = dDir;
						bpos[traceDir] = pos;
						bdir[traceDir] = dirStoC;
					}
					if(!traceDirLocked && !crossedMLine[traceDir]) {
						heuristicValue[traceDir] = Math.max(heuristicValue[traceDir],
								Math.sqrt((sx-cx)*(sx-cx)+(sy-cy)*(sy-cy))+
								Math.sqrt((tx-cx)*(tx-cx)+(ty-cy)*(ty-cy))*HEURISTIC_WEIGHT_MAP_UGLINESS);
					}
					lastdDir[traceDir] = dDir;
				}
				if(!flag) { i--; break; }
			}
			findTangentProgress = i;
		} else {
			if(scanx==-1) {
				scanx = sx;
				scany = sy;
			}
			for(int step=0; step<MLINE_SCAN_STEPS_PER_TURN_NOT_TRACING; step++) {
				if(scanx==tx && scany==ty) break;
				
				// go towards dest until we hit a wall
				int dirTowards = NavigatorUtilities.getDirTowards(tx-scanx, ty-scany);
				scanx += d[dirTowards][0];
				scany += d[dirTowards][1];
				
				if(firstWallHitX==-1 && map[scanx][scany]) {
					// we've hit a wall! start tracing
					startTraceHelper(scanx - d[dirTowards][0], 
							scany - d[dirTowards][1], 
							dirTowards);
					startedTracingDuringCurrentPrepCycle = true;
					break;
				}
			}
		}
	}
	
	public int[] computeMove(int sx, int sy) {
		if(Math.abs(sx-tx)<=1 && Math.abs(sy-ty)<=1) return new int[] {tx-sx, ty-sy};
		
//		 if(tracing) for(int y=0; y<ymax; y++) { for(int x=0; x<xmax; x++) System.out.print((wallCache[x][y]/BUFFER_LENGTH==curWallCacheID)?'#':'.'); System.out.println(); }
//		 if(tracing) for(int i=BUFFER_START-30; i<=BUFFER_START+30; i++) System.out.println("  "+i+" "+buffer[i][0]+","+buffer[i][1]);
		
		if(!tracing) {
			int[] ret = d[NavigatorUtilities.getDirTowards(tx-sx, ty-sy)];
			return map[sx+ret[0]][sy+ret[1]] ? new int[] {0,0} : ret;
		} else if(!hitWallCache && !startedTracingDuringCurrentPrepCycle) {
			System.out.println("didn't hit my wall cache");
			reset();
			int[] ret = d[NavigatorUtilities.getDirTowards(tx-sx, ty-sy)];
			return map[sx+ret[0]][sy+ret[1]] ? new int[] {0,0} : ret;
		}
		
		System.out.println("tracedirlocked: "+traceDirLocked);
		System.out.println("hitWallPos: "+hitWallPos);
		
		//find better direction by taking smaller heuristic value
		int bestTraceDir =  heuristicValue[0]<heuristicValue[1]?0:1;
		traceDirLastTurn = bestTraceDir;
		int finalDir = bdir[bestTraceDir];
		
		System.out.println(" currently at: "+sx+","+sy);
		System.out.println(" finalDir: "+finalDir);
		if(finalDir==-1) {
			// this happens when there were no valid tangent points found
			System.out.println("final dir ended up being -1");
			reset();
			return new int[] {0,0};
		} else while(true) {
			
			//TODO Replace this hack to get around obstacles with a directional bug system. 
			// (This will prevent getting stuck in cases where an intermediate wall blocks you from the waypoint.)
			int x = sx+d[finalDir][0];
			int y = sy+d[finalDir][1];
			if(map[x][y]) {
				finalDir = (finalDir+(traceDirLastTurn==0?1:-1)+8)%8;
			} else {
				break;
			}
		}
		
		if(doneTracingClockwise && doneTracingCounterclockwise) 
			traceDirLocked = true;
		return d[finalDir];
		
	}
	
	private void startTraceHelper(int x, int y, int dir) {
		tracing = true;
		curWallCacheID++;
		buffer[BUFFER_START][0] = x;
		buffer[BUFFER_START][1] = y;
		bufferLeft = BUFFER_START-1;
		bufferRight = BUFFER_START+1;
		leftWallDir = dir;
		rightWallDir = dir;
		traceDirLastTurn = -1;
		doneTracingClockwise = false;
		doneTracingCounterclockwise = false;
		traceDirLocked = false;
		findTangentProgress = 0;
	}
	
	private void traceClockwiseHelper() {
		if(bufferLeft+1!=BUFFER_START && 
				buffer[bufferLeft+1][0]==buffer[BUFFER_START][0] && 
				buffer[bufferLeft+1][1]==buffer[BUFFER_START][1]) {
			doneTracingClockwise = true;
			return;
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
				doneTracingClockwise = true;
				return;
			}
		}
		for(int wx=-1, wy=-1, ti=0; ti<d.length; ti++) {
			int i = (-1*ti + leftWallDir + d.length) % d.length;
			int x = buffer[bufferLeft+1][0]+d[i][0];
			int y = buffer[bufferLeft+1][1]+d[i][1];
			if(map[x][y]) {
				wx = x; 
				wy = y;
				if(wallCache[wx][wy]/BUFFER_LENGTH!=curWallCacheID)
					wallCache[wx][wy] = curWallCacheID*BUFFER_LENGTH+bufferLeft+1;
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
	
	private void traceCounterclockwiseHelper() {
		if(bufferRight-1!=BUFFER_START && 
				buffer[bufferRight-1][0]==buffer[BUFFER_START][0] && 
				buffer[bufferRight-1][1]==buffer[BUFFER_START][1]) {
			doneTracingCounterclockwise = true;
			return;
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
				doneTracingCounterclockwise = true;
				return;
			}
		}
		for(int wx=-1, wy=-1, ti=0; ti<d.length; ti++) {
			int i = (1*ti + rightWallDir + d.length) % d.length;
			int x = buffer[bufferRight-1][0]+d[i][0];
			int y = buffer[bufferRight-1][1]+d[i][1];
			if(map[x][y]) {
				wx = x; 
				wy = y;
				if(wallCache[wx][wy]/BUFFER_LENGTH!=curWallCacheID)
					wallCache[wx][wy] = curWallCacheID*BUFFER_LENGTH+bufferRight-1;
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
}
