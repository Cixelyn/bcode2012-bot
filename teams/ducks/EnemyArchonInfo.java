package ducks;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;

public class EnemyArchonInfo {

	private BaseRobot br;
	private int[] deadEnemyArchonIDs;
	private int timeUntilBroadcast;
	
	public EnemyArchonInfo(BaseRobot myBR) {
		br = myBR;
		deadEnemyArchonIDs = new int[GameConstants.NUMBER_OF_ARCHONS];
		for (int i = 0; i < GameConstants.NUMBER_OF_ARCHONS; i++) {
			deadEnemyArchonIDs[i] = -1;
		}
		timeUntilBroadcast = Constants.SOLDIER_BROADCAST_FREQUENCY;
	}
	
	public void reportEnemyArchonKill(int id) {
		for (int i = 0; i < GameConstants.NUMBER_OF_ARCHONS; i++) {
			if (deadEnemyArchonIDs[i] == id) {
				return;
			}
			if (deadEnemyArchonIDs[i] == -1) {
				deadEnemyArchonIDs[i] = id;
				return;
			}
		}
	}
	
	public void reportEnemyArchonKills(int[] ids) {
		for (int id : ids) {
			reportEnemyArchonKill(id);
		}
	}
	
	public int getNumEnemyArchons() {
		for (int i = 0; i < GameConstants.NUMBER_OF_ARCHONS; i++) {
			if (deadEnemyArchonIDs[i] == -1) {
				return GameConstants.NUMBER_OF_ARCHONS - i;
			}
		}
		return 0;
	}
	
	public void sendDeadEnemyArchonIDs() throws GameActionException {
		if (--timeUntilBroadcast <= 0) {
			br.io.sendShorts("#xd", getDeadEnemyArchonIDs());
			timeUntilBroadcast = Constants.SOLDIER_BROADCAST_FREQUENCY;
		}
	}
	
	private int[] getDeadEnemyArchonIDs() {
		int[] ans = new int[GameConstants.NUMBER_OF_ARCHONS - getNumEnemyArchons()];
		for (int i = 0; i < GameConstants.NUMBER_OF_ARCHONS; i++) {
			if (deadEnemyArchonIDs[i] == -1) {
				break;
			} else {
				ans[i] = deadEnemyArchonIDs[i];
			}
		}
		return ans;
	}
}
