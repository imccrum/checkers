package imccrum.com.checkers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

public class Player implements Serializable {

  private static final long serialVersionUID = 1L;

  private int numPieces;
  private boolean isTop;
  private boolean isTurn;
  private ArrayList<Piece> pieces;
  private String username;

  public Player(boolean isTurn, boolean isTop, String username, ArrayList<Piece>pieces) {

    numPieces = 12;
    this.isTop = isTop;
    this.pieces = pieces;
    this.isTurn = isTurn;
    this.username = username;
  }

  public int getNumPieces() {
    return numPieces;
  }

  public void setNumPieces(int numPieces) {
    this.numPieces = numPieces;
  }

  public int takePiece(Piece capture) {
    numPieces--;
    Iterator<Piece> it = pieces.iterator();
    while (it.hasNext()) {
      Piece piece = it.next();
      if (piece == capture) {
        it.remove();
      }
    }
    return pieces.size();
  }

  public ArrayList<Piece> getPieces() {
    return pieces;
  }

  public boolean isTurn() {
    return isTurn;
  }

  public boolean isTop() { return isTop; }

  public void setTurn(boolean isTurn) {
    this.isTurn = isTurn;
  }

  public String getUsername() {
    return username;
  }

  public Piece getPiece(int id) {
    for (Piece piece: pieces) {
      if (piece.getId() == id) {
        return piece;
      }
    }
    return null;
  }

}
