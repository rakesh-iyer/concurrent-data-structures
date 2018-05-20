class ConcurrentLinkedQueue<T> {
    Pointer<T> qHead;
    Pointer<T> qTail;

    class Pointer<T> {
        LinkedNode<T> ptr;
        int count;

        Pointer<T> clone() {
            Pointer<T> pointer = new Pointer<>();
            pointer.ptr = ptr;
            pointer.count = count;
        }

        public boolean equals(Pointer<T> pointer) {
            return pointer.ptr == ptr && pointer.count == count;
        }

        public int hashCode() {
            int prime = 31;
            int result = 1;

            if (ptr != null) {
                result = 31 * result + ptr.hashCode();
            }

            result = 31 * result + count;

            return result;
        }
    }

    class LinkedNode<T> {
        T value;
        Pointer<T> next;
        LinkedNode() {
            next = new Pointer<T>();
        }
    }

    ConcurrentLinkedQueue() {
        dummy = new LinkedNode<T>();
        pointer = new Pointer<T>();
        pointer.ptr = dummy;
        head = pointer;
        tail = pointer;
    }

    void add(T value) {
        LinkedNode<T> node = new LinkedNode<>();
        node.value = value;

        Pointer<T> tail = qTail.clone();
        Pointer<T> next = tail.ptr.next;

        while (true) {
            if (qTail.equals(tail)) {
                if (next.ptr == null) {
                    
                } else {
                }
            } 
        }
        
    } 

    T remove() {
    }

    public static main(String args[]) {
        ConcurrentLinkedQueue queue = new ConcurrentLinkedQueue<String>();
        // test data.
    }
}
