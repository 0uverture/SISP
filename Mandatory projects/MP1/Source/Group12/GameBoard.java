package Group12;

import java.lang.*;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

public class GameBoard {
    public enum Winner {
        NONE, BLUE, RED, TIE
    }

    private final static int MAX_TIME = 10000;

    private final int timeBufferInitial = 20000;
    private final int timeBufferIncrement = 500;
    private int timeBuffer = timeBufferInitial;

    private final int width, height;
    private final int maxMoves;

    private final HashMap<Long, Integer>[] startMoves;

    private byte state[][];

    private long playerBoard;
    private long opponentBoard;
    private long emptyBoard;
    private Masks masks;

    private final byte player;
    private final byte opponent;

    private Winner winner = Winner.NONE;
    private int[] columnHeights;
    private int moves;

    private int[] bestColumns;

    private HashMap<BoardState, Float> cache;
    private int cacheHits = 0;
    private int cacheMisses = 0;

    private Stopwatch stopwatch;

    private long searchCount = 0;
    private Statistics statistics;

    public GameBoard(int width, int height, int player) {
        this.width = width;
        this.height = height;
        this.maxMoves = width * height;

        this.state = new byte[width][height];

        this.masks = new Masks(width, height);

        this.player = player == 1 ? Coin.BLUE : Coin.RED;
        this.opponent = player == 2 ? Coin.BLUE : Coin.RED;

        this.state = new byte[width][height];
        this.columnHeights = new int[width];

        this.bestColumns = new int[width];
        for (int i = 0; i < width; i++) {
            // Middle out
            bestColumns[width - i - 1] = i % 2 == 0 ? i/2 : width - (i+1)/2;
        }

        this.cache = new HashMap<>();

        this.stopwatch = new Stopwatch();

        if (width == 7) {
            startMoves = new HashMap[2];

            startMoves[0] = new HashMap<>();
            startMoves[0].put(0L, 3);

            startMoves[1] = new HashMap<>();
            startMoves[1].put(masks.getCoinMask(0, 0), 3);
            startMoves[1].put(masks.getCoinMask(1, 0), 2);
            startMoves[1].put(masks.getCoinMask(2, 0), 3);
            startMoves[1].put(masks.getCoinMask(3, 0), 3);
            startMoves[1].put(masks.getCoinMask(4, 0), 3);
            startMoves[1].put(masks.getCoinMask(5, 0), 4);
            startMoves[1].put(masks.getCoinMask(6, 0), 3);
        }
        else startMoves = null;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();

        for (int row = height - 1; row >= 0; row--) {
            for (int column = 0; column < width; column++) {
                byte coin = state[column][row];
                char symbol = ' ';

                if (coin == Coin.BLUE)
                    symbol = 'B';

                else if (coin == Coin.RED)
                    symbol = 'R';

                builder.append(symbol);

                if (column < width)
                    builder.append(' ');
            }

            if (row != 0)
                builder.append('\n');
        }

        return builder.toString();
    }

    public Winner getWinner() {
        return winner;
    }

    public void insertCoin(int column, int player) {
        byte coin = player == 1 ? Coin.BLUE : Coin.RED;
        addCoin(column, coin);
    }

