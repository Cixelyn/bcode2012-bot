package ypstrategytest;

public class StrategyHeap
{
	public static final int MAX_STRATEGY_HEAP_SIZE = 100;
	public StrategyElement[] heap;
	public int size;
	
	public StrategyHeap()
	{
		size = 0;
		heap = new StrategyElement[MAX_STRATEGY_HEAP_SIZE];
	}
	
	public StrategyHeap(StrategyHeap other)
	{
		size = other.size;
		heap = new StrategyElement[MAX_STRATEGY_HEAP_SIZE];
		System.arraycopy(other.heap, 0, heap, 0, size);
	}
	
	public void insert(StrategyElement e)
	{
		int n = ++size;
		int a;
		int p = e.priority;
		while (n>1)
		{
			a = n>>1;
			if (heap[a].priority<p)
				heap[n] = heap[a];
			else
				break;
			n = a;
		}
		heap[n] = e;
	}
	
	public StrategyElement pop()
	{
		final StrategyElement ret = heap[1];
		int n = 1;
		int a1,a2,p1,p2;
		final StrategyElement last = heap[size];
		final int p = last.priority;
		final int stop = (--size)>>1;
		while (n<stop)
		{
			a1 = n<<1;
			a2 = a1|1;
			p1 = heap[a1].priority;
			p2 = heap[a2].priority;
			if (p1>=p2)
			{
				if (p<p1)
				{
					heap[n] = heap[a1];
					n = a1;
				} else break;
			} else
			{
				if (p<p2)
				{
					heap[n] = heap[a2];
					n = a2;
				} else break;
			}
		}
		if (n==stop)
		{
			a1 = n<<1;
			if (a1==size)
			{
				if (p<heap[a1].priority)
				{
					heap[n] = heap[a1];
					heap[a1] = last;
				} else
				{
					heap[n] = last;
				}
			} else
			{
				a2 = a1|1;
				p1 = heap[a1].priority;
				p2 = heap[a2].priority;
				if (p1>=p2)
				{
					if (p<p1)
					{
						heap[n] = heap[a1];
						heap[a1] = last;
					} else
					{
						heap[n] = last;
					}
				} else
				{
					if (p<p2)
					{
						heap[n] = heap[a2];
						heap[a2] = last;
					} else
					{
						heap[n] = last;
					}
				}
			}
		} else
		{
			heap[n] = last;
		}
		return ret;
	}
}
