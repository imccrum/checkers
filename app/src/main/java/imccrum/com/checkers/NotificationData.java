package imccrum.com.checkers;

/**
 * Created by hal on 03/05/15.
 */
public class NotificationData {

  private String gameId;
  private int id;

  public NotificationData(String gameId, int id) {
    this.gameId = gameId;
    this.id = id;
  }

  public String getGameId() {
    return gameId;
  }

  public int getId() {
    return id;
  }
}
