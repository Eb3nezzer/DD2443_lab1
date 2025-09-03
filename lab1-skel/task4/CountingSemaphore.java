public class CountingSemaphore {
	private volatile int n = 0;

	public CountingSemaphore(int n) {
		this.n = n; // initial number of resources
	}

	public synchronized void signal() {
		n++;
		if (n <= 0) {
			notify();
		}
	}

	public synchronized void s_wait() throws InterruptedException{
		n--;
		if (n < 0) {
			while (true) {
				try{
					wait();
					break;
				} catch (InterruptedException e) {
					// check if the condition in the original if statement is still true after spurious wakeup
                    if (n < 0) {
                        continue;
                    }
                    throw e;
				}
			}
		} 
	}
}
