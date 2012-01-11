package ducks;

import battlecode.common.*;

public class DataCache {
	
	BaseRobot br;
	MapLocation[] alliedArchons;
	int alliedArchonsTime = -1;
	int moveableDirectionsTime = -1;
	boolean[] moveableDirections = new boolean[8];
	int moveableLandTime = -1;
	boolean[] moveableLand = new boolean[8];
	
	
	public DataCache(BaseRobot br) {
		this.br = br;
	}

	
	public MapLocation[] getAlliedArchons() {
		
		if(br.currRound > alliedArchonsTime) {
			alliedArchons = br.rc.senseAlliedArchons();
			alliedArchonsTime = br.currRound;
		}
		
		return alliedArchons;
		
	}
	
	public boolean[] getMovableDirections()
	{
		if (br.currRound > moveableDirectionsTime)
		{
			moveableDirections[0] = br.rc.canMove(Direction.NORTH);
			moveableDirections[1] = br.rc.canMove(Direction.NORTH_EAST);
			moveableDirections[2] = br.rc.canMove(Direction.EAST);
			moveableDirections[3] = br.rc.canMove(Direction.SOUTH_EAST);
			moveableDirections[4] = br.rc.canMove(Direction.SOUTH);
			moveableDirections[5] = br.rc.canMove(Direction.SOUTH_WEST);
			moveableDirections[6] = br.rc.canMove(Direction.WEST);
			moveableDirections[7] = br.rc.canMove(Direction.NORTH_WEST);
			moveableDirectionsTime = br.currRound;
		}
		return moveableDirections;
	}
	
	public boolean[] getMovableLand()
	{
		if (br.currRound > moveableLandTime)
		{
			moveableLand[0] = br.rc.senseTerrainTile(br.currLoc.add(Direction.NORTH))==TerrainTile.LAND;
			moveableLand[1] = br.rc.senseTerrainTile(br.currLoc.add(Direction.NORTH_EAST))==TerrainTile.LAND;
			moveableLand[2] = br.rc.senseTerrainTile(br.currLoc.add(Direction.EAST))==TerrainTile.LAND;
			moveableLand[3] = br.rc.senseTerrainTile(br.currLoc.add(Direction.SOUTH_EAST))==TerrainTile.LAND;
			moveableLand[4] = br.rc.senseTerrainTile(br.currLoc.add(Direction.SOUTH))==TerrainTile.LAND;
			moveableLand[5] = br.rc.senseTerrainTile(br.currLoc.add(Direction.SOUTH_WEST))==TerrainTile.LAND;
			moveableLand[6] = br.rc.senseTerrainTile(br.currLoc.add(Direction.WEST))==TerrainTile.LAND;
			moveableLand[7] = br.rc.senseTerrainTile(br.currLoc.add(Direction.NORTH_WEST))==TerrainTile.LAND;
			moveableLandTime = br.currRound;
		}
		return moveableLand;
	}
	
	
}
