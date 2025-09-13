/**
 * Sort using Java's ForkJoinPool.
 */

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class ForkJoinPoolSort implements Sorter {
        public final int threads;

        public ForkJoinPoolSort(int threads) {
                this.threads = threads;
        }

        public void sort(int[] arr) {
                // TODO: sort arr.
        }

        public int getThreads() {
                return threads;
        }

        private static class Worker extends RecursiveAction {
                Worker() {
                }

                protected void compute() {
                }
        }
}
