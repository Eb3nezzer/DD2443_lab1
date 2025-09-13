/**
 * Sort using Java's Thread, Runnable, start(), and join().
 */

public class ThreadSort implements Sorter {
        public final int threads;

        public ThreadSort(int threads) {
                this.threads = threads;
        }

        public void sort(int[] arr) {
                // TODO: sort arr.
        }

        public int getThreads() {
                return threads;
        }

        private static class Worker implements Runnable {
                Worker() {
                }

                public void run() {
                }
        }
}
