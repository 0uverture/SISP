public class Adaptor{
	IGameLogic bot;
	int botId;
	int mCols = 1, mRows = 1, player = 1;
	int[] cols;

	public Adaptor(){
		bot = new MaekGameLogic();
	}

	public void setColumns(int cols) {
		mCols = cols;
		this.cols = new int[mCols];
		//System.out.println("Setting cols: " + mCols);
		init();
	}

	public void setRows(int rows) {
		mRows = rows;
		//System.out.println("Setting rows" +  mRows);
		init();
	}

	public void setBotId(int id){
		botId = id;
	}

	public void update(int[][] field){
		for(int i = 0; i < mCols; i++){
			if(cols[i] == -1)
				continue;
			int id = field[i][cols[i]]; 
			if(id != 0){
				cols[i]--;
				int pl = (botId == id) ? 1 : 2;
				bot.insertCoin(i, pl);
				return;
			}
		}
	}

	public int makeTurn(int time){
		return bot.decideNextMove();
	}

	private void init(){
		for(int i = 0; i < mCols; i++){
			cols[i] = mRows - 1;
		}
		bot.initializeGame(mCols, mRows, player);
	}
}