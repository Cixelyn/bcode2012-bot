package ducks;

public class DebugSystem {
	
	private final BaseRobot br;
	private char launch_owner;
	
	public DebugSystem(BaseRobot br) {
		this.br = br;
		this.launch_owner = 'z';
	}

	public void setOwner(char owner) {
		this.launch_owner = owner;
	}
	
	public void println(char owner, String msg) {
		if(launch_owner == owner)
			System.out.println(msg);
	}
	public void setIndicatorString(char owner, int position, String msg) {
		if(launch_owner == owner) {
			br.rc.setIndicatorString(position,msg);
		}
	}

}
