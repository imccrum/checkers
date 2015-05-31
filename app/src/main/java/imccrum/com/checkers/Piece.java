package imccrum.com.checkers;

import java.io.Serializable;

public class Piece implements Serializable {

  private static final long serialVersionUID = 1L;

  private int id;
  private boolean isKing;
  private int row;
  private int col;
  private float posCol;
  private float posRow;
  private boolean isPlayer;
  private Player player;

  public Piece(int id, int col, int row, boolean isPlayer, boolean isKing) {
    // just for display
    this.id = id;
    this.row = row;
    this.col = col;
    this.isPlayer = isPlayer;
    this.isKing = isKing;
  }

  public Piece(Player player, int id, int col, int row, boolean isPlayer, boolean isKing) {

    this.player = player;
    this.id = id;
    this.row = row;
    this.col = col;
    this.isPlayer = isPlayer;
    this.isKing = isKing;
  }

  public Piece(Piece piece) {

    this.player = piece.getPlayer();
    this.id = piece.getId();
    this.row = piece.getRow();
    this.col = piece.getCol();
    this.posCol = piece.getPosCol();
    this.posRow = piece.getPosRow();
    this.isPlayer = piece.isPlayer();
    this.isKing = piece.isKing();
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getRow() {
    return row;
  }

  public void setRow(int row) {
    this.row = row;
  }

  public int getCol() {
    return col;
  }

  public void setCol(int y) {
    this.col = y;
  }

  public float getPosCol() {
    return posCol;
  }

  public void setPosCol(float posCol) {
    this.posCol = posCol;
  }

  public float getPosRow() {
    return posRow;
  }

  public void setMove(int col, int row, float posCol, float posRow, boolean isKing) {
    this.col = col;
    this.row = row;
    this.posCol = posCol;
    this.posRow = posRow;
    this.isKing = isKing;
  }

  public void setPosRow(float posRow) {
    this.posRow = posRow;
  }

  public boolean isPlayer() {
    return isPlayer;
  }

  public Player getPlayer() { return player; }

  public boolean isKing() {
    return isKing;
  }

  public void setKing(boolean isKing) {
    this.isKing = isKing;
  }
}
