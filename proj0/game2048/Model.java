package game2048;

import java.util.Formatter;
import java.util.Iterator;
import java.util.Observable;


/** The state of a game of 2048.
 *  @author TODO: filwsx
 */
public class Model extends Observable {
    /** Current contents of the board. */
    private Board board;
    /** Current score. */
    private int score;
    /** Maximum score so far.  Updated when game ends. */
    private int maxScore;
    /** True iff game is ended. */
    private boolean gameOver;

    /* Coordinate System: column C, row R of the board (where row 0,
     * column 0 is the lower-left corner of the board) will correspond
     * to board.tile(c, r).  Be careful! It works like (x, y) coordinates.
     */

    /** Largest piece value. */
    public static final int MAX_PIECE = 2048;

    /** A new 2048 game on a board of size SIZE with no pieces
     *  and score 0. */
    public Model(int size) {
        board = new Board(size);
        score = maxScore = 0;
        gameOver = false;
    }

    /** A new 2048 game where RAWVALUES contain the values of the tiles
     * (0 if null). VALUES is indexed by (row, col) with (0, 0) corresponding
     * to the bottom-left corner. Used for testing purposes. */
    public Model(int[][] rawValues, int score, int maxScore, boolean gameOver) {
        int size = rawValues.length;
        board = new Board(rawValues, score);
        this.score = score;
        this.maxScore = maxScore;
        this.gameOver = gameOver;
    }

    /** Return the current Tile at (COL, ROW), where 0 <= ROW < size(),
     *  0 <= COL < size(). Returns null if there is no tile there.
     *  Used for testing. Should be deprecated and removed.
     *  */
    public Tile tile(int col, int row) {
        return board.tile(col, row);
    }

    /** Return the number of squares on one side of the board.
     *  Used for testing. Should be deprecated and removed. */
    public int size() {
        return board.size();
    }

    /** Return true iff the game is over (there are no moves, or
     *  there is a tile with value 2048 on the board). */
    public boolean gameOver() {
        checkGameOver();
        if (gameOver) {
            maxScore = Math.max(score, maxScore);
        }
        return gameOver;
    }

    /** Return the current score. */
    public int score() {
        return score;
    }

    /** Return the current maximum game score (updated at end of game). */
    public int maxScore() {
        return maxScore;
    }

    /** Clear the board to empty and reset the score. */
    public void clear() {
        score = 0;
        gameOver = false;
        board.clear();
        setChanged();
    }

    /** Add TILE to the board. There must be no Tile currently at the
     *  same position. */
    public void addTile(Tile tile) {
        board.addTile(tile);
        checkGameOver();
        setChanged();
    }

    /** Tilt the board toward SIDE. Return true iff this changes the board.
     *
     * 1. If two Tile objects are adjacent in the direction of motion and have
     *    the same value, they are merged into one Tile of twice the original
     *    value and that new value is added to the score instance variable
     * 2. A tile that is the result of a merge will not merge again on that
     *    tilt. So each move, every tile will only ever be part of at most one
     *    merge (perhaps zero).
     * 3. When three adjacent tiles in the direction of motion have the same
     *    value, then the leading two tiles in the direction of motion merge,
     *    and the trailing tile does not.
     * */
    public boolean tilt(Side side) {
        boolean changed;
        changed = false;

        // TODO: Modify this.board (and perhaps this.score) to account
        // for the tilt to the Side SIDE. If the board changed, set the
        // changed local variable to true.
        int len = board.size();
		// 把所有的滑動方向都轉化爲向上滑
        board.setViewingPerspective(side);
		// 對每一列進行處理
        for (int col = 0; col < len; col++) {
			/*
			 * 核心思想就是只處理兩個相鄰非空tile之間的關係
			 * 如何跳過null的title。
			 * index保存著要處理的tile的索引
			 * 邏輯梳理:
				 * 假設下一個不爲空的tile的value和index一樣，就合并
				 * 不一樣的話，可能是index指向爲空，那麽還是可以移動
				 * 以上都不是，那麽就要下移index。此處要注意的一個問題，是我在玩的過程中發現，下班路上相同的
				 * 就是nextTile不爲空，也不和IndexTile一樣，但是兩者之間有空隙，對此情況不處理會導致不移動情況產生
				 * 這種是想不到的，只能測試運行分析，從而發現。
			*/
            int index = len - 1;
            for (int row = len-2; row >= 0; row--) {
                Tile indexTile = board.tile(col,index);
                Tile nextTile = board.tile(col,row);
				// 跳過null元素
                if(nextTile == null){
                    continue;
                }
                else{
                    // 可以替換
                    if(indexTile == null){
                        board.move(col,index,nextTile);
                        changed = true;
						// 此處沒有index--是因爲，這種move不是合并，存在下一個tile可以合并
                    }
                    // 可以合并
                    else if(indexTile.value() == nextTile.value()){
                        int value = indexTile.value();
                        if(board.move(col,index,nextTile)){
                            score += value*2;
                        }
                        changed = true;
						// 該tile合并過了，需要調過了來實現rule2.
                        index--;
                    }
                    // 既不能替換，也不能合并，看index接下來是不是爲空
                    else {
                        index--;
						// 兩個tile之間存在空隙，那麽不能合并也可以靠近
                        if(index - row > 0){
                            board.move(col,index,nextTile);
                            changed = true;
                        }
                    }
                }
            }
        }
		// 恢復二維表到正常順序。不然還是reverser過的，test過不去。
		// test Model一直過不去的重要原因就是此語句。我也是觀察力test反饋的結果，逐漸意識到的
		// test對寫代碼真有用，好的測試樣例編寫也困難，因爲覆蓋盡可能所有情況。
        board.setViewingPerspective(Side.NORTH);
        checkGameOver();
        if (changed) {
            setChanged();
        }
        return changed;
    }

