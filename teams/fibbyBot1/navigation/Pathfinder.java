package fibbyBot1.navigation;

public abstract class Pathfinder {
	static int[][] d = new int[][] {{-1,-1}, {0,-1}, {1,-1}, {1,0}, 
		{1,1}, {0,1}, {-1,1}, {-1,0}};
	
	/** Computes which move to make for the current turn.
	 * 
	 * @param map matrix of the world map containing only 0's and 1's, 
	 * 		where 0's represent unmovable squares
	 * 		and 1's represent movable squares
	 * @param sx current position x-coord
	 * @param sy current position y-coord
	 * @param tx destination x-coord
	 * @param ty destination y-coord
	 * @return an array of two integers representing the move to make next
	 */
	public abstract int[] computeMove(int[][] map, int sx, int sy, int tx, int ty);
	
	protected boolean isMovable(int[][] map, int x, int y) {
		return x>=0 && x<map.length && y>=0 && y<map[0].length && map[x][y]==1;
	}
}
