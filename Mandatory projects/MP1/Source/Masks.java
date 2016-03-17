public class Masks {
    private final int width;
    private final int height;

    public final Won won;

    public Masks(int width, int height) {
        this.width = width;
        this.height = height;

        won = new Won();
    }

    public int getOffset(int column, int row) {
        return row * width + column;
    }

    public long getCoinMask(int column, int row) {
        return 1l << getOffset(column, row);
    }

    public long getLineMask(int column, int row, int up, int right, int length) {
        long line = 0;
        while(column >= 0 && column < width && row >= 0 && row < height) {
            if (length-- <= 0) break;

            line |= getCoinMask(column, row);

            column += right;
            row += up;
        }

        return line;
    }

    public void printMask(long mask) {
        for (long i = 0; i <= 41; i++) {
            System.out.print(((1l << i) & mask) != 0 ? 1 : 0);

            if ((i + 1) % width == 0)
                System.out.println();
        }
    }

    public static boolean intersectsMask(long target, long mask) {
        return mask != 0 && (target & mask) == mask;
    }

    public static int hammingWeight(long x) {
        x -= (x >> 1) & 0x5555555555555555L;
        x = (x & 0x3333333333333333L) + ((x >> 2) & 0x3333333333333333L);
        x = (x + (x >> 4)) & 0x0f0f0f0f0f0f0f0fL;
        return (int)((x * 0x0101010101010101L)>>56);
    }

    public class Won {
        public final long[] N;
        public final long[] NE;
        public final long[] E;
        public final long[] SE;
        public final long[] S;
        public final long[] SW;
        public final long[] W;
        public final long[] NW;

        public Won() {
            N  = getAllWonLineMasks(1, 0);
            NE = getAllWonLineMasks(1, 1);
            E  = getAllWonLineMasks(0, 1);
            SE = getAllWonLineMasks(-1, 1);
            S  = getAllWonLineMasks(-1, 0);
            SW = getAllWonLineMasks(-1, -1);
            W  = getAllWonLineMasks(0, -1);
            NW = getAllWonLineMasks(1, -1);
        }

        private long[] getAllWonLineMasks(int up, int right) {
            long[] lines = new long[width * height];
            final int upLength = up * 4;
            final int rightLength = right * 4;

            for (int column = 0; column < width; column++) {
                for (int row = 0; row < height; row++) {
                    int offset = getOffset(column, row);

                    if (column + rightLength > width || column + rightLength < -1 || row + upLength > height || row + upLength < -1)
                        lines[offset] = 0;
                    else
                        lines[offset] = getLineMask(column, row, up, right, 4);
                }
            }

            return lines;
        }
    }
}