package imccrum.com.checkers;

/**
 * Created by ian on 27/01/15.
 */
public class Content {

  private boolean isArray;
  private String[] obj;

  public Content(boolean isArray, String[] obj) {

    this.isArray = isArray;
    this.obj = obj;
  }

  public boolean isArray() {
    return isArray;
  }

  public String[] getObj() {
    return obj;
  }

}
