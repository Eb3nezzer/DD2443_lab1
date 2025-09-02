# Lab 1 - Basic Concurrency in Java

- Group X
- Lastname, Firstname and Lastname, Firstname

## Task 1: Simple Synchronization

### Task 1a: Race conditions

Source files:

- `task1/MainA.java` (main file)

To compile and execute:
```
javac MainA.java
java MainA
```

### Task 1b: Synchronized keyword
Source files:

- `task1/MainB.java` (main file)

To compile and execute:
```
javac MainB.java
java MainB
```

### Task 1c: Synchronization performance

Source files:

- `task1/MainC.java` (main file)

To compile and execute:
```
javac MainC.java
java MainC <N>
```
Where `N` is number of threads to execute with.

In figure 1, we see how the execution time scaled with the number of threads.
...

![My plot for task 1c](data/task1c.png)

## Task 2: Guarded blocks using wait()/notify()

## Task 3: Producer-Consumer Buffer using Condition Variables

## Task 4: Counting Semaphore

## Task 5: Dining Philosophers
