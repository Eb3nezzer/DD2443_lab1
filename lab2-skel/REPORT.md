# Lab 2 - Java Parallel Programming and Sorting Algorithms
- Group X
- Perrson, David and Harrison, Max

## Task 1: Sequential Sort
We chose to implement MergeSort.

Source files:

- `SequentialSort.java`

## Task 2: Amdahl's Law

Our Amdahl's law ...

Here is a plot of our version of Amdahl's law ...

![amdahl's law plot](data/amdahl.png)

We see that ...

## Task 3: ExecutorServiceSort

Source files:

- `ExecutorServiceSort.java`

We decided to ...

## Task 4: ForkJoinPoolSort

Source files:

- `ForkJoinPoolSort.java`

Benefits of ForkJoinPool:

1) Work Stealing: Idle threads can steal work from busy threads, leading to better load balancing.
2) Natural Recursion: The recursive structure matches the divide-and-conquer nature of merge sort perfectly.
3) Efficient Memory Usage: The alternating array technique minimizes memory allocation and copying.
4) Automatic Parallelization: The framework automatically manages thread creation and task distribution.

The class now follows the fork-join paradigm more naturally while maintaining the same sorting performance and parallelization benefits.

## Task 5: ParallelStreamSort

Source files:

- `ForkJoinPoolSort.java`

We decided to ...

## Task 6: Performance measurements with PDC

We decided to sort 10,000,000 integers ...

![pdc plot](data/pdc.png)

We see that ...
