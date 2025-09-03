import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MainA {

	public static class Philosopher implements Runnable {
		private Lock leftChopstick;
		private Lock rightChopstick;
		private int seat;
		

		public Philosopher(int seat, Lock leftChopstick, Lock rightChopstick) {
			this.seat = seat;
			this.leftChopstick = leftChopstick;
			this.rightChopstick = rightChopstick;
		}

		public void run() {
			while (true) {
				// Think for a bit
				System.out.println("Philosopher " + seat + " is thinking " +" at time " + System.nanoTime());
				try {
                    Thread.sleep((long) (Math.random() * 1000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

				// Attempt to pick up both chopsticks
				leftChopstick.lock();
				System.out.println("Philosopher " + seat + " has chopstick " + seat + " at time " + System.nanoTime());
				rightChopstick.lock();
				System.out.println("Philosopher " + seat + " has chopstick " + (seat+1) +" at time " + System.nanoTime());

				// Once have gotten both, start eating
				System.out.println("Philosopher " + seat + " is eating" +" at time " + System.nanoTime());
				try {
                    Thread.sleep((long) (Math.random() * 1000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

				// Once finished eating, place both chopsticks down
				leftChopstick.unlock();
				rightChopstick.unlock();
				System.out.println("Philosopher " + seat + " has released their chopsticks" +" at time " + System.nanoTime());
			}
		}
	}

	public static void main(String [] args) throws InterruptedException {
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
		
		// Wait for all to finish
		for (Thread p : philosophers) {
			p.join();
		}

		System.out.println("All philsophers have finished eating");
	}
}
