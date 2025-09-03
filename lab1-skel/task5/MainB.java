import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class MainB {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    
    private static String getTimestamp() {
        return LocalTime.now().format(TIME_FORMAT);
    }
    
    private static void timestampedPrint(String message) {
        System.out.println("[" + getTimestamp() + "] " + message);
    }
    
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
                timestampedPrint("Philosopher " + seat + " is thinking");
                try {
                    Thread.sleep((long) (Math.random() * 1000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                
                // Attempt to pick up left chopstick first
                timestampedPrint("Philosopher " + seat + " is trying to acquire left chopstick " + seat);
                leftChopstick.lock();
                timestampedPrint("Philosopher " + seat + " has acquired left chopstick " + seat);
                
                // Attempt to pick up right chopstick
                timestampedPrint("Philosopher " + seat + " is trying to acquire right chopstick " + ((seat + 1) % getNumPhilosophers()));
                rightChopstick.lock();
                timestampedPrint("Philosopher " + seat + " has acquired right chopstick " + ((seat + 1) % getNumPhilosophers()));
                
                // Once have gotten both, start eating
                timestampedPrint("Philosopher " + seat + " is eating");
                try {
                    Thread.sleep((long) (Math.random() * 1000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                
                // Once finished eating, place both chopsticks down
                timestampedPrint("Philosopher " + seat + " is releasing left chopstick " + seat);
                leftChopstick.unlock();
                timestampedPrint("Philosopher " + seat + " is releasing right chopstick " + ((seat + 1) % getNumPhilosophers()));
                rightChopstick.unlock();
                timestampedPrint("Philosopher " + seat + " has released both chopsticks");
            }
        }
    }
    
    private static int numPhilosophers = 5; // Default value, will be updated in main
    
    private static int getNumPhilosophers() {
        return numPhilosophers;
    }
    
    public static void main(String[] args) throws InterruptedException {
        if (args.length != 1) {
            timestampedPrint("No number of philosophers supplied. Using default value of 5.");
            numPhilosophers = 5;
        } else {
            numPhilosophers = Integer.parseInt(args[0]);
        }
        
        timestampedPrint("Starting dining philosophers problem with " + numPhilosophers + " philosophers");
        
        // We create as many chopsticks as there are philosophers
        Lock[] chopsticks = new Lock[numPhilosophers];
        for (int i = 0; i < numPhilosophers; i++) {
            chopsticks[i] = new ReentrantLock();
            timestampedPrint("Created chopstick " + i);
        }
        
        // Create philosophers
        Thread[] philosophers = new Thread[numPhilosophers];
        for (int i = 0; i < numPhilosophers; i++) {
            Lock leftChopstick = chopsticks[i];
            Lock rightChopstick = chopsticks[(i + 1) % numPhilosophers];
            philosophers[i] = new Thread(new Philosopher(i, leftChopstick, rightChopstick));
            timestampedPrint("Created philosopher " + i + " (left chopstick: " + i + ", right chopstick: " + ((i + 1) % numPhilosophers) + ")");
        }
        
        // Start philosophers
        timestampedPrint("Starting all philosophers...");
        for (Thread p : philosophers) {
            p.start();
        }
        
        // Wait for all to finish (this will run indefinitely unless interrupted)
        for (Thread p : philosophers) {
            p.join();
        }
        
        timestampedPrint("All philosophers have finished eating");
    }
}