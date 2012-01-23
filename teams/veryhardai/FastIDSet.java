package veryhardai;

import battlecode.common.Clock;

public class FastIDSet {
	
	
	private final FastUShortSet mergeSet;
	private StringBuilder rawBlockSet;
	private String curBlock;
	
	private int maxBlocks;
	private int numBlocks;
	
	private static final char DELIMITER_C = (char)-1;
	private static final String DELIMITER_S = String.valueOf(DELIMITER_C);
	
	public FastIDSet(int size) {
		rawBlockSet = new StringBuilder();
		curBlock = "";
		
		mergeSet = new FastUShortSet();
		maxBlocks = size;
		numBlocks = 0;
	}
	
	
	public void addID(int robotID) {
		curBlock = curBlock.concat(String.valueOf((char) robotID));
	}
	
	
	public void removeID(int robotID) {
		mergeSet.remove(robotID);
	}
	
	public void endRound() {
		addIDBlock(curBlock); //add current block
		curBlock = "";  // fresh block
		if(numBlocks > maxBlocks) {
			removeOldBlock();
		}
	}
	
	
	public int size() {
		return mergeSet.size();
	}
	
	public int getID(int index) {
		return mergeSet.get(index);
	}
	
	
	private void addIDBlock(String block) {
		
		// add the new block
		rawBlockSet.append(block.concat(DELIMITER_S));
		
		// fill in the merge set
		for(int i=block.length(); --i>=0;) {
			mergeSet.add(block.charAt(i));
		}
		
		numBlocks++;
	}

	
	/**
	 * This call is rather expensive so we optimize the hell out of it
	 */
	private void removeOldBlock() {

		int i;
		String oldBlock;
		String recentBlocks;
		
		//grab the first block
		int idx = rawBlockSet.indexOf(DELIMITER_S);
		oldBlock = rawBlockSet.substring(0, idx);
		recentBlocks =  rawBlockSet.substring(idx+1, rawBlockSet.length());
		
		for(i=oldBlock.length(); --i>=0;) {
			
			// if the robot doesn't exist in new messages
			char robotID = oldBlock.charAt(i);
			if(recentBlocks.indexOf(String.valueOf(robotID)) < 0) {
				mergeSet.remove(robotID);
			}
		}
		
		// wipe out the first block
		rawBlockSet = new StringBuilder(recentBlocks);
		
		numBlocks--;
	}
	
	
	public String toString() {
		return mergeSet.toString();
	}
	
	
	private void debug() {
		System.out.println();
		System.out.println("RAW: " + rawBlockSet);
		System.out.println("SET: " + mergeSet);
		System.out.println("NUM: " + numBlocks);
	}
	
	public static void main(String[] args) {
		FastIDSet a = new FastIDSet(3);
		a.addID('a');
		a.addID('b');
		a.addID('b');
		a.addID('c');
		a.endRound();
		a.debug();
		
		a.addID('a');
		a.addID('a');
		a.addID('e');
		a.addID('e');
		a.endRound();
		a.debug();
	
		a.addID('a');
		a.addID('b');
		a.addID('e');
		a.addID('f');
		a.endRound();
		a.debug();
		
		a.endRound();
		a.debug();
		
		a.addID('f');
		a.addID('g');
		a.debug();
		a.endRound();
		
		a.addID('a');
		a.debug();
		
		a.endRound();
		a.debug();
		
		a.endRound();
		a.debug();
		
		a.endRound();
		a.debug();
		
		a.endRound();
		a.debug();
	}
	
	

}
