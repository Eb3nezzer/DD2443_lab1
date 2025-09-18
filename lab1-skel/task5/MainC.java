import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MainC {

    public static class Philosopher implements Runnable {
        private final int id;
        private final Lock firstChopstick;
        private final Lock secondChopstick;

        public Philosopher(int id, Lock leftChopstick, Lock rightChopstick, int totalPhilosophers) {
            this.id = id;
            
            // Break circular wait: last philosopher picks up right first
            if (id == totalPhilosophers - 1) {
                this.firstChopstick = rightChopstick;
                this.secondChopstick = leftChopstick;
            } else {
                this.firstChopstick = leftChopstick;
                this.secondChopstick = rightChopstick;
            }
        }

        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    think();
                    eat();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private void think() throws InterruptedException {
            System.out.println("Philosopher " + id + " is thinking");
            Thread.sleep((long) (Math.random() * 1000));
        }

        private void eat() throws InterruptedException {
            // Acquire chopsticks in defined order
            firstChopstick.lock();
            try {
                System.out.println("Philosopher " + id + " picked up first chopstick");
                
                secondChopstick.lock();
                try {
                    System.out.println("Philosopher " + id + " picked up second chopstick");
                    System.out.println("Philosopher " + id + " is eating");
                    Thread.sleep((long) (Math.random() * 1000));
                } finally {
                    secondChopstick.unlock();
                    System.out.println("Philosopher " + id + " put down second chopstick");
                }
            } finally {
                firstChopstick.unlock();
                System.out.println("Philosopher " + id + " put down first chopstick");
            }
        }
    }

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
			chopsticks[i] = new ReentrantLock(true);
		}

		// Create philosophers
		Thread[] philosophers = new Thread[num_philsophers];
		for (int i = 0; i < num_philsophers; i++) {
			Lock leftChopstick = chopsticks[i];
			Lock rightChopstick = chopsticks[(i+1) % num_philsophers];
			philosophers[i] = new Thread(new Philosopher(i, leftChopstick, rightChopstick, num_philsophers));
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