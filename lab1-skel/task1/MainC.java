import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class MainC {
	private static volatile int sum = 0;
	public static class Incrementer implements Runnable {
		public void run() {
			for (int j = 0; j < 10; j++) {
                synchronized(MainC.class){
            	    sum++;
                }
        	}
		}
	}

	long run_experiments(int n) throws InterruptedException{
        Thread[] threads = new Thread[n];

        // Create threads
        for (int i = 0; i < n; i++) {
            threads[i] = new Thread(new Incrementer());
        }
        
		long startTime = System.nanoTime();
	
        // Start threads
        for (Thread t : threads) {
            t.start();
        }
        
        // Wait for all to finish
        for (Thread t : threads) {
            t.join();
        }
        
        long endTime = System.nanoTime();

        // Print result
        System.out.println(sum);
        sum = 0;

		return endTime - startTime;
	}

	public static void main(String [] args) {
        String fileName = "task1c_test.dat";
        int X = 2;
        int Y = 1;

        MainC program = new MainC(); // create an instance
        try (PrintWriter out = new PrintWriter(new FileWriter(fileName))) {
            for (int n = 1; n <= 64; n = n*2 ){
                // warmup phase
                for (int i = 0; i < X; i++){
                    try {
                        program.run_experiments(n);
                    }catch (InterruptedException e) {
                        e.printStackTrace();
                    }   
                }
                // measurement phase
                for (int i = 0; i < Y; i++){
                    try {
                        long timeTaken = program.run_experiments(n);
                        out.printf("%d\t%d%n", n, timeTaken);
                        // System.out.println("Time taken (ns): " + timeTaken);
                    }catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }catch (IOException e) {
            e.printStackTrace();
        }

        
	}
}
