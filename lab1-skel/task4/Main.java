public class Main {

    public static class Runner implements Runnable {
        private final CountingSemaphore sem;
        private final int id;

        public Runner(CountingSemaphore sem, int id) {
            this.sem = sem;
            this.id = id;
        }

        public void run() {
            try {
                // Thread tries to get resource
                sem.s_wait();

                System.out.println("Thread number " + id + " entering critical section");

                // simulate some work
                Thread.sleep(500);

                System.out.println("Thread " + id + " leaving critical section");

                sem.signal(); // after the thread is done it releases one resource
            } catch (InterruptedException e) {
                System.out.println("Thread " + id + " interrupted");
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int counterValue = 0;   // number of allowed concurrent threads
        int numberOfThreads = 10; // number of created threads

        CountingSemaphore sem = new CountingSemaphore(counterValue);

        Thread[] threads = new Thread[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            threads[i] = new Thread(new Runner(sem, i));
            threads[i].start();
        }

        for (Thread t : threads) {
            t.join();
        }

        System.out.println("All threads finished");
    }
}