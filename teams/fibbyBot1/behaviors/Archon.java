package fibbyBot1.behaviors;

import fibbyBot1.Constants;
import battlecode.common.*;

public class Archon extends Unit{

	RobotType spawnType;
	int timeUntilBroadcast;
	
	public Archon(RobotController RC) {
		super(RC);
		this.setSpawnType();
		timeUntilBroadcast = Constants.BROADCAST_FREQUENCY;
	}
	
	public void run() throws GameActionException {
		// if beginning of game, determine archon IDs
		// TODO(jven): implement me
		// set and broadcast rally
		this.setRally();
		RobotInfo nearbyEnemyInfo = this.util.senseNearbyEnemy();
		if (nearbyEnemyInfo != null) {
			this.target = nearbyEnemyInfo.location;
			this.targetPriority += 1;
		} else {
			this.target = this.myRC.getLocation().add(
					this.bearing, GameConstants.MAP_MAX_HEIGHT);
		}
		if (timeUntilBroadcast == 0) {
			this.util.broadcastRally(
					this.bearing, this.target, this.targetPriority);
			timeUntilBroadcast += Constants.BROADCAST_FREQUENCY;
		}
		timeUntilBroadcast--;
		// distribute flux
		this.util.distributeFlux();
		// movement commands
		if (!myRC.isMovementActive()) {
			if (myRC.getFlux() > this.spawnType.spawnCost +
					Constants.FLUX_TO_FILL * this.spawnType.maxFlux) {
				if (this.util.spawn(this.spawnType)) {
					setSpawnType();
				}
			} else {
				this.util.navigate(this.target);
			}
		}
		this.myRC.setIndicatorString(0, "Target: " + this.target);
		this.myRC.setIndicatorString(1, "Priority: " + this.targetPriority);
	}
	
	public void setSpawnType() {
		double p = Math.random();
		if (p < 0.8) {
			this.spawnType = RobotType.SOLDIER;
		} else {
			this.spawnType = RobotType.SCOUT;
		}
	}
}
