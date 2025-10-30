import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Comparator;

public class GlobalLogSkipList<T extends Comparable<T>> implements LockFreeSet<T> {
    /* Number of levels */
    private static final int MAX_LEVEL = 16;

    private final Node<T> head = new Node<T>();
    private final Node<T> tail = new Node<T>();
    
    // Global lock-free log using ConcurrentLinkedQueue
    private final ConcurrentLinkedQueue<Log.Entry> globalLog;

    public GlobalLogSkipList() { 
        for (int i = 0; i < head.next.length; i++) {
            head.next[i] = new AtomicMarkableReference<GlobalLogSkipList.Node<T>>(tail, false);
        }
        
        // Initialise global lock-free log
        globalLog = new ConcurrentLinkedQueue<>();
    }

    private static final class Node<T> {
        private final T value;
        private final AtomicMarkableReference<Node<T>>[] next;
        private final int topLevel;
        // Track linearisation timestamp for this node's removal
        private volatile long removalTimestamp = -1;

        @SuppressWarnings("unchecked")
        public Node() {
            value = null;
            next = (AtomicMarkableReference<Node<T>>[])new AtomicMarkableReference[MAX_LEVEL + 1];
            for (int i = 0; i < next.length; i++) {
                next[i] = new AtomicMarkableReference<Node<T>>(null, false);
            }
            topLevel = MAX_LEVEL;
        }

        @SuppressWarnings("unchecked")
        public Node(T x, int height) {
            value = x;
            next = (AtomicMarkableReference<Node<T>>[])new AtomicMarkableReference[height + 1];
            for (int i = 0; i < next.length; i++) {
                next[i] = new AtomicMarkableReference<Node<T>>(null, false);
            }
            topLevel = height;
        }
    }

    /* Returns a level between 0 to MAX_LEVEL,
     * P[randomLevel() = x] = 1/2^(x+1), for x < MAX_LEVEL.
     */
    private static int randomLevel() {
        int r = ThreadLocalRandom.current().nextInt();
        int level = 0;
        r &= (1 << MAX_LEVEL) - 1;
        while ((r & 1) != 0) {
            r >>>= 1;
            level++;
        }
        return level;
    }

