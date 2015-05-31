package imccrum.com.checkers;

/**
 * Created by ian on 18/02/15.
 */
public class Move {

  private int id;
  private int moveCol;
  private int moveRow;
  private int captId;

  public Move(int id, int moveCol, int moveRow, int captId) {

    this.id = id;
    this.moveCol = moveCol;
    this.moveRow = moveRow;
    this.captId = captId;
  }

  public int getId() {
    return id;
  }

  public int getMoveCol() {
    return moveCol;
  }

  public int getMoveRow() {
    return moveRow;
  }

  public int getCaptId() {
    return captId;
  }

}
