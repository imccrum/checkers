package imccrum.com.checkers;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class SocketService extends Service {

  public static final String TAG = "SocketService";
  public Context context;
  private static boolean isRunning = false;
  private static String serverAddress = "ws://10.0.2.2:3000";
  //private static String serverAddress = "wss://polar-everglades-1414.herokuapp.com";
  // Activity (incoming)
  public static final int MSG_REGISTER_CLIENT = 1;
  public static final int MSG_UNREGISTER_CLIENT = 2;
  public static final int MSG_CONNECT_TO_SOCKET = 3;
  public static final int MSG_GAME_REQUEST = 5;
  public static final int MSG_CANCEL_USERS_REQUEST = 6;
  public static final int MSG_NEW_PLAYER_MOVE = 7;
  public static final int MSG_LOGIN = 8;
  public static final int MSG_SIGNUP = 9;
  public static final int MSG_GET_CURRENT_USERS = 10;
  public static final int MSG_GET_CURRENT_REQUESTS = 12;
  public static final int MSG_LOGOUT = 11;
  public static final int MSG_VALIDATE = 13;
  public static final int MSG_ACCEPT_REQUEST = 14;
  public static final int MSG_GET_CURRENT_GAMES = 15;
  public static final int MSG_GET_GAME_STATE = 16;
  public static final int MSG_SEND_MOVE = 17;
  public static final int MSG_ACKNOWLEDGE_MOVE_RECEIVED = 18;
  // Service (outgoing)
  public static final int CONNECTED_TO_SERVER = 20;
  public static final int SIGNUP_SUCCESSFUL = 21;
  public static final int LOGIN_SUCCESSFUL = 22;
  public static final int VALIDATION_SUCCESSFUL = 23;
  public static final int UPDATE_ACTIVE_GAMES = 24;
  public static final int UPDATE_ACTIVE_USERS = 25;
  public static final int UPDATE_GAME_REQUESTS = 26;
  public static final int GAME_REQUEST = 27;
  public static final int ACCEPT_GAME = 28;
  public static final int GAME_POSITIONS = 29;
  public static final int MOVE_RECEIVED = 30;
  public static final int ACKNOWLEDGE_MOVE_SENT = 31;
  public static final int REGISTERED_TO_SERVICE = 40;

  private SharedPreferences pref;

  private final Messenger mMessenger = new Messenger(new IncomingHandler());
  int mValue = 0; // Holds last value set by a client.
  ArrayList<Messenger> mClients = new ArrayList<Messenger>(); // Keeps track
  List<BasicNameValuePair> extraHeaders = Arrays.asList(new BasicNameValuePair("Cookie",
          "session=abcd"));
  WebSocketClient client = new WebSocketClient(URI.create(serverAddress),
          new WebSocketClient.Listener() {

    @Override
    public void onConnect() {
      setConnected(true);
      System.out.println("send connected message to activity");
      sendMessageToActivity(CONNECTED_TO_SERVER);
    }

    @Override
    public void onMessage(String message) {

      HashMap<String, String> map = new HashMap<>();
      try {
        JSONObject json = new JSONObject(message);
        Log.d(TAG, "JSON RESPONSE: " + json.toString());
        Iterator i = json.keys();
        while (i.hasNext()) {
          String key = i.next().toString();
          String value = json.getString(key);
          map.put(key, value);
        }
      } catch (JSONException je) {
      }

      //Log.d(TAG, "message reply type received from the server " + Integer.parseInt(map.get("type")));
      Boolean sendToActivity = true;
      Log.i(TAG, message);
      switch (Integer.parseInt(map.get("type"))) {
        case MOVE_RECEIVED:;
          if (!pref.getBoolean("gameActivityRunning", false)) {
            createNotification(map);
            sendToActivity = false;
          }
          break;
      }
      if (sendToActivity) {
        sendMessageToActivity(Integer.parseInt(map.get("type")), map);
      }
    }
    @Override
    public void onMessage(byte[] data) {
    }

    @Override
    public void onDisconnect(int code, String reason) {
      Log.d(TAG, String.format("Disconnected! Code: %d Reason: %s", code, reason));
      setConnected(false);
    }

    @Override
    public void onError(Exception error) {
      Log.e(TAG, "Error!", error);
      setConnected(false);
    }
  }, extraHeaders);

  public static boolean isRunning() {
    return isRunning;
  }

  public void createNotification(HashMap<String, String> map) {

    int notificationId;
    SharedPreferences notificationPref = getSharedPreferences("notifications", Context.MODE_PRIVATE);
    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    HashMap<String, Integer> notifications = (HashMap<String, Integer>) notificationPref.getAll();

    if(notifications == null) {
      notifications = new HashMap<>();
    }
    if (notifications.containsKey(map.get("gameId"))) {
      notificationId = notifications.get(map.get("gameId"));
    } else {
       Log.i(TAG, "gameId not found in sp getting a new one");
      notificationId = pref.getInt("notificationCount", 0);
      if (notificationId == Integer.MAX_VALUE) {
        notificationId = 0;
      }
      notifications.put(map.get("gameId"), notificationId);
    }
    Intent intent = new Intent(this, GameActivity.class);
    intent.putExtra("opponent", map.get("opponent"));
    intent.putExtra("gameId", map.get("gameId"));
    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    PendingIntent pIntent = PendingIntent.getActivity(getApplicationContext(),0,intent, PendingIntent.FLAG_UPDATE_CURRENT);

    Notification n  = new Notification.Builder(this)
            .setContentTitle(map.get("opponent") + " has sent you a new move. Click to view")
            .setContentText("New move received")
            .setSmallIcon(R.drawable.grey_circle)
            .setContentIntent(pIntent)
            .setAutoCancel(true)
            .build();
    notificationManager.notify(notificationId, n);

    SharedPreferences.Editor editor = notificationPref.edit();
    for (String s : notifications.keySet()) {
      Log.i(TAG, s);
      editor.putInt(s, notifications.get(s));
    }
    editor.commit();
    pref.edit().putInt("notificationCount", ++notificationId);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    Log.d(TAG, "SocketService created");
    context = this;
    pref = PreferenceManager.getDefaultSharedPreferences(this);
    setConnected(false);
    isRunning = true;
  }

  @Override
  public IBinder onBind(Intent intent) {
    return mMessenger.getBinder();
  }

  private void setConnected(boolean connectionStatus) {
    pref.edit().putBoolean("isConnectedToServer", connectionStatus).commit();
  }

  private void sendMessageToActivity(int type) {

    for (int i = mClients.size() - 1; i >= 0; i--) {
      try {
        Message msg = Message.obtain();
        msg.what = type;
        mClients.get(i).send(msg);
      } catch (RemoteException e) {

        // The client is dead. Remove it from the list; we are going
        // through the list from back to front so this is safe to do
        // inside the loop.
        mClients.remove(i);
      }
    }
  }

  private void sendMessageToActivity(int type, HashMap<String, String> map) {

    for (int i = mClients.size() - 1; i >= 0; i--) {
      try {
        Message msg = Message.obtain();
        msg.what = type;
        msg.obj = map;
        mClients.get(i).send(msg);
      } catch (RemoteException e) {

        // The client is dead. Remove it from the list; we are going
        // through the list from back to front so this is safe to do
        // inside the loop.
        mClients.remove(i);
      }
    }
  }

  private void connectToServer() {
    client.connect();
  }

  private void sendMessageToServer(int type, JSONObject msg) {

    try {
      msg.put("type", type);
      client.send(msg.toString());
    } catch (JSONException jE) {
    }
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    System.out.println("Service Started");
    Log.i(TAG, "Received start id " + startId + ": " + intent);
    return START_STICKY; // run until explicitly stopped.
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.i(TAG, "Service destroyed... disconnect and cancel notifications");
    isRunning = false;
    client.disconnect();
    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    notificationManager.cancelAll();
  }


  class IncomingHandler extends Handler { // Handler of incoming messages from Activities.

    @Override
    public void handleMessage(Message msg) {

      JSONObject msgJSON = new JSONObject();

      if (msg.obj != null) {

        Iterator<String> i = ((HashMap<String, Content>) msg.obj).keySet().iterator();
        try {
          while (i.hasNext()) {
            String key = i.next().toString();
            if (((HashMap<String, Content>) msg.obj).get(key).isArray()) {
              JSONArray arr = new JSONArray();
              for (String item : ((HashMap<String, Content>) msg.obj).get(key).getObj()) {
                arr.put(item);
              }
              msgJSON.put(key, arr);
            } else {
              String value = ((HashMap<String, Content>) msg.obj).get(key).getObj()[0];
              msgJSON.put(key, value);
            }
          }
        } catch (JSONException je) {
        }
      }

      switch (msg.what) {

        case MSG_REGISTER_CLIENT:
          Log.i(TAG, "adding to reply list");
          mClients.add(msg.replyTo);
          sendMessageToActivity(REGISTERED_TO_SERVICE);
          break;
        case MSG_UNREGISTER_CLIENT:
          Log.i(TAG, "delete from reply list");
          mClients.remove(msg.replyTo);
          break;
        case MSG_CONNECT_TO_SOCKET:
          Log.i(TAG, "checking server connection");
          connectToServer();
          break;
        case MSG_LOGIN:
          Log.i(TAG, "user attempted to login with existing credentials");
          sendMessageToServer(msg.what, msgJSON);
          break;
        case MSG_LOGOUT:
          Log.i(TAG, "user requested logout");
          sendMessageToServer(msg.what, msgJSON);
          break;
        case MSG_SIGNUP:
          Log.i(TAG, "user attempted to create a new account");
          sendMessageToServer(msg.what, msgJSON);
          break;
        case MSG_VALIDATE:
          Log.i(TAG, "validate credentials with existing token");
          sendMessageToServer(msg.what, msgJSON);
          break;
        case MSG_GET_CURRENT_USERS:
          Log.i(TAG, "user requested list of current users");
          sendMessageToServer(msg.what, msgJSON);
          break;
        case MSG_GET_CURRENT_REQUESTS:
          Log.i(TAG, "user requested list of current users");
          sendMessageToServer(msg.what, msgJSON);
          break;
        case MSG_GET_CURRENT_GAMES:
          Log.i(TAG, "user requested list of current users");
          sendMessageToServer(msg.what, msgJSON);
          break;
        case MSG_GAME_REQUEST:
          Log.i(TAG, "sending data to server");
          sendMessageToServer(msg.what, msgJSON);
          break;
        case MSG_GET_GAME_STATE:
          Log.i(TAG, "user requested list of current users");
          sendMessageToServer(msg.what, msgJSON);
          break;
        case MSG_CANCEL_USERS_REQUEST:
          Log.i(TAG, "sending disconnect message");
          client.disconnect();
          break;
        case MSG_ACCEPT_REQUEST:
          Log.i(TAG, "requesting new game from the sever");
          sendMessageToServer(msg.what, msgJSON);
          break;
        case MSG_SEND_MOVE:
          Log.i(TAG, "sending new move to server");
          sendMessageToServer(msg.what, msgJSON);
          break;
        case MSG_ACKNOWLEDGE_MOVE_RECEIVED:
          Log.i(TAG, "sending new move to server");
          sendMessageToServer(msg.what, msgJSON);
          break;
        default:
          super.handleMessage(msg);
      }
    }
  }
}