    @SuppressWarnings("unchecked")
    public boolean add(int threadId, T x) {
        int topLevel = randomLevel();
        int bottomLevel = 0;
        Node<T>[] preds = (Node<T>[]) new Node[MAX_LEVEL + 1];
        Node<T>[] succs = (Node<T>[]) new Node[MAX_LEVEL + 1];
        
        while (true) {
            // Store the last curr position for potential unsuccessful add
            FindResult findResult = new FindResult();
            boolean found = find(x, preds, succs, findResult);
            
            if (found) {
                // Unsuccessful add - linearisation point was in find()
                // Use the timestamp from the find operation
                addLogEntry(threadId, Log.Method.ADD, x.hashCode(), false, findResult.timestamp);
                return false;
            } else {
                Node<T> newNode = new Node<T>(x, topLevel);
                for (int level = bottomLevel; level <= topLevel; level++) {
                    Node<T> succ = succs[level];
                    newNode.next[level].set(succ, false);
                }
                Node<T> pred = preds[bottomLevel];
                Node<T> succ = succs[bottomLevel];
                
                // Linearisation point for successful add: capture timestamp immediately before CAS
                long timestamp = System.nanoTime();
                if (pred.next[bottomLevel].compareAndSet(succ, newNode, false, false)) {
                    // CAS succeeded - this is the linearisation point
                    addLogEntry(threadId, Log.Method.ADD, x.hashCode(), true, timestamp);
                } else {
                    // CAS failed, retry
                    continue;
                }

                // Link at higher levels (after linearisation)
                for (int level = bottomLevel + 1; level <= topLevel; level++) {
                    while (true) {
                        pred = preds[level];
                        succ = succs[level];
                        if (pred.next[level].compareAndSet(succ, newNode, false, false))
                            break;
                        find(x, preds, succs, null);
                    }
                }

                return true;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public boolean remove(int threadId, T x) {
        int bottomLevel = 0;
        Node<T>[] preds = (Node<T>[]) new Node[MAX_LEVEL + 1];
        Node<T>[] succs = (Node<T>[]) new Node[MAX_LEVEL + 1];
        Node<T> succ;
        
        while (true) {
            // Store the last curr position for potential unsuccessful remove
            FindResult findResult = new FindResult();
            boolean found = find(x, preds, succs, findResult);
            
            if (!found) {
                // Unsuccessful remove - linearisation point was in find()
                addLogEntry(threadId, Log.Method.REMOVE, x.hashCode(), false, findResult.timestamp);
                return false;
            } else {
                Node<T> nodeToRemove = succs[bottomLevel];
                
                // Mark upper level links
                for (int level = nodeToRemove.topLevel; level >= bottomLevel+1; level--) {
                    boolean[] marked = {false};
                    succ = nodeToRemove.next[level].get(marked);
                    while (!marked[0]) {
                        nodeToRemove.next[level].compareAndSet(succ, succ, false, true);
                        succ = nodeToRemove.next[level].get(marked);
                    }
                }
                
                boolean[] marked = {false};
                succ = nodeToRemove.next[bottomLevel].get(marked);
                
                while (true) {
                    // Capture timestamp immediately before attempting to mark bottom level
                    long timestamp = System.nanoTime();
                    boolean iMarkedIt = nodeToRemove.next[bottomLevel].compareAndSet(succ, succ, false, true);
                    
                    if (iMarkedIt) {
                        // This thread marked it - this is the linearisation point
                        nodeToRemove.removalTimestamp = timestamp;
                        addLogEntry(threadId, Log.Method.REMOVE, x.hashCode(), true, timestamp);
                        // Call find() to clean up
                        find(x, preds, succs, null);
                        return true;
                    } else {
                        // Check if someone else marked it
                        succ = nodeToRemove.next[bottomLevel].get(marked);
                        if (marked[0]) {
                            // Another thread marked it - use their linearisation point
                            long otherThreadTimestamp = nodeToRemove.removalTimestamp;
                            if (otherThreadTimestamp == -1) {
                                // Race condition: use current timestamp as approximation
                                otherThreadTimestamp = timestamp;
                            }
                            addLogEntry(threadId, Log.Method.REMOVE, x.hashCode(), false, otherThreadTimestamp);
                            return false;
                        }
                        // No one marked it yet, retry with new succ
                    }
                }
            }
        }
    }

    public boolean contains(int threadId, T x) {
        int bottomLevel = 0;
        boolean[] marked = {false};
        Node<T> pred = head;
        Node<T> curr = null;
        Node<T> succ = null;
        long lastBottomTimestamp = -1;
        
        for (int level = MAX_LEVEL; level >= bottomLevel; level--) {
            // Capture timestamp when curr is set at bottom level from previous level
            if (level == bottomLevel) {
                curr = pred.next[level].getReference();
                lastBottomTimestamp = System.nanoTime();
            } else {
                curr = pred.next[level].getReference();
            }
            
            while (true) {
                succ = curr.next[level].get(marked);
                while (marked[0]) {
                    // Capture timestamp when curr is updated at bottom level from marking
                    if (level == bottomLevel) {
                        curr = succ;
                        lastBottomTimestamp = System.nanoTime();
                    } else {
                        curr = succ;
                    }
                    
                    succ = curr.next[level].get(marked);
                }
                if (curr.value != null && x.compareTo(curr.value) < 0) {
                    pred = curr;
                    
                    // Capture timestamp when curr is updated at bottom level from traversal
                    if (level == bottomLevel) {
                        curr = succ;
                        lastBottomTimestamp = System.nanoTime();
                    } else {
                        curr = succ;
                    }
                } else {
                    break;
                }
            }
        }
        
        boolean result = curr.value != null && x.compareTo(curr.value) == 0;
        
        // Log using the last captured timestamp
        addLogEntry(threadId, Log.Method.CONTAINS, x.hashCode(), result, lastBottomTimestamp);
        
        return result;
    }

    // Helper class to pass timestamp from find() back to caller
    private static class FindResult {
        long timestamp = -1;
    }

    private boolean find(T x, Node<T>[] preds, Node<T>[] succs, FindResult result) {
        int bottomLevel = 0;
        boolean[] marked = {false};
        boolean snip;
        Node<T> pred = null;
        Node<T> curr = null;
        Node<T> succ = null;
        long lastBottomTimestamp = -1;
        
retry:
        while (true) {
            pred = head;
            for (int level = MAX_LEVEL; level >= bottomLevel; level--) {
                // Capture timestamp when curr is set at bottom level from previous level
                if (level == bottomLevel && result != null) {
                    curr = pred.next[level].getReference();
                    lastBottomTimestamp = System.nanoTime();
                } else {
                    curr = pred.next[level].getReference();
                }
                
                while (true) {
                    succ = curr.next[level].get(marked);
                    while (marked[0]) {
                        snip = pred.next[level].compareAndSet(curr, succ, false, false);
                        if (!snip) continue retry;
                        
                        // Capture timestamp when curr is updated at bottom level during snipping
                        if (level == bottomLevel && result != null) {
                            curr = succ;
                            lastBottomTimestamp = System.nanoTime();
                        } else {
                            curr = succ;
                        }
                        
                        succ = curr.next[level].get(marked);
                    }
                    if (curr.value != null && x.compareTo(curr.value) < 0) {
                        pred = curr;

                        // Capture timestamp when curr is updated at bottom level during traversal
                        if (level == bottomLevel && result != null) {
                            curr = succ;
                            lastBottomTimestamp = System.nanoTime();
                        } else {
                            curr = succ;
                        }

                    } else {
                        break;
                    }
                }
                
                preds[level] = pred;
                succs[level] = curr;
            }
            
            boolean found = curr.value != null && x.compareTo(curr.value) == 0;
            // Return the last captured timestamp from bottom-level curr updates
            if (result != null) {
                result.timestamp = lastBottomTimestamp;
            }
    
            return found;
        }
    }

    private void addLogEntry(int threadId, Log.Method method, int arg, boolean ret, long timestamp) {
        // Add to global lock-free queue
        globalLog.add(new Log.Entry(method, arg, ret, timestamp));
    }

    public Log.Entry[] getLog() {
        // Convert queue to array and sort by timestamp
        List<Log.Entry> entries = new ArrayList<>(globalLog);
        Log.Entry[] sortedLog = entries.toArray(new Log.Entry[0]);
        Arrays.sort(sortedLog, Comparator.comparingLong(e -> e.timestamp));
        
        return sortedLog;
    }

    public void reset() {
        for (int i = 0; i < head.next.length; i++) {
            head.next[i] = new AtomicMarkableReference<GlobalLogSkipList.Node<T>>(tail, false);
        }
        
        // Clear the global log
        globalLog.clear();
    }
}