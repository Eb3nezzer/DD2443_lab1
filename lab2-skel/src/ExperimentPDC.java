import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class ExperimentPDC {

    public static void main(String[] args) {
        System.err.println("Available Processors: " + Runtime.getRuntime().availableProcessors());

        // Define output filename
        String outputFile = "skip_list_performance.csv";
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            // Write CSV header
            writer.println("threads,distribution,mix,average_time,standard_deviation");
            
            // Define test parameters
            String[] distributions = {"Normal", "Uniform"};
            String[] mixes = {"1:1:8", "1:1:0"};
            int[] threads = {1, 2, 4, 8};
            
            // Process each configuration
            for (String distribution : distributions) {
                for (String mix : mixes) {
                    for (int threadCount : threads) {
                        double[] results = measurement(threadCount, distribution, mix);
                        writer.printf("%d,%s,%s,%.6f,%.6f%n", 
                                    threadCount, distribution, mix, results[0], results[1]);
                        writer.flush();
                        System.out.printf("Completed: %d threads, %s distribution, %s mix - Avg: %.6f, StdDev: %.6f%n", 
                                        threadCount, distribution, mix, results[0], results[1]);
                    }
                }
            }
            
            System.out.printf("Results written to %s%n", outputFile);
            
        } catch (IOException e) {
            System.err.printf("ERROR: Could not write to file %s: %s%n", outputFile, e.getMessage());
            System.exit(1);
        }
    }

    private static double[] measurement(int threads, String distribution, String mix) {
        // Test parameters
        int maxValue = 1000;
        int operationsPerThread = 100000;
        int warmupRounds = 10;
        int measurementRounds = 100;
        
        // Parse the mix ratio
        String[] mixParts = mix.split(":");
        int addRatio = Integer.parseInt(mixParts[0]);
        int removeRatio = Integer.parseInt(mixParts[1]);
        int containsRatio = Integer.parseInt(mixParts[2]);
        
        // Create the skip list
        LockFreeSkipList skipList = new LockFreeSkipList("Default");
        
        // Warm-up phase
        for (int i = 0; i < warmupRounds; i++) {
            runExperiment(skipList, threads, distribution, maxValue, addRatio, removeRatio, containsRatio, operationsPerThread);
        }
        
        // Measurement phase
        long[] results = new long[measurementRounds];
        for (int i = 0; i < measurementRounds; i++) {
            long startTime = System.nanoTime();
            runExperiment(skipList, threads, distribution, maxValue, addRatio, removeRatio, containsRatio, operationsPerThread);
            long endTime = System.nanoTime();
            results[i] = (endTime - startTime) / 1_000_000; // Convert to milliseconds
        }
        
        return calculateStatistics(results);
    }
    
    private static void runExperiment(LockFreeSkipList skipList, int threads, String distribution, 
                                    int maxValue, int addRatio, int removeRatio, int containsRatio, int operationsPerThread) {
        Thread[] threadArray = new Thread[threads];
        Random random = new Random(42); // Fixed seed for reproducibility
        
        // Calculate total ratio for normalization
        int totalRatio = addRatio + removeRatio + containsRatio;
        
        for (int i = 0; i < threads; i++) {
            final int threadId = i;
            threadArray[i] = new Thread(() -> {
                Random localRandom = new Random(random.nextInt());
                for (int op = 0; op < operationsPerThread; op++) {
                    int value = distribution.equals("Normal") ? 
                               (int) (Math.abs(localRandom.nextGaussian()) * maxValue / 3) % maxValue :
                               localRandom.nextInt(maxValue);
                    
                    int opType = localRandom.nextInt(totalRatio);
                    
                    if (opType < addRatio) {
                        skipList.add(value);
                    } else if (opType < addRatio + removeRatio) {
                        skipList.remove(value);
                    } else {
                        skipList.contains(value);
                    }
                }
            });
        }
        
        // Start all threads
        for (Thread t : threadArray) {
            t.start();
        }
        
        // Wait for all threads to complete
        for (Thread t : threadArray) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
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
        
        return new double[]{average, stdDev};
    }
}

// Simplified LockFreeSkipList implementation for demonstration
class LockFreeSkipList {
    private final String version;
    private final ConcurrentSkipListSet<Integer> set;
    
    public LockFreeSkipList(String version) {
        this.version = version;
        this.set = new ConcurrentSkipListSet<>();
    }
    
    public boolean add(int value) {
        return set.add(value);
    }
    
    public boolean remove(int value) {
        return set.remove(value);
    }
    
    public boolean contains(int value) {
        return set.contains(value);
    }
}