package ypstrategytest;

public class FasterQueue {
	String larger;
	String smaller;
	int size;
	
//	private static final char MAX = (char)32000;
//	private static final char MIN = (char)31999;
	private final char MAX;
	private final char MIN;
	
	static String string(int len, char c)
	{
		return new String(new char[len]).replace('\0', c);
	}
	
	public FasterQueue(int maxval)
	{
		MIN = 0;
		MAX = (char)(maxval+4); 
		larger = string(MAX,MAX);
		smaller = string(MAX,MIN);
		size = 0;
	}
	
	public void insert(int val)
	{
		char l = larger.charAt(val);
		char s = smaller.charAt(val);
		
		larger = larger.substring(0, s).concat(string(val-s,(char) val)).concat(larger.substring(val));
		smaller = smaller.substring(0, val+1).concat(string(l-val-1,(char) val)).concat(larger.substring(l));
		size++;
	}
	
	public void remove(int val)
	{
		char l = larger.charAt(val);
		char s = smaller.charAt(val);
		
		larger = larger.substring(0, s).concat(string(val-s,(char) s)).concat(larger.substring(val));
		smaller = smaller.substring(0, val+1).concat(string(l-val-1,(char) l)).concat(larger.substring(l));
		size--;
	}
	
	public int popLargest()
	{
		int val = smaller.charAt(MAX-1);
		int l = larger.charAt(val);
		int s = smaller.charAt(val);
		
		larger = larger.substring(0, s).concat(string(val-s,(char) l)).concat(larger.substring(val));
		smaller = smaller.substring(0, val+1).concat(string(l-val-1,(char) s)).concat(larger.substring(l));
		size--;
		return val;
	}
	
	public int popSmallest()
	{
		int val = larger.charAt(MIN);
		int l = larger.charAt(val);
		int s = smaller.charAt(val);
		
		larger = larger.substring(0, s).concat(string(val-s,(char) s)).concat(larger.substring(val));
		smaller = smaller.substring(0, val+1).concat(string(l-val-1,(char) l)).concat(larger.substring(l));
		size--;
		return val;
	}
	
	
	
}
