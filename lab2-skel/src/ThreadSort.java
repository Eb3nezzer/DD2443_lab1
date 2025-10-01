/**
 * Sort using Java's Thread, Runnable, start(), and join().
 * 
 * We use bottom-up merge sort. On input arr[] with n elements:
 * 1) A work array is allocated. The arr[] list is split into chunks of sequential_threshold elements.
 * 2) Each thread sequentially sorts a sequential_threshold element list in place. All chunks are sorted.
 * 3) We then have n/sequential_threshold sorted sublists. The first two sublists are merged, the second two are merged, and so on.
 * 4) We then have n/(2*sequential_threshold) sorted sublists. The first two sublists are merged, the second two are merged, and so on.
 * 5) This continues at each level until only two sublists are remaining, which are finally merged.
 */

public class ThreadSort implements Sorter {
    public final int threads;
    public final int sequential_threshold;

    public ThreadSort(int threads, int sequential_threshold) {
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

        try {
            // Initial round of sequential sorts (in-place in arr)
            int sequential_len = sequential_threshold;
            int num_sublists = (arr.length + sequential_len - 1) / sequential_len;
            
            // Create threads for initial sequential sorting
            Thread[] avail_threads = new Thread[Math.min(threads, num_sublists)];
            int chunks_per_thread = (num_sublists + threads - 1) / threads;
            
            for (int t = 0; t < avail_threads.length; t++) {
                final int index = t;
                // Manually allocate tasks to threads
                avail_threads[t] = new Thread(() -> {
                    int start_chunk = index * chunks_per_thread;
                    int end_chunk = Math.min(start_chunk + chunks_per_thread, num_sublists);
                    
                    // Loop through allocated sublists in chunk
                    for (int i = start_chunk; i < end_chunk; i++) {
                        int begin = i * sequential_len;
                        int end = Math.min(begin + sequential_len, arr.length);
                        
                        // Sort this chunk in place using sequential merge sort
                        sequentialMergeSort(arr, work, begin, end);
                    }
                });
                // Start this thread
                avail_threads[t].start();
            }

            // Wait for all initial sequential sorts to complete
            for (Thread thread : avail_threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            

            // Now perform bottom-up merging
            int current_size = sequential_len;
            boolean use_work_as_input = false; // arr has sorted chunks, work is scratch space
            
            while (current_size < arr.length) {
                int[] input_arr = use_work_as_input ? work : arr;
                int[] output_arr = use_work_as_input ? arr : work;
                
                // Calculate number of merges
                int next_size = current_size * 2;
                int num_merges = (arr.length + next_size - 1) / next_size;

                // Create threads for merging
                Thread[] merge_threads = new Thread[Math.min(threads, num_merges)];
                int merges_per_thread = (num_merges + threads - 1) / threads;
                
                for (int t = 0; t < merge_threads.length; t++) {
                    final int thread_index = t;
                    final int sublist_size = current_size;
                    
                    merge_threads[t] = new Thread(() -> {
                        int task_count = 0;
                        int target_start = thread_index * merges_per_thread;
                        int target_end = Math.min(target_start + merges_per_thread, num_merges);
                        
                        for (int left = 0; left < arr.length && task_count < target_end; left += 2 * sublist_size) {
                            if (task_count >= target_start) {
                                int right = Math.min(left + sublist_size, arr.length);
                                int end = Math.min(left + 2 * sublist_size, arr.length);
                                
                                if (right < arr.length) {
                                    // Merge two sublists
                                    Worker worker = new Worker(input_arr, output_arr, left, right, end);
                                    worker.run();
                                } else {
                                    // Copy remaining elements
                                    System.arraycopy(input_arr, left, output_arr, left, right - left);
                                }
                            }
                            task_count++;
                        }
                    });
                    merge_threads[t].start();
                }
                
                // Wait for all merges at this level to complete
                for (Thread thread : merge_threads) {
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                
                current_size = next_size;
                use_work_as_input = !use_work_as_input;
            }
            
            // If the final result is in work, transfer to arr
            if (use_work_as_input) {
                System.arraycopy(work, 0, arr, 0, arr.length);
            }
            
        } catch (Exception e) {
            // Handle any unexpected exceptions
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Sequential merge sort - sorts arr[begin..end) using merge sort
     * Uses work as temporary space
     */
    private void sequentialMergeSort(int[] arr, int[] work, int begin, int end) {
        // Base case
        if (end - begin <= 1) {
            return;
        }
        
        int middle = (begin + end) / 2;
        
        // Recursively sort both halves
        sequentialMergeSort(arr, work, begin, middle);
        sequentialMergeSort(arr, work, middle, end);
        
        // Merge the two sorted halves
        int i = begin, j = middle, k = begin;
        
        // Merge into work array
        while (i < middle && j < end) {
            if (arr[i] <= arr[j]) {
                work[k++] = arr[i++];
            } else {
                work[k++] = arr[j++];
            }
        }
        
        // Copy remaining elements
        while (i < middle) {
            work[k++] = arr[i++];
        }
        while (j < end) {
            work[k++] = arr[j++];
        }
        
        // Copy back to arr
        System.arraycopy(work, begin, arr, begin, end - begin);
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


}