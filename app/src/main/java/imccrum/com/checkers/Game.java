package imccrum.com.checkers;

/**
 * Created by ian on 02/02/15.
 */

import java.io.Serializable;


public class Game implements Serializable {

  private static final long serialVersionUID = 1L;

  private Player player;
  private Player opponent;
  private String gameId;
  private Square[][] board;

  public Game(String gameId, Square[][] board) {

    this.gameId = gameId;
    this.board = board;
  }

  public Game(Player player, Player opponent, Square[][] board) {

    this.player = player;
    this.opponent = opponent;
    this.board = board;
  }

  public void setPlayers(Player player, Player opponent) {
    this.player = player;
    this.opponent = opponent;
  }

  public String getId() {
    return gameId;
  }

  public Player getPlayer() {
    return player;
  }

  public Player getOpponent() {
    return opponent;
  }

  public Square[][] getBoard() {
    return board;
  }
}
