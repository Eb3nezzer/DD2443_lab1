public class MainA {
	private static volatile int sharedInt = 0;

	public static class Incrementer implements Runnable {
		public void run() {
			for (int j = 0; j < 1_000_000; j++) {
            	sharedInt++;
        	}
		}
	}

	public static class Printer implements Runnable {
		public void run() {
			System.out.println(sharedInt);
		}
	}

	public static void main(String [] args) throws InterruptedException {
		int n = 2; // number of threads
        Thread[] threads = new Thread[n];

        // Create threads
		threads[0] = new Thread(new Incrementer());
		threads[1] = new Thread(new Printer());

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
