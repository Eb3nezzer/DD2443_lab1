public class Main {
	private static int buffer_size = 1_000_000;
	private static volatile Buffer buffer = new Buffer(buffer_size);

	public static class Producer implements Runnable {
		public void run() {
			for (int i = 0; i<1_000_000; i++) {
				try {
					buffer.add(i);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			buffer.close();
		}
	}

	public static class Consumer implements Runnable {
		public void run() {
			try {
				while (true) {
					try {
						int value = buffer.remove();
						System.out.println(value);
					} catch (IllegalStateException e) {
						// Buffer is closed and empty - exit normally
						break;
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println("Buffer is now closed, exiting");
		}
	}

	public static void main(String [] args) throws InterruptedException  {
		int n = 2; // number of threads
        Thread[] threads = new Thread[n];

        // Create threads
		threads[0] = new Thread(new Producer());
		threads[1] = new Thread(new Consumer());

        // Start threads
        for (Thread t : threads) {
            t.start();
        }

        // Wait for all to finish
        for (Thread t : threads) {
            t.join();
        }
	}
}
