public class SequentialSort implements Sorter {

        public SequentialSort() {
        }

        public void sort(int[] arr) {
                // Base case
                if (arr.length <= 1) {
                        return;
                }

                // Allocate work array
                int[] work = arr.clone();

                // Sort data from the work array into the original array
                split(arr, work, 0, arr.length);
        }

        /**
         * Split hold_arr into 2 sublists, sort both sublists into work_arr, merge both sublists back into hold_arr
         * @param hold_arr Main input array that holds values
         * @param work_arr Work array
         * @param begin index of first element in hold_arr to sort
         * @param end exclusive index of last element in hold_arr to sort
         */
        public void split(int[] hold_arr, int[] work_arr, int begin, int end) {
                // Base case
                if (end-begin <= 1) {
                        return;
                }

                // Split a list with more than two items into sublists
                int middle = (begin + end)/2;

                // Recursively sort both sublists from hold_arr to work_arr
                split(work_arr, hold_arr, begin, middle);
                split(work_arr, hold_arr, middle, end);

                // Merge the resulting sublists back into hold_arr
                merge(work_arr, begin, middle, end, hold_arr);
        }

        /**
         * Merge two sublists in input_arr to output_arr
         * @param input_arr Array containing sublists
         * @param output_arr Array to merge into
         * @param begin index of first element of first sublist
         * @param middle index of first element of second sublist
         * @param end exclusive index of last element in second sublist
         */
        public void merge(int[] input_arr, int begin, int middle, int end, int[] output_arr) {
                // Start indexes at the beginning of sublists
                int i = begin, j = middle;
 
                // Loop through all k elements, placing them in order
                for (int k = begin; k < end; k++) {
                        // if still elements in first sublist and less than second sublist, put in array
                        // otherwise take from second sublist
                        if (i < middle && (j >= end || input_arr[i] <= input_arr[j])) {
                                output_arr[k] = input_arr[i];
                                i++;
                        } else {
                                output_arr[k] = input_arr[j];
                                j++;
                        }
                }
        }

        public int getThreads() {
                return 1;
        }
}
