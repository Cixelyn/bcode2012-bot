package ducks;

public class FastIDSet {
	
	
	private final FastUShortSet mergeSet;
	private final StringBuilder rawBlockSet;
	private StringBuilder curBlock;
	
	private int maxBlocks;
	private int numBlocks;
	
	private static final char DELIMITER_C = (char)-1;
	private static final String DELIMITER_S = String.valueOf(DELIMITER_C);
	
	public FastIDSet(int size) {
		rawBlockSet = new StringBuilder();
		curBlock = new StringBuilder();
		
		mergeSet = new FastUShortSet();
		maxBlocks = size;
		numBlocks = 0;
	}
	
	
	
	
	public void addID(int robotID) {
		
		
		
	}
	
	public void removeID(int robotID) {
		mergeSet.remove(robotID);
	}
	
	
	public void endRound() {
		
		
	}
	
	
	
	public int size() {
		return mergeSet.count();
	}
	
	public int getID(int index) {
		return 0;
	}
	
	
	
	
	public void addIDBlock(StringBuilder block) {
		
		// add the new block
		rawBlockSet.append(block);
		rawBlockSet.append(DELIMITER_C);
		
		// fill in the merge set
		for(int i=block.length(); --i>=0;) {
			mergeSet.add(block.charAt(i));
		}
		
		numBlocks++;
	}
	
//	public void removeOldBlock() {
//		
//		//grab the first block
//		int idx = rawBlockSet.indexOf(DELIMITER_S);
//		String oldBlock = rawBlockSet.substring(0, idx);
//		
//		for(int i=oldBlock.length(); --i>=0;) {
//			int id = String.valueOf(oldBlock.charAt(i));
//		}
//		
//		numBlocks--;
//		
//	}
	
	
	
	
	public String toString() {
		return mergeSet.toString();
	}
	
	
	public static void main(String[] args) {
		System.out.println("Hello World");
	}
	
	

}
