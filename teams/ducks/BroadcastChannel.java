package ducks;

public enum BroadcastChannel {

	ALL,
	ARCHONS,
	SOLDIERS,
	SCOUTS,
	EXTENDED_RADAR,
	EXPLORERS;
	
	public final String chanHeader;
	
	BroadcastChannel() {
		chanHeader = "#" + (char)(this.ordinal() + 0x100);
	}

	public static String RobotChannel(int id) {
		return "#" + (char)(id + 0x200);
	}
}
