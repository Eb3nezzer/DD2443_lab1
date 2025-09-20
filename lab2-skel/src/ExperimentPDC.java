import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class ExperimentPDC {

        public static void main(String [] args) {
            System.err.println("Available Processors: " + Runtime.getRuntime().availableProcessors());

            // Define output filename
            String outputFile = "performance_results.csv";
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
                // Write CSV header
                writer.println("sorter_name,thread_count,average_time,standard_deviation");
                
                // Define test parameters
                String[] sorters = {"Sequential", "JavaSort", "ExecutorService", "ForkJoinPool", "ParallelStream"};
                int[] threads = {1, 2, 4, 8, 16, 32, 48, 64, 96};
                
                // Process each sorter
                for (int i = 0; i < sorters.length; i++) {
                    if (i == 0 || i == 1) {
                        // Sequential and JavaSort: only test with one thread
                        double[] results = measurement(1, sorters[i]);
                        writer.printf("%s,%d,%.6f,%.6f%n", 
                                    sorters[i], 1, results[0], results[1]);
                        writer.flush();
                        System.out.printf("Completed: %s with 1 thread - Avg: %.6f, StdDev: %.6f%n", 
                                        sorters[i], results[0], results[1]);
                    } else {
                        // Other sorters: test with full thread range
                        for (int j = 0; j < threads.length; j++) {
                            double[] results = measurement(threads[j], sorters[i]);
                            writer.printf("%s,%d,%.6f,%.6f%n", 
                                        sorters[i], threads[j], results[0], results[1]);
                                writer.flush();
                            System.out.printf("Completed: %s with %d threads - Avg: %.6f, StdDev: %.6f%n", 
                                            sorters[i], threads[j], results[0], results[1]);
                        }
                    }
                }
                
                System.out.printf("Results written to %s%n", outputFile);
                
            } catch (IOException e) {
                System.err.printf("ERROR: Could not write to file %s: %s%n", outputFile, e.getMessage());
                System.exit(1);
            }
        }

        private static double[] measurement(int threads, String sorter_name) {
            // Number of values.
            int arrSize = 10_000_000;
            // Number of warm-up rounds.
            int warmUps = 50;
            // Number of measurement rounds.
            int measurements = 50;
            // Seed for RNG
            int seed = 42;

            // Sorting algorithm.
            Sorter sorter = getSorter(sorter_name, threads);

            // Warm-up but also check correctness.
            if (!Auxiliary.validate(sorter, arrSize, seed, warmUps)) {
                    System.err.printf("ERROR: Sorting error.\n");
                    System.exit(2);
            }
            
            // Take measurements
            long[] results = Auxiliary.measure(sorter, arrSize, seed, measurements);
            return Auxiliary.statistics(results);
        }

        private static Sorter getSorter(String name, int threads) {
                switch (name) {
                case "Sequential":
                        return new SequentialSort();
                case "Thread":
                        return new ThreadSort(threads);
                case "ExecutorService":
                        return new ExecutorServiceSort(threads);
                case "ForkJoinPool":
                        return new ForkJoinPoolSort(threads);
                case "ParallelStream":
                        return new ParallelStreamSort(threads);
                case "JavaSort":
                        return new JavaSort(threads);
                default:
                        return null;
                }
        }
}
