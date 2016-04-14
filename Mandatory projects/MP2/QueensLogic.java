/**
 * This class implements the logic behind the BDD for the n-queens problem
 * You should implement all the missing methods
 * 
 * @author Stavros Amanatidis
 *
 */
import java.util.*;

import net.sf.javabdd.*;

public class QueensLogic {
    private int width = 0;
    private int height = 0;
    private int size = 0;
    private int[][] board;
        
    private BDDFactory fact;
    private BDD[] boardVars;
    private BDD[] boardConstraints;
    private BDD problem;

    public QueensLogic() {
    }

    public void initializeGame(int size) {
        this.width = size;
        this.height = size;
        this.size = size*size;
        this.board = new int[size][size];
        
        this.fact = JFactory.init(size, size);
        fact.setVarNum(size*size);

        this.boardVars = createBoardVars();
        this.boardConstraints = createBoardConstraints();
    }

    private BDD[] createBoardVars() {
        BDD[] vars = new BDD[size];
        for (int i = 0; i < size; i++) {
            vars[i] = board[i%width][i/width] == 1 ? fact.one() : fact.zero();
        }
        return vars;
    }

    private BDD[] createBoardConstraints() {
        BDD[] constraints = new BDD[size];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = y*width+x;

                constraints[i] =
                    createAttackBDD(x, y, -1,  0 ).andWith( // N
                    createAttackBDD(x, y, -1,  1)).andWith( // NE
                    createAttackBDD(x, y,  0,  1)).andWith( // E
                    createAttackBDD(x, y,  1,  1)).andWith( // SE
                    createAttackBDD(x, y,  1,  0)).andWith( // S
                    createAttackBDD(x, y,  1, -1)).andWith( // SW
                    createAttackBDD(x, y,  0, -1)).andWith( // W
                    createAttackBDD(x, y, -1, -1));         // NW
            }
        }

        problem = fact.one();

        for (int y = 0; y < height; y++) {
            if (rowHasQueen(y)) continue;

            BDD row = fact.zero();

            for (int x = 0; x < width; x++) {
                int i = y*width+x;

                row.orWith(constraints[i].id());
            }

            problem.andWith(row);
        }

        return constraints;
    }

    private boolean rowHasQueen(int y) {
        int c = 0;
        for (int x = 0; x < width; x++) {
            if (board[x][y] == 1)
                c++;
        }
        return c == 1;
    }

    private BDD createAttackBDD(int x, int y, int down, int right) {
        BDD currVar = fact.zero();
        BDD result = fact.one();

        while(x >= 0 && x < width && y >= 0 && y < height) {
            int i = y*width+x;
            BDD constraint = currVar.apply(boardVars[i], BDDFactory.nand);
            result.andWith(constraint);

            x += right;
            y += down;
        }

        return result;
    }

    public int[][] getGameBoard() {
        int[][] resultBoard = new int[width][height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = y*width+x;

                int original = board[x][y];
                board[x][y] = 1;

                boardVars = createBoardVars();
                boardConstraints = createBoardConstraints();


                int result = original;
                double satCount = problem.satCount();

                System.out.println(i +  ": " + satCount);

                if (satCount == 0 && result != 1)
                    result = -1;

                resultBoard[x][y] = result;


                board[x][y] = original;
            }
        }

        return resultBoard;
    }

    public boolean insertQueen(int x, int y) {
        board[x][y] = 1;

        boardVars = createBoardVars();
        boardConstraints = createBoardConstraints();

        return true;
    }
}
