package imccrum.com.checkers;

public class Suggestion {

  private int sugX;
  private int sugY;

  public Suggestion(int sugX, int sugY) {

    this.sugX = sugX;
    this.sugY = sugY;
  }

  public int getSugX() {
    return sugX;
  }

  public void setSugX(int sugX) {
    this.sugX = sugX;
  }

  public int getSugY() {
    return sugY;
  }

  public void setSugY(int sugY) {
    this.sugY = sugY;
  }
}
