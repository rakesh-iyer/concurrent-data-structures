import java.util.concurrent.atomic.AtomicStampedReference;
import java.util.HashSet;
import java.util.Set;

class ConcurrentLinkedQueue<T> {
    AtomicStampedReference<LinkedNode<T>> qHead;
    AtomicStampedReference<LinkedNode<T>> qTail;

    static class LinkedNode<T> {
        T value;
        AtomicStampedReference<LinkedNode<T>> next;
        LinkedNode() {
            next = new AtomicStampedReference<LinkedNode<T>>(null, 0);
        }
    }

    ConcurrentLinkedQueue() {
        LinkedNode<T> dummy = new LinkedNode<T>();
        qHead = new AtomicStampedReference<LinkedNode<T>>(dummy, 0);
        qTail = new AtomicStampedReference<LinkedNode<T>>(dummy, 0);
    }

	static class LinkedNodeAndCounter<T> {
		LinkedNode<T> ptr;
		int count;

		LinkedNodeAndCounter(LinkedNode<T> ptr, int count) {
			this.ptr = ptr;
			this.count = count;
		}

		boolean equals(LinkedNodeAndCounter<T> o) {
			return o.ptr == ptr && count == o.count;
		}
	}
	
	LinkedNodeAndCounter<T> getLinkedNodeAndCounterAtomically(AtomicStampedReference<LinkedNode<T>> atomicReference) {
		int count[] = new int[1];
        LinkedNode<T> ref = atomicReference.get(count);

		return new LinkedNodeAndCounter<T>(ref, count[0]);
	}

    void enQueue(T value) {
        LinkedNode<T> node = new LinkedNode<>();
        node.value = value;

		LinkedNodeAndCounter<T> tail;
        while (true) {
			tail = getLinkedNodeAndCounterAtomically(qTail);
        	LinkedNodeAndCounter<T> next = getLinkedNodeAndCounterAtomically(tail.ptr.next);

            if (getLinkedNodeAndCounterAtomically(qTail).equals(tail)) {
                if (next.ptr == null) {
					if (tail.ptr.next.compareAndSet(next.ptr, node, next.count, next.count+1)) {
						break;
					}
                } else {
					qTail.compareAndSet(tail.ptr, next.ptr, tail.count, tail.count+1);
                }
            } 
        }
		qTail.compareAndSet(tail.ptr, node, tail.count, tail.count+1);
    } 

    T deQueue() {
		T value;
		while (true) {
			LinkedNodeAndCounter<T> head = getLinkedNodeAndCounterAtomically(qHead);
			LinkedNodeAndCounter<T> tail = getLinkedNodeAndCounterAtomically(qTail);
			LinkedNodeAndCounter<T> next = getLinkedNodeAndCounterAtomically(head.ptr.next);

			if (getLinkedNodeAndCounterAtomically(qHead).equals(head)) {
				if (head.ptr == tail.ptr) {
					if (next.ptr == null) {
						// no entry to return.
						return null;
					}
					qTail.compareAndSet(tail.ptr, next.ptr, tail.count, tail.count+1);
				} else {
					value = next.ptr.value;
					if (qHead.compareAndSet(head.ptr, next.ptr, head.count, head.count+1)) {
						break;
					}
				}
			}
		}
		// need to ensure head.ptr does not have any refs left so it will get gced.
		return value;
	}

	static class QueueTester implements Runnable {
		ConcurrentLinkedQueue<String> queue;
		boolean enQueue;
		int offset;
		QueueTester(ConcurrentLinkedQueue<String> queue, int offset, boolean enQueue) {
			this.queue = queue;
			this.enQueue = enQueue;
			this.offset = offset;
		}

		public void run() {
			System.out.println("Thread running");
			int i = 0;
			int prev = -1;
			while (true) {
				if (enQueue) {
					queue.enQueue(String.valueOf(offset+i));
					i++;
				} else {
					String str;
					Set<Integer> receivedSet = new HashSet<>();
					while ((str = queue.deQueue()) != null) {
						boolean dup = receivedSet.add(Integer.valueOf(str));
						if (!dup) {
							System.out.println("Duplicate received");
							return;
						}

						if (receivedSet.size() % 10000 == 0) {
							System.out.println("Fine upto " + receivedSet.size());
						}
					}
				}
			}
		}
	}

    public static void main(String args[]) throws InterruptedException {
        ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
        // test data.
		Thread sender1 = new Thread(new QueueTester(queue, 1000000, true));
		Thread sender2 = new Thread(new QueueTester(queue, 0, true));
		Thread receiver = new Thread(new QueueTester(queue, 0, false));

		sender1.start();
		sender2.start();
		receiver.start();

		sender1.join();
		sender2.join();
		receiver.join();
    }
}
