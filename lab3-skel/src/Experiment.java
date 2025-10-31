import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.Arrays;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class Experiment {

    public static long run(int threads, int opsPerThread, LockFreeSet<Integer> list, Distribution ops, Distribution values) {
        ExecutorService executorService = Executors.newFixedThreadPool(threads);

        Task[] tasks = new Task[threads];
        for (int i = 0; i < tasks.length; ++i) {
            tasks[i] = new Task(i, opsPerThread, list, ops.copy(i), values.copy(-i));
        }

        try {
            long startTime = System.nanoTime();
            executorService.invokeAll(Arrays.asList(tasks));
            long endTime = System.nanoTime();
            executorService.shutdown();
            return endTime - startTime;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static class Task implements Callable<Void> {
        private final int threadId;
        private final LockFreeSet<Integer> set;
        private final Distribution ops, values;
        private final int opsPerThread;

        public Task(int threadId, int opsPerThread, LockFreeSet<Integer> set, Distribution ops, Distribution values) {
            this.threadId = threadId;
            this.set = set;
            this.ops = ops;
            this.values = values;
            this.opsPerThread = opsPerThread;
        }

        public Void call() throws Exception {
            for (int i = 0; i < opsPerThread; ++i) {
                int val = values.next();
                int op = ops.next();
                switch (op) {
                    case 0:
                        set.add(threadId, val);
                        break;
                    case 1:
                        set.remove(threadId, val);
                        break;
                    case 2:
                        set.contains(threadId, val);
                        break;
                }
            }
            return null;
        }
    }

    private static class RunResult {
        final long time;
        final int discrepancies;

        public RunResult(long itime, int idisc) {
            time = itime;
            discrepancies = idisc;
        }
    }
    
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java Experiment <system> <setType>");
            System.exit(1);
        }

        String system = args[0];
        String setType = args[1];
        
        if (!system.equals("local") && !system.equals("pdc")) {
            System.err.println("ERROR: system must be either 'local' or 'pdc'");
            System.exit(1);
        }

        String[] valid_types = {"Default", "Locked", "LocalLog", "GlobalLog", "CustomLog"};
        if (!Arrays.asList(valid_types).contains(setType)) {
            System.err.println("ERROR: must have valid set type");
            System.exit(1);
        }

        System.err.println("Available Processors: " + Runtime.getRuntime().availableProcessors());
        System.err.println("Running in " + system + " mode with set type: " + setType);

        // Define test parameters based on system
        int[] threads;
        int opsPerThread;
        String outputFile;
        
        if (system.equals("local")) {
            threads = new int[]{1, 2, 4, 8};
            opsPerThread = 100_000;
            outputFile = "local_performance.csv";
        } else { // pdc
            threads = new int[]{1, 2, 4, 8, 16, 32, 48};
            opsPerThread = 1_000_000;
            outputFile = "pdc_performance.csv";
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            // Write CSV header based on set type
            if (setType.equals("Default")) {
                writer.println("threads,distribution,mix,average_time,standard_deviation");
            } else {
                writer.println("threads,distribution,mix,average_time,standard_deviation,average_discrepancies");
            }
            
            // Define test parameters
            String[] distributions = {"Normal", "Uniform"};
            String[] mixes = {"1:1:8", "1:1:0"};
            
            // Process each configuration
            for (String distribution : distributions) {
                for (String mix : mixes) {
                    for (int threadCount : threads) {
                        RunResult[] results = runExperiment(threadCount, distribution, mix, setType, opsPerThread);
                        double[] processed = calculateStatistics(results, setType);
                        
                        // Output based on set type
                        if (setType.equals("Default")) {
                            writer.printf("%d,%s,%s,%.6f,%.6f%n", 
                                        threadCount, distribution, mix, processed[0], processed[1]);
                            System.out.printf("Completed: %d threads, %s distribution, %s mix - Avg: %.6f, StdDev: %.6f%n", 
                                            threadCount, distribution, mix, processed[0], processed[1]);
                        } else {
                            writer.printf("%d,%s,%s,%.6f,%.6f,%.0f%n", 
                                        threadCount, distribution, mix, processed[0], processed[1], processed[2]);
                            System.out.printf("Completed: %d threads, %s distribution, %s mix - Avg: %.6f, StdDev: %.6f, Disc: %.0f%n", 
                                            threadCount, distribution, mix, processed[0], processed[1], processed[2]);
                        }
                        writer.flush();
                    }
                }
            }
            
            System.out.printf("Results written to %s%n", outputFile);
            
        } catch (IOException e) {
            System.err.printf("ERROR: Could not write to file %s: %s%n", outputFile, e.getMessage());
            System.exit(1);
        }
    }
    
    @SuppressWarnings("unused")
    private static RunResult[] runExperiment(int threads, String distribution, String opsDist, String setName, int opsPerThread) {
        // Max input value
        int maxValue = 100_000;
        // Distribution of adds/removes/contains
        int[] ops = Arrays.stream(opsDist.split(":"))
                .mapToInt(v -> Integer.parseInt(v)).toArray();
        // Warm up rounds
        int warmups = 5;
        // Measurement rounds
        int measurements = 10;

        LockFreeSet<Integer> set = getSet(setName, threads);
        Distribution opsDistribution = new Distribution.Discrete(42, ops);
        Distribution valuesDistribution = getDistribution(distribution, maxValue);

        for (int i = 0; i < warmups; ++i) {
            long time = Experiment.run(threads, opsPerThread, set, opsDistribution, valuesDistribution);
            // Only validate if not using Default set
            if (!setName.equals("Default")) {
                int discrepancy = Log.validate(set.getLog());
            }
            set.reset();
        }

        RunResult[] results = new RunResult[measurements];
        for (int i = 0; i < measurements; ++i) {
            long time = Experiment.run(threads, opsPerThread, set, opsDistribution, valuesDistribution);
            int discrepancy = 0;
            // Only validate if not using Default set
            if (!setName.equals("Default")) {
                discrepancy = Log.validate(set.getLog());
            }
            results[i] = new RunResult(time, discrepancy);
            set.reset();
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
                return new LockFreeSkipList<Integer>();
            case "Locked":
                return new GlobalLockSkipList<Integer>();
            case "LocalLog":
                return new LocalLogSkipList<Integer>();
            case "GlobalLog":
                return new GlobalLogSkipList<Integer>();
            case "CustomLog":
                return new CustomLogSkipList<Integer>();
            default: 
                return null;
        }
    }

    private static double[] calculateStatistics(RunResult[] results, String setType) {
        double sum = 0;
        double sumSquared = 0;
        int total_disc = 0;
        
        for (RunResult result : results) {
            sum += result.time;
            sumSquared += result.time * result.time;
            // max_disc = (result.discrepancies > max_disc) ? result.discrepancies : max_disc;
            total_disc += result.discrepancies;
        }
        
        double average = sum / results.length;
        double variance = (sumSquared / results.length) - (average * average);
        double stdDev = Math.sqrt(variance);
        double ave_disc = total_disc / results.length;
        
        // Return different arrays based on set type
        if (setType.equals("Default")) {
            return new double[]{average, stdDev};
        } else {
            return new double[]{average, stdDev, ave_disc};
        }
    }
}