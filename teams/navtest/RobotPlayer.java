package navtest;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.PowerNode;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class RobotPlayer {
	RobotController rc;
	

	public static void run(RobotController rc) {
		while (true) {
			try {
//				if (rc.getRobot().getID()!=3) rc.suicide(); // only use on puddles as team a
				Runnable r = null;
				switch (rc.getType()) {
				case ARCHON:
					r = new Archon(rc);
					break;
				case DISRUPTER:
					break;
				case SCORCHER:
					break;
				case SCOUT:
					break;
				case SOLDIER:
					break;
				case TOWER:
					break;
				}
				while (true) {
					try {
						r.run();
					} catch (Exception e) {
						System.out.println("caught exception:");
						e.printStackTrace();
					}
				}
			} catch (Exception e) {
				System.out.println("caught exception:");
				e.printStackTrace();
			}
		}
	}
}

class MovementController {
	RobotController rc;
	public MovementController (RobotController rc) throws GameActionException {
		this.rc = rc;
		init();
	}
	
	public void init() throws GameActionException
	{
		MapLocation ml = rc.getLocation();
		cx = qx = ml.x;
		cy = qy = ml.y;
		qx = GameConstants.MAP_MAX_WIDTH-qx;
		qy = GameConstants.MAP_MAX_HEIGHT-qy;
		cx = cx+qx;
		cy = cy+qy;
		scanSurroundings();
	}
	
	int cx,cy;
	MapLocation cl;
	int previoustx,previousty;
	boolean prepared = false;
	boolean lastCW = false;
	static final int MAXWAYPOINTS = 100;
	int[][] moves = new int[MAXWAYPOINTS][2];
	int moveindex = 0, totalmoves=0;
	static final int MAPH = GameConstants.MAP_MAX_HEIGHT*2;
	static final int MAPW = GameConstants.MAP_MAX_WIDTH*2;
	int[][] map = new int[MAPW][MAPH];
	boolean[][] scanned = new boolean[MAPW][MAPH];
	int qx,qy;
	
	public static final int[][][] scanlocs = new int[][][]{ // TODO -> do this later
		{
			{-6,0},{-5,0},{-5,-1},{-5,-2},{-5,-3},{-4,-3},{-4,-4,},{-3,-4},{-3,-5},{-2,-5},{-1,-5},{0,-5},{0,-6}
		},
		{
			{-6,0},{-5,0},{-5,-1},{-5,-2},{-5,-3},{-4,-3},{-4,-4,},{-3,-4},{-3,-5},{-2,-5},{-1,-5},{0,-5},{0,-6}
		},
		{
			{-6,0},{-5,0},{-5,-1},{-5,-2},{-5,-3},{-4,-3},{-4,-4,},{-3,-4},{-3,-5},{-2,-5},{-1,-5},{0,-5},{0,-6}
		},
		{
			{-6,0},{-5,0},{-5,-1},{-5,-2},{-5,-3},{-4,-3},{-4,-4,},{-3,-4},{-3,-5},{-2,-5},{-1,-5},{0,-5},{0,-6}
		},
	};
	
	void scanSurroundings() throws GameActionException
	{
		int bytecode = Clock.getBytecodeNum();
		int ix,iy, jx,jy;
		ix = cx-6;
		jx = cx+6;
		iy = cy-6;
		jy = cy+6;
		if (ix<0) ix = 0;
		if (jx>scanned.length) jx = scanned.length;
		if (iy<0) iy = 0;
		if (jy>scanned[0].length) jy = scanned[0].length;
		
		for (int x=ix; x<jx; x++)
		{
			for (int y=iy; y<jy; y++)
			{
				if (scanned[x][y]) continue;
				MapLocation ml = new MapLocation(x-qx,y-qy);
				if (rc.canSenseSquare(ml)) {
					if (rc.senseTerrainTile(ml)==TerrainTile.LAND)
						map[x][y] = 0;
					else map[x][y] = 1;
					scanned[x][y] = true;
				}
			}
		}
		bytecode = Clock.getBytecodeNum()-bytecode;
		rc.setIndicatorString(0, "scan used "+bytecode);
	}
	
