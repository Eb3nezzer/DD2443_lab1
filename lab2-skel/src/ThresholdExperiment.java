import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class ThresholdExperiment {

    public static void main(String[] args) {
        System.err.println("Available Processors: " + Runtime.getRuntime().availableProcessors());

        // Define output filename
        String outputFile = "threshold_performance_results.csv";
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            // Write CSV header
            writer.println("sorter_name,thread_count,threshold,average_time,standard_deviation");
            
            // Define test parameters
            String[] sorters = {"ExecutorService", "Thread"};
            int[] threads = {2, 4, 8};
            int[] thresholds = {250, 500, 1000, 1250, 1500};
            
            // Process each sorter
            for (String sorter : sorters) {
                for (int threadCount : threads) {
                    for (int threshold : thresholds) {
                        double[] results = measurement(threadCount, sorter, threshold);
                        double seconds = results[0]/1_000_000_000;
                        double sd = results[1]/1_000_000_000;
                        writer.printf("%s,%d,%d,%.6f,%.6f%n", 
                                    sorter, threadCount, threshold, seconds, sd);
                        writer.flush();
                        System.out.printf("Completed: %s with %d threads, threshold %d - Avg: %.6f, StdDev: %.6f%n", 
                                        sorter, threadCount, threshold, seconds, sd);
                    }
                }
            }
            
            System.out.printf("Results written to %s%n", outputFile);
            
        } catch (IOException e) {
            System.err.printf("ERROR: Could not write to file %s: %s%n", outputFile, e.getMessage());
            System.exit(1);
        }
    }

    private static double[] measurement(int threads, String sorter_name, int threshold) {
        // Number of values - 10,000,000 as requested
        int arrSize = 10_000_000;
        // Number of warm-up rounds
        int warmUps = 10;  // Reduced for faster experimentation
        // Number of measurement rounds
        int measurements = 10;  // Reduced for faster experimentation
        // Seed for RNG
        int seed = 42;

        // Sorting algorithm with specified threshold
        Sorter sorter = getSorter(sorter_name, threads, threshold);

        // Warm-up but also check correctness
        if (!Auxiliary.validate(sorter, arrSize, seed, warmUps)) {
            System.err.printf("ERROR: Sorting error for %s with %d threads, threshold %d\n", 
                            sorter_name, threads, threshold);
            System.exit(2);
        }
        
        // Take measurements
        long[] results = Auxiliary.measure(sorter, arrSize, seed, measurements);
        return Auxiliary.statistics(results);
    }

    private static Sorter getSorter(String name, int threads, int threshold) {
        switch (name) {
            case "Thread":
                return new ThreadSort(threads, threshold);
            case "ExecutorService":
                return new ExecutorServiceSort(threads, threshold);
            case "Sequential":
                return new SequentialSort();
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