    public int decideNextMove() {
        timeBuffer = Math.min(timeBufferInitial, timeBuffer + timeBufferIncrement);

        // Statistics
        statistics = new Statistics();
        stopwatch.reset();
        searchCount = 0;
        cacheHits = 0;
        cacheMisses = 0;

        long elapsed = 0;
        int depth = 0;

        int bestColumn = -1;
        float bestResult = Float.NEGATIVE_INFINITY;

        // Use hard coded starter moves if available
        if (startMoves != null && moves < startMoves.length) {
            return startMoves[moves].get(opponentBoard);
        }

        AbstractMap.SimpleEntry<Integer, Float>[] results = null;

        while(depth < maxMoves - moves) {
            bestResult = Float.NEGATIVE_INFINITY;
            cache.clear();

            // Order best columns
            if (results != null) {
                // First order from middle out
                Arrays.sort(results, new ResultKeyComparator());

                // Then order from best to worst result of last iteration
                Arrays.sort(results, new ResultValueComparator());

                for (int i = 0; i < width; i++) {
                    bestColumns[i] = results[i].getKey();
                }
            }
            else {
                results = new AbstractMap.SimpleEntry[width];
            }

            for (int column = 0; column < width; column++) {
                if (columnHeights[column] == height) {
                    // This column should not be prioritized at later iterations
                    results[column] = new AbstractMap.SimpleEntry<>(column, Float.NEGATIVE_INFINITY);

                    continue;
                }

                // Apply move to board
                addCoin(column, player);

                // Evaluate maximum
                float result = evaluate(false, depth);
                if (result >= bestResult) {
                    bestResult = result;
                    bestColumn = column;
                }

                // Undo move
                removeCoin(column);

                // If a winning move is possible, take it
                if (depth == 0 && result == Float.POSITIVE_INFINITY) {
                    return column;
                }

                // Save result for ordering of later iterations
                results[column] = new AbstractMap.SimpleEntry<>(column, result);
            }

            long delta = stopwatch.elapsed() - elapsed;
            long left = MAX_TIME - elapsed;
            elapsed = stopwatch.elapsed();

            if((float)delta * Math.pow(1.08, depth + 1 - (moves * 0.2)) >
                    left - (timeBufferInitial * 0.75 - timeBuffer) / (maxMoves - moves))
                break;

            if (depth == 0) depth = 7;
            else depth++;
        }

        if(bestResult == Float.NEGATIVE_INFINITY)
            System.out.println("No non-losing move available");

        // Statistics
        statistics.column       = bestColumn;
        statistics.depth        = depth;
        statistics.elapsed      = stopwatch.elapsed();
        statistics.mnodes       = searchCount / 1000000f;
        statistics.mnodesPerSec = (float)searchCount / ((float)stopwatch.elapsed() / 1000f) / 1000000f;
        statistics.cacheSize    = cache.size();
        statistics.cacheHits    = cacheHits;
        statistics.cacheMisses  = cacheMisses;
        System.out.println(statistics);
        System.out.println();

        timeBuffer -= elapsed;
        return bestColumn;
    }

    private float evaluate(boolean maximize, float alpha, float beta, int depthLeft) {
        searchCount++;

        BoardState bs = new BoardState(playerBoard, opponentBoard);
        if (cache.containsKey(bs)) {
            cacheHits++;
            return cache.get(bs);
        }
        else cacheMisses++;

        float eval;
        if (depthLeft <= 0 || isTerminal()) {
            if (winner == Coin.toWinner(player))
                eval = Float.POSITIVE_INFINITY;

            else if (winner == Coin.toWinner(opponent))
                eval = Float.NEGATIVE_INFINITY;

            else if (winner == Winner.NONE)
                eval = heuristic();

            else eval = 0;
        } else {
            float best = maximize ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;

            for (int i = 0; i < width; i++) {
                int column = bestColumns[i];

                if (columnHeights[column] == height)
                    continue;

                // Apply move to board
                addCoin(column, maximize ? player : opponent);

                // Evaluate child node
                float result = evaluate(!maximize, alpha, beta, depthLeft - 1);
                best = maximize ? Math.max(result, best) : Math.min(result, best);

                // Alpha-beta pruning
                boolean prune = false;
                if (maximize) {
                    if (best >= beta)
                        prune = true;

                    alpha = Math.max(alpha, best);
                } else {
                    if (best <= alpha)
                        prune = true;

                    beta = Math.min(beta, best);
                }

                // Undo move
                removeCoin(column);

                if (prune) {
                    statistics.prunes[depthLeft + 1]++;
                    break;
                }
            }

            eval = best;
        }

        cache.put(bs, eval);
        return eval;
    }

    private float evaluate(boolean maximize, int depthLeft) {
        return evaluate(maximize, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, depthLeft);
    }