	public void move(MapLocation ml) throws GameActionException
	{
		cl = rc.getLocation();
		int ncx = cl.x+qx;
		int ncy = cl.y+qy;
		if (cx!=ncx || cy!=ncy)
		{
			scanSurroundings();
		}
		cx = ncx;
		cy = ncy;
		
		int tx,ty;
		tx = ml.x+qx;
		ty = ml.y+qy;
		
		if (previoustx!=tx || previousty!=ty) {
			prepared = false;
		}
		
		int[] result = null;
		while (result==null)
		{
			if (!prepared) {
				if (lastCW)
				{
					bugCCW(cx, cy, tx, ty);
					int i=0;
					pruneWaypoints();
					if (i==0);
					lastCW = false;
				} else
				{
					bugCW(cx, cy, tx, ty);
					int i=0;
					pruneWaypoints();
					if (i==0);
					lastCW = true;
				}
			}
			boolean moved = executeWaypoints(cx, cy, tx, ty);
			if (moved) break;
		}
	}
	
	public boolean executeWaypoints(int sx, int sy, int tx, int ty) throws GameActionException {
		int ax,ay;
		ax = moves[moveindex][0];
		ay = moves[moveindex][1];
		while (ax==sx && ay==sy) {
			moveindex++;
			if (moveindex==totalmoves)
			{
				prepared = false;
				return false;
			}
			ax = moves[moveindex][0];
			ay = moves[moveindex][1];
		}
		if (ax-sx+1<=2 && ax-sx+1>=0 && ay-sy+1<=2 && ay-sy>=0)
		{
			int dir = getDirTowards(sx, sy, ax, ay);
			if (map[sx+d[dir][0]][sy+d[dir][1]]==0)
			{
				if (rc.canMove(mdirs[dir]))
				{
					if (rc.getDirection() == mdirs[dir])
						rc.moveForward();
					else rc.setDirection(mdirs[dir]);
					return true;
				}
			}
			prepared = false;
			return false;
		}
		int[] dirs = getDirsTowardsFree(sx, sy, ax, ay);
		for (int x=0; x<dirs.length; x++)
		{
			if (map[sx+d[dirs[x]][0]][sy+d[dirs[x]][1]]==0)
			{
				if (rc.canMove(mdirs[dirs[x]]))
				{
					if (rc.getDirection() == mdirs[dirs[x]])
						rc.moveForward();
					else rc.setDirection(mdirs[dirs[x]]);
					return true;
				}
			}
		}
		prepared = false;
		return false;
	}
	
