package imccrum.com.checkers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.SurfaceHolder;

/**
 * Created by hal on 18/04/15.
 */
public class GameThread implements Runnable {
  private Thread runThread;
  private boolean running = false;
  private boolean paused = false;

  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private Context context;
  private SurfaceHolder sh;
  private int viewWidth;
  private int viewHeight;
  private float radius;
  private float sqWidth;
  private float sqHeight;
  private boolean resetPositions;
  private Square[][] board;
  private boolean inPlay;

  public GameThread(SurfaceHolder surfaceHolder, Context context, Handler handler,
                    Square[][] board, int viewWidth, int viewHeight, float sqWidth,
                    float sqHeight, float radius, boolean inPlay) {

    this.sh = surfaceHolder;
    this.context = context;
    this.board = board;
    this.viewWidth = viewWidth;
    this.viewHeight = viewHeight;
    this.sqWidth = sqWidth;
    this.sqHeight = sqHeight;
    this.radius = radius;
    this.inPlay = inPlay;
    resetPositions = true;
  }



  public void start() {
    running = true;
    paused = false;
    if (runThread == null || !runThread.isAlive()) runThread = new Thread(this);
    else if (runThread.isAlive()) throw new IllegalStateException("Thread already started.");
    runThread.start();
  }

  public void stop() {
    System.out.println("trying to stop thread");
    if (runThread == null) throw new IllegalStateException("Thread not started.");
    synchronized (runThread) {
      try {

        running = false;
        runThread.notify();
        runThread.join(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  public boolean isAlive() {
    return runThread.isAlive();
  }

  public void delayedPause() {
    new CountDownTimer(1000, 1000) {

      public void onTick(long millisUntilFinished) {
      }

      public void onFinish() {
        pause();
      }
    }.start();
  }

  public void isResetPositions(boolean resetPositions) {
    this.resetPositions = resetPositions;
  }

  public void pause() {
    if (runThread == null) throw new IllegalStateException("Thread not started.");
    synchronized (runThread) {
      paused = true;
    }
  }

  public void resume() {
    if (runThread == null) throw new IllegalStateException("Thread not started.");
    synchronized (runThread) {
      paused = false;
      runThread.notify();
    }
  }

  public void run() {

    long sleep = 0, before;
    while (running) {

      // get the time before we do our game logic
      before = System.currentTimeMillis();
      Canvas c = null;
      try {
        c = sh.lockCanvas(null);
        synchronized (sh) {
          doDraw(c);
        }
      } finally {
        if (c != null) {
          sh.unlockCanvasAndPost(c);
        }
      }

      if (!inPlay) {
        stop();
      }

      if (resetPositions) {
        resetPositions = false;
        pause();
      }
      // move player and do all game logic
      try {
        // sleep for 100 - how long it took us to do our game logic
        sleep = 100 - (System.currentTimeMillis() - before);
        Thread.sleep(sleep > 0 ? sleep : 0);
      } catch (InterruptedException ex) {
      }
      synchronized (runThread) {
        if (paused) {
          try {
            runThread.wait();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    }
    paused = false;
  }

  private void doDraw(Canvas canvas) {

    int darkBrown = context.getResources().getColor(R.color.darkbrown);
    int lightBrown = context.getResources().getColor(R.color.lightbrown);
    int lightBlue = context.getResources().getColor(R.color.lightblue);
    paint.setColor(darkBrown);
    canvas.drawRect(0, viewHeight, viewWidth, 0, paint);

    for (int i = 0; i < 8; i++) {
      for (int j = 0; j < 8; j++) {

        if (board[i][j].isHint()) {

          paint.setColor(lightBlue);
          canvas.drawRect(board[i][j].getX(), board[i][j].getY(), board[i][j].getX() + sqWidth, board[i][j].getY() + sqHeight, paint);
        } else {

          if (board[i][j].getColor() == 0) {

            paint.setColor(lightBrown);
            canvas.drawRect(board[i][j].getX(), board[i][j].getY(), board[i][j].getX() + sqWidth, board[i][j].getY() + sqHeight, paint);
          }
        }
      }
    }

    for (int i = 0; i < 8; i++) {
      for (int j = 0; j < 8; j++) {

        if (board[i][j].getPiece() != null) {

          if (board[i][j].getPiece().isPlayer()) {

            paint.setColor(Color.RED);
            canvas.drawCircle(board[i][j].getPiece().getPosCol(), board[i][j].getPiece().getPosRow(), radius, paint);
          }

          if (!board[i][j].getPiece().isPlayer()) {

            paint.setColor(Color.BLACK);
            canvas.drawCircle(board[i][j].getPiece().getPosCol(), board[i][j].getPiece().getPosRow(), radius, paint);
          }

          if (board[i][j].getPiece().isKing()) {

            paint.setColor(Color.WHITE);
            canvas.drawCircle(board[i][j].getPiece().getPosCol(), board[i][j].getPiece().getPosRow(), radius * 0.5f, paint);
          }
        }
      }
    }
  }
}