package ypstrategytest;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;

import battlecode.common.Clock;


public class TestClass {
	
	
	
	@SuppressWarnings("unused")
	public static void run() throws Exception
	{
		int aaaa;
		
		int t1,t2;
		char[] c;
		FasterQueue fq;
		
		t1 = Clock.getBytecodeNum();
		fq = new FasterQueue(300);
		t2 = Clock.getBytecodeNum();
		System.out.println(t2-t1);
		
		
		t1 = Clock.getBytecodeNum();
		fq.insert(34);
		t2 = Clock.getBytecodeNum();
		System.out.println(t2-t1);
		
		
		t1 = Clock.getBytecodeNum();
		fq.insert(68);
		t2 = Clock.getBytecodeNum();
		System.out.println(t2-t1);
		
		t1 = Clock.getBytecodeNum();
		System.out.println(fq.popLargest());
		t2 = Clock.getBytecodeNum();
		System.out.println(t2-t1);
		
		t1 = Clock.getBytecodeNum();
		System.out.println(fq.popLargest());
		t2 = Clock.getBytecodeNum();
		System.out.println(t2-t1);
	}
	
	@SuppressWarnings("unused")
	public static void run2() throws Exception
	{
		int t1,t2;
		t1 = Clock.getBytecodeNum();
		StringWriter sw = new StringWriter();
		sw.append("testtesttest", 2, 6);
		String s = sw.toString();
		t2 = Clock.getBytecodeNum();
		System.out.println("string writer used: "+(t2-t1));
		
		t1 = Clock.getBytecodeNum();
		String s2 = "testtesttest".substring(2,6);
		t2 = Clock.getBytecodeNum();
		System.out.println("string writer used: "+(t2-t1));
		
		ArrayList<Integer> a = new ArrayList<Integer>();
		for (int x=0; x<100; x++)
			a.add(x);
		
		ArrayList<Integer> b = new ArrayList<Integer>();
		t1 = Clock.getBytecodeNum();
		b.addAll(a);
		t2 = Clock.getBytecodeNum();
		System.out.println("string writer used: "+(t2-t1));
		
//		Method[] m = ArrayList.class.getMethods();
//		ArrayList<Integer> c = new ArrayList<Integer>();
		
		
		t1 = Clock.getBytecodeNum();
		"asdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdf".hashCode();
		t2 = Clock.getBytecodeNum();
		System.out.println("string writer used: "+(t2-t1));
		

		PipedInputStream pis = new PipedInputStream();
		PipedOutputStream pos = new PipedOutputStream(pis);
		ObjectOutputStream oos = new ObjectOutputStream(pos);
		ObjectInputStream ois = new ObjectInputStream(pis);
		
		Test a1 = new Test();
		
		int[] i1 = new int[100];
		for (int x=0; x<100; x++)
			i1[x] = x;
		t1 = Clock.getBytecodeNum();
		a1.a = "abcde";
		a1.i = i1;
		oos.writeObject(a1);
		Test a2 = (Test) ois.readObject();
		a2.a = "lol";
		oos.writeObject(a2);
		Test a3 = (Test) ois.readObject();
		a3.a = "lol2";
		t2 = Clock.getBytecodeNum();
		System.out.println("string writer used: "+(t2-t1));
		System.out.println(a1.a);
		System.out.println(a2.a);
		System.out.println(a3.a);
		for (int x=0; x<100; x++)
			a2.i[x] = x-9;
		for (int x=0; x<100; x++)
			a3.i[x] = x-100;
		System.out.println(a1.i[0]);
		System.out.println(a2.i[0]);
		System.out.println(a3.i[0]);
		
		
		t1 = Clock.getBytecodeNum();
		StrategyHeap sheap = new StrategyHeap();
		t2 = Clock.getBytecodeNum();
		System.out.println("heap init used: "+(t2-t1));
		
		t1 = Clock.getBytecodeNum();
		StrategyHeap sheap2 = (StrategyHeap) Cloner.clone(sheap);
		t2 = Clock.getBytecodeNum();
		System.out.println("heap init used: "+(t2-t1));
		sheap2.size = 1;
		
		t1 = Clock.getBytecodeNum();
		StrategyHeap sheap3 = (StrategyHeap) Cloner.clone(sheap);
		t2 = Clock.getBytecodeNum();
		System.out.println("heap init used: "+(t2-t1));
		sheap3.size = 2;
		
		t1 = Clock.getBytecodeNum();
		StrategyHeap sheap4 = (StrategyHeap) Cloner.clone(sheap);
		t2 = Clock.getBytecodeNum();
		System.out.println("heap init used: "+(t2-t1));
		sheap4.size = 3;
		
		System.out.println(sheap.size);
		System.out.println(sheap2.size);
		System.out.println(sheap3.size);
		System.out.println(sheap4.size);
		
		genStrategyElts();
		
		t1 = Clock.getBytecodeNum();
		StrategyQueue sq2 = (StrategyQueue) Cloner.clone(sq);
		t2 = Clock.getBytecodeNum();
		System.out.println("string writer used: "+(t2-t1));
		
		t1 = Clock.getBytecodeNum();
		StrategyQueue sq3 = (StrategyQueue) Cloner.clone(sq);
		t2 = Clock.getBytecodeNum();
		System.out.println("string writer used: "+(t2-t1));
	}
	
//	t1 = Clock.getBytecodeNum();
//	
//	t2 = Clock.getBytecodeNum();
//	System.out.println("string writer used: "+(t2-t1));
	
//	public static void main(String[] args) {
//		Method[] m = ArrayList.class.getMethods();
//		for (int x=0; x<m.length; x++)
//		{
//			if (m[x].toString().contains("addAll"))
//				System.out.println(m[x]+" "+x);
//		}
//	}

	
	static StrategyQueue sq;
	static StrategyElement[] sa;
	public static void genStrategyElts()
	{
		int bc = Clock.getBytecodeNum();
		sa = new StrategyElement[] {
			new StrategyElement(StrategyConstants.values[0], 2, 	0, 		1000),
			new StrategyElement(StrategyConstants.values[1], 5, 	0, 		500),
			new StrategyElement(StrategyConstants.values[2], 7, 	300,	650),
			new StrategyElement(StrategyConstants.values[3], 1, 	450, 	500),
			new StrategyElement(StrategyConstants.values[4], 4, 	600, 	700),
			new StrategyElement(StrategyConstants.values[5], 3, 	600, 	1200),
			new StrategyElement(StrategyConstants.values[6], 9, 	630, 	900),
			new StrategyElement(StrategyConstants.values[7], 2, 	1000, 	1500),
			new StrategyElement(StrategyConstants.values[0], 2, 	0, 		1000),
			new StrategyElement(StrategyConstants.values[1], 5, 	0, 		500),
			new StrategyElement(StrategyConstants.values[2], 7, 	300,	650),
			new StrategyElement(StrategyConstants.values[3], 1, 	450, 	500),
			new StrategyElement(StrategyConstants.values[4], 4, 	600, 	700),
			new StrategyElement(StrategyConstants.values[5], 3, 	600, 	1200),
			new StrategyElement(StrategyConstants.values[6], 9, 	630, 	900),
			new StrategyElement(StrategyConstants.values[7], 2, 	1000, 	1500),
			new StrategyElement(StrategyConstants.values[4], 4, 	600, 	700),
			new StrategyElement(StrategyConstants.values[4], 4, 	600, 	700),
		};
		System.out.println("gen array: "+(Clock.getBytecodeNum()-bc));
		bc = Clock.getBytecodeNum();
		sq = new StrategyQueue(sa);
		System.out.println("gen queue: "+(Clock.getBytecodeNum()-bc));
	}
}

class Test implements Serializable,Externalizable {
	String a;
	int[] i;
	public Test() {
//		for (int x=0; x<100; x++)
//			if (x==50) a = null;
//		System.out.println("constructor called");
	}
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(a);
		out.writeObject(i);
		for (int x=0; x<100; x++)
			if (x==50) a = null;
	}
	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		a = (String) in.readObject();
		i = (int[]) in.readObject();
	}
	
//	private void readObject(ObjectInputStream ois)
//			throws ClassNotFoundException, IOException {
//		ois.defaultReadObject();
//		a = "abcdefg";
//		if (a.length()>2)
//			a = "abc";
//		else
//			a = "abccd";
//	}
	
//	private void writeObject(ObjectOutputStream oos)
//			throws ClassNotFoundException, IOException {
//		a = "abcdefg";
//		if (a.length() > 2)
//			a = "abc";
//		else
//			a = "abccd";
//		for (int x=0; x<100; x++)
//			if (x==50) a = null;
//		oos.defaultWriteObject();
//	}
	
//	public Object readResolve()
//	{
//		for (int x=0; x<100; x++)
//			if (x==50) a = null;
//		a = "test";
//		return this;
//	}
}