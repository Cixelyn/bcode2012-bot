package verydisrupterai;


public class DebugSystem {
	
	private final BaseRobot br;
	private char launch_owner;
	private boolean encrypted;
	
	public DebugSystem(BaseRobot br, boolean encrypted) {
		this.br = br;
		this.launch_owner = 'e';
		this.encrypted = encrypted;
	}

	public void setOwner(char owner) {
		this.launch_owner = owner;
	}
	
	public void println(char owner, String msg) {
		if(launch_owner == owner || owner == 'e')
			System.out.println(msg);
	}
	
	public void setIndicatorString(char owner, int position, String msg) {
		if(launch_owner == owner || owner == 'e') {
			if (encrypted) {
				msg = Encryption.encryptString(msg, br.curRound);
			}
			br.rc.setIndicatorString(position,msg);
		}
	}

}
