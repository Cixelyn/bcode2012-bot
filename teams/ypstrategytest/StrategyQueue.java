package ypstrategytest;

public class StrategyQueue {
	StrategyQueueNode head;
	StrategyQueueNode tail;
	
	public StrategyQueue() {
		head = tail = null;
	}
	
	public StrategyQueue(StrategyElement[] elts)
	{
		if (elts.length<2)
			if (elts.length==1)
				head = tail = new StrategyQueueNode(null, elts[0]);
			else
				head = tail = null;
		else
		{
			int i = elts.length-1;
			tail = new StrategyQueueNode(null, elts[i]);
			for (; i>=0; i--)
			{
				head = new StrategyQueueNode(head, elts[i]);
			}
		}
	}
	
	public void pushBack(StrategyElement o) {
		if (tail==null)
			head = tail = new StrategyQueueNode(null, o);
		else
		{
			tail.next = new StrategyQueueNode(null, o);
			tail = tail.next;
		}
	}
	
	public void pushFront(StrategyElement o) {
		if (head==null)
			head = tail = new StrategyQueueNode(null, o);
		else
			head = new StrategyQueueNode(head.next, o);
	}
}

class StrategyQueueNode {
	public StrategyQueueNode next;
	public StrategyElement data;
	public StrategyQueueNode(StrategyQueueNode next, StrategyElement data) {
		this.next = next;
		this.data = data;
	}
	
	public StrategyElement deleteNext() {
		if (next==null) return null;
		StrategyElement o = next.data;
		next = next.next;
		return o;
	}
	
	public void addNext(StrategyElement o) {
		next = new StrategyQueueNode(next, o);
	}
}