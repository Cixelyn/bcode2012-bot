package normalai;

public enum BroadcastChannel {

	ALL,
	ARCHONS,
	SOLDIERS,
	SCOUTS,
	EXPLORERS;
	
	public final String chanHeader;
	
	BroadcastChannel() {
		chanHeader = "#" + (char)(this.ordinal() + 0x100);
	}

	public static String RobotChannel(int id) {
		return "#" + (char)(id + 0x200);
	}
}