    private float heuristic() {
        int goodValue = 0;
        int badValue = 0;

        long goodBoard = playerBoard | emptyBoard;
        long badBoard =  opponentBoard | emptyBoard;

        for (int i = 0; i < maxMoves; i++) {
            int row = (i / width);
            int value = height - row;

            if (Masks.intersectsMask(goodBoard, masks.won.N [i])) goodValue += value;
            if (Masks.intersectsMask(goodBoard, masks.won.NE[i])) goodValue += value;
            if (Masks.intersectsMask(goodBoard, masks.won.E [i])) goodValue += value;
            if (Masks.intersectsMask(goodBoard, masks.won.SE[i])) goodValue += value;
            if (Masks.intersectsMask(goodBoard, masks.won.S [i])) goodValue += value;
            if (Masks.intersectsMask(goodBoard, masks.won.SW[i])) goodValue += value;
            if (Masks.intersectsMask(goodBoard, masks.won.W [i])) goodValue += value;
            if (Masks.intersectsMask(goodBoard, masks.won.NW[i])) goodValue += value;

            if (Masks.intersectsMask(badBoard, masks.won.N [i])) badValue += value;
            if (Masks.intersectsMask(badBoard, masks.won.NE[i])) badValue += value;
            if (Masks.intersectsMask(badBoard, masks.won.E [i])) badValue += value;
            if (Masks.intersectsMask(badBoard, masks.won.SE[i])) badValue += value;
            if (Masks.intersectsMask(badBoard, masks.won.S [i])) badValue += value;
            if (Masks.intersectsMask(badBoard, masks.won.SW[i])) badValue += value;
            if (Masks.intersectsMask(badBoard, masks.won.W [i])) badValue += value;
            if (Masks.intersectsMask(badBoard, masks.won.NW[i])) badValue += value;
        }

        return goodValue - badValue;
    }

    private void addCoin(int column, byte coin) {
        int row = columnHeights[column]++;
        state[column][row] = coin;

        if (coin == player) playerBoard |= masks.getCoinMask(column, row);
        else if (coin == opponent) opponentBoard |= masks.getCoinMask(column, row);
        emptyBoard = ~(playerBoard | opponentBoard);

        moves++;
        winner = checkWinner(column, coin);
    }

    private void removeCoin(int column) {
        int row = --columnHeights[column];
        state[column][row] = Coin.NONE;

        long coinMask = masks.getCoinMask(column, row);
        playerBoard   |= coinMask;
        playerBoard   ^= coinMask;
        opponentBoard |= coinMask;
        opponentBoard ^= coinMask;
        emptyBoard = ~(playerBoard | opponentBoard);

        moves--;
        winner = Winner.NONE;
    }

    private boolean isTerminal() {
        return winner != Winner.NONE;
    }

    private Winner checkWinner(int column, byte coin) {
        if (checkLine(column, coin))
            return Coin.toWinner(coin);

        if (moves == maxMoves)
            return Winner.TIE;

        return Winner.NONE;
    }

    private int checkLineLength(int right, int up, int column, int row, byte coin) {
        int count = 0;

        column += right;
        row += up;

        while(column >= 0 && column < width && row >= 0 && row < height) {
            if(state[column][row] == coin) count++;
            else break;

            column += right;
            row += up;
        }

        return count;
    }

    private boolean checkLine(int column, byte coin) {
        int row = columnHeights[column] - 1;

        if (checkLineLength(-1, 1, column, row, coin) + checkLineLength(1,  -1, column, row, coin) >= 3 || // NW + SE
                checkLineLength(1, 1, column, row, coin)  + checkLineLength(-1, -1, column, row, coin) >= 3 || // NE + SW
                checkLineLength(-1, 0, column, row, coin)  + checkLineLength(1,  0, column, row, coin) >= 3 || // W + E
                checkLineLength(0, -1, column, row, coin) >= 3) // S
            return true;

        return false;
    }

    public class Statistics {
        public int column;
        public int depth;

        public long elapsed;

        public float mnodes;
        public float mnodesPerSec;

        public int[] prunes = new int[maxMoves];

