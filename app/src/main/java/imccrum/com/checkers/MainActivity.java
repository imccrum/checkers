package imccrum.com.checkers;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends Activity {

  private boolean isRemote;
  private int headingSize;
  private FrameLayout splashGameboard;
  private GameSurfaceView gameSurfaceView;

  public int getStatusBarHeight() {
    int result = 0;
    int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
    if (resourceId > 0) {
      result = getResources().getDimensionPixelSize(resourceId);
    }
    return result;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    this.requestWindowFeature(Window.FEATURE_NO_TITLE);
    LinearLayout getDimensions = new LinearLayout(this);
    getDimensions.setOrientation(LinearLayout.VERTICAL);
    setContentView(R.layout.activity_main);

    DisplayMetrics metrics = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(metrics);

    int screenHeight = metrics.heightPixels;
    int screenWidth = metrics.widthPixels;
    int menuHeight = getStatusBarHeight();

    splashGameboard = (FrameLayout) findViewById(R.id.main_gameboard);
    LayoutParams params = splashGameboard.getLayoutParams();

    if (screenHeight > screenWidth) {
      params.height = screenWidth;
      params.width = screenWidth;
    } else {
      params.height = screenHeight - menuHeight;
      params.width = screenHeight - menuHeight;
    }

    headingSize = params.height / 8;
    TextView heading = (TextView) findViewById(R.id.heading);
    heading.setTextSize(TypedValue.COMPLEX_UNIT_PX, headingSize);

    Game displayGame = getDisplayGame();
    gameSurfaceView = new GameSurfaceView(this, params.height, params.width, displayGame);
    splashGameboard.addView(gameSurfaceView);

    int buttonTextSize = params.height / 12;

    final Button localGameButton = (Button) findViewById(R.id.local_game_button);
    localGameButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, buttonTextSize);
    localGameButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        isRemote = false;
        newGame(isRemote);
      }
    });

    final Button remoteGameButton = (Button) findViewById(R.id.remote_game_button);
    remoteGameButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, buttonTextSize);
    remoteGameButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        isRemote = true;
        newGame(isRemote);
      }
    });
  }

  public Game getDisplayGame() {

    ArrayList<Piece> playerPieces = new ArrayList<>();
    ArrayList<Piece> opponentPieces = new ArrayList<>();
    int id = 1;
    int row = 0;
    int col = 0;
    int offset = 0;
    for (int x = 0; x < 12; x++) {
      playerPieces.add(new Piece(id, (col % 8) + offset, row, false, false));
      opponentPieces.add(new Piece(id + 12, ((col + 1) % 8) - offset, row + 5, true, false));
      col += 2;
      if (id % 4 == 0) {
        row++;
        if (row % 2 != 0) {
          offset = 1;
        } else {
          offset = 0;
        }
      }
      id++;
    }
    return new Game(new Player(true, false, "", playerPieces), new Player(false, true, "", opponentPieces), new Square[8][8]);
  }

  public void newGame(boolean isRemote) {

    Intent intent = new Intent(this, SetupActivity.class);
    intent.putExtra("is_remote", isRemote);
    intent.putExtra("heading_size", headingSize);
    startActivity(intent);
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
  }

  @Override
  public void onPause() {
    super.onPause();
  }

  @Override
  public void onResume() {
    super.onResume();
  }


  @Override
  protected void onSaveInstanceState(Bundle savedInstanceState) {
    super.onSaveInstanceState(savedInstanceState);
  }
}
