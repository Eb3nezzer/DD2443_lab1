/**
 * Sort using Java's ExecutorService.
 * 
 * We use bottom-up merge sort. On input arr[] with n elements:
 * 1) A work array B[] is allocated. The arr[] list is split into chunks of 16 elements.
 * 2) Each thread sequentially sorts a 16-chunk list into B[]. All 16-chunks are sorted.
 * 3) We then have n/16 sorted sublists. The first two sublists are merged, the second two are merged, and so on.
 * 4) We then have n/8 sorted sublists. The first two sublists are merged, the second two are merged, and so on.
 * 5) This continues at each level until only two sublists are remaining, which are finally merged.
 */

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ExecutorServiceSort implements Sorter {
        public final int threads;
        public final int sequential_threshold = 16;

        public ExecutorServiceSort(int threads) {
                this.threads = threads;
        }

        public int getThreads() {
                return threads;
        }

        public void sort(int[] arr) {
                // Base case
                if (arr.length <= 1) {
                        return;
                }

                // Allocate work array. Copy is needed due to sequential sort handling.
                int[] work = arr.clone();

                // Initialise executor service
                ExecutorService pool = Executors.newFixedThreadPool(threads);

                try {
                        // Initial round of sequential sorts into work[]
                        int sequential_len = sequential_threshold;
                        int num_sublists = Math.ceilDiv(arr.length, sequential_len);
                        
                        for (int i = 0; i < num_sublists; i++) {
                                int start_ind = i * sequential_len;
                                int end_ind = Math.min(start_ind + sequential_len, arr.length);
                                
                                Runnable runnableTask = () -> {
                                        // Sort sublists sequentially into arr
                                        seq_split(arr, work, start_ind, end_ind);
                                };
                                pool.execute(runnableTask);
                        }

                        // Wait for all initial sequential sorts to complete
                        pool.shutdown();
                        try {
                                pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                        } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                return;
                        }

                        // Now perform bottom-up merging
                        int current_size = sequential_len;
                        boolean use_work_as_input = false; // Toggle between arr and work as input/output
                        
                        while (current_size < arr.length) {
                                pool = Executors.newFixedThreadPool(threads);
                                
                                int[] input_arr = use_work_as_input ? work : arr;
                                int[] output_arr = use_work_as_input ? arr : work;
                                
                                // Merge adjacent sublists of current_size
                                for (int left = 0; left < arr.length; left += 2 * current_size) {
                                        int right = Math.min(left + current_size, arr.length);
                                        int end = Math.min(left + 2 * current_size, arr.length);
                                        
                                        // Only merge if there's a right sublist
                                        if (right < arr.length) {
                                                Worker worker = new Worker(input_arr, output_arr, left, right, end);
                                                pool.execute(worker);
                                        } else {
                                                // Copy remaining elements if there's no right sublist to merge
                                                final int finalLeft = left;
                                                final int finalRight = right;
                                                pool.execute(() -> {
                                                        System.arraycopy(input_arr, finalLeft, output_arr, finalLeft, finalRight - finalLeft);
                                                });
                                        }
                                }
                                
                                // Wait for all merges at this level to complete
                                pool.shutdown();
                                try {
                                        pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                                } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                        return;
                                }
                                
                                current_size *= 2;
                                use_work_as_input = !use_work_as_input;
                        }
                        
                        // If the final result is in work array, copy it back to arr
                        if (use_work_as_input) {
                                System.arraycopy(work, 0, arr, 0, arr.length);
                        }
                        
                } finally {
                        if (!pool.isShutdown()) {
                                pool.shutdown();
                        }
                }
        }
        
        /**
         * Worker class to merge two adjacent sublists in parallel
         */
        private static class Worker implements Runnable {
                int[] input_arr, output_arr;
                int left, right, end;

                Worker(int[] input_arr, int[] output_arr, int left, int right, int end) {
                        this.input_arr = input_arr;
                        this.output_arr = output_arr;
                        this.left = left;
                        this.right = right;
                        this.end = end;
                }
                
                public void run() {
                        int i = left, j = right;
                        
                        // Merge the two sublists
                        for (int k = left; k < end; k++) {
                                // If left sublist is exhausted or right element is smaller
                                if (i >= right || (j < end && input_arr[j] < input_arr[i])) {
                                        output_arr[k] = input_arr[j];
                                        j++;
                                } else {
                                        output_arr[k] = input_arr[i];
                                        i++;
                                }
                        }
                }
        }

        /**
         * Sequentially split hold_arr into 2 sublists, sort both sublists into work_arr, merge both sublists back into hold_arr
         * @param hold_arr Main input array that holds values
         * @param work_arr Work array
         * @param begin index of first element in hold_arr to sort
         * @param end exclusive index of last element in hold_arr to sort
         */
        public void seq_split(int[] hold_arr, int[] work_arr, int begin, int end) {
                // Base case
                if (end-begin <= 1) {
                        return;
                }

                // Split a list with more than two items into sublists
                int middle = (begin + end)/2;

                // Recursively sort both sublists from hold_arr to work_arr
                seq_split(work_arr, hold_arr, begin, middle);
                seq_split(work_arr, hold_arr, middle, end);

                // Merge the resulting sublists back into hold_arr
                seq_merge(work_arr, begin, middle, end, hold_arr);
        }

        /**
         * Sequentially merge two sublists in input_arr to output_arr
         * @param input_arr Array containing sublists
         * @param output_arr Array to merge into
         * @param begin index of first element of first sublist
         * @param middle index of first element of second sublist
         * @param end exclusive index of last element in second sublist
         */
        public void seq_merge(int[] input_arr, int begin, int middle, int end, int[] output_arr) {
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
}