	public void moveTowardStraight(int mx, int my) throws GameActionException
	{
		int[] dirs = getDirsTowardsFree(cx,cy,mx,my);
		for (int x=0; x<dirs.length; x++)
		{
			TerrainTile tt = rc.senseTerrainTile(cl.add(mdirs[dirs[x]]));
			if (tt == TerrainTile.LAND)
			{
				if (rc.getDirection() == mdirs[dirs[x]])
				{
					if (rc.canMove(mdirs[dirs[x]]))
						rc.moveForward();
						
				} else rc.setDirection(mdirs[dirs[x]]);
				return;
			}
		}
	}
	
public void bugCW(int sx, int sy, int tx, int ty) {
		
		prepared = true;
		
		previoustx = tx;
		previousty = ty;
		
		moveindex = 0;
		totalmoves = 0;
		moves[0][0] = tx;
		moves[0][1] = ty;
		
		int[] dirs;
		int ax,ay, xx, yy;
		int nx,ny,px,py;
		int dir,odir;
		boolean changed;
		ax = tx;
		ay = ty;
		ax = xx = sx;
		ay = yy = sy;
		
		while (xx!=tx || yy!=ty)
		{
			while (true)
			{
				dirs = getDirsTowardsFree(xx, yy, tx, ty);
				changed = false;
				for (int x=0; x<dirs.length; x++)
				{
					if (map[xx+d[dirs[x]][0]][yy+d[dirs[x]][1]]==0)
					{
						xx += d[dirs[x]][0];
						yy += d[dirs[x]][1];
						changed = true;
						break;
					}
				}
				if (!changed) break;
				if (xx==tx && yy==ty)
				{
					moves[totalmoves][0] = tx;
					moves[totalmoves][1] = ty;
					totalmoves++;
//					System.out.println("total waypoints:"+(totalmoves));
					moveindex = 0;
					return;
				}
			}
			
			dir = getDirTowards(xx, yy, tx, ty);
			nx=xx+d[dir][0];
			ny=yy+d[dir][1];
			while (map[nx][ny]!=0)
			{
				dir = getDirTowards(nx, ny, tx, ty);
				nx += d[dir][0];
				ny += d[dir][1];
			}
			System.out.println("hitwall at "+xx+","+yy+" next point at "+nx+","+ny);
//			ax = moves[totalmoves][0] = xx;
//			ay = moves[totalmoves][1] = yy;
//			totalmoves++;
			moves[totalmoves][0] = xx;
			moves[totalmoves][1] = yy;
			dir = getDirTowards(xx, yy, nx, ny);
			while (map[xx+d[dir][0]][yy+d[dir][1]]!=0) dir = (dir+7)%8;
			
			while (xx!=nx || yy!=ny) 
			{
				if (xx-nx+1<=2 && xx-nx+1>=0 && yy-ny+1<=2 && yy-ny>=0)
				{
//					ax = moves[totalmoves][0] = nx;
//					ay = moves[totalmoves][1] = ny;
//					totalmoves++;
					moves[totalmoves][0] = xx;
					moves[totalmoves][1] = yy;
					break;
				}
				dir = (dir+3)%8;
				while (map[xx+d[dir][0]][yy+d[dir][1]]!=0) dir = (dir+7)%8;
				px = xx + d[dir][0];
				py = yy + d[dir][1];
				odir = getDirTowards(px,py,ax,ay);
				if (map[px+d[odir][0]][py+d[odir][1]]!=0)
				{
					ax = moves[totalmoves][0] = xx;
					ay = moves[totalmoves][1] = yy;
					totalmoves++;
				}
				xx = px;
				yy = py;
			}
		}
		moves[totalmoves][0] = tx;
		moves[totalmoves][1] = ty;
		totalmoves++;
//		System.out.println("total waypoints:"+(totalmoves));
		moveindex = 0;
		return;
	}
	
public void bugCCW(int sx, int sy, int tx, int ty) {
		
		prepared = true;
		
		previoustx = tx;
		previousty = ty;
		
		moveindex = 0;
		totalmoves = 0;
		moves[0][0] = tx;
		moves[0][1] = ty;
		
		int[] dirs;
		int ax,ay, xx, yy;
		int nx,ny,px,py;
		int dir,odir;
		boolean changed;
		ax = tx;
		ay = ty;
		ax = xx = sx;
		ay = yy = sy;
		
		while (xx!=tx || yy!=ty)
		{
			while (true)
			{
				dirs = getDirsTowardsFree(xx, yy, tx, ty);
				changed = false;
				for (int x=0; x<dirs.length; x++)
				{
					if (map[xx+d[dirs[x]][0]][yy+d[dirs[x]][1]]==0)
					{
						xx += d[dirs[x]][0];
						yy += d[dirs[x]][1];
						changed = true;
						break;
					}
				}
				if (!changed) break;
				if (xx==tx && yy==ty)
				{
					moves[totalmoves][0] = tx;
					moves[totalmoves][1] = ty;
					totalmoves++;
					System.out.println("total waypoints:"+(totalmoves));
					moveindex = 0;
					return;
				}
			}
			
			dir = getDirTowards(xx, yy, tx, ty);
			nx=xx+d[dir][0];
			ny=yy+d[dir][1];
			while (map[nx][ny]!=0)
			{
				dir = getDirTowards(nx, ny, tx, ty);
				nx += d[dir][0];
				ny += d[dir][1];
			}
			System.out.println("hitwall at "+xx+","+yy+" next point at "+nx+","+ny);
//			ax = moves[totalmoves][0] = xx;
//			ay = moves[totalmoves][1] = yy;
//			totalmoves++;
			moves[totalmoves][0] = xx;
			moves[totalmoves][1] = yy;
			dir = getDirTowards(xx, yy, nx, ny);
			while (map[xx+d[dir][0]][yy+d[dir][1]]!=0) dir = (dir+1)%8;
			
			while (xx!=nx || yy!=ny) 
			{
				if (xx-nx+1<=2 && xx-nx+1>=0 && yy-ny+1<=2 && yy-ny>=0)
				{
//					ax = moves[totalmoves][0] = nx;
//					ay = moves[totalmoves][1] = ny;
//					totalmoves++;
					moves[totalmoves][0] = xx;
					moves[totalmoves][1] = yy;
					break;
				}
				dir = (dir+5)%8;
				while (map[xx+d[dir][0]][yy+d[dir][1]]!=0) dir = (dir+1)%8;
				px = xx + d[dir][0];
				py = yy + d[dir][1];
				odir = getDirTowards(px,py,ax,ay);
				if (map[px+d[odir][0]][py+d[odir][1]]!=0)
				{
					ax = moves[totalmoves][0] = xx;
					ay = moves[totalmoves][1] = yy;
					totalmoves++;
				}
				xx = px;
				yy = py;
			}
		}
		moves[totalmoves][0] = tx;
		moves[totalmoves][1] = ty;
		totalmoves++;
		System.out.println("total waypoints:"+(totalmoves));
		moveindex = 0;
		return;
	}
	
