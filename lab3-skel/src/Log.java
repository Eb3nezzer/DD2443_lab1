public class Log {
        private Log() {
                // Do not implement
        }

        public static int validate(Log.Entry[] log) {
                // Implement this.
                // Should return the number of discrepancies in the log.
                return -1;
        }

        // Log entry for linearization point.
        public static class Entry {
                public Method method;
                public int arg;
                public boolean ret;
                public long timestamp;
                public Entry(Method method, int arg, boolean ret, long timestamp) {
                        this.method = method;
                        this.arg = arg;
                        this.ret = ret;
                        this.timestamp = timestamp;
                }
        }

        public static enum Method {
                ADD, REMOVE, CONTAINS
        }
}
