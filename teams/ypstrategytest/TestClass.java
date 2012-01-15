package ypstrategytest;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;

import battlecode.common.Clock;

public class TestClass {
	
	public static void run() throws Exception
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

}

class Test implements Serializable {
	String a;
	int[] i;
	public Test() {
		for (int x=0; x<100; x++)
			if (x==50) a = null;
		System.out.println("constructor called");
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
	public Object readResolve()
	{
		for (int x=0; x<100; x++)
			if (x==50) a = null;
		a = "test";
		return this;
	}
}