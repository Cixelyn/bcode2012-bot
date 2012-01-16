package ducks;

public enum RobotState {
	INITIALIZE,
	EXPLORE,
	GOHOME,
	BUILD_ARMY,
	ATTACK_MOVE_INITIAL, ATTACK_MOVE,
	DEFEND_BASE,
	POWER_CAP,
	
	HOLD_POSITION,
	
	// to delete errors
	DEFEND,
	GOTO_POWER_CORE,
	SPAWN_SOLDIERS,
	SPLIT,
	POWER_SAVE,
	RUSH,
	CHASE,
	BACK_OFF
}