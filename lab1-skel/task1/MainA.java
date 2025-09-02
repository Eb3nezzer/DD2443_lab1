public class MainA {
	private static volatile int sum = 0; 

	public static class Incrementer implements Runnable {
		public void run() {
			for (int j = 0; j < 1_000_000; j++) {
            	sum++;
        	}
		}
	}

	public static void main(String [] args) throws InterruptedException {
        int n = 4; // number of threads
        Thread[] threads = new Thread[n];

        // Create threads
        for (int i = 0; i < n; i++) {
            threads[i] = new Thread(new Incrementer());
        }

        // Start threads
        for (Thread t : threads) {
            t.start();
        }

        // Wait for all to finish
        for (Thread t : threads) {
            t.join();
        }

        // Print result
        System.out.println(sum);
    }
}
