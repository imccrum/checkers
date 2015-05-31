package imccrum.com.checkers;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
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
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class SetupActivity extends Activity {

  public static final String TAG = "SetupActivity";
  private String opponentName;
  final IncomingHandler incomingHandler = new IncomingHandler();
  final Messenger mMessenger = new Messenger(incomingHandler);
  boolean mIsBound;
  Messenger mService = null;

  private ServiceConnection mConnection = new ServiceConnection() {

    public void onServiceConnected(ComponentName className, IBinder service) {
      mService = new Messenger(service);
      System.out.println("received register message from service");
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
  private Button headerBtn;
  private TextView headerMessage;
  private SharedPreferences pref;
  private int headingSize;
  private ArrayList<String> receivedList = new ArrayList<String>();
  private ArrayList<String> requestsList = new ArrayList<String>();
  private ArrayList<String> usersList = new ArrayList<String>();
  private ArrayList<GameInfo> gamesList = new ArrayList<GameInfo>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    startService();
    Intent intent = getIntent();
    Bundle extras = intent.getExtras();
    headingSize = extras.getInt("heading_size");

    setContentView(R.layout.activity_setup);
    headerBtn = (Button) findViewById(R.id.header_msg_btn);
    headerMessage = (TextView) findViewById(R.id.header_msg);
    headerMessage.setTextSize(TypedValue.COMPLEX_UNIT_PX, headingSize * 0.8f);
    pref = PreferenceManager.getDefaultSharedPreferences(this);
    setHeaderMsg("...", "Logout");

    setButtons();
  }

  public void setHeaderMsg(String headerText, String buttonText) {

    headerMessage.setText(headerText);
    headerBtn.setText(buttonText);
  }

  public void saveLogin(String username, String token) {

    SharedPreferences.Editor editor = pref.edit();
    editor.putString("username", username);
    editor.putString("token", token);
    editor.commit();
  }

  public void saveLogout() {

    SharedPreferences.Editor editor = pref.edit();
    editor.remove("username");
    editor.remove("token");
    editor.commit();
  }

  public void getListItems(String username, String token, int type) {

    HashMap<String, Content> map = new HashMap<String, Content>();
    map.put("username", new Content(false, new String[]{username}));
    map.put("token", new Content(false, new String[]{token}));
    sendMessageToService(type, map);
  }

  public void populateList(HashMap<String, String> obj, String key, ArrayList list) {

    list.clear();
    if (obj != null) {
      try {
        JSONArray users = new JSONArray(obj.get("data"));
        for (int count = 0; count < users.length(); count++) {
          list.add(new JSONObject(users.getString(count)).getString(key));
        }
      } catch (JSONException je) {
      }
    }
  }

  public void populateGameList(HashMap<String, String> obj) {

    gamesList.clear();
    if (obj != null) {
      try {
        JSONArray games = new JSONArray(obj.get("data"));
        for (int count = 0; count < games.length(); count++) {
          gamesList.add(new GameInfo(new JSONObject(games.getString(count)).getString("_gameId"),
                  new JSONObject(games.getString(count)).getString("opponent")));
        }
      } catch (JSONException je) {
      }
    }
  }

  public void startGameActivity(HashMap<String, String> obj) {

    Intent intent = new Intent(this, GameActivity.class);
    intent.putExtra("gameId", obj.get("gameId"));
    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT);
    Log.d(TAG, obj.get("gameId"));
    doUnbindService();
    try {
      pIntent.send();
    } catch (PendingIntent.CanceledException e) {
      e.printStackTrace();
    }
  }

  public void setButtons() {

    final ArrayAdapter<String> usersSpinAdapter = new ArrayAdapter<String>(getActivity(),
            android.R.layout.simple_spinner_dropdown_item, usersList);
    final ArrayAdapter<GameInfo> gamesSpinAdapter = new ArrayAdapter<GameInfo>(getActivity(),
            android.R.layout.simple_spinner_dropdown_item, gamesList);

    headerBtn.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {

        String username = pref.getString("username", "");

        if (username != "") {
          HashMap<String, Content> map = new HashMap<String, Content>();
          map.put("username", new Content(false, new String[]{username}));
          sendMessageToService(SocketService.MSG_LOGOUT, map);
          saveLogout();
          setHeaderMsg("User logged out", "Login");
        } else {

          AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
          LayoutInflater inflater = getActivity().getLayoutInflater();
          builder.setView(inflater.inflate(R.layout.login_fragment,
                  null)).setTitle("Login or sign up").setPositiveButton("Login",
                  new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                      String username = ((EditText) ((AlertDialog) dialog).findViewById(R.id
                              .username_input)).getText().toString();
                      String password = ((EditText) ((AlertDialog) dialog).findViewById(R.id
                              .password_input)).getText().toString();

                      if (username.matches("")) {
                        Toast.makeText(getActivity(), "You did not enter a username",
                                Toast.LENGTH_SHORT).show();
                      } else if (password.matches("")) {
                        Toast.makeText(getActivity(), "You did not enter a password",
                                Toast.LENGTH_SHORT).show();
                      } else {
                        login(username, password);
                      }
                    }
                  }).setNegativeButton("Sign up", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {

              String username;
              username = ((EditText) ((AlertDialog) dialog).findViewById(R.id.username_input))
                      .getText().toString();
              String password;
              password = ((EditText) ((AlertDialog) dialog).findViewById(R.id.password_input))
                      .getText().toString();

              if (username.matches("")) {
                Toast.makeText(getActivity(), "You did not enter a username",
                        Toast.LENGTH_SHORT).show();
              } else if (password.matches("")) {
                Toast.makeText(getActivity(), "You did not enter a password",
                        Toast.LENGTH_SHORT).show();
              } else {
                signup(username, password);
              }
            }
          }).create().show();
        }
      }
    });

    Button currentUsersBtn = (Button) findViewById(R.id.users_btn);
    currentUsersBtn.setOnClickListener(new View.OnClickListener() {

      ArrayList<Integer> selectedItems;

      public void onClick(View v) {

        if (usersList.size() == 0) {
          usersList.add("No current users");
        }
        usersSpinAdapter.notifyDataSetChanged();

        selectedItems = new ArrayList();
        final String[] list = usersList.toArray(new String[usersList.size()]);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Invite someone to play")
          .setMultiChoiceItems(list, null, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
              if (isChecked) {
                  selectedItems.add(which);
              } else if (selectedItems.contains(which)) {
                  selectedItems.remove(Integer.valueOf(which));
              }
            }
          }).setPositiveButton("Send Request", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
              HashMap<String, Content> map = new HashMap<String, Content>();
              String[] users = new String[selectedItems.size()];
              for (int i = 0; i < selectedItems.size(); i++) {
                users[i] = list[selectedItems.get(i)];
              }
              map.put("users", new Content(true, users));
              map.put("username", new Content(false, new String[]{pref.getString("username", ""),}));
              map.put("token", new Content(false, new String[]{pref.getString("token", ""),}));
              sendMessageToService(SocketService.MSG_GAME_REQUEST, map);
              dialog.dismiss();
            }
          }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
              dialog.dismiss();
            }
        }).create().show();
      }
    });

    Button gameRequestsBtn = (Button) findViewById(R.id.requests_btn);
    gameRequestsBtn.setOnClickListener(new View.OnClickListener() {

      ArrayList<Integer> selectedItems;

      public void onClick(View v) {

        if (requestsList.size() == 0) {
          requestsList.add("No requests received");
        }
        usersSpinAdapter.notifyDataSetChanged();

        selectedItems = new ArrayList();
        final String[] list = requestsList.toArray(new String[requestsList.size()]);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Game requests received from:").setMultiChoiceItems(list, null,
                new DialogInterface.OnMultiChoiceClickListener() {

                  @Override
                  public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    if (isChecked) {
                      selectedItems.add(which);
                    } else if (selectedItems.contains(which)) {
                      selectedItems.remove(Integer.valueOf(which));
                    }
                  }
                }).setPositiveButton("Accept request", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int id) {
            HashMap<String, Content> map = new HashMap<String, Content>();
            String[] opponents = new String[selectedItems.size()];
            for (int i = 0; i < selectedItems.size(); i++) {
              opponents[i] = list[selectedItems.get(i)];
            }
            System.out.println("users" + opponents[0]);
            map.put("username", new Content(false, new String[]{pref.getString("username", ""),}));
            map.put("token", new Content(false, new String[]{pref.getString("token", ""),}));
            map.put("opponents", new Content(true, opponents));
            sendMessageToService(SocketService.MSG_ACCEPT_REQUEST, map);
            dialog.dismiss();
          }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int id) {
            dialog.dismiss();
          }
        }).create().show();
      }
    });

    Button activeGamesBtn = (Button) findViewById(R.id.games_btn);
    activeGamesBtn.setOnClickListener(new View.OnClickListener() {

      public void onClick(View v) {

        //getGames();
        gamesSpinAdapter.notifyDataSetChanged();

        new AlertDialog.Builder(getActivity()).setTitle("Current games:")
                .setAdapter(gamesSpinAdapter, new DialogInterface.OnClickListener() {

          @Override
          public void onClick(DialogInterface dialog, int which) {
            HashMap<String, Content> map = new HashMap<String, Content>();
            map.put("username", new Content(false, new String[]{pref.getString("username", ""),}));
            map.put("token", new Content(false, new String[]{pref.getString("token", ""),}));
            map.put("gameId", new Content(false, new String[]{gamesList.get(which).getGameId()}));
            Log.i(TAG, "selected id: " + gamesList.get(which).getGameId());
            // FIXME try to return this from the server again
            opponentName = gamesList.get(which).getOpponent();
            sendMessageToService(SocketService.MSG_GET_GAME_STATE, map);

            dialog.dismiss();
          }
        }).setPositiveButton("Cancel request", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int id) {
            dialog.dismiss();
          }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int id) {
            dialog.dismiss();
          }
        }).create().show();
      }
    });
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

  private void sendMessageToService(int type, HashMap<String, Content> list) {

    if (mIsBound) {
      if (mService != null) {
        try {
          Message msg = Message.obtain();
          msg.replyTo = mMessenger;
          msg.what = type;
          msg.obj = list;
          mService.send(msg);
        } catch (RemoteException e) {
        }
      }
    }
  }

  public Activity getActivity() {
    return this;
  }

  public void login(String username, String password) {

    HashMap<String, Content> map = new HashMap<String, Content>();
    map.put("username", new Content(false, new String[]{username}));
    map.put("password", new Content(false, new String[]{password}));
    sendMessageToService(SocketService.MSG_LOGIN, map);
  }

  public void validate(String username, String token) {

    HashMap<String, Content> map = new HashMap<String, Content>();
    map.put("username", new Content(false, new String[]{username}));
    map.put("token", new Content(false, new String[]{token}));
    sendMessageToService(SocketService.MSG_VALIDATE, map);
  }

  public void signup(String username, String password) {

    HashMap<String, Content> map = new HashMap<String, Content>();
    map.put("username", new Content(false, new String[]{username}));
    map.put("password", new Content(false, new String[]{password}));
    sendMessageToService(SocketService.MSG_SIGNUP, map);
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
  }

  @Override
  public void onResume() {
    super.onResume();
    startService();
  }

  @Override
  public void onPause() {
    super.onPause();
    doUnbindService();
  }

  @Override
  public void onStop() {
    super.onStop();
    SharedPreferences.Editor editor = pref.edit();
    editor.putBoolean("setupActivityRunning", false);
    editor.commit();
  }

  @Override
  protected void onSaveInstanceState(Bundle savedInstanceState) {
    super.onSaveInstanceState(savedInstanceState);
    // savedInstanceState.putSerializable("squares", squares);
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

  class IncomingHandler extends Handler {
    @Override
    public void handleMessage(Message msg) {

      switch (msg.what) {
        // from service thread
        case SocketService.UPDATE_ACTIVE_USERS:
          Log.d(TAG, "Message received from SocketService: Printing user list");
          Log.d(TAG, msg.obj.toString());
          populateList((HashMap<String, String>) msg.obj, "username", usersList);
          break;
        case SocketService.GAME_REQUEST:
          Log.d(TAG, "Message received from SocketService: game request from following user");
          break;
        case SocketService.ACCEPT_GAME:
          Log.d(TAG, "Message received from SocketService: game accepted, starting...");
          break;
        case SocketService.REGISTERED_TO_SERVICE:
          Log.d(TAG, "Service running and bound to activity.. checking server connection");
          SharedPreferences.Editor editor = pref.edit();
          editor.putBoolean("setupActivityRunning", true);
          editor.commit();
          if (pref.getBoolean("isConnectedToServer", false)) {
            checkCredentialsForLogin();
          } else {
            sendMessageToService(SocketService.MSG_CONNECT_TO_SOCKET);
          }
          break;
        case SocketService.CONNECTED_TO_SERVER:
          Log.d(TAG, "SocketService has registered websocket, connection checking user credentials");
          checkCredentialsForLogin();
          break;
        case SocketService.UPDATE_GAME_REQUESTS:
          Log.d(TAG, "Request list received from socket");
          populateList((HashMap<String, String>) msg.obj, "from", requestsList);
          break;
        case SocketService.UPDATE_ACTIVE_GAMES:
          Log.d(TAG, "Request list received from socket");
          populateGameList((HashMap<String, String>) msg.obj);
          break;
        case SocketService.GAME_POSITIONS:
          Log.d(TAG, "Got game state from the server");
          startGameActivity((HashMap<String, String>) msg.obj);
          break;
        case SocketService.SIGNUP_SUCCESSFUL:
          Log.d(TAG, "Received signup success message + token from socket service");
          saveLogin(((HashMap<String, String>) msg.obj).get("username"), ((HashMap<String,
                  String>) msg.obj).get("token"));
          setHeaderMsg("Hi " + ((HashMap<String, String>) msg.obj).get("username") + "!", "Logout");
          getListItems(((HashMap<String, String>) msg.obj).get("username"),
                  pref.getString("token", ""), SocketService.MSG_GET_CURRENT_USERS);
          getListItems(((HashMap<String, String>) msg.obj).get("username"),
                  pref.getString("token", ""), SocketService.MSG_GET_CURRENT_REQUESTS);
          break;
        case SocketService.LOGIN_SUCCESSFUL:
          Log.d(TAG, "Received signup success message + token from socket service");
          saveLogin(((HashMap<String, String>) msg.obj).get("username"), ((HashMap<String,
                  String>) msg.obj).get("token"));
          setHeaderMsg("Hi " + ((HashMap<String, String>) msg.obj).get("username") + "!", "Logout");
          getListItems(((HashMap<String, String>) msg.obj).get("username"),
                  pref.getString("token", ""), SocketService.MSG_GET_CURRENT_USERS);
          getListItems(((HashMap<String, String>) msg.obj).get("username"),
                  pref.getString("token", ""), SocketService.MSG_GET_CURRENT_REQUESTS);
          break;
        case SocketService.VALIDATION_SUCCESSFUL:
          Log.d(TAG, "Received signup success message + token from socket service");
          setHeaderMsg("Hi " + ((HashMap<String, String>) msg.obj).get("username") + "!", "Logout");
          getListItems(((HashMap<String, String>) msg.obj).get("username"),
                  pref.getString("token", ""), SocketService.MSG_GET_CURRENT_USERS);
          getListItems(((HashMap<String, String>) msg.obj).get("username"),
                  pref.getString("token", ""), SocketService.MSG_GET_CURRENT_REQUESTS);
          getListItems(((HashMap<String, String>) msg.obj).get("username"),
                  pref.getString("token", ""), SocketService.MSG_GET_CURRENT_GAMES);
          break;
      }
    }
  }


  public void checkCredentialsForLogin() {

    String username = pref.getString("username", "");
    String token = pref.getString("token", "");
    if (username != "") {
      Log.d(TAG, "Credentials found in shared prefs, attempting to login");
      validate(username, token);
    } else {
      Log.d(TAG, "No credentials found, prompt login");
      setHeaderMsg("Login to play", "Login");
    }
  }

  public void startService() {

    if (!serviceIsRunning()) {
      startService(new Intent(SetupActivity.this, SocketService.class));
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

  void doBindService() {
    bindService(new Intent(this, SocketService.class), mConnection, Context.BIND_AUTO_CREATE);
    mIsBound = true;
  }

  private boolean serviceIsRunning() {
    return SocketService.isRunning();
  }
}



