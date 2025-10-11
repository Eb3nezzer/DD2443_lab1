file://<WORKSPACE>/lab2-skel/src/ExecutorServiceSort.java
### java.util.NoSuchElementException: next on empty iterator

occurred in the presentation compiler.

presentation compiler configuration:


action parameters:
uri: file://<WORKSPACE>/lab2-skel/src/ExecutorServiceSort.java
text:
```scala
/**
 * Sort using Java's ExecutorService.
 * 
 * We use bottom-up merge sort. On input arr[] with n elements:
 * 1) A work array is allocated. The arr[] list is split into chunks of 16 elements.
 * 2) Each thread sequentially sorts a 16 element list into work[]. All 16-chunks are sorted.
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
                        int num_sublists = (arr.length + sequential_len - 1) / sequential_len;
                        
                        for (int i = 0; i < num_sublists; i++) {
                                int start_ind = i * sequential_len;
                                int end_ind = Math.min(start_ind + sequential_len, arr.length);
                                
                                Runnable runnable_task = () -> {
                                        // Sort sublists sequentially into arr
                                        sequential_sort(arr, start_ind, end_ind);
                                };
                                pool.execute(runnable_task);
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
                                                final int final_left = left;
                                                final int final_right = right;
                                                pool.execute(() -> {
                                                        System.arraycopy(input_arr, final_left, output_arr, final_left, final_right - final_left);
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
         * Sequential sort for small arrays
         */
        private static void sequential_sort(int[] arr, int begin, int end) {
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
```



#### Error stacktrace:

```
scala.collection.Iterator$$anon$19.next(Iterator.scala:973)
	scala.collection.Iterator$$anon$19.next(Iterator.scala:971)
	scala.collection.mutable.MutationTracker$CheckedIterator.next(MutationTracker.scala:76)
	scala.collection.IterableOps.head(Iterable.scala:222)
	scala.collection.IterableOps.head$(Iterable.scala:222)
	scala.collection.AbstractIterable.head(Iterable.scala:935)
	dotty.tools.dotc.interactive.InteractiveDriver.run(InteractiveDriver.scala:164)
	dotty.tools.pc.CachingDriver.run(CachingDriver.scala:45)
	dotty.tools.pc.WithCompilationUnit.<init>(WithCompilationUnit.scala:31)
	dotty.tools.pc.SimpleCollector.<init>(PcCollector.scala:351)
	dotty.tools.pc.PcSemanticTokensProvider$Collector$.<init>(PcSemanticTokensProvider.scala:63)
	dotty.tools.pc.PcSemanticTokensProvider.Collector$lzyINIT1(PcSemanticTokensProvider.scala:63)
	dotty.tools.pc.PcSemanticTokensProvider.Collector(PcSemanticTokensProvider.scala:63)
	dotty.tools.pc.PcSemanticTokensProvider.provide(PcSemanticTokensProvider.scala:88)
	dotty.tools.pc.ScalaPresentationCompiler.semanticTokens$$anonfun$1(ScalaPresentationCompiler.scala:111)
```
#### Short summary: 

java.util.NoSuchElementException: next on empty iterator