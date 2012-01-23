package ducks;

public class Encryption {
	
	private static final int[] SHUFFLE = new int[] {2,4,0,8,7,3,6,5,1};
	private static final int[] DESHUFFLE = new int[] {2,8,0,5,1,7,6,4,3};
	
	public static String encryptString(String s) {
		String t = "";
		int l = s.length() / SHUFFLE.length;
		for (int i : SHUFFLE) {
			t = t.concat(s.substring(i*l, (i+1)*l));
		}
		return t.concat(s.substring(SHUFFLE.length*l));
	}
	
	public static String decryptString(String t) {
		String s = "";
		int l = t.length() / DESHUFFLE.length;
		for (int i : DESHUFFLE) {
			s = s.concat(t.substring(i*l, (i+1)*l));
		}
		return s.concat(t.substring(DESHUFFLE.length*l));
	}
	
	public static void main(String[] args) {
		String[] myStrings = new String[] {
				"I'm an Archon in rush mode.",
				"I hope no one sees my super secret tangent bug.",
				"My swarm target is [555,555].",
				"!!!!!-----#####     "};
		for (String s : myStrings) {
			System.out.println("Original:" + s);
			System.out.println("Encrypted:" + encryptString(s));
			System.out.println("Decrypted:" + decryptString(encryptString(s)));
		}
	}
}
