package ducks;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

/**
 * This system is used to communicate objective information between units.
 * @author jven
 */
public class RallySystem {

	private BaseRobot br;
	private int timeUntilBroadcast;
	
	public RallySystem(BaseRobot myBR) {
		br = myBR;
		timeUntilBroadcast = Constants.ARCHON_BROADCAST_FREQUENCY;
	}
	
	public MapLocation getCurrentObjective() throws GameActionException {
		if (br.dc.getClosestEnemy() != null) {
			return br.dc.getClosestEnemy().location;
		} else {
			return br.mc.guessEnemyPowerCoreLocation();
		}
	}
	
	public void broadcastRally() throws GameActionException {
		if (--timeUntilBroadcast <= 0) {
			// share exploration information
			br.ses.broadcastMapEdges();
			// send objective
			br.io.sendMapLoc("#xr", getCurrentObjective());
			timeUntilBroadcast = Constants.ARCHON_BROADCAST_FREQUENCY;
		}
	}
}
