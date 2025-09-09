import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

public class MainC {
	private static volatile int sum = 0;
	public static class Incrementer implements Runnable {
		public void run() {
			for (int j = 0; j < 1000000; j++) {
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

        sum = 0;

		return endTime - startTime;
	}

	public static void main(String [] args) {
        String fileName = "task1c_test.dat";
        int X = 3;
        int Y = 3;
        double[] intervals = new double[Y];

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
                        double timeTaken = program.run_experiments(n);
                        intervals[i] = timeTaken;
                    }catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                double stdDev = 0;
                double mean = Arrays.stream(intervals).average().orElse(0.0);
                double variance = 0.0;
                for (double t : intervals) {
                    variance += Math.pow(t - mean, 2);
                }
                variance /= (Y - 1); // sample std deviation
                stdDev = Math.sqrt(variance);
                System.out.println("The mean for " + n + " threads is: " + mean);
                System.out.println("The standard deviation for " + n + " threads is " + stdDev);
                
                out.printf("%d\t%.2f\t%.2f%n", n, mean, stdDev);
            }
        }catch (IOException e) {
            e.printStackTrace();
        }

        
	}
}