	public void pruneWaypoints()
	{
		int xx,yy,ax,ay;
		for (int x=0; x<totalmoves; x++)
		{
			xx = moves[x][0];
			yy = moves[x][1];
			for (int y=totalmoves-1; y>x; y--)
			{
				ax = moves[y][0];
				ay = moves[y][1];
				if (xx-ax+1<=2 && xx-ax+1>=0 && yy-ay+1<=2 && yy-ay>=0)
				{
					for (x=x+1; x<y; x++)
					{
						moves[x][0] = xx;
						moves[x][1] = yy;
					}
					
					break;
				}
			}
		}
	}

	static private int getDirTowards(int sx, int sy, int tx, int ty) {
		if(tx==sx) {
			if(ty>sy) return 5;
			else return 1;                 
		} else if (ty==sy) {
			if(tx>sx) return 3;
			else return 7;
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
	
	static private final int[][] DIRSTOWARDS = new int[][]{
		{5},
		{1},
		{3},
		{7},
		{5,4},
		{4,5},
		{4,3},
		{3,4},
		{3,2},
		{2,3},
		{2,1},
		{1,2},
		{1,0},
		{0,1},
		{0,7},
		{7,0},
		{7,6},
		{6,7},
		{6,5},
		{5,6},
	};
	
	
	final Direction[] mdirs = new Direction[] {
			Direction.NORTH_WEST,
			Direction.NORTH,
			Direction.NORTH_EAST,
			Direction.EAST,
			Direction.SOUTH_EAST,
			Direction.SOUTH,
			Direction.SOUTH_WEST,
			Direction.WEST
			};
	
	static final int[][] d = new int[][] {{-1,-1}, {0,-1}, {1,-1}, {1,0}, 
		{1,1}, {0,1}, {-1,1}, {-1,0}};
	
	static private final int[][] DIRSTOWARDSFREE = new int[][]{
		{5,4,6},
		{1,2,0},
		{3,4,2},
		{7,0,6},
		{5,4,6},
		{4,5,3},
		{4,3,5},
		{3,4,2},
		{3,2,4},
		{2,3,1},
		{2,1,3},
		{1,2,0},
		{1,0,2},
		{0,1,7},
		{0,7,1},
		{7,0,6},
		{7,6,0},
		{6,7,5},
		{6,5,7},
		{5,6,4},
	};
	
	static private int[] getDirsTowardsFree(int sx, int sy, int tx, int ty) {
		if(tx==sx) {
			if(ty>sy) return DIRSTOWARDSFREE[0];
			else return DIRSTOWARDSFREE[1];
		} else if(ty==sy) {
			if(tx>sx) return DIRSTOWARDSFREE[2];
			else return DIRSTOWARDSFREE[3];
		}
		double slope = ((double)(ty-sy))/(tx-sx);
		if(tx>sx) {
			if(slope>2.414) return DIRSTOWARDSFREE[4];
			else if(slope>1) return DIRSTOWARDSFREE[5];
			else if(slope>0.414) return DIRSTOWARDSFREE[6];
			else if(slope>0) return DIRSTOWARDSFREE[7];
			else if(slope>-0.414) return DIRSTOWARDSFREE[8];
			else if(slope>-1) return DIRSTOWARDSFREE[9];
			else if(slope>-2.414) return DIRSTOWARDSFREE[10];
			else return DIRSTOWARDSFREE[11];
		} else {
			if(slope>2.414) return DIRSTOWARDSFREE[12];
			else if(slope>1) return DIRSTOWARDSFREE[13];
			else if(slope>0.414) return DIRSTOWARDSFREE[14];
			else if(slope>0) return DIRSTOWARDSFREE[15];
			else if(slope>-0.414) return DIRSTOWARDSFREE[16];
			else if(slope>-1) return DIRSTOWARDSFREE[17];
			else if(slope>-2.414) return DIRSTOWARDSFREE[18];
			else return DIRSTOWARDSFREE[19];
		}
	}
	

}

class Archon implements Runnable {
	RobotController rc;
	MovementController mc;
	public Archon(RobotController rc) throws GameActionException {
		this.rc = rc;
		this.mc = new MovementController(rc);
		
		PowerNode pn = rc.sensePowerCore();
		nodes[nodesize++] = pn.getLocation();
		MapLocation[] neighbors = pn.neighbors();
		addNewLocs(neighbors);
		
		MapLocation cur = rc.getLocation();
		
		int b1,b2;
		int k1,k2,k3;
		TerrainTile tt;
		b1 = Clock.getBytecodeNum();
		k1 = mc.map[60+1][60+3];
		b2 = Clock.getBytecodeNum();
		System.out.println((b2-b1)+" bytecode to access 2d array");
		
		b1 = Clock.getBytecodeNum();
		tt = rc.senseTerrainTile(new MapLocation(cur.x+1,cur.y+3));
		b2 = Clock.getBytecodeNum();
		System.out.println((b2-b1)+" bytecode to sense terrain tile");
		rc.resign();
	}
	
	void addNewLocs(MapLocation[] locs)
	{
		int bytecode = Clock.getBytecodeNum();
		boolean add = false;
		for (int x=0; x<locs.length; x++)
		{
			MapLocation m = locs[x];
			add = true;
			for (int y=0; y<nodesize; y++)
			{
				if (nodes[y].x==m.x&&nodes[y].y==m.y)
				{
					add = false;
					break;
				}
			}
			if (add)
				nodes[nodesize++] = m;
		}
		System.out.println("added, now has "+nodesize+" nodes");
		bytecode = Clock.getBytecodeNum()-bytecode;
		rc.setIndicatorString(2, "adding new loc used "+bytecode);
	}
	
	boolean[] reached = new boolean[GameConstants.MAX_POWER_NODES+2];
	MapLocation[] nodes = new MapLocation[GameConstants.MAX_POWER_NODES+2];
	int nodesize = 0;
	int nodeindex = 0;
	
	
	@Override
	public void run() {
		
		while (true)
		{
			try {
				rc.yield();
				rc.setIndicatorString(2, "no add");
				
				if (nodes[nodeindex]==null || nodeindex==nodesize) //reset
				{
					nodeindex = 0;
					nodesize = 1;
					nodes[0] = rc.sensePowerCore().getLocation();
				}
				
				MapLocation cur = rc.getLocation();
				if (cur.isAdjacentTo(nodes[nodeindex]))
				{
					System.out.println("next to "+nodes[nodeindex]);
					GameObject go = rc.senseObjectAtLocation(nodes[nodeindex], RobotLevel.ON_GROUND);
					RobotInfo ri;
					boolean done = false;
					if (go==null){
						while (rc.isMovementActive()) rc.yield();
						rc.setDirection(cur.directionTo(nodes[nodeindex]));
						Direction ddd = cur.directionTo(nodes[nodeindex]);
						System.out.println("facing "+nodes[nodeindex]);
						while (rc.getFlux() < RobotType.TOWER.spawnCost) rc.yield();
						System.out.println("has flux for "+nodes[nodeindex]);
						
						while ((!rc.canMove(ddd)) || rc.isMovementActive()) {
							go = rc.senseObjectAtLocation(nodes[nodeindex], RobotLevel.ON_GROUND);
							if (go!=null)
							{
								ri = rc.senseRobotInfo((Robot) go);
								if (ri.type == RobotType.TOWER)
								{
									done = true;
									break;
								} 
							}
							rc.yield();
						}
						if (!done)
						{
							System.out.println("spawning "+nodes[nodeindex]);
							rc.spawn(RobotType.TOWER);
//							PowerNode pn = (PowerNode)rc.senseObjectAtLocation(nodes[nodeindex], RobotLevel.POWER_NODE);
//							MapLocation[] neighbors = pn.neighbors();
//							addNewLocs(neighbors);
						}
					}
					PowerNode pn = (PowerNode)rc.senseObjectAtLocation(nodes[nodeindex], RobotLevel.POWER_NODE);
					MapLocation[] neighbors = pn.neighbors();
					addNewLocs(neighbors);
					nodeindex++;
				} else if (rc.canSenseSquare(nodes[nodeindex]))
				{
					GameObject go = rc.senseObjectAtLocation(nodes[nodeindex], RobotLevel.ON_GROUND);
					if (go!=null)
					{
						PowerNode pn = (PowerNode)rc.senseObjectAtLocation(nodes[nodeindex], RobotLevel.POWER_NODE);
						MapLocation[] neighbors = pn.neighbors();
						addNewLocs(neighbors);
						nodeindex++;
					}
				}
				
				if (nodeindex<nodesize && !rc.isMovementActive())
				{
					int bytecode = Clock.getBytecodeNum();
					rc.setIndicatorString(0, "no scan");
					mc.move(nodes[nodeindex]);
					bytecode = Clock.getBytecodeNum()-bytecode;
					rc.setIndicatorString(1, "move used "+bytecode);
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
