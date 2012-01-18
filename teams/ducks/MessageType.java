package ducks;

public enum MessageType {
	
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
	MessageType() {
		header = (char)(this.ordinal() + 0x100);
	}
	
	public static MessageType decode(char header) {
		return MessageType.values()[header - 0x100];
	}
}
