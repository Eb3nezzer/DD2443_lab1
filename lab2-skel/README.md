# Lab 2: Java Parallel Sorting

**This lab should be done in groups of 2. Join a Lab 2 group!**

This lab must be done using Java version 17.

Before starting, please review the entire assignment.

In this lab, you will pick one of two algorithms, Mergesort and Quicksort, and implement one of them in 5 different ways: one sequential, and four parallel.

The purpose of the lab is to learn how Java's API support for parallelization and understand how they work under the hood, and also to get experience parallelizing non-trivial algorithms.

---

## Lab 2 Skeleton

- `REPORT.pdf` / `REPORT.md`: Report template in markdown and compiled using `pandoc -o REPORT.pdf REPORT.md`.
- `data/`: Gnuplot code, data and figures.
- `MeasureMain.java`: The main file of a program doing performance measurements.
    - `java MeasureMain <SorterName> <Threads> <Array Size> <Warm-up rounds> <Measurement rounds> <PRNG seed>`
    - During the warm-up rounds, the program also validate that the sorting algorithms work correctly.
- `Auxiliary.java`: Contains helper functions for taking measurements and doing sanity checks.
- `Sorter.java`: An interface for the sorting algorithms. Classes that implements Sorter are:
    - `SequentialSort.java`: Sequential sorting algorithm.
    - `ExecutorServiceSort.java`: Implementation using ExecutorService.
    - `ForkJoinPoolSort.java`: Implementation using ForkJoinPool.
    - `ParallelStreamSort.java`: Implementation using ParallelStream.
    - `ThreadSort.java` (extra): Implementation using Thread's start()/join().
    - `JavaSort.java`: Implementation using Java's native library.
- `test_JavaSort.sh`: Bash script testing JavaSort.java.

The sorting algorithms must be packaged as a class implementing the following interface (as in the skeleton code):

```java
public interface Sorter {
        // Sort 'arr'.
        public void sort(int[] arr);
        // Number of threads used by the sorter.
        public int getThreads();
}
```

---

## Sanity check

When implementing the sequential sort or the parallel sorts, remember to sanity check your solutions.

- Does the implementation actually sort the array?
    - Ensure that the array is unsorted before applying the sorting algorithm.
    - Ensure that the array is sorted after applying the sorting algorithm.
    - Ensure that all of the sorter's threads have terminated.
- Are you measuring execution time in the right way?
- When you increase the number of threads for the parallel sorts, does the execution time decrease as predicted? (It must decrease!)

To get reproducible results, remember to warm-up the JVM before taking measurements.

---

## Tasks

When implementing the parallel sorting algorithms, you may resort to sequentially sorting (sub-)arrays if further parallelization does not improve performance or makes sense. For instance, creating a thread/task to sort an array of size <16 is probably unecessary. Use your own judgement.

**When implementing the following sorting algorithms, sanity check them!**

### 1. Sequential Sort
Implement a sequential sort of your algorithm of choice, mergesort or quicksort, sorting integer arrays.

### 2. Amdahl's law
Amdahl's law cannot be applied naively to the algorithm.

- Formulate your version of Amdahl's law for the algorithms for 2, 4, 8, and 16 threads. It does not have to be exact, but do your best.
- Motivate why your version may be better.
- Plot the speedup given by your solution with 2, 4, 8, and 16 threads, and p (parallelizable part) equal to 0.2, 0.4, 0.6, and 0.8.

### 3. ExecutorSerice

Implement a parallelized sort using Java's `ExecutorService` (`Executors.newFixedThreadPool(n)`).

### 4. ForkJoinPool and RecursiveAction

Implement a parallelized sort using Java's `ForkJoinPool` and `RecursiveAction`.

### 5. ParallelStream and Lambda Functions

Implement a parallelized sort using Java's `ParallelStream` and Lambda functions.

### 6. Performance measurements

Instrument (setting up measurements) your implementations from task 1, 3, 4, 5, 6 to measure the execution time.
Test your instrumentation locally.
Ensure you get consistent outputs and that the sorting actually work.
**If you have sanity checked your solutions, this should have been done.**

Measure the execution time of your parallelized implementations on PDC.
Sort a suitably large integer array, for example, 10,000,000 elements

Use 2, 4, 8, 16, 32, 48, 64 and 96 threads.
Also, measure the execution time of the sequential implementation as baseline, and the Java library implementation (JavaSort.java).
Plot the speedup of the implementations, normalizing using your sequential implementation's execution time.

Explain the results/plots. For instance,
- Are the performance gains/drops between different numbers of threads what you expected?
- What implementation ran the fastest/slowest? Why?

Also,
- What method was the easiest to implement?
- What method do you prefer?

### 7. Thread start() and join() (extra task)

**If you miss the deadline, or have to grave errors in your solution.**
Implement a parallelized sort using the Thread class (start() and join()), and include it in task 6 measurements.

---

## Submission Requirements

**This lab should be done in groups of 2. Join a Lab 2 group!**

To ease the assessment of your submissions, we require them to hold a specific standard. This accelerates the evaluation process, minimizing errors and ensuring efficient feedback. While the report primarily serves as a tool for you to structure your work and thoughts ahead of presentations, it also provides us an opportunity to revisit the work when needed.

- The submission must be a zip or tar.gz archive.
- The submission must include a REPORT.pdf.
    - Within the REPORT.pdf document, please provide a clear overview of your source code's structure and its relation to the tasks.
    - Each task should be addressed in its dedicated section within the REPORT.pdf.
    - Ensure that the REPORT.pdf incorporates answers to questions and discussions relevant to the assigned tasks.
    - Be succinct.
    - Any plots associated with the tasks must be integrated into the REPORT.pdf. Include labels and captions.
- Include all source code, scripts (including PDC scripts), and data within your submission package.
- Document your code.
- Ensure that your results can be replicated using PDC.
