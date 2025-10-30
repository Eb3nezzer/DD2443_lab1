import java.util.HashSet;
import java.util.Arrays;

public class Log {
        private Log() {
                // Do not implement
        }

        public static int validate(Log.Entry[] log) {
                // Sort the log entries by linearisation timestamp
                Arrays.sort(log, (a,b) -> Long.compare(a.timestamp, b.timestamp));
                
                HashSet<Integer> replaySet = new HashSet<>();
                int num_disc = 0;

                for (Entry entry: log) {
                        boolean succ;
                        switch (entry.method) {
                                case ADD:
                                        succ = replaySet.add(entry.arg);
                                        break;
                                case REMOVE:
                                        succ = replaySet.remove(entry.arg);
                                        break;
                                case CONTAINS:
                                        succ = replaySet.contains(entry.arg);
                                        break;
                                default:
                                        succ = false;
                        }
                        if (succ != entry.ret) {
                                num_disc ++;
                        }
                }

                return num_disc;
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
