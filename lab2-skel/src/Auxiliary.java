/**
 * Helper methods.
 */

import java.util.Arrays;
import java.util.Random;
import java.util.stream.LongStream;

public class Auxiliary {
        /**
         * Generate a pseudo-random array of length `n`.
         */
        public static int[] arrayGenerate(int seed, int n) {
                Random prng = new Random(seed);
                int[] arr = new int[n];
                for (int i = 0; i < n; ++i)
                        arr[i] = prng.nextInt();
                return arr;
        }

        /**
         * Calculates mean and standard deviation of array elements.
         * @param res array of longs
         * @return stats, such that, stats[0] = mean, stats[1] = std
         */
        public static double[] statistics(long[] res)
        {
                double mean = LongStream.of(res).sum() / res.length;
                double variance = LongStream.of(res)
                        .mapToDouble(v -> (v - mean) * (v - mean))
                        .sum() / (res.length - 1); 
                return new double[]{mean, Math.sqrt(variance)};
        }

        /**
         * Measures the execution time of the 'sorter'.
         * @param sorter Sorting algorithm
         * @param n Size of list to sort
         * @param initSeed Initial seed used for array generation
         * @param m Measurment rounds.
         * @return array of execution time
         */
        public static long[] measure(Sorter sorter, int n, int initSeed, int m) {
                long[] result = new long[m];
                for (int i = 0; i < m; i++) {
                        int[] arr = arrayGenerate(initSeed + i, n);
                        long startTime = System.nanoTime();
                        sorter.sort(arr);
                        long endTime = System.nanoTime();
                        result[i] = endTime - startTime;
                }
                return result;
        }

        /**
         * Checks that the 'sorter' sorts.
         * @param sorter Sorting algorithm
         * @param n Size of list to sort
         * @param initSeed Initial seed used for array generation
         * @param m Number of attempts.
         * @return True if the sorter successfully sorted all generated arrays.
         */
        public static boolean validate(Sorter sorter, int n, int initSeed, int m) {
                for (int i = 0; i < m; ++i) {
                        int[] arr = arrayGenerate(initSeed - i, n);
                        int[] arrCopy = Arrays.copyOf(arr, n);
                        Arrays.parallelSort(arrCopy);
                        sorter.sort(arr);
                        if (!Arrays.equals(arr, arrCopy))
                                return false;
                }
                return true;
        }

}
