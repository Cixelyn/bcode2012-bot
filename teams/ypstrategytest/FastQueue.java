package ypstrategytest;

public class FastQueue {
	FastQueueNode head;
	FastQueueNode tail;
	
	public FastQueue() {
		head = tail = null;
	}
	
	public void pushBack(Object o) {
		if (tail==null)
			head = tail = new FastQueueNode(null, o);
		else
		{
			tail.next = new FastQueueNode(null, o);
			tail = tail.next;
		}
	}
	
	public void pushFront(Object o) {
		if (head==null)
			head = tail = new FastQueueNode(null, o);
		else
			head = new FastQueueNode(head.next, o);
	}
}

class FastQueueNode {
	public FastQueueNode next;
	public Object data;
	public FastQueueNode(FastQueueNode next, Object data) {
		this.next = next;
		this.data = data;
	}
	
	public Object deleteNext() {
		if (next==null) return null;
		Object o = next.data;
		next = next.next;
		return o;
	}
	
	public void addNext(Object o) {
		next = new FastQueueNode(next, o);
	}
}