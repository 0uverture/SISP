import Group12.*;

public class Group12GameLogic implements IGameLogic {
    private int x = 0;
    private int y = 0;
    private int playerID;

    private GameBoard board;

    public void initializeGame(int x, int y, int playerID) {
        this.x = x;
        this.y = y;
        this.playerID = playerID;

        board = new GameBoard(x, y, playerID);
    }
	
    public Winner gameFinished() {
        switch (board.getWinner()) {
            case NONE:
                return Winner.NOT_FINISHED;
            case BLUE:
                return Winner.PLAYER1;
            case RED:
                return Winner.PLAYER2;
            case TIE:
                return Winner.TIE;
            default:
                throw new RuntimeException();
        }
    }

    public void insertCoin(int column, int playerID) {
        board.insertCoin(column, playerID);
    }

    public int decideNextMove() {
        return board.decideNextMove();
    }
}
