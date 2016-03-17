public class Stopwatch { 
    private long start;

    public Stopwatch() {
        reset();
    } 

    public long elapsed() {
        return System.currentTimeMillis() - start;
    }

    public void reset() {
        start = System.currentTimeMillis();
    }
}