        public int cacheSize;
        public int cacheHits;
        public int cacheMisses;

        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append("Statistics\n");
            sb.append("  COLUMN              = " + column                               + "\n");
            sb.append("  DEPTH               = " + depth                                + "\n");
            sb.append("  ELAPSED             = " + elapsed                              + " ms\n");
            sb.append("  TOTAL               = " + String.format("%.2f", mnodes)        + " Mnodes\n");
            sb.append("  SPEED               = " + String.format("%.2f", mnodesPerSec)  + " Mnodes/sec\n");

            sb.append("\n");
            sb.append("Pruning\n");

            sb.append("  DEPTH                 ");
            for (int i = 0; i < 10 && i < prunes.length; i++) {
                sb.append(String.format("%02d   ", i));
            }
            sb.append("\n");

            sb.append("  PRUNES (k)            ");
            for (int i = 0; i < 10 && i < prunes.length; i++) {
                sb.append(String.format("%04d ", prunes[i]/1000));
            }
            sb.append("\n");

            sb.append("\n");
            sb.append("Cache\n");
            sb.append("  SIZE                = " + cacheSize / 1000   + " k\n");
            sb.append("  HITS                = " + cacheHits / 1000   + " k\n");
            sb.append("  MISSES              = " + cacheMisses / 1000 + " k\n");
            sb.append("  RATIO (hits/misses) = " + String.format("%.1f", ((float)cacheHits / (float)cacheMisses)) + "\n");
            sb.append("  RATIO (hits/total)  = " + String.format("%.1f", ((float)cacheHits / (mnodes * 1000000)))             + "\n");

            return sb.toString();
        }
    }

    private static class Coin {
        public static final byte NONE = 0;
        public static final byte BLUE = 1;
        public static final byte RED = 2;

        private static Winner toWinner(byte coin) {
            switch (coin) {
                case Coin.BLUE:
                    return Winner.BLUE;
                case Coin.RED:
                    return Winner.RED;
                default:
                    throw new RuntimeException();
            }
        }
    }

    private class BoardState {
        public long playerBoard;
        public long opponentBoard;

        public BoardState(long playerBoard, long opponentBoard) {
            this.playerBoard = playerBoard;
            this.opponentBoard = opponentBoard;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            for (int row = 0; row < height; row++) {
                for (int column = 0; column < width/2; column++) {
                    long leftMask = masks.getCoinMask(column, row);
                    long rightMask = masks.getCoinMask(width - column - 1, row);

                    int left = 0, right = 0;

                    if (Masks.intersectsMask(playerBoard, leftMask)) left = 1;
                    else if (Masks.intersectsMask(this.opponentBoard, leftMask)) left = 2;

                    if (Masks.intersectsMask(playerBoard, rightMask)) right = 1;
                    else if (Masks.intersectsMask(this.opponentBoard, rightMask)) right = 2;

                    hash = hash * 7 + left + right;
                }
            }

            return hash;
        }

        @Override
        public boolean equals(Object that) {
            if (that == null) return false;
            if (this == that) return true;
            if (getClass() != that.getClass()) return false;

            BoardState thatBoardState = (BoardState) that;
            if (thatBoardState.playerBoard == playerBoard && thatBoardState.opponentBoard == opponentBoard)
                return true;

            return false;
        }
    }

    private class ResultKeyComparator implements Comparator<AbstractMap.SimpleEntry<Integer, Float>> {
        @Override
        public int compare(AbstractMap.SimpleEntry<Integer, Float> a, AbstractMap.SimpleEntry<Integer, Float> b) {
            int center = width / 2;

            return Math.abs(center - a.getKey()) - Math.abs(center - b.getKey());
        }
    }

    private class ResultValueComparator implements Comparator<AbstractMap.SimpleEntry<Integer, Float>> {
        @Override
        public int compare(AbstractMap.SimpleEntry<Integer, Float> a, AbstractMap.SimpleEntry<Integer, Float> b) {
            return Float.compare(b.getValue(), a.getValue());
        }
    }
}