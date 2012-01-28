package normalai;


/**
 * Fast set for storing unsigned shorts
 */
public class FastUShortSet {
	
	private final StringBuilder setContainer;

	public FastUShortSet() {
		setContainer = new StringBuilder();
	}

	/**
	 * Adds an element to the set
	 * @param i - unsigned 16-bit short
	 */
	public void add(int i) {
		if(setContainer.indexOf(String.valueOf((char)i)) < 0){
			setContainer.append((char)i);
		}
	}

	/**
	 * Pops an element out of the set. Will throw an error if the set is empty
	 * @return random element
	 */
	public int pop() {
		int res = setContainer.charAt(0);
		setContainer.deleteCharAt(0);
		return res;
	}

	/**
	 * @return Whether the set is empty
	 */
	public boolean isEmpty() {
		return setContainer.length() == 0;
	}

	/**
	 * Returns the internal stringbuilder representation
	 */
	public String toString() {
		return setContainer.toString();
	}
	
	
	public static void main(String[] args) {
		FastUShortSet a = new FastUShortSet();
		a.add(81); //Q
		a.add(81); //Q
		a.add(82); //R
		a.add(83); //S
		a.add(81); //Q
		a.pop();
		a.pop();
		
		System.out.println(a);
	}
}
