import java.util.*;
import net.sf.javabdd.*;

public class QueensLogic {
    private int total;
    private int n;
    private int[][] board;

    private BDDFactory fact;
    private BDD[][] boardRules;
    private BDD queenRule;

    public QueensLogic() {

    }

    public void initializeGame(int size) {
        this.total = size*size;
        this.n     = size;
        this.board = new int[n][n];

        this.fact = JFactory.init(2000000, 200000);
        fact.setVarNum(total);

        this.boardRules = new BDD[n][n];
        buildBoardRules();
        buildQueenRule();
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
        if (board[x][y] != 0) return true;

        //restrictBoardRules(x, y);
        queenRule.restrictWith(var(x, y));

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
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                BDD rule = boardRules[x][y] = fact.one();

                // Add W->E rule
                for (int i = 0; i < n; i++) {
                    if (i == x) continue;

                    BDD var  = var(x, y);
                    BDD that = var(i, y);
                    rule.andWith(var.apply(that, BDDFactory.nand));
                }

                // Add N->S rule
                for (int i = 0; i < n; i++) {
                    if (i == y) continue;

                    BDD var  = var(x, y);
                    BDD that = var(x, i);
                    rule.andWith(var.apply(that, BDDFactory.nand));
                }

                // Add NW->SE rule
                // TODO

                // Add NE->SW rule
                // TODO
            }
        }
    }

    // private void restrictBoardRules(int vx, int vy) {
    //     for (int ry = 0; ry < n; ry++) {
    //         for (int rx = 0; rx < n; rx++) {
    //             BDD var = var(vx, vy);
    //             boardRules[rx][ry].restrictWith(var);
    //         }
    //     }
    // }

    private void buildQueenRule() {
        queenRule = fact.one();

        for (int y = 0; y < n; y++) {
            BDD rowRule = fact.zero();

            for (int x = 0; x < n; x++) {
                BDD boardRule = boardRules[x][y];
                rowRule.orWith(boardRule.id());
            }

            queenRule.andWith(rowRule);
        }
    }
}

// for (int y = 0; y < n; y++) {
//     for (int x = 0; x < n; x++) {
           
//     }
// }