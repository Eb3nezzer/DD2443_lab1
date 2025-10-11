import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

public class ExperimentLocal {
    public static void main(String[] args) {
        System.err.println("Available Processors: " + Runtime.getRuntime().availableProcessors());

        // Define output filename
        String outputFile = "local_performance.csv";
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            // Write CSV header
            writer.println("threads,distribution,mix,average_time,standard_deviation");
            
            // Define test parameters
            String[] distributions = {"Normal", "Uniform"};
            String[] mixes = {"1:1:8", "1:1:0"};
            int[] threads = {1, 2, 4, 8, 16, 32, 48};
            
            // Process each configuration
            for (String distribution : distributions) {
                for (String mix : mixes) {
                    for (int threadCount : threads) {
                        long[] results = runExperiment(threadCount, distribution, mix);
                        double[] processed = calculateStatistics(results);
                        // processed[0] = mean, processed[1] = stdDev
                        writer.printf("%d,%s,%s,%.6f,%.6f%n", 
                                    threadCount, distribution, mix, processed[0], processed[1]);
                        writer.flush();
                        System.out.printf("Completed: %d threads, %s distribution, %s mix - Avg: %.6f, StdDev: %.6f%n", 
                                        threadCount, distribution, mix, processed[0], processed[1]);
                    }
                }
            }
            
            System.out.printf("Results written to %s%n", outputFile);
            
        } catch (IOException e) {
            System.err.printf("ERROR: Could not write to file %s: %s%n", outputFile, e.getMessage());
            System.exit(1);
        }
    }
    
    private static long[] runExperiment(int threads, String distribution, String opsDist) {
        // LockFreeSet type to use
        String setName = "Default";
        // Max input value
        int maxValue = 100_000;
        // Distribution of adds/removes/contains
        int[] ops = Arrays.stream(opsDist.split(":"))
                .mapToInt(v -> Integer.parseInt(v)).toArray();
        // Number of operations executed per thread.
        int opsPerThread = 1_000_000;
        // Warm up rounds
        int warmups = 10;
        // Measurement rounds
        int measurements = 50;

        LockFreeSet<Integer> set = getSet(setName, threads);
        Distribution opsDistribution = new Distribution.Discrete(42, ops);
        Distribution valuesDistribution = getDistribution(distribution, maxValue);

        for (int i = 0; i < warmups; ++i) {
                long time = Experiment.run(threads, opsPerThread, set, opsDistribution, valuesDistribution);
                // int discrepancy = Log.validate(set.getLog());
                // System.err.println("Warmup time: " + time);
                // System.err.println("Warmup discrepancy: " + discrepancy);
        }

        long[] results = new long[measurements];
        for (int i = 0; i < measurements; ++i) {
                long time = Experiment.run(threads, opsPerThread, set, opsDistribution, valuesDistribution);
                // int discrepancy = Log.validate(set.getLog());
                // System.err.println("Measurement time: " + time);
                // System.err.println("Measurement discrepancy: " + discrepancy);
                results[i] = time;
        }

        return results;
        }

        public static Distribution getDistribution(String name, int maxValue) {
                switch (name) {
                case "Uniform": 
                        return new Distribution.Uniform(0xdeadbeef, 0, maxValue);
                case "Normal":
                        return new Distribution.Normal(0xcafecafe, 10, 0, maxValue);
                default: 
                        return null;
                }
        }

        public static LockFreeSet<Integer> getSet(String name, int threads) {
                switch (name) {
                case "Default": 
                        return new LockFreeSkipList();
                case "Locked":
                        // TODO: Add your own set
                case "LocalLog":
                        // TODO: Add your own set
                case "GlobalLog":
                        // TODO: Add your own set
                default: 
                        return null;
                }
        }

        private static double[] calculateStatistics(long[] results) {
                double sum = 0;
                double sumSquared = 0;
                
                for (long time : results) {
                        sum += time;
                        sumSquared += time * time;
                }
                
                double average = sum / results.length;
                double variance = (sumSquared / results.length) - (average * average);
                double stdDev = Math.sqrt(variance);
                double nanoseconds = 1_000_000_000;
                
                return new double[]{average/nanoseconds, stdDev/nanoseconds};
        }
}