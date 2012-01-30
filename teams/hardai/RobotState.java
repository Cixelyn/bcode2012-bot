package hardai;

public enum RobotState {
	INITIALIZE,
	EXPLORE,
	GOHOME,
	BUILD_ARMY,
	ATTACK_MOVE_INITIAL, ATTACK_ENEMY_BASE,
	DEFEND_BASE,
	POWER_CAP,
	
	// soldier bots states
	HOLD_POSITION,
	HIBERNATE,
	CHASE,
	SWARM,
	ROAM,
	SUICIDE,
	
	// to delete errors
	DEFEND,
	GOTO_POWER_CORE,
	SPAWN_SOLDIERS,
	SPLIT,
	POWER_SAVE,
	RUSH,
//		CHASE,
	BACK_OFF
}