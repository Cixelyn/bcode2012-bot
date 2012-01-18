package ducks;

public enum MessageChannel {

	ALL,
	ARCHONS,
	SOLDIERS,
	EXPLORERS;
	
	public final String chanName;
	MessageChannel() {
		chanName = "#" + (char)(this.ordinal() + 0x100);
	}

	public static String RobotChannel(int id) {
		return "#" + (char)(id + 0x200);
	}
}
