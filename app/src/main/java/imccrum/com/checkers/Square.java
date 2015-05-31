package imccrum.com.checkers;

import android.view.SurfaceHolder;

import java.io.Serializable;

public class Square implements Serializable {

  private static final long serialVersionUID = 1L;

  private int color;
  private float x;
  private float y;
  private boolean hint;
  private Piece piece;

  public Square(int color, float x, float y) {
    this.color = color;
    this.x = x;
    this.y = y;
    hint = false;
    piece = null;
  }

  public Piece getPiece() {
    return piece;
  }

  public void setPiece(Piece piece) {
    this.piece = piece;
  }

  public void setNull(SurfaceHolder sh) {
    synchronized (sh) {
      piece = null;
    }
  }

  public int getColor() {
    return color;
  }

  public void setColor(int color) {
    this.color = color;
  }

  public boolean isHint() {
    return hint;
  }

  public void setHint(boolean hint) {
    this.hint = hint;
  }

  public float getX() {
    return x;
  }

  public void setX(float x) {
    this.x = x;
  }

  public float getY() {
    return y;
  }

  public void setY(float y) {
    this.y = y;
  }
}
