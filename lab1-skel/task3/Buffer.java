import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Buffer {
	final Lock lock = new ReentrantLock();
	final java.util.concurrent.locks.Condition notFull = lock.newCondition();
	final Condition notEmpty = lock.newCondition();

	final int[] ints;
	int add_index = 0, remove_index = 0;
	int count = 0;
	final int capacity;

	boolean closed = false;

	public Buffer(int size) {
		ints = new int[size];
		capacity = size;
	}

	void add(int i) throws InterruptedException {
		// claim the lock
		lock.lock();
		try {
			// check that there are spaces available, if not wait on condition
			while (count == capacity && !closed) {
				notFull.await();
			}

			// check that the buffer is not closed
			if (closed) {
				throw new IllegalStateException("Buffer is closed");
			}

			// add item to buffer
			ints[add_index] = i;
			add_index = (add_index + 1) & capacity;
			count++;

			// signal that buffer is not empty
			notEmpty.signal();
		} finally {
			// release the lock
			lock.unlock();
		}
	}

	public int remove() throws InterruptedException {
		// claim the lock
		lock.lock();
		try {
			// check that there are items available, if not wait on condition
			while (count == 0 && !closed) {
				notEmpty.await();
			}

			// check if buffer is closed and empty
			if (closed && count == 0) {
				throw new IllegalStateException("Buffer is closed and empty");
			}

			// take item from buffer
			int obj = ints[remove_index];
			remove_index = (remove_index + 1) % capacity;
			count--;

			// signal that buffer is not full
			notFull.signal();

			return obj;
		} finally {
			// release the lock
			lock.unlock();
		}
	}

	public void close() {
		// claim the lock
		lock.lock();
		try {
			// check that the buffer is not already closed
			if (closed) {
				throw new IllegalStateException("Buffer already closed");
			} else {
				closed = true;
			}
		} finally {
			// release the lock
			lock.unlock();
		}

	}
}
