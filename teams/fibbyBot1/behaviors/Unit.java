package fibbyBot1.behaviors;

import fibbyBot1.Util;
import battlecode.common.*;

public abstract class Unit {

	protected RobotController myRC;
	protected Util util;
	protected Direction bearing;
	protected MapLocation target;
	protected int targetPriority;
	
	public Unit(RobotController RC) {
		this.myRC = RC;
		this.util = new Util(RC);
		this.bearing = Direction.EAST;
		this.target = RC.getLocation().add(
				this.bearing, GameConstants.MAP_MAX_HEIGHT);
		this.targetPriority = 0;
	}
	
	public abstract void run() throws GameActionException;
	
	public void setRally() {
		for (Message m : this.myRC.getAllMessages()) {
			if (m.ints[0] == 1337) {
				if (m.ints[2] > this.targetPriority) {
					this.bearing = Direction.values()[m.ints[1]];
					this.target = m.locations[0];
					this.targetPriority = m.ints[2];
				}
			}
		}
		MapLocation loc;
		if (!bearing.isDiagonal()) {
			int range = 0;
			while (range * range < this.myRC.getType().sensorRadiusSquared) {
				range++;
			}
			range--;
			loc = this.myRC.getLocation().add(bearing, range);
		} else {
			int range = 0;
			while (2 * range * range < this.myRC.getType().sensorRadiusSquared) {
				range++;
			}
			range--;
			loc = this.myRC.getLocation().add(bearing, range);
		}
		if (myRC.senseTerrainTile(loc) == TerrainTile.OFF_MAP) {
			this.bearing = this.bearing.rotateRight().rotateRight().rotateRight();
			this.targetPriority++;
		}
	}
}
