package ducks;

public class Encryption {
	
	private static final int[] SHUFFLE = new int[] {2,4,0,8,7,3,6,5,1};
	private static final int[] DESHUFFLE = new int[] {2,8,0,5,1,7,6,4,3};
	
	public static String encryptString(String s, int roundNum) {
		String t = "";
		int l = s.length() / SHUFFLE.length;
		for (int i = 0; i < SHUFFLE.length; i++) {
			int j = SHUFFLE[(5 * roundNum + i) % SHUFFLE.length];
			t = t.concat(s.substring(j*l, (j+1)*l));
		}
		t = t.concat(s.substring(SHUFFLE.length*l));
		return t;
	}
	
	public static String decryptString(String t, int roundNum) {
		String s = "";
		int l = t.length() / DESHUFFLE.length;
		for (int i = 0; i < DESHUFFLE.length; i++) {
			int j = (DESHUFFLE[i] + (DESHUFFLE.length - 5) * roundNum) % DESHUFFLE.length;
			s = s.concat(t.substring(j*l, (j+1)*l));
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
			System.out.println("Encrypted:" + encryptString(s, 1));
			System.out.println("Decrypted:" + decryptString(encryptString(s, 1), 1));
		}
		System.out.println("");
		String myString = "I'm a really long test indicator string.";
		for (int i = 0; i < 20; i++) {
			System.out.println("Encrypted: " + encryptString(myString, i));
			System.out.println("Decrypted: " + decryptString(encryptString(myString, i), i));
		}
	}
}

//2 4 0 8 7 3 6 5 1
//2 8 0 5 1 7 6 4 3
//
//3 6 5 1 2 4 0 8 7
//6 3 4 0 5 2