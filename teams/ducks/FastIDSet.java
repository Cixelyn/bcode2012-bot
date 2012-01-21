package ducks;

public class FastIDSet {

	private final FastUShortSet mergeSet;
	private final StringBuilder rawBlockSet;
	
	private int maxBlocks;
	private int numBlocks;
	
	private static final char DELIMITER_C = (char)-1;
	private static final String DELIMITER_S = String.valueOf(DELIMITER_C);
	
	
	public FastIDSet(int size) {
		rawBlockSet = new StringBuilder();
		mergeSet = new FastUShortSet();
		maxBlocks = size;
	}
	
	public void addID(int robotID) {}
	public void removeID(int robotID) {}
	public void endRound() {}
	public void size() {}
	public void getID(int index) {}
	
	
//	public void addIDBlock(int[] ids) {
//		for(int id : ids) {
//			
//			
//		}
//	}
//	
//	
//	public void addIDBlock(StringBuilder block) {
//		
//		// add the new block
//		rawBlockSet.append(block);
//		rawBlockSet.append(DELIMITER_C);
//		
//		// fill in the merge set
//		for(int i=block.length(); --i>=0;) {
//			mergeSet.add(block.charAt(i));
//		}
//		
//		numBlocks++;
//	}
//	
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
//	
//	
//	
//	
//	public void removeID(int robotID) {
//		mergeSet.remove(robotID);
//	}
//	
//	
//	public int count() {
//		return mergeSet.count();
//	}
//	
//	public String toString() {
//		return mergeSet.toString();
//	}
//	
//	
//	public static void main(String[] args) {
//		System.out.println("Hello World");
//	}
//	
//	

}
