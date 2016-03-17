import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;

public class Benchmark {
    private enum Coin {
        NONE, BLUE, RED
    }

    private static final int WIDTH = 7;
    private static final int HEIGHT = 6;

    private static final int BLUE = 1;
    private static final int RED = 2;

    private static final String BASE_URL = "http://cloud.cs.berkeley.edu:8090/gcweb/service/gamesman/puzzles/connect4/getNextMoveValues;";

    private IGameLogic gameLogic;

    private Coin[][] board;
    private int[] columnHeights;
    private ArrayList<Integer> columns;
    private ArrayList<Integer> boardDists;
    private ArrayList<Integer> boardDeltas;

    public Benchmark(IGameLogic gameLogic) {
        this.gameLogic = gameLogic;

        board = new Coin[WIDTH][HEIGHT];
        columnHeights = new int[WIDTH];
        columns = new ArrayList();
        boardDists = new ArrayList();
        boardDeltas = new ArrayList();

        for (int column = 0; column < WIDTH; column++)
            for (int row = 0; row < HEIGHT; row++)
                board[column][row] = Coin.NONE;

        run();
    }

    private void run() {
        gameLogic.initializeGame(WIDTH, HEIGHT, BLUE);

        int turn = BLUE;
        while (gameLogic.gameFinished() == IGameLogic.Winner.NOT_FINISHED) {
            if (turn == BLUE) {
                int column = gameLogic.decideNextMove();

                ArrayList<Move> moves = fetchMoves();
                if (moves == null) break;

                moves.sort(new MoveComparator());
                Move bestMove = moves.get(0);
                int bestValue = bestMove.value != MoveValue.WIN ? 42 + (42 - bestMove.remoteness) : bestMove.remoteness;

                for (int i = 0; i < moves.size(); i++) {
                    if (moves.get(i).column == column) {
                        Move move = moves.get(i);
                        int value = move.value != MoveValue.WIN ? 42 + (42 - move.remoteness) : move.remoteness;

                        boardDists.add(value);
                        boardDeltas.add(value - bestValue);

                        break;
                    }
                }

                columns.add(column);
                board[column][columnHeights[column]++] = Coin.BLUE;
                gameLogic.insertCoin(column, BLUE);
            }
            else {
                ArrayList<Move> moves = fetchMoves();
                if (moves == null) break;

                moves.sort(new MoveComparator());
                Move chosen = moves.get(0);

                boardDists.add(chosen.value != MoveValue.LOSE ? 42 + (42 - chosen.remoteness) : chosen.remoteness);
                boardDeltas.add(0);
                int column = moves.get(0).column;

                columns.add(column);
                board[column][columnHeights[column]++] = Coin.RED;
                gameLogic.insertCoin(column, RED);
            }

            turn = turn == BLUE ? RED : BLUE;
        }

        System.out.println("Game over");
        System.out.println("Winner = " + gameLogic.gameFinished());
        System.out.println();
        System.out.println("PLY  COL  DIST  DELTA");
        for (int i = 0; i < boardDists.size(); i++) {
            System.out.println(String.format("%02d   %02d   %02d    %02d", i, columns.get(i), boardDists.get(i), boardDeltas.get(i)));
        }
    }

    private ArrayList<Move> fetchMoves() {
        String response;

        try {
            URL url = new URL(BASE_URL +
                    "board=" + getBoardString() +
                    ";width=" + WIDTH +
                    ";height=" + HEIGHT +
                    ";pieces=4");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();

            response = sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
        }

        JSONObject json = new JSONObject(response);
        JSONArray jsonMoves = json.getJSONArray("response");

        if (jsonMoves.length() == 0)
            return null; // Game over

        ArrayList<Move> moves = new ArrayList();
        for (int i = 0; i < jsonMoves.length(); i++) {
            JSONObject jsonMove = jsonMoves.getJSONObject(i);

            Move move = new Move();
            move.column     = jsonMove.getInt("move");
            move.remoteness = jsonMove.getInt("remoteness");
            move.value      = MoveValue.fromString(jsonMove.getString("value"));

            moves.add(move);
        }

        return moves;
    }

    private String getBoardString() {
        StringBuilder sb = new StringBuilder();

        for (int row = 0; row < HEIGHT; row++) {
            for (int column = 0; column < WIDTH; column++) {
                String symbol;
                switch (board[column][row]) {
                    case BLUE:
                        symbol = "X";
                        break;
                    case RED:
                        symbol = "O";
                        break;
                    default:
                        symbol = "%20";
                        break;
                }

                sb.append(symbol);
            }
        }

        return sb.toString();
    }

    public static void main(String[] args) {
        IGameLogic gameLogic = null;

        boolean err = args.length < 1;
        String errMsg = "";

        try {
            gameLogic = parseGameLogicParam(args[0]);
        } catch(ClassNotFoundException cnf) {
            errMsg = cnf.toString();
            err = true;
        } catch(NoSuchMethodException nsme) {
            errMsg = "Your GameInstance had no constructor.";
            err = true;
        } catch(InstantiationException ie) {
            errMsg = "Your GameInstance could not be instantiated.";
            err = true;
        } catch(IllegalAccessException iae) {
            errMsg = "Your GameInstance caused an illegal access exception.";
            err = true;
        } catch(InvocationTargetException ite) {
            errMsg = "Your GameInstance constructor threw an exception: " + ite.toString();
            err = true;
        }

        if(err) {
            System.out.println(errMsg);
            System.exit(1);
        }

        Benchmark benchmark = new Benchmark(gameLogic);
    }

    public static IGameLogic parseGameLogicParam(String cmdParam)
            throws ClassNotFoundException, NoSuchMethodException,
            InstantiationException, IllegalAccessException,
            InvocationTargetException {
        return (IGameLogic)Class.forName(cmdParam).getConstructor().newInstance();
    }

    private enum MoveValue {
        WIN, LOSE, TIE;

        public static MoveValue fromString(String s) {
            switch (s) {
                case "win":  return WIN;
                case "lose": return LOSE;
                case "tie":  return TIE;
                default:     throw new RuntimeException();
            }
        }
    }

    private class Move {

        public int column;
        public int remoteness;
        public MoveValue value;
    }

    private class MoveComparator implements Comparator<Move> {
        @Override
        public int compare(Move a, Move b) {
            int aReal = a.remoteness;
            if (a.value == MoveValue.LOSE) aReal = 42 + (42 - aReal);

            int bReal = b.remoteness;
            if (b.value == MoveValue.LOSE) bReal = 42 + (42 - bReal);

            return aReal - bReal;
        }
    }
}
