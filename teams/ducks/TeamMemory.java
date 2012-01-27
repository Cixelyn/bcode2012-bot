package ducks;

public class TeamMemory {

	public final BaseRobot br;
	public final long[] mem;
	
	public TeamMemory(BaseRobot br) {
		this.br = br;
		mem = br.rc.getTeamMemory();
	}

	/** @return the current round number (0th, 1st, 2nd match) */
	public int getRound() {
		return (int)mem[0];
	}
	
	/** Archon zero should advance the round counter */
	public void advanceRound() {
		br.rc.setTeamMemory(0, getRound() + 1);
	}
	

}
