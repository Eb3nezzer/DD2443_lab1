/**
 * We implement bottom-up merge sort using parallel streams and lambda functions:
 * 1) We initially break up the array into chunks of length sequential_threshold to be sequentially sorted
 *      a) This is done by generating a stream of chunk indices, which are then mapped through lambda functions to sequentially sort that chunk.
 * 2) We iterate through successively doubling chunk sizes until the entire list is sorted:
 *      a) We generate a stream of chunk indices for a given chunk size, which are then mapped through lambda functions to merge adjacent chunks
 */

import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

public class ParallelStreamSort implements Sorter {
        public final int threads;
        private final static int sequential_threshold = 16;

        public ParallelStreamSort(int threads) {
                this.threads = threads;
        }

        public int getThreads() {
                return threads;
        }

        public void sort(int[] arr) {
                // Base case
                if (arr.length <= 1) {
                        return;
                } else if (arr.length <= sequential_threshold) {
                        // Array smaller than sequential threshold, just sort
                        sequentialSort(arr, 0, arr.length);
                        return;
                }

                // Create a pool to limit thread count (defaults to system available)
                ForkJoinPool pool = new ForkJoinPool(threads);
                // Create work array for merging
                int[] work = new int[arr.length];

                try {
                        // Find number of sequential base lists below threshold
                        int num_sublists = (arr.length + sequential_threshold - 1) / sequential_threshold;
                        // Initially sequentially sort sublists of length sequential_threshold
                        pool.submit(() -> {
                                IntStream.range(0, num_sublists+1)
                                .parallel()
                                .forEach(listIndex -> {
                                        int begin = listIndex * sequential_threshold;
                                        int end = Math.min(begin + sequential_threshold, arr.length);
                                        sequentialSort(arr, begin, end);
                                });
                                
                                return null;
                        }).get();

                        // Sort in parallel the remaining sublists
                        mergeSort(arr, work);
                } catch (Exception e) {
                        throw new RuntimeException("Sorting failed", e);
                } finally {
                        pool.shutdown();
                }
        }

        private void mergeSort(int[] arr, int[] work) {
                // Start with chunks of sequential threshold, then double
                for (int chunk_size = sequential_threshold; chunk_size < arr.length; chunk_size *= 2) {
                        final int current_size = chunk_size;
                        final int next_size = chunk_size * 2;
                
                        // Create a stream of merge operations
                        // Each operation merges two adjacent chunks of currentChunkSize
                        IntStream.range(0, (arr.length + next_size - 1) / next_size)
                                .parallel() 
                                .forEach(chunk_index -> {
                                        // Find indices for the chunks to be merged
                                        int left = chunk_index * next_size;
                                        int mid = Math.min(left + current_size, arr.length);
                                        int right = Math.min(left + next_size, arr.length);
                                        
                                        // Only merge if there are two chunks to merge
                                        if (mid < right) {
                                                merge(arr, work, left, mid, right);
                                        }
                                });
                }
        }

        /**
         * Merge two sublists in hold_arr (and keep there)
         * @param hold_arr Array containing sublists to be merged into
         * @param work_arr Array used as workspace (overwritten)
         * @param begin index of first element of first sublist
         * @param middle index of first element of second sublist
         * @param end exclusive index of last element in second sublist
         */
        public void merge(int[] hold_arr, int[] work_arr, int begin, int middle, int end) {
                // Copy data to work array
                System.arraycopy(hold_arr, begin, work_arr, begin, end - begin);

                // Start indexes at the beginning of sublists
                int i = begin, j = middle;
 
                // Loop through all k elements, placing them in order
                for (int k = begin; k < end; k++) {
                        // if still elements in first sublist and less than second sublist, put in array
                        // otherwise take from second sublist
                        if (i < middle && (j >= end || work_arr[i] <= work_arr[j])) {
                                hold_arr[k] = work_arr[i];
                                i++;
                        } else {
                                hold_arr[k] = work_arr[j];
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