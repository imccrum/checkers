package imccrum.com.checkers;

/**
 * Created by ian on 08/02/15.
 */
public class GameInfo {

  private String gameId;
  private String opponent;

  public GameInfo(String gameId, String opponent) {
    this.gameId = gameId;
    this.opponent = opponent;
  }

  public String getGameId() {
    return gameId;
  }

  public String getOpponent() {
    return opponent;
  }
}
