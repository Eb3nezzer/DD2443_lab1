import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

public class MainD {
	private static volatile int sharedInt = 0;
	private static volatile boolean done = false;
	private static long inc_done = 0;
	private static long pri_start = 0;

	public static class IncrementerBusy implements Runnable {
		public void run() {
			for (int j = 0; j < 1_000_000; j++) {
            	sharedInt++;
        	}
			inc_done = System.nanoTime();
			done = true;
		}
	}

	public static class PrinterBusy implements Runnable {
		public void run() {
			while (!done) {}
			pri_start = System.nanoTime();
		}
	}

	public static class IncrementerGuarded implements Runnable {
		public void run() {
			for (int j = 0; j < 1_000_000; j++) {
            	sharedInt++;
        	}
			synchronized(MainD.class) {
				done = true;
				inc_done = System.nanoTime();
				MainD.class.notify();
			}
			
		}
	}

	public static class PrinterGuarded implements Runnable {
		public void run() {
			synchronized(MainD.class) {
				while (!done) {
					try {
						MainD.class.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			pri_start = System.nanoTime();
		}
	}

	static long run_experiments(int num_rounds, boolean guarded) throws InterruptedException{
		long[] intervals = new long[num_rounds];
	
		for (int i = 0; i < num_rounds; i++) {
			// Reset state for each experiment
			sharedInt = 0;
            done = false;
            inc_done = 0;
            pri_start = 0;
            
			// Create threads
			Thread[] threads = new Thread[2];
			if (guarded) {
				threads[0] = new Thread(new IncrementerGuarded());
				threads[1] = new Thread(new PrinterGuarded());
			} else {
				threads[0] = new Thread(new IncrementerBusy());
				threads[1] = new Thread(new PrinterBusy());
			}

			// Start threads
			for (Thread t : threads) {
				t.start();
			}
			
			// Wait for all to finish
			for (Thread t : threads) {
				t.join();
			}

			intervals[i] = pri_start - inc_done;
		}
		return (Arrays.stream(intervals).sum())/num_rounds;
	}

	public static void main(String[] args) {
		int num_rounds = 0;
		if (args.length != 1) {
			System.out.println("No number of rounds supplied. Using default value of 10.");
			num_rounds = 10;
		} else {
			num_rounds = Integer.parseInt(args[0]);
		}

		String fileName = "task2d_test.dat";
		try (PrintWriter out = new PrintWriter(new FileWriter(fileName))) {
			// warmup phase
			long ave_interval_busy = run_experiments(10, false);
			long ave_interval_guarded = run_experiments(10, true);
			// measurement phase
			ave_interval_busy = run_experiments(num_rounds, false);
			ave_interval_guarded = run_experiments(num_rounds, true);
			// write to file
			out.printf("BusyWait\t%d%n", ave_interval_busy);
			out.printf("Guarded\t%d%n", ave_interval_guarded);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
			e.printStackTrace();
		}
    }
}
