/**
 * Sort using Java's ExecutorService.
 * 
 * We use bottom-up merge sort. On input arr[] with n elements:
 * 1) A work array is allocated. The arr[] list is split into chunks of sequential_threshold elements.
 * 2) Each thread sequentially sorts a chunk into work[]. All chunks are sorted.
 * 3) We then have n/sequential_threshold sorted sublists. The first two sublists are merged, the second two are merged, and so on.
 * 4) We then have n/(2*sequential_threshold) sorted sublists. The first two sublists are merged, the second two are merged, and so on.
 * 5) This continues at each level until only two sublists are remaining, which are finally merged.
 */

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Callable;

public class ExecutorServiceSort implements Sorter {
    public final int threads;
    public final int sequential_threshold;

    public ExecutorServiceSort(int threads, int sequential_threshold) {
        this.threads = threads;
        this.sequential_threshold = sequential_threshold;
    }

    public int getThreads() {
        return threads;
    }

    public void sort(int[] arr) {
        // Base case
        if (arr.length <= 1) {
            return;
        }

        // Allocate work array
        int[] work = new int[arr.length];

        // Initialize executor service once at the beginning
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        try {
            // Initial round of sequential sorts directly into work[]
            int sequential_len = sequential_threshold;
            int num_sublists = (arr.length + sequential_len - 1) / sequential_len;
            
            List<Callable<Void>> tasks = new ArrayList<>();
            
            for (int i = 0; i < num_sublists; i++) {
                final int start_ind = i * sequential_len;
                final int end_ind = Math.min(start_ind + sequential_len, arr.length);
                
                Callable<Void> task = () -> {
                    // Sort sublists sequentially directly from arr into work
                    sequential_sort(arr, work, start_ind, end_ind);
                    return null;
                };
                tasks.add(task);
            }

            // Wait for all initial sequential sorts to complete using invokeAll
            try {
                pool.invokeAll(tasks);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            tasks.clear();

            // Now perform bottom-up merging
            int current_size = sequential_len;
            boolean use_work_as_input = true; // We start with work as input since sequential sorts wrote to work
            
            while (current_size < arr.length) {
                int[] input_arr = use_work_as_input ? work : arr;
                int[] output_arr = use_work_as_input ? arr : work;
                
                // Merge adjacent sublists of current_size
                for (int left = 0; left < arr.length; left += 2 * current_size) {
                    int right = Math.min(left + current_size, arr.length);
                    int end = Math.min(left + 2 * current_size, arr.length);
                    
                    // Only merge if there's a right sublist
                    if (right < arr.length) {
                        final int final_left = left;
                        final int final_right = right;
                        final int final_end = end;
                        final int[] final_input_arr = input_arr;
                        final int[] final_output_arr = output_arr;
                        
                        Callable<Void> task = () -> {
                            mergeSublists(final_input_arr, final_output_arr, final_left, final_right, final_end);
                            return null;
                        };
                        tasks.add(task);
                    } else {
                        // Copy remaining elements if there's no right sublist to merge
                        final int final_left = left;
                        final int final_right = right;
                        final int[] final_input_arr = input_arr;
                        final int[] final_output_arr = output_arr;
                        
                        Callable<Void> task = () -> {
                            System.arraycopy(final_input_arr, final_left, final_output_arr, final_left, final_right - final_left);
                            return null;
                        };
                        tasks.add(task);
                    }
                }
                
                // Wait for all merges at this level to complete using invokeAll
                try {
                    pool.invokeAll(tasks);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                tasks.clear();
                
                current_size *= 2;
                use_work_as_input = !use_work_as_input;
            }
            
            // If the final result is in work array, copy it back to arr
            if (use_work_as_input) {
                System.arraycopy(work, 0, arr, 0, arr.length);
            }
            
        } finally {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Merge two adjacent sublists
     */
    private void mergeSublists(int[] input_arr, int[] output_arr, int left, int right, int end) {
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

    /**
     * Sequential merge sort that sorts from input array to output array
     */
    private void sequential_sort(int[] input, int[] output, int begin, int end) {
        int n = end - begin;
        
        if (n <= 1) {
            // Base case: single element, just copy
            if (n == 1) {
                output[begin] = input[begin];
            }
            return;
        }
        
        // For small subarrays, use iterative bottom-up merge sort directly from input to output
        for (int size = 1; size < n; size *= 2) {
            for (int left = begin; left < end; left += 2 * size) {
                int mid = Math.min(left + size, end);
                int right = Math.min(left + 2 * size, end);
                
                // Merge from input to output
                merge(input, output, left, mid, right);
            }
            
            // After each pass, copy from output back to input for next iteration
            // except for the last pass where we want the final result in output
            if (size * 2 < n) {
                System.arraycopy(output, begin, input, begin, n);
            }
        }
    }
    
    /**
     * Helper method for sequential merge sort - merge two sorted subarrays from input to output
     */
    private void merge(int[] input, int[] output, int left, int mid, int right) {
        int i = left, j = mid, k = left;
        
        while (i < mid && j < right) {
            if (input[i] <= input[j]) {
                output[k++] = input[i++];
            } else {
                output[k++] = input[j++];
            }
        }
        
        while (i < mid) {
            output[k++] = input[i++];
        }
        
        while (j < right) {
            output[k++] = input[j++];
        }
    }
}