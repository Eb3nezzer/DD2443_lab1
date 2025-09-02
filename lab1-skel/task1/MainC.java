public class MainC {
	private static volatile int sum = 0;
	public static class Incrementer implements Runnable {
		public void run() {
			for (int j = 0; j < 1_000_000; j++) {
                synchronized(MainC.class){
            	    sum++;
                }
        	}
		}
	}

	long run_experiments(int n) throws InterruptedException{
		long startTime = System.nanoTime();
	
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


		long endTime = System.nanoTime();
		return endTime - startTime;
	}

	public static void main(String [] args) {
		MainC program = new MainC(); // create an instance

        try {
            long timeTaken = program.run_experiments(4);
            System.out.println("Time taken (ns): " + timeTaken);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
	}
}
