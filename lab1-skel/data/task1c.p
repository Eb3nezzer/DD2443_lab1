# Execute this program using $ gnuplot task1c.p

# Output size
set terminal png size 800,500

# Output filename
set output 'task1c.png'

# Graphics title
set title 'My plot for task 1c'

# Set x and y label
set xlabel 'threads'
set ylabel 'ns'

# Plot the data
# using X:Y means plot using column X and column Y
# Here column 1 is number of threads
# Column 2 and 3 are average execution time for local machine and PDC resp.
plot "task1c.dat" using 1:2 with lines title 'Local machine', \
     "task1c.dat" using 1:3 with lines title 'PDC Dardel'
