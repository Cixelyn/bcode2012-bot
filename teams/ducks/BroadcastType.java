package ducks;

public enum BroadcastType {
	
	// shared exploration
	POWERNODE_FRAGMENTS,
	MAP_FRAGMENTS,
	MAP_EDGES,

	// random shit
	OWNERSHIP_CLAIM,
	ENEMY_ARCHON_KILL,

	// rally code
	RALLY;
	
	
	

	public char header;
	BroadcastType() {
		header = (char)(this.ordinal() + 0x100);
	}
	
	public static BroadcastType decode(char header) {
		return BroadcastType.values()[header - 0x100];
	}
}
