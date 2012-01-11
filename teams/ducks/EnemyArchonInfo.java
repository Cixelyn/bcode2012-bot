package ducks;

import battlecode.common.GameConstants;

public class EnemyArchonInfo {

	public int[] deadEnemyArchonIDs;
	
	public EnemyArchonInfo() {
		deadEnemyArchonIDs = new int[GameConstants.NUMBER_OF_ARCHONS];
		for (int i = 0; i < GameConstants.NUMBER_OF_ARCHONS; i++) {
			deadEnemyArchonIDs[i] = -1;
		}
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
	
	public int getNumEnemyArchons() {
		for (int i = 0; i < GameConstants.NUMBER_OF_ARCHONS; i++) {
			if (deadEnemyArchonIDs[i] == -1) {
				return GameConstants.NUMBER_OF_ARCHONS - i;
			}
		}
		return 0;
	}
	
	public int[] getDeadEnemyArchonIDs() {
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
