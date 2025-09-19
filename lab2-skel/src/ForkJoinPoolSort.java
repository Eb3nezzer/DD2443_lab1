/**
 * Sort using Java's ForkJoinPool.
 *  
 * We use top-down merge sort with fork-join parallelism:
 * 1) A work array B[] is allocated. 
 * 2) The input list is recursively divided until sublists are below some threshold. These sublists are sorted sequentially.
 * 3) Then the larger sublists are split and processed in parallel using fork-join.
 * 4) The sublists are merged together recursively until the entire list is merged.
 */

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class ForkJoinPoolSort implements Sorter {
        private final int threads;
        private final static int sequential_threshold = 16;

        public ForkJoinPoolSort(int threads) {
                this.threads = threads;
        }

        public void sort(int[] arr) {
                if (arr.length <= 1) {
                return;
                }

                // Create work array
                int[] work = new int[arr.length];
                
                // Create ForkJoinPool and execute the sort task
                ForkJoinPool pool = new ForkJoinPool(threads);
                try {
                        MergeSortTask task = new MergeSortTask(arr, work, 0, arr.length, true);
                        pool.invoke(task);
                } finally {
                        pool.shutdown();
                }

                // We have sorted into work array, copy across
                System.arraycopy(work, 0, arr, 0, arr.length);
        }

        public int getThreads() {
                return threads;
        }
        
        /**
         * RecursiveAction class to specify recurivsive thread task.
         */
        private static class MergeSortTask extends RecursiveAction {
                private final int[] src;      // Source array
                private final int[] dest;     // Destination array
                private final int begin;        // Start index (inclusive)
                private final int end;       // End index (exclusive)
                private final boolean srcToDest; // Direction of sort (src->dest or dest->src)

                MergeSortTask(int[] src, int[] dest, int begin, int end, boolean srcToDest) {
                        this.src = src;
                        this.dest = dest;
                        this.begin = begin;
                        this.end = end;
                        this.srcToDest = srcToDest;
                }

                @Override
                protected void compute() {
                        int length = end - begin;
                        
                        // Base case: use sequential sort for small arrays
                        if (length <= sequential_threshold) {
                                if (srcToDest) {
                                        // Sorting src array into dest array
                                        // Copy the data from src to dest, then sort in place
                                        System.arraycopy(src, begin, dest, begin, length);
                                        sequentialSort(dest, begin, end);
                                } else {
                                        // Sorting dest array into src, so can write over
                                        sequentialSort(src, begin, end);
                                }
                                return;
                        }

                        // Else need to parallel sort. Divide into two sublists.
                        int mid = begin + (end - begin) / 2;
                        MergeSortTask leftTask = new MergeSortTask(src, dest, begin, mid, !srcToDest);
                        MergeSortTask rightTask = new MergeSortTask(src, dest, mid, end, !srcToDest);

                        // Hand off left sublist, continue with right sublist
                        leftTask.fork();
                        rightTask.compute(); 
                        leftTask.join();      // Wait for left sublist to complete

                        // Combine: merge the sorted halves
                        if (srcToDest) {
                                merge(src, dest, begin, mid, end);
                        } else {
                                merge(dest, src, begin, mid, end);
                        }
                }
        }

        /**
         * Sequentially merge two sublists in input_arr to output_arr
         * @param input_arr Array containing sublists
         * @param output_arr Array to merge into
         * @param begin index of first element of first sublist
         * @param middle index of first element of second sublist
         * @param end exclusive index of last element in second sublist
         */
        private static void merge(int[] input_arr, int[] output_arr, int begin, int middle, int end) {
                // Start indexes at the beginning of sublists
                int i = begin, j = middle;
 
                // Loop through all k elements, placing them in order
                for (int k = begin; k < end; k++) {
                        // if still elements in first sublist and less than second sublist, put in array
                        // otherwise take from second sublist
                        if (i < middle && (j >= end || input_arr[i] <= input_arr[j])) {
                                output_arr[k] = input_arr[i];
                                i++;
                        } else {
                                output_arr[k] = input_arr[j];
                                j++;
                        }
                }
        }

        /**
         * Sequential sort for small arrays
         */
        private static void sequentialSort(int[] arr, int begin, int end) {
                for (int i = begin + 1; i < end; i++) {
                        int key = arr[i];
                        int j = i - 1;
                        
                        // Move elements greater than key one position ahead
                        while (j >= begin && arr[j] > key) {
                                arr[j + 1] = arr[j];
                                j--;
                        }
                        arr[j + 1] = key;
                }
        }
}