import java.util.*;
import java.math.BigInteger;
import net.sf.javabdd.*;

public class QueensLogic {
    private int total;
    private int n;
    private int[][] board;

    private BDDFactory fact;
    private BDD queenRule;

    public void initializeGame(int size) {
        this.total = size*size;
        this.n     = size;
        this.board = new int[n][n];

        this.fact = JFactory.init(2000000, 200000);
        fact.setVarNum(total);

        buildQueenRule();
        buildBoardRules();
    }

    public int[][] getGameBoard() {
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                BDD r = queenRule.restrict(var(x, y));
                if (r.isZero()) board[x][y] = -1;
            }
        }

        return board;
    }

    public boolean insertQueen(int x, int y) {
        if (board[x][y] != 0) return false;

        queenRule.restrictWith(var(x, y));

        if (queenRule.pathCount() == 1) {
            for (int j = 0; j < n; j++) {
                for (int i = 0; i < n; i++) {
                    board[i][j] = board[i][j] == 0 ? 1 : board[i][j];
                }
            }
        }

        board[x][y] = 1;
        return true;
    }

    private int index(int x, int y) {
        return y*n + x;
    }

    private BDD var(int x, int y) {
        return fact.ithVar(index(x, y));
    }

    private void buildBoardRules() {
        BDD we   = fact.one(),
            ns   = fact.one(),
            nwse = fact.one(),
            nesw = fact.one();

        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                int delta;

                // Add W->E rule
                for (int i = 0; i < n; i++) {
                    if (i == x) continue;

                    BDD var  = var(x, y);
                    BDD that = var(i, y);
                    we.andWith(var.apply(that, BDDFactory.nand));
                }

                // Add N->S rule
                for (int i = 0; i < n; i++) {
                    if (i == y) continue;

                    BDD var  = var(x, y);
                    BDD that = var(x, i);
                    ns.andWith(var.apply(that, BDDFactory.nand));
                }

                // Add NW->SE rule
                delta = Math.min(x, y);
                for (int i = 0; i < n - Math.abs(x - y); i++) {
                    int dx = x - delta + i;
                    int dy = y - delta + i;

                    if (dx == x && dy == y) continue;

                    BDD var  = var(x, y);
                    BDD that = var(dx, dy);
                    nwse.andWith(var.apply(that, BDDFactory.nand));
                }

                // Add NE->SW rule
                delta = Math.min(n - x - 1, y);
                for (int i = 0; i < n - Math.abs(n - x - y - 1); i++) {
                    int dx = x + delta - i;
                    int dy = y - delta + i;

                    if (dx == x && dy == y) continue;

                    BDD var  = var(x, y);
                    BDD that = var(dx, dy);
                    nesw.andWith(var.apply(that, BDDFactory.nand));
                }
            }
        }

        we.andWith(ns);
        nwse.andWith(we);
        nesw.andWith(nwse);
        queenRule.andWith(nesw);
    }

    private void buildQueenRule() {
        queenRule = fact.one();

        for (int y = 0; y < n; y++) {
            BDD rowRule = fact.zero();

            for (int x = 0; x < n; x++) {
                rowRule.orWith(var(x, y));
            }

            queenRule.andWith(rowRule);
        }
    }
}