    /** Checks if the game is over and sets the gameOver variable
     *  appropriately.
     */
    private void checkGameOver() {
        gameOver = checkGameOver(board);
    }

    /** Determine whether game is over. */
    private static boolean checkGameOver(Board b) {
        return maxTileExists(b) || !atLeastOneMoveExists(b);
    }

    /** Returns true if at least one space on the Board is empty.
     *  Empty spaces are stored as null.
     * */
    public static boolean emptySpaceExists(Board b) {
        // TODO: Fill in this function.
        for (Iterator<Tile> iterator = b.iterator(); iterator.hasNext(); ) {
            Tile tile = iterator.next();
            if (tile == null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if any tile is equal to the maximum valid value.
     * Maximum valid value is given by MAX_PIECE. Note that
     * given a Tile object t, we get its value with t.value().
     */
    public static boolean maxTileExists(Board b) {
        // TODO: Fill in this function.
        for (Iterator<Tile> iterator = b.iterator(); iterator.hasNext(); ) {
            Tile tile = iterator.next();
            if (tile != null && tile.value() == MAX_PIECE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if there are any valid moves on the board.
     * There are two ways that there can be valid moves:
     * 1. There is at least one empty space on the board.
     * 2. There are two adjacent tiles with the same value.
     */
    public static boolean atLeastOneMoveExists(Board b) {
        // TODO: Fill in this function.
        if (emptySpaceExists(b)){
            return true;
        }

        for (Iterator<Tile> iterator = b.iterator(); iterator.hasNext(); ) {
            Tile tile = iterator.next();
            int tileValue = tile.value();
            int i = tile.col();
            int j = tile.row();
            if (i - 1 >= 0) {
                if (tileValue == b.tile(i - 1, j).value()) {
                    return true;
                }
            }
            if (i + 1 < b.size()) {
                if (tileValue == b.tile(i + 1, j).value()) {
                    return true;
                }
            }
            if (j - 1 >= 0) {
                if (tileValue == b.tile(i, j - 1).value()) {
                    return true;
                }
            }
            if (j + 1 < b.size()) {
                if (tileValue == b.tile(i, j + 1).value()) {
                    return true;
                }
            }
        }
        return false;
    }


    @Override
     /** Returns the model as a string, used for debugging. */
    public String toString() {
        Formatter out = new Formatter();
        out.format("%n[%n");
        for (int row = size() - 1; row >= 0; row -= 1) {
            for (int col = 0; col < size(); col += 1) {
                if (tile(col, row) == null) {
                    out.format("|    ");
                } else {
                    out.format("|%4d", tile(col, row).value());
                }
            }
            out.format("|%n");
        }
        String over = gameOver() ? "over" : "not over";
        out.format("] %d (max: %d) (game is %s) %n", score(), maxScore(), over);
        return out.toString();
    }

    @Override
    /** Returns whether two models are equal. */
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (getClass() != o.getClass()) {
            return false;
        } else {
            return toString().equals(o.toString());
        }
    }

    @Override
    /** Returns hash code of Model’s string. */
    public int hashCode() {
        return toString().hashCode();
    }
}
