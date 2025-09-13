import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.Arrays;

public class JavaSort implements Sorter {

        private final ForkJoinPool pool;

        public JavaSort(int threads) {
                this.pool = new ForkJoinPool(threads);
        }

        public void sort(int[] arr) {
                try {
                        pool.submit(() -> Arrays.parallelSort(arr)).get(); 
                } catch (Exception e) {
                        e.printStackTrace();
                }
        }

        public int getThreads() {
                return pool.getParallelism();
        }
}
