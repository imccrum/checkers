package imccrum.com.checkers;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class GameActivity extends Activity {

  private static final String TAG = "GameActivity";

  private View chatView;
  private GameSurfaceView gameSurfaceView;
  private ViewGroup parent;
  private Game game;

  private boolean mIsBound;
  private SharedPreferences pref;

  final IncomingHandler incomingHandler = new IncomingHandler();
  final Messenger mMessenger = new Messenger(incomingHandler);

  class IncomingHandler extends Handler {
    @Override
    public void handleMessage(Message msg) {

      switch (msg.what) {
        // from service thread
        case SocketService.REGISTERED_TO_SERVICE:
          Log.d(TAG, "Service running and bound to activity");
          SharedPreferences.Editor editor = pref.edit();
          editor.putBoolean("gameActivityRunning", true);
          editor.commit();
          if (pref.getBoolean("isConnectedToServer", false)) {
            HashMap<String, Content> map = new HashMap<String, Content>();
            map.put("username", new Content(false, new String[]{pref.getString("username", ""),}));
            map.put("token", new Content(false, new String[]{pref.getString("token", ""),}));
            map.put("gameId", new Content(false, new String[]{game.getId()}));
            sendMessageToService(SocketService.MSG_GET_GAME_STATE, map);
          } else {
            sendMessageToService(SocketService.MSG_CONNECT_TO_SOCKET);
          }
          break;
        case SocketService.CONNECTED_TO_SERVER:
          Log.d(TAG, "SocketService has registered websocket, connection checking user credentials");
          HashMap<String, Content> map = new HashMap<String, Content>();
          map.put("username", new Content(false, new String[]{pref.getString("username", ""),}));
          map.put("token", new Content(false, new String[]{pref.getString("token", ""),}));
          map.put("gameId", new Content(false, new String[]{game.getId()}));
          sendMessageToService(SocketService.MSG_GET_GAME_STATE, map);
          break;
        case SocketService.GAME_POSITIONS:
          Log.d(TAG, "Got game state from the server");
          setupGameBoard((HashMap<String, String>) msg.obj);
          gameSurfaceView.setPositions();
          gameSurfaceView.isResetPositions(true);
          gameSurfaceView.resumeThreads();

          if (((HashMap<String, String>) msg.obj).containsKey("move")) {
            gameSurfaceView.displayMove((HashMap<String, String>) msg.obj);
          }

          if (game.getPlayer().isTurn()) {
            gameSurfaceView.setTouchable(true);
          }
          break;
        case SocketService.MOVE_RECEIVED:
          if (((HashMap<String, String>) msg.obj).get("gameId").equals(game.getId())) {
            Log.d(TAG, "moved received from opponent, updating positions");
            gameSurfaceView.displayMove((HashMap<String, String>) msg.obj);
          }
        break;
        case SocketService.ACKNOWLEDGE_MOVE_SENT:
          Log.d(TAG, "new move sent to opponent");
        break;
      }
    }
  }

  @Override
  protected void onNewIntent(Intent intent){

    intent = getIntent();
    Bundle extras = intent.getExtras();
    String gameId = extras.getString("gameId", "");

    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    SharedPreferences notificationPref = getSharedPreferences("notifications", Context.MODE_PRIVATE);
    HashMap<String, Integer> notifications = (HashMap<String, Integer>) notificationPref.getAll();
    if(notifications == null) {
      notifications = new HashMap<>();
    }
    if (notifications.containsKey(gameId)) {
      notificationManager.cancel(notifications.get(gameId));
      notifications.remove(gameId);
      SharedPreferences.Editor editor = notificationPref.edit();
      for (String s : notifications.keySet()) {
        editor.putInt(s, notifications.get(s));
      }
      editor.commit();
    }

    game = new Game(gameId, new Square[8][8]);

    this.requestWindowFeature(Window.FEATURE_NO_TITLE);
    LinearLayout getDimensions = new LinearLayout(this);
    getDimensions.setOrientation(LinearLayout.VERTICAL);

    setContentView(R.layout.activity_game);
    DisplayMetrics metrics = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(metrics);

    int screenHeight = metrics.heightPixels;
    int screenWidth = metrics.widthPixels;
    int menuHeight = getStatusBarHeight();

    FrameLayout gameFrame = (FrameLayout) findViewById(R.id.gameboard);
    LayoutParams params = gameFrame.getLayoutParams();

    if (screenHeight > screenWidth) {
      params.height = screenWidth;
      params.width = screenWidth;
    } else {

      params.height = screenHeight - menuHeight;
      params.width = screenHeight - menuHeight;
    }

    parent = (ViewGroup) findViewById(R.id.container);
    gameSurfaceView = new GameSurfaceView(this, params.height, params.width, game);
    gameFrame.addView(gameSurfaceView);
    setupMessageView();

  }

  Messenger mService = null;
  private ServiceConnection mConnection = new ServiceConnection() {

    public void onServiceConnected(ComponentName className, IBinder service) {
      mService = new Messenger(service);
      try {
        Message msg = Message.obtain(null, SocketService.MSG_REGISTER_CLIENT);
        msg.replyTo = mMessenger;
        mService.send(msg);
      } catch (RemoteException e) {
        // In this case the service has crashed before we could even do anything with it
      }
    }

    public void onServiceDisconnected(ComponentName className) {
      // This is called when the connection with the service has been unexpectedly disconnected -
      // process crashed.
      mService = null;
    }
  };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    pref = PreferenceManager.getDefaultSharedPreferences(this);
    startService();
    Intent intent = getIntent();
    Bundle extras = intent.getExtras();
    String gameId = extras.getString("gameId", "");

    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    SharedPreferences notificationPref = getSharedPreferences("notifications", Context.MODE_PRIVATE);
    HashMap<String, Integer> notifications = (HashMap<String, Integer>) notificationPref.getAll();
    if(notifications == null) {
      notifications = new HashMap<>();
    }
    if (notifications.containsKey(gameId)) {
      notificationManager.cancel(notifications.get(gameId));
      notifications.remove(gameId);
      SharedPreferences.Editor editor = notificationPref.edit();
      for (String s : notifications.keySet()) {
        editor.putInt(s, notifications.get(s));
      }
      editor.commit();
    }

    game = new Game(gameId, new Square[8][8]);

    this.requestWindowFeature(Window.FEATURE_NO_TITLE);
    LinearLayout getDimensions = new LinearLayout(this);
    getDimensions.setOrientation(LinearLayout.VERTICAL);

    setContentView(R.layout.activity_game);
    DisplayMetrics metrics = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(metrics);

    int screenHeight = metrics.heightPixels;
    int screenWidth = metrics.widthPixels;
    int menuHeight = getStatusBarHeight();

    FrameLayout gameFrame = (FrameLayout) findViewById(R.id.gameboard);
    LayoutParams params = gameFrame.getLayoutParams();

    if (screenHeight > screenWidth) {
      params.height = screenWidth;
      params.width = screenWidth;
    } else {

      params.height = screenHeight - menuHeight;
      params.width = screenHeight - menuHeight;
    }

    parent = (ViewGroup) findViewById(R.id.container);
    gameSurfaceView = new GameSurfaceView(this, params.height, params.width, game);
    gameFrame.addView(gameSurfaceView);
    setupMessageView();
  }

  public void setupGameBoard(HashMap<String, String> obj) {

    Player player = new Player(Boolean.valueOf(obj.get("isPlayerTurn")),
            Boolean.valueOf(obj.get("isTop")), pref.getString("username", ""),
            new ArrayList<Piece>());
    Player opponent = new Player(!Boolean.valueOf(obj.get("isPlayerTurn")),
            !Boolean.valueOf(obj.get("isTop")), obj.get("opponent"), new ArrayList<Piece>());

    try {
      JSONArray pieces = new JSONArray(obj.get("positions"));
      for (int count = 0; count < pieces.length(); count++) {
        if (opponent.isTop()) {
          if (new JSONObject(pieces.getString(count)).getBoolean("p")) {
            player.getPieces().add(new Piece(player, new JSONObject(pieces.getString(count))
                    .getInt("id"), new JSONObject(pieces.getString(count)).getInt("c"),
                    new JSONObject(pieces.getString(count)).getInt("r"),
                    new JSONObject(pieces.getString(count)).getBoolean("p"),
                    new JSONObject(pieces.getString(count)).getBoolean("k")));
          } else {
            opponent.getPieces().add(new Piece(opponent, new JSONObject(pieces.getString(count)
            ).getInt("id"), new JSONObject(pieces.getString(count)).getInt("c"),
                    new JSONObject(pieces.getString(count)).getInt("r"),
                    new JSONObject(pieces.getString(count)).getBoolean("p"),
                    new JSONObject(pieces.getString(count)).getBoolean("k")));
          }
        } else {
          if (!new JSONObject(pieces.getString(count)).getBoolean("p")) {
            player.getPieces().add(new Piece(player, new JSONObject(pieces.getString(count))
                    .getInt("id"), new JSONObject(pieces.getString(count)).getInt("c"),
                    new JSONObject(pieces.getString(count)).getInt("r"),
                    new JSONObject(pieces.getString(count)).getBoolean("p"),
                    new JSONObject(pieces.getString(count)).getBoolean("k")));
          } else {
            opponent.getPieces().add(new Piece(opponent, new JSONObject(pieces.getString(count)
            ).getInt("id"), new JSONObject(pieces.getString(count)).getInt("c"),
                    new JSONObject(pieces.getString(count)).getInt("r"),
                    new JSONObject(pieces.getString(count)).getBoolean("p"),
                    new JSONObject(pieces.getString(count)).getBoolean("k")));
          }
        }
      }
    } catch (JSONException je) {
    }

    game.setPlayers(player, opponent);
  }

  public void setupMessageView() {

    chatView = LayoutInflater.from(getBaseContext()).inflate(R.layout.send_message_layout, null);
    parent.addView(chatView);

    EditText messageText = (EditText) findViewById(R.id.chat_text);
    messageText.setOnEditorActionListener(new OnEditorActionListener() {

      @Override
      public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        boolean handled = false;
        if (actionId == EditorInfo.IME_ACTION_SEND) {
          handled = true;
        }
        return handled;
      }
    });

    final Button chatButton = (Button) findViewById(R.id.chat_button);
    chatButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {

        EditText messageText = (EditText) findViewById(R.id.chat_text);
        String message = messageText.getText().toString();
      }
    });
  }

  public void sendMessageToService(int type, HashMap<String, Content> map) {
    if (mIsBound) {
      if (mService != null) {
        try {
          Message msg = Message.obtain();
          msg.replyTo = mMessenger;
          msg.what = type;
          msg.obj = map;
          mService.send(msg);
        } catch (RemoteException e) {
        }
      }
    }
  }

  private void sendMessageToService(int type) {
    if (mIsBound) {
      if (mService != null) {
        try {
          Message msg = Message.obtain();
          msg.replyTo = mMessenger;
          msg.what = type;
          mService.send(msg);
        } catch (RemoteException e) {

        }
      }
    }
  }

  public int getStatusBarHeight() {
    int result = 0;
    int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
    if (resourceId > 0) {
      result = getResources().getDimensionPixelSize(resourceId);
    }
    return result;
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
  }

  @Override
  public void onPause() {
    super.onPause();
    gameSurfaceView.stopThreads();
    doUnbindService();
  }

  @Override
  public void onStop() {
    super.onStop();
    SharedPreferences.Editor editor = pref.edit();
    editor.putBoolean("gameActivityRunning", false);
    editor.commit();
  }

  @Override
  public void onResume() {
    super.onResume();
    startService();
  }


  @Override
  protected void onSaveInstanceState(Bundle savedInstanceState) {
    super.onSaveInstanceState(savedInstanceState);

    savedInstanceState.putSerializable("game", game);
  }


  public void startService() {

    if (!serviceIsRunning()) {
      startService(new Intent(this, SocketService.class));
      new CountDownTimer(5000, 1000) {
        public void onTick(long millisUntilFinished) {
          if (serviceIsRunning()) {
            doBindService();
            Log.d(TAG, "service status bound: " + mIsBound);
            this.cancel();
          }
        }
        public void onFinish() {
          // mTextField.setText("done!");
        }
      }.start();
    } else {
      doBindService();
      Log.d(TAG, "service status bound: " + mIsBound);
    }
  }

  void doUnbindService() {

    if (mIsBound) {
      // If we have received the service, and hence registered with it,
      // then now is the time to unregister.
      if (mService != null) {
        try {
          Message msg = Message.obtain(null, SocketService.MSG_UNREGISTER_CLIENT);
          msg.replyTo = mMessenger;
          mService.send(msg);
        } catch (RemoteException e) {
          // There is nothing special we need to do if the service has
          // crashed.
        }
      }
      // Detach our existing connection.
      unbindService(mConnection);
      mIsBound = false;
    }
  }

  void doBindService() {
    bindService(new Intent(this, SocketService.class), mConnection, Context.BIND_AUTO_CREATE);
    mIsBound = true;
  }

  private boolean serviceIsRunning() {
    return SocketService.isRunning();
  }
  
}

