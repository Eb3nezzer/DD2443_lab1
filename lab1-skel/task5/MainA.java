import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import Philosopher;

public class MainA {

    public static void main(String[] args) throws InterruptedException {
        int num_philsophers = 0;
		if (args.length != 1) {
			System.out.println("No number of philosophers supplied. Using default value of 5.");
			num_philsophers = 5;
		} else {
			num_philsophers = Integer.parseInt(args[0]);
		}

        // We create as many chopsticks as there are philosophers
		Lock[] chopsticks = new Lock[num_philsophers];
		for (int i = 0; i < num_philsophers; i++) {
			chopsticks[i] = new ReentrantLock();
		}

		// Create philosophers
		Thread[] philosophers = new Thread[num_philsophers];
		for (int i = 0; i < num_philsophers; i++) {
			Lock leftChopstick = chopsticks[i];
			Lock rightChopstick = chopsticks[(i+1) % num_philsophers];
			philosophers[i] = new Thread(new Philosopher(i, leftChopstick, rightChopstick));
		}

		// Start philosophers
		for (Thread p : philosophers) {
			p.start();
		}

        // Let them run for a while
        Thread.sleep(30000);

        // Clean shutdown
        for (Thread p : philosophers) {
            p.interrupt();
        }
        
        for (Thread p : philosophers) {
            p.join();
        }

        System.out.println("All philosophers have finished");
    }
}