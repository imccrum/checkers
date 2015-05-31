package imccrum.com.checkers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class GameSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

  private static final String TAG = "GameSurfaceView";
  private SurfaceHolder sh;

  private boolean touchable;
  private boolean dropable;
  private boolean takingPiece;
  private boolean checked;
  private boolean inPlay;

  private int viewWidth;
  private int viewHeight;
  private float sqHeight;
  private float sqWidth;
  private float radius;

  private final static int PALE = 0;
  private final static int DARK = 1;
  private final static int TOP_ROW = 0;
  private final static int BOTTOM_ROW = 7;

  private Context context;
  private GameActivity gameActivity;
  private GameThread gameThread;
  private Game game;
  private Piece pieceSelected;
  private Hint hint;
  private ArrayList<Suggestion> obligations;
  private ArrayList<Move> combinations = new ArrayList<>();
  private SharedPreferences pref;


  public GameSurfaceView(Context context, int viewWidth, int viewHeight, Game game) {

    super(context);

    this.context = context;
    this.viewWidth = viewWidth;
    this.viewHeight = viewHeight;
    this.game = game;
    inPlay = false;
    touchable = false;
    sh = getHolder();
    sh.addCallback(this);
    setFocusable(false);
  }

  public GameSurfaceView(GameActivity gameActivity, int viewWidth, int viewHeight, Game game) {

    super(gameActivity.getApplicationContext());

    context = gameActivity.getApplicationContext();
    this.gameActivity = gameActivity;
    this.viewWidth = viewWidth;
    this.viewHeight = viewHeight;
    this.game = game;
    inPlay = true;
    touchable = false;
    sh = getHolder();
    sh.addCallback(this);
    setFocusable(true);
    pref = PreferenceManager.getDefaultSharedPreferences(gameActivity);
  }

  public void surfaceCreated(SurfaceHolder holder) {

    sqWidth = (float) viewWidth / 8;
    sqHeight = (float) viewHeight / 8;
    radius = (float) (sqWidth * 0.42);

    for (int i = 0; i < 8; i++) {
      for (int j = 0; j < 8; j++) {
        if ((i % 2) == 0) {
          if ((j % 2) == 0) {
            game.getBoard()[i][j] = new Square(PALE, i * sqWidth, j * sqHeight);
          } else {
            game.getBoard()[i][j] = new Square(DARK, i * sqWidth, j * sqHeight);
          }
        } else {
          if ((j % 2) != 0) {
            game.getBoard()[i][j] = new Square(PALE, i * sqWidth, j * sqHeight);
          } else {
            game.getBoard()[i][j] = new Square(DARK, i * sqWidth, j * sqHeight);
          }
        }
      }
    }

    if (game.getPlayer() != null) {
      setPositions();
    }
    gameThread = new GameThread(sh, context, new Handler(), game.getBoard(), viewWidth,
            viewHeight, sqWidth, sqHeight, radius, inPlay);
    gameThread.start();
  }

  public void setPositions() {

    for (Piece piece : game.getPlayer().getPieces()) {
      piece.setPosCol(piece.getCol() * sqWidth + (sqWidth / 2));
      piece.setPosRow(piece.getRow() * sqHeight + (sqHeight / 2));
      game.getBoard()[piece.getCol()][piece.getRow()].setPiece(piece);
    }
    for (Piece piece : game.getOpponent().getPieces()) {
      piece.setPosCol(piece.getCol() * sqWidth + (sqWidth / 2));
      piece.setPosRow(piece.getRow() * sqHeight + (sqHeight / 2));
      game.getBoard()[piece.getCol()][piece.getRow()].setPiece(piece);
    }
  }

  public void displayMove(HashMap<String, String> obj) {

    ArrayList<Move> moves = new ArrayList<>();
    ArrayList<String> captures = new ArrayList<>();
    try {
      JSONObject move = new JSONObject(obj.get("move"));
      JSONArray moveCols = move.getJSONArray("moveCols");
      JSONArray moveRows = move.getJSONArray("moveRows");
      JSONArray captIds = move.getJSONArray("captIds");
      for (int count = 0; count < moveRows.length(); count++) {
        if (count < captIds.length()) {
          moves.add(new Move(move.getInt("id"), moveCols.getInt(count), moveRows.getInt(count), captIds.getInt(count)));
        } else {
          moves.add(new Move(move.getInt("id"), moveCols.getInt(count), moveRows.getInt(count), 0));
        }
      }
    } catch (JSONException je) {
      System.out.println(je);
    }

    if (game.getPlayer().getPiece(moves.get(0).getId()) != null) {
      for (int i = 0; i < moves.size(); i++) {
        captures = updatePositionsForMove(game.getOpponent(), game.getPlayer(), moves, captures, i);
      }
      gameThread.isResetPositions(true);
      gameThread.resume();
    } else {
    MoveTimer moveTimer = new MoveTimer((moves.size() + 1) * 800, 800, moves, captures);
    moveTimer.start();
    }
  }

  public ArrayList<String> prepareOpponentMove(Player captured, Player mover, ArrayList<Move>
          moves, ArrayList<String> captures, Integer i) {
    return updatePositionsForMove(captured, mover, moves, captures, i);
  }

  public ArrayList<String> updatePositionsForMove(Player captured, Player mover, ArrayList<Move>
          moves, ArrayList<String> captures, Integer i) {

    int oldCol = mover.getPiece(moves.get(i).getId()).getCol();
    int oldRow = mover.getPiece(moves.get(i).getId()).getRow();
    mover.getPiece(moves.get(i).getId()).setCol(moves.get(i).getMoveCol());
    mover.getPiece(moves.get(i).getId()).setRow(moves.get(i).getMoveRow());
    mover.getPiece(moves.get(i).getId()).setPosCol(moves.get(i).getMoveCol() * sqWidth + sqWidth / 2);
    mover.getPiece(moves.get(i).getId()).setPosRow(moves.get(i).getMoveRow() * sqHeight + sqHeight / 2);
    game.getBoard()[moves.get(i).getMoveCol()][moves.get(i).getMoveRow()].setPiece(mover.getPiece(moves.get(i).getId()));
    game.getBoard()[oldCol][oldRow].setNull(sh);

    if (!mover.getPiece(moves.get(i).getId()).isKing() && (!mover.getPiece(moves.get(i).getId()).getPlayer().isTop() && moves.get(i).getMoveRow() == TOP_ROW || mover.getPiece(moves.get(i).getId()).getPlayer().isTop() && moves.get(i).getMoveRow() == BOTTOM_ROW)) {
      mover.getPiece(moves.get(i).getId()).setKing(true);
    }

    if (moves.get(i).getCaptId() != 0) {
      game.getBoard()[captured.getPiece(moves.get(i).getCaptId()).getCol()][captured.getPiece(moves.get(i).getCaptId()).getRow()].setNull(sh);
      captures.add(Integer.toString(moves.get(i).getCaptId()));
    }
    return captures;
  }

  class MoveTimer extends CountDownTimer  {

    int index = 0;
    ArrayList<Move> moves;
    ArrayList<String> captures;

    public MoveTimer(long millisInFuture, long countDownInterval, ArrayList<Move> moves,
                     ArrayList<String> captures) {
      super(millisInFuture, countDownInterval);
      this.moves = moves;
      this.captures = captures;
    }
    @Override
    public void onTick(long millisUntilFinished) {
      gameThread.resume();
      if (index < moves.size()) {
        captures = prepareOpponentMove(game.getPlayer(), game.getOpponent(), moves, captures, index);
        index++;
      }
    }

    @Override
    public void onFinish() {
      gameThread.pause();
      game.getPlayer().setTurn(true);
      game.getOpponent().setTurn(false);
      touchable = true;
      HashMap<String, Content> map = new HashMap<>();
      map.put("username", new Content(false, new String[]{pref.getString("username", "")}));
      map.put("token", new Content(false, new String[]{pref.getString("token", "")}));
      map.put("gameId", new Content(false, new String[]{game.getId()}));
      map.put("id", new Content(false, new String[]{Integer.toString(moves.get(0).getId())}));
      map.put("captIds", new Content(true, captures.toArray(new String[captures.size()])));
      map.put("col", new Content(false, new String[]{Integer.toString(game.getOpponent().getPiece(moves.get(0).getId()).getCol())}));
      map.put("row", new Content(false, new String[]{Integer.toString(game.getOpponent().getPiece(moves.get(0).getId()).getRow())}));
      map.put("isKing", new Content(false, new String[]{Boolean.toString(game.getOpponent().getPiece(moves.get(0).getId()).isKing())}));
      gameActivity.sendMessageToService(SocketService.MSG_ACKNOWLEDGE_MOVE_RECEIVED, map);
    }
  }

  public void setTouchable(boolean touchable) {
    this.touchable = touchable;
  }

  public boolean onTouchEvent(MotionEvent e) {

    if (touchable) {

      float touchX = e.getX();
      float touchY = e.getY();

      switch (e.getAction()) {

        case MotionEvent.ACTION_DOWN:

          pieceSelected = checkTouch(touchX, touchY);

          if (pieceSelected != null) {

            if (pieceSelected.getPlayer() != game.getPlayer() || combinations.size() != 0 &&
                    pieceSelected.getId() != combinations.get(0).getId()) {
              pieceSelected = null;
            }
          }
          break;

        case MotionEvent.ACTION_MOVE:

          if (pieceSelected != null) {

            gameThread.resume();
            if (touchX - radius < 0 || touchX + radius > viewWidth || touchY - radius < 0 ||
                    touchY + radius > viewHeight) {
              break;
            }
            if (!checked) {
              obligations = checkTake(pieceSelected);
              checked = true;
            }
            int movePiece = checkSquare(pieceSelected, touchX, touchY);

            switch (movePiece) {

              case 0:
                if (hint != null) {
                  game.getBoard()[hint.getCol()][hint.getRow()].setHint(false);
                  hint = null;
                  dropable = false;
                  takingPiece = false;
                }

                break;

              case 1:

                if (game.getBoard()[pieceSelected.getCol() + 1][pieceSelected.getRow() - 1]
                        .getPiece() == null) {
                  if (obligations.size() == 0) {
                    game.getBoard()[pieceSelected.getCol() + 1][pieceSelected.getRow() - 1]
                            .setHint(true);
                    hint = new Hint(pieceSelected.getCol() + 1, pieceSelected.getRow() - 1);
                    dropable = true;
                  }
                }

                break;

              case 2:

                if (game.getBoard()[pieceSelected.getCol() - 1][pieceSelected.getRow() - 1]
                        .getPiece() == null) {
                  if (obligations.size() == 0) {
                    game.getBoard()[pieceSelected.getCol() - 1][pieceSelected.getRow() - 1]
                            .setHint(true);
                    hint = new Hint(pieceSelected.getCol() - 1, pieceSelected.getRow() - 1);
                    dropable = true;
                  }
                }

                break;

              case 3:

                if (game.getBoard()[pieceSelected.getCol() + 1][pieceSelected.getRow() + 1]
                        .getPiece() == null) {
                  if (obligations.size() == 0) {
                    game.getBoard()[pieceSelected.getCol() + 1][pieceSelected.getRow() + 1]
                            .setHint(true);
                    hint = new Hint(pieceSelected.getCol() + 1, pieceSelected.getRow() + 1);
                    dropable = true;
                  }
                }

                break;

              case 4:

                if (game.getBoard()[pieceSelected.getCol() - 1][pieceSelected.getRow() + 1]
                        .getPiece() == null) {
                  if (obligations.size() == 0) {
                    game.getBoard()[pieceSelected.getCol() - 1][pieceSelected.getRow() + 1]
                            .setHint(true);
                    hint = new Hint(pieceSelected.getCol() - 1, pieceSelected.getRow() + 1);
                    dropable = true;
                  }
                }
                break;

              case 5:

                if (!pieceSelected.getPlayer().isTop() || pieceSelected.isKing()) {
                  if (game.getBoard()[pieceSelected.getCol() + 1][pieceSelected.getRow() - 1]
                          .getPiece() != null && game.getBoard()[pieceSelected.getCol() +
                          1][pieceSelected.getRow() - 1].getPiece().getPlayer() != pieceSelected
                          .getPlayer() && game.getBoard()[pieceSelected.getCol() +
                          2][pieceSelected.getRow() - 2].getPiece() == null) {

                    game.getBoard()[pieceSelected.getCol() + 2][pieceSelected.getRow() - 2]
                            .setHint(true);
                    hint = new Hint(pieceSelected.getCol() + 2, pieceSelected.getRow() - 2);
                    dropable = true;
                    takingPiece = true;
                  }
                }

                break;

              case 6:

                if (!pieceSelected.getPlayer().isTop() || pieceSelected.isKing()) {
                  if (game.getBoard()[pieceSelected.getCol() - 1][pieceSelected.getRow() - 1]
                          .getPiece() != null && game.getBoard()[pieceSelected.getCol() -
                          1][pieceSelected.getRow() - 1].getPiece().getPlayer() != pieceSelected
                          .getPlayer() && game.getBoard()[pieceSelected.getCol() -
                          2][pieceSelected.getRow() - 2].getPiece() == null) {

                    game.getBoard()[pieceSelected.getCol() - 2][pieceSelected.getRow() - 2]
                            .setHint(true);
                    hint = new Hint(pieceSelected.getCol() - 2, pieceSelected.getRow() - 2);
                    dropable = true;
                    takingPiece = true;
                  }
                }

                break;

              case 7:

                if (pieceSelected.getPlayer().isTop() || pieceSelected.isKing()) {
                  if (game.getBoard()[pieceSelected.getCol() + 1][pieceSelected.getRow() + 1]
                          .getPiece() != null && game.getBoard()[pieceSelected.getCol() +
                          1][pieceSelected.getRow() + 1].getPiece().getPlayer() != pieceSelected
                          .getPlayer() && game.getBoard()[pieceSelected.getCol() +
                          2][pieceSelected.getRow() + 2].getPiece() == null) {

                    game.getBoard()[pieceSelected.getCol() + 2][pieceSelected.getRow() + 2]
                            .setHint(true);
                    hint = new Hint(pieceSelected.getCol() + 2, pieceSelected.getRow() + 2);
                    dropable = true;
                    takingPiece = true;
                  }
                }
                break;

              case 8:

                if (pieceSelected.getPlayer().isTop() || pieceSelected.isKing()) {
                  if (game.getBoard()[pieceSelected.getCol() - 1][pieceSelected.getRow() + 1]
                          .getPiece() != null && game.getBoard()[pieceSelected.getCol() -
                          1][pieceSelected.getRow() + 1].getPiece().getPlayer() != pieceSelected
                          .getPlayer() && game.getBoard()[pieceSelected.getCol() -
                          2][pieceSelected.getRow() + 2].getPiece() == null) {

                    game.getBoard()[pieceSelected.getCol() - 2][pieceSelected.getRow() + 2]
                            .setHint(true);
                    hint = new Hint(pieceSelected.getCol() - 2, pieceSelected.getRow() + 2);
                    dropable = true;
                    takingPiece = true;
                  }
                }
                break;
            }

            pieceSelected.setPosCol(touchX);
            pieceSelected.setPosRow(touchY);
          }

          break;

        case MotionEvent.ACTION_UP:

          int numPieces;
          int capturedId = 0;

          if (pieceSelected != null) {
            if (dropable) {

              if (!pieceSelected.getPlayer().isTop() && hint.getRow() == TOP_ROW || pieceSelected
                      .getPlayer().isTop() && hint.getRow() == BOTTOM_ROW) {
                pieceSelected.setKing(true);
              }

              if (takingPiece) {
                capturedId = game.getBoard()[pieceSelected.getCol() + (hint.getCol() -
                        pieceSelected.getCol()) / 2][pieceSelected.getRow() + (hint.getRow() -
                        pieceSelected.getRow()) / 2].getPiece().getId();
              }

              int oldCol = game.getPlayer().getPiece(pieceSelected.getId()).getCol();
              int oldRow = game.getPlayer().getPiece(pieceSelected.getId()).getRow();
              game.getPlayer().getPiece(pieceSelected.getId()).setCol(hint.getCol());
              game.getPlayer().getPiece(pieceSelected.getId()).setRow(hint.getRow());
              game.getPlayer().getPiece(pieceSelected.getId()).setPosCol(hint.getCol() * sqWidth
                      + sqWidth / 2);
              game.getPlayer().getPiece(pieceSelected.getId()).setPosRow(hint.getRow() * sqHeight
                      + sqHeight / 2);
              game.getBoard()[hint.getCol()][hint.getRow()].setHint(false);
              game.getBoard()[hint.getCol()][hint.getRow()].setPiece(game.getPlayer().getPiece(pieceSelected.getId()));
              game.getBoard()[oldCol][oldRow].setNull(sh);

              if (takingPiece) {
                game.getBoard()[game.getOpponent().getPiece(capturedId).getCol()][game.getOpponent().getPiece(capturedId).getRow()].setNull(sh);
                numPieces = game.getOpponent().takePiece(game.getOpponent().getPiece(capturedId));
                obligations = checkContinue(game.getPlayer().getPiece(pieceSelected.getId()));

                if (obligations.size() != 0) {
                  showHint((ArrayList<Suggestion>) obligations.clone());
                  combinations.add(new Move(pieceSelected.getId(), pieceSelected.getCol(),
                          pieceSelected.getRow(), capturedId));
                } else {
                  if (numPieces == 0) {
                    System.out.println("Congratulations " + pieceSelected.getPlayer().getUsername
                            () + " you won!");
                  }
                  HashMap<String, Content> map = createMoveMsg(pieceSelected, hint, capturedId);
                  sendMove(map);
                  endTurn();
                }
              } else {
                HashMap<String, Content> map = createMoveMsg(pieceSelected, hint, 0);
                sendMove(map);
                endTurn();
              }

              obligations.clear();
              takingPiece = false;
              dropable = false;
              checked = false;
              gameThread.delayedPause();
              System.out.println("P1: " + game.getPlayer().getNumPieces() + " OPP: " + game
                      .getOpponent().getNumPieces());
            } else {
              // The current piece is not dropable just track the users movements
              pieceSelected.setPosCol(pieceSelected.getCol() * sqWidth + sqWidth / 2);
              pieceSelected.setPosRow(pieceSelected.getRow() * sqHeight + sqHeight / 2);
              if (obligations.size() != 0) {
                showHint((ArrayList<Suggestion>) obligations.clone());
              } else {
                gameThread.delayedPause();
              }
            }
          }
          // The user did not touch a piece
          pieceSelected = null;
          break;
      }
    }
    return true;
  }

  public void sendMove(HashMap<String, Content> map) {

    combinations.clear();
    gameActivity.sendMessageToService(SocketService.MSG_SEND_MOVE, map);
  }

  public void endTurn() {

    if (game.getPlayer().isTurn()) {
      game.getPlayer().setTurn(false);
      touchable = true;
      game.getOpponent().setTurn(true);
    } else {
      game.getOpponent().setTurn(false);
      touchable = true;
      game.getPlayer().setTurn(true);
    }
  }

  public HashMap<String, Content> createMoveMsg(Piece pieceSelected, Hint hint, int capturedId) {

    HashMap<String, Content> map = new HashMap<>();
    map.put("username", new Content(false, new String[]{pref.getString("username", ""),}));
    map.put("token", new Content(false, new String[]{pref.getString("token", ""),}));
    map.put("opponent", new Content(false, new String[]{game.getOpponent().getUsername()}));
    map.put("id", new Content(false, new String[]{Integer.toString(pieceSelected.getId())}));
    map.put("gameId", new Content(false, new String[]{game.getId()}));
    String[] movesCol = new String[combinations.size() + 1];
    String[] movesRow = new String[combinations.size() + 1];

    for (int i = 0; i < combinations.size(); i++) {
      movesCol[i] = Integer.toString(combinations.get(i).getMoveCol());
      movesRow[i] = Integer.toString(combinations.get(i).getMoveRow());
    }

    movesCol[combinations.size()] = Integer.toString(hint.getCol());
    movesRow[combinations.size()] = Integer.toString(hint.getRow());
    map.put("moveCol", new Content(true, movesCol));
    map.put("moveRow", new Content(true, movesRow));

    ArrayList<String> captureList = new ArrayList<>();
    if (takingPiece) {
      for (int i = 0; i < combinations.size(); i++) {
        captureList.add(Integer.toString(combinations.get(i).getCaptId()));
      }
      captureList.add(Integer.toString(capturedId));
    }
    final String[] captureIds = captureList.toArray(new String[captureList.size()]);
    map.put("captId", new Content(true, captureIds));
    return map;
  }

  public void showHint(ArrayList<Suggestion> obligations) {
    showHintRunnable hintThread = new showHintRunnable(obligations);
    (new Thread(hintThread)).start();
  }

  public Piece checkTouch(float touchX, float touchY) {

    for (int i = 0; i < 8; i++) {
      for (int j = 0; j < 8; j++) {
        if (game.getBoard()[i][j].getPiece() != null) {
          if ((touchX - game.getBoard()[i][j].getPiece().getPosCol()) * (touchX - game.getBoard()
                  [i][j].getPiece().getPosCol()) + (touchY - game.getBoard()[i][j].getPiece()
                  .getPosRow()) * (touchY - game.getBoard()[i][j].getPiece().getPosRow()) <
                  radius * radius && game.getBoard()[i][j].getPiece().getPlayer().isTurn()) {
            return game.getBoard()[i][j].getPiece();
          }
        }
      }
    }
    return null;
  }

  public ArrayList<Suggestion> checkTake(Piece pieceSelected) {

    ArrayList<Suggestion> obligations = new ArrayList<>();

    for (int i = 0; i < 8; i++) {
      for (int j = 0; j < 8; j++) {

        Piece candidate;
        if (game.getBoard()[i][j].getPiece() != null) {
          candidate = game.getBoard()[i][j].getPiece();
          if (pieceSelected.getPlayer() == candidate.getPlayer()) {

            if (candidate.getCol() < 6 && candidate.getRow() > 1) {
              if (!candidate.getPlayer().isTop() || candidate.isKing()) {
                if (game.getBoard()[candidate.getCol() + 1][candidate.getRow() - 1].getPiece() !=
                        null && game.getBoard()[candidate.getCol() + 1][candidate.getRow() - 1]
                        .getPiece().getPlayer() != candidate.getPlayer() && game.getBoard()
                        [candidate.getCol() + 2][candidate.getRow() - 2].getPiece() == null) {

                  obligations.add(new Suggestion(candidate.getCol() + 2, candidate.getRow() - 2));
                }
              }
            }

            if (candidate.getCol() > 1 && candidate.getRow() > 1) {
              if (!candidate.getPlayer().isTop() || candidate.isKing()) {
                if (game.getBoard()[candidate.getCol() - 1][candidate.getRow() - 1].getPiece() !=
                        null && game.getBoard()[candidate.getCol() - 1][candidate.getRow() - 1]
                        .getPiece().getPlayer() != candidate.getPlayer() && game.getBoard()
                        [candidate.getCol() - 2][candidate.getRow() - 2].getPiece() == null) {

                  obligations.add(new Suggestion(candidate.getCol() - 2, candidate.getRow() - 2));
                }
              }
            }

            if (candidate.getCol() < 6 && candidate.getRow() < 6) {
              if (candidate.getPlayer().isTop() || candidate.isKing()) {
                if (game.getBoard()[candidate.getCol() + 1][candidate.getRow() + 1].getPiece() !=
                        null && game.getBoard()[candidate.getCol() + 1][candidate.getRow() + 1]
                        .getPiece().getPlayer() != candidate.getPlayer() && game.getBoard()
                        [candidate.getCol() + 2][candidate.getRow() + 2].getPiece() == null) {

                  obligations.add(new Suggestion(candidate.getCol() + 2, candidate.getRow() + 2));
                }
              }
            }

            if (candidate.getCol() > 1 && candidate.getRow() < 6) {
              if (candidate.getPlayer().isTop() || candidate.isKing()) {
                if (game.getBoard()[candidate.getCol() - 1][candidate.getRow() + 1].getPiece() !=
                        null && game.getBoard()[candidate.getCol() - 1][candidate.getRow() + 1]
                        .getPiece().getPlayer() != candidate.getPlayer() && game.getBoard()
                        [candidate.getCol() - 2][candidate.getRow() + 2].getPiece() == null) {

                  obligations.add(new Suggestion(candidate.getCol() - 2, candidate.getRow() + 2));
                }
              }
            }
          }
        }
      }
    }

    return obligations;
  }

  public ArrayList<Suggestion> checkContinue(Piece candidate) {

    ArrayList<Suggestion> obligations = new ArrayList<>();

    if (candidate.getCol() < 6 && candidate.getRow() > 1) {
      if (!candidate.getPlayer().isTop() || candidate.isKing()) {
        if (game.getBoard()[candidate.getCol() + 1][candidate.getRow() - 1].getPiece() != null &&
                game.getBoard()[candidate.getCol() + 1][candidate.getRow() - 1].getPiece()
                        .getPlayer() != candidate.getPlayer() && game.getBoard()[candidate.getCol
                () + 2][candidate.getRow() - 2].getPiece() == null) {

          obligations.add(new Suggestion(candidate.getCol() + 2, candidate.getRow() - 2));
        }
      }
    }
    if (candidate.getCol() > 1 && candidate.getRow() > 1) {
      if (!candidate.getPlayer().isTop() || candidate.isKing()) {
        if (game.getBoard()[candidate.getCol() - 1][candidate.getRow() - 1].getPiece() != null &&
                game.getBoard()[candidate.getCol() - 1][candidate.getRow() - 1].getPiece()
                        .getPlayer() != candidate.getPlayer() && game.getBoard()[candidate.getCol
                () - 2][candidate.getRow() - 2].getPiece() == null) {

          obligations.add(new Suggestion(candidate.getCol() - 2, candidate.getRow() - 2));
        }
      }
    }
    if (candidate.getCol() < 6 && candidate.getRow() < 6) {
      if (candidate.getPlayer().isTop() || candidate.isKing()) {
        if (game.getBoard()[candidate.getCol() + 1][candidate.getRow() + 1].getPiece() != null &&
                game.getBoard()[candidate.getCol() + 1][candidate.getRow() + 1].getPiece()
                        .getPlayer() != candidate.getPlayer() && game.getBoard()[candidate.getCol
                () + 2][candidate.getRow() + 2].getPiece() == null) {

          obligations.add(new Suggestion(candidate.getCol() + 2, candidate.getRow() + 2));
        }
      }
    }
    if (candidate.getCol() > 1 && candidate.getRow() < 6) {
      if (candidate.getPlayer().isTop() || candidate.isKing()) {
        if (game.getBoard()[candidate.getCol() - 1][candidate.getRow() + 1].getPiece() != null &&
                game.getBoard()[candidate.getCol() - 1][candidate.getRow() + 1].getPiece()
                        .getPlayer() != candidate.getPlayer() && game.getBoard()[candidate.getCol
                () - 2][candidate.getRow() + 2].getPiece() == null) {

          obligations.add(new Suggestion(candidate.getCol() - 2, candidate.getRow() + 2));
        }
      }
    }
    return obligations;
  }

  public int checkSquare(Piece pieceSelected, float touchX, float touchY) {

    if (pieceSelected.isKing() || pieceSelected.isPlayer()) {

      if (touchX > pieceSelected.getCol() * sqWidth + sqWidth && touchX < pieceSelected.getCol()
              * sqWidth + sqWidth * 2 && touchY < pieceSelected.getRow() * sqHeight && touchY >
                pieceSelected.getRow() * sqHeight - sqHeight) {
        return 1;
      }

      if (touchX > pieceSelected.getCol() * sqWidth + sqWidth * 2 && touchX < pieceSelected
              .getCol() * sqWidth + sqWidth * 3 && touchY < pieceSelected.getRow() * sqHeight -
              sqHeight &&
              touchY > pieceSelected.getRow() * sqHeight - sqHeight * 2) {

        return 5;
      }

      if (touchX < pieceSelected.getCol() * sqWidth && touchX > pieceSelected.getCol() * sqWidth
              - sqWidth && touchY < pieceSelected.getRow() * sqHeight && touchY > pieceSelected
              .getRow() * sqHeight - sqHeight) {

        return 2;
      }

      if (touchX < pieceSelected.getCol() * sqWidth - sqWidth && touchX > pieceSelected.getCol()
              * sqWidth - sqWidth * 2 && touchY < pieceSelected.getRow() * sqHeight - sqHeight &&
              touchY > pieceSelected.getRow() * sqHeight - sqHeight * 2) {

        return 6;
      }
    }

    if (pieceSelected.isKing() || !pieceSelected.isPlayer()) {

      if (touchX > pieceSelected.getCol() * sqWidth + sqWidth && touchX < pieceSelected.getCol()
              * sqWidth + sqWidth * 2 && touchY > pieceSelected.getRow() * sqHeight + sqHeight &&
              touchY < pieceSelected.getRow() * sqHeight + sqHeight * 2) {

        return 3;
      }

      if (touchX > pieceSelected.getCol() * sqWidth + sqWidth * 2 && touchX < pieceSelected
              .getCol() * sqWidth + sqWidth * 3 && touchY > pieceSelected.getRow() * sqHeight +
              sqHeight * 2 && touchY < pieceSelected.getRow() * sqHeight + sqHeight * 3) {

        return 7;
      }

      if (touchX < pieceSelected.getCol() * sqWidth && touchX > pieceSelected.getCol() * sqWidth
              - sqWidth && touchY > pieceSelected.getRow() * sqHeight + sqHeight && touchY <
              pieceSelected.getRow() * sqHeight + sqHeight * 2) {

        return 4;
      }

      if (touchX < pieceSelected.getCol() * sqWidth - sqWidth && touchX > pieceSelected.getCol()
              * sqWidth - sqWidth * 2 && touchY > pieceSelected.getRow() * sqHeight + sqHeight *
              2  &&
              touchY < pieceSelected.getRow() * sqHeight + sqHeight * 3) {

        return 8;
      }
    }

    return 0;
  }

  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    System.out.println("width: " + width + " height " + height);
  }

  public void surfaceDestroyed(SurfaceHolder holder) {

  }

  @Override
  protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
    super.onSizeChanged(xNew, yNew, xOld, yOld);
  }

  public void stopThreads() {
    gameThread.stop();
  }

  public void resumeThreads() {
    gameThread.resume();
  }

  public void isResetPositions(boolean isResetPositions) {
    gameThread.isResetPositions(isResetPositions);
  }

  public class DoNewMove implements Runnable {

    private ArrayList<Move> moves;
    private String gameId;

    public DoNewMove(String gameId, ArrayList<Move> moves) {
      this.gameId = gameId;
      this.moves = moves;
    }

    public void run() {

      gameThread.resume();
      ArrayList<String> captures = new ArrayList<>();
      int opponentId = moves.get(0).getId();
      for (int i = 0; i < moves.size(); i++) {

        int oldCol = game.getOpponent().getPiece(moves.get(i).getId()).getCol();
        int oldRow = game.getOpponent().getPiece(moves.get(i).getId()).getRow();
        game.getOpponent().getPiece(moves.get(i).getId()).setCol(moves.get(i).getMoveCol());
        game.getOpponent().getPiece(moves.get(i).getId()).setRow(moves.get(i).getMoveRow());
        game.getOpponent().getPiece(moves.get(i).getId()).setPosCol(moves.get(i).getMoveCol() *
                sqWidth + sqWidth / 2);
        game.getOpponent().getPiece(moves.get(i).getId()).setPosRow(moves.get(i).getMoveRow() *
                sqHeight + sqHeight / 2);
        game.getBoard()[moves.get(i).getMoveCol()][moves.get(i).getMoveRow()].setPiece(game.getOpponent()
                .getPiece(moves.get(i).getId()));
        game.getBoard()[oldCol][oldRow].setNull(sh);

        if (!game.getOpponent().getPiece(moves.get(i).getId()).isKing() && (!game.getOpponent()
                .getPiece(moves.get(i).getId()).getPlayer().isTop() && moves.get(i).getMoveRow() == TOP_ROW || game
                .getOpponent().getPiece(moves.get(i).getId()).getPlayer().isTop() && moves.get(i)
                .getMoveRow() == BOTTOM_ROW)) {

          game.getOpponent().getPiece(moves.get(i).getId()).setKing(true);
        }

        if (moves.get(i).getCaptId() != 0) {
          game.getBoard()[game.getPlayer().getPiece(moves.get(i).getCaptId()).getCol()][game.getPlayer().getPiece(moves.get(i).getCaptId()).getRow()].setNull(sh);
          captures.add(Integer.toString(moves.get(i).getCaptId()));
        }

        try {
          Thread.sleep(500);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      gameThread.pause();
      game.getPlayer().setTurn(true);
      game.getOpponent().setTurn(false);
      touchable = true;

      HashMap<String, Content> map = new HashMap<>();
      map.put("username", new Content(false, new String[]{pref.getString("username", "")}));
      map.put("token", new Content(false, new String[]{pref.getString("token", "")}));
      map.put("gameId", new Content(false, new String[]{gameId}));
      map.put("id", new Content(false, new String[]{Integer.toString(opponentId)}));
      map.put("captIds", new Content(true, captures.toArray(new String[captures.size()])));
      map.put("col", new Content(false, new String[]{Integer.toString(game.getOpponent().getPiece(opponentId).getCol())}));
      map.put("row", new Content(false, new String[]{Integer.toString(game.getOpponent().getPiece(opponentId).getRow())}));
      map.put("isKing", new Content(false, new String[]{Boolean.toString(game.getOpponent().getPiece(opponentId).isKing())}));
      gameActivity.sendMessageToService(SocketService.MSG_ACKNOWLEDGE_MOVE_RECEIVED, map);
    }
  }

  public class showHintRunnable implements Runnable {

    private ArrayList<Suggestion> obligations;

    public showHintRunnable(ArrayList<Suggestion> obligations) {
      this.obligations = obligations;
    }

    public void run() {
      for (int i = 0; i < obligations.size(); i++) {
        try {
          game.getBoard()[obligations.get(i).getSugX()][obligations.get(i).getSugY()].setHint(true);
          Thread.sleep(500);
          game.getBoard()[obligations.get(i).getSugX()][obligations.get(i).getSugY()].setHint
                  (false);
          hint = null;
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      try {
        Thread.sleep(500);
        gameThread.pause();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}