package fibbyBot1.navigation;

public class Bug extends Pathfinder{
	
	
	/** -1 means not tracing, 0 means tracing wall on left, 1 means on right. */
	int tracing = -1;
	/** direction of the wall we're currently hugging. */
	int traceDirection = -1;
	/** x-coord of point we started tracing at. */
	int xTraceStart = -1;
	/** y-coord of point we started tracing at. */
	int yTraceStart = -1;
	/** x-coord of the destination as of last turn. */
	int xPrevDest = -1;
	/** y-coord of the destination as of last turn. */
	int yPrevDest = -1;
	/** number of turns that the bug has been tracing for. */
	int turnsTraced = 0;
	/** distance to destination we started tracing at. 
	 * Leave trace mode when current distance to destination is below this value. */
	double traceDistance = -1;
	/** the default direction to trace. Changes every time we trace too far. */
	int defaultTraceDirection = 0;
	/** trace threshold to reset to every time we get a new destination. */
	final int INITIAL_TRACE_THRESHOLD = 5;
	/** number of turns to trace before resetting. */
	int traceThreshold = -1;
	
	
	public Bug() {
		defaultTraceDirection = (int)(Math.random()+0.5);
		traceThreshold = INITIAL_TRACE_THRESHOLD;
	}
	
	@Override
	public int[] computeMove(int[][] map, int sx, int sy, int tx, int ty) {
		int xmax = map.length;
		int ymax = map[0].length;
		if(sx==tx && sy==ty) return new int[] {0, 0};
		
		double dist = (sx-tx)*(sx-tx)+(sy-ty)*(sy-ty);
		if(tx!=xPrevDest || ty!=yPrevDest) {
			tracing = -1;
			traceThreshold = INITIAL_TRACE_THRESHOLD;
		} 
		xPrevDest = tx;
		yPrevDest = ty;
		if(tracing!=-1) {
			turnsTraced++;
			if(dist<traceDistance) {
				tracing = -1;
			} else if(turnsTraced>=traceThreshold) {
				tracing = -1;
				traceThreshold *= 2;
				defaultTraceDirection = 1-defaultTraceDirection;
			} else {
				
			}
		}
		if(tracing==-1) {
			int best = -1;
			double bestValue = dist;
			for(int i=0; i<d.length; i++) {
				int x = sx+d[i][0];
				int y = sy+d[i][1];
				if(x<0 || y<0 || x>=xmax || y>=ymax || map[x][y]==0) continue;
				double value = (x-tx)*(x-tx)+(y-ty)*(y-ty);
				if(bestValue==-1 || value<bestValue) {
					best = i;
					bestValue = value;
				}
			}
			if(best!=-1) return d[best];
			else {
				tracing = defaultTraceDirection;
				traceDistance = dist;
				xTraceStart = sx;
				yTraceStart = sy;
				turnsTraced = 0;
				for(int i=0; i<d.length; i++) {
					int x = sx+d[i][0];
					int y = sy+d[i][1];
					if(x<0 || y<0 || x>=xmax || y>=ymax || map[x][y]==1) continue;
					double value = (x-tx)*(x-tx)+(y-ty)*(y-ty);
					if(value<bestValue) {
						traceDirection = i;
						break;
					}
				}
			}
		} 
		if(tracing==0 || tracing==1) {
			int wx=-1,wy=-1;
			for(int ti=0; ti<d.length; ti++) {
				int i = ((tracing==0?1:-1)*ti + traceDirection + d.length) % d.length;
				int x = sx+d[i][0];
				int y = sy+d[i][1];
				if(x<0 || y<0 || x>=xmax || y>=ymax || map[x][y]==0) {
					wx = x; wy = y;
				} else {
					for(int j=0; j<d.length; j++) {
						if(x+d[j][0]==wx && y+d[j][1]==wy) {
							traceDirection = j;
							break;
						}	
					}
					return d[i];
				}
			}
		}
		return new int[] {0, 0};
	}
}
