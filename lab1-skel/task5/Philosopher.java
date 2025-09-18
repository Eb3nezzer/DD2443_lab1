import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Philosopher implements Runnable {
        private final int id;
        private final Lock firstChopstick;
        private final Lock secondChopstick;

        public Philosopher(int id, Lock leftChopstick, Lock rightChopstick) {
            this.id = id;
			this.firstChopstick = leftChopstick;
			this.secondChopstick = rightChopstick;
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