import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

// An implementation of a lock-free unbounded queue derived from Section 10.5 of HSLS.
// The main structure and enq()/deq() methods are copied directly from the book.
// toArray() is used to extract the nodes to an array structure for log validation

public class LockFreeQueue<T> {
    AtomicReference<Node> head, tail;
    public LockFreeQueue() {
        Node node = new Node(null);
        head = new AtomicReference<>(node);
        tail = new AtomicReference<>(node);
    }

    public class Node {
        public T value;
        public AtomicReference<Node> next;
        public Node(T value) {
            this.value = value;
            next = new AtomicReference<Node>(null);
        }
    }

    public void enq(T value) {
        // Construct new node to insert
        Node node = new Node(value);
        while (true) {
            // Get current last tail and subsequent node
            Node last = tail.get();
            Node next = last.next.get();
            if (last == tail.get()) {
                // If last is still the current tail, check what next is
                if (next == null) {
                    // Try to link new node at the end
                    if (last.next.compareAndSet(null, node)) {
                        // Successfully linked; try to move tail to the new node
                        tail.compareAndSet(last, node);
                        return;
                    }
                    // If linking failed, loop and retry
                } else {
                    // Another thread already inserted after last; help advance tail and retry
                    tail.compareAndSet(last, next);
                }
            }
            // If last is not still current tail, loop and retry.
        }
    }

    public T deq() throws IllegalStateException {
        while (true) {
            // Get sentinel nodes and the first actual node
            Node first = head.get();
            Node last = tail.get();
            Node next = first.next.get();
            if (first == head.get()) {
                // If the first node is still the first node, check whether some node exists
                if (first == last) {
                    // No actual nodes, so queue is empty
                    if (next == null) {
                        throw new IllegalStateException();
                    }
                    tail.compareAndSet(last, next);
                } else {
                    // There exists some node, take and remove from queue.
                    T value = next.value;
                    if (head.compareAndSet(first, next)) {
                        return value;
                    }
                }
            }
        }
    }

    public T[] toArray(T[] a) {
         // Start at the first node, add it to the array list, iterate through until reach the tail node
        ArrayList<T> store = new ArrayList<T>();
        Node curr = head.get().next.get();
        while (curr != null) {
            store.add(curr.value);
            curr = curr.next.get();
        }
        return store.toArray(a);
    }

    public void clear() {
        Node newNode = new Node(null);
        while (true) {
            Node currHead = head.get();
            Node currTail = tail.get();
            // Try to update tail, but don't fail if it doesn't work
            if (head.compareAndSet(currHead, newNode)) {
                tail.compareAndSet(currTail, newNode);
                return;
            }
        }
    }
}
