package veryhardai;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;

/** A utility for fixed-bytecode deep copying of serializable objects. */
public class Cloner 
{
	static PipedInputStream pis;
	static PipedOutputStream pos;
	static ObjectOutputStream oos;
	static ObjectInputStream ois;
	
	private Cloner() {}
	
	static {
		try {
			pis = new PipedInputStream();
			pos = new PipedOutputStream(pis);
			oos = new ObjectOutputStream(pos);
			ois = new ObjectInputStream(pis);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static Object clone(Serializable o) throws IOException, ClassNotFoundException {
		oos.reset();
		oos.writeObject(o);
		return ois.readObject();
	}
	
}
