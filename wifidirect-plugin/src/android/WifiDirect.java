package com.mijzcx.cordova.plugin;

import org.apache.cordova.PluginResult;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import android.util.*;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;

import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pConfig;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.app.Activity;
import android.os.Looper;
import android.os.Handler;
import android.os.Message;
import android.net.NetworkInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.ServerSocket;
import java.lang.Thread;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;

// import com.mijzcx.cordova.plugin.Constants;
// import com.mijzcx.cordova.plugin.Actions;

public class WifiDirect extends CordovaPlugin {

  public static final String LOG_TITLE = "chromium";

  private WifiManager mWifiManager;
  private WifiP2pManager mManager;
  private Channel mChannel;
  private Activity mActivity;
  private IntentFilter mIntentFilter;
  private WifiBroadcastReceiver mReceiver;

  public static final String ACTION_WIFI_STATUS = "wifiStatus";
  public static final String ACTION_ACTIVATE_WIFI = "activateWifi";
  public static final String ACTION_DISCOVER_PEERS = "discoverPeers";
  public static final String ACTION_STOP_DISCOVER_PEERS = "stopDiscoverPeers";
  public static final String ACTION_CONNECT_TO_PEER = "connectToPeer";
  public static final String ACTION_DISCONNECT_PEER = "disconnectPeer";
  public static final String ACTION_SEND_MESSAGE = "sendMessage";

  private static final int PORT = 8888;
  private static final int MESSAGE_READ = 1;
  private ServerThread serverThread;
  private ClientThread clientThread;
  private SendReceiveThread sendReceiveThread;
  private Handler handler;
  private Map<String, WifiP2pDevice> mapWifiP2pDevices;

  // start - Helpers methods
  private static void hlpSuccessMessage(
      String method, 
      String event, 
      Object message, 
      CallbackContext ctx) {

    try {
      JSONObject resp = new JSONObject();
      String type = method + ":" + event;
      resp.put("type", type);
      resp.put("message", message);
      PluginResult res = new PluginResult(PluginResult.Status.OK, resp);
      res.setKeepCallback(true);
      ctx.sendPluginResult(res);
    } catch (JSONException e) {
      ctx.error("Error encountered: " + e.getMessage());
    }
  }

  private void hlpInit(CallbackContext cb) {
    mReceiver.setCallbackContext(cb);

    handler = hlpCreateHandler(cb);
  }

  private void hlpNulled() {
    mActivity = null;
    mIntentFilter = null;
    mManager = null;
    mChannel = null;
    mWifiManager = null;
    mReceiver = null;
    hlpNulledSockets();
    hlpNulledHandler();
    hlpNulledMapWifiP2pDevices();
  }

  private void hlpNulledSockets() {
    serverThread = null;
    clientThread = null;
    if (sendReceiveThread != null) {
      sendReceiveThread.off();
      sendReceiveThread = null;
    }
  }

  private void hlpNulledHandler() {
    handler = null;
  }

  private void hlpClearMapWifiP2pDevices() {
    if (mapWifiP2pDevices != null) {
      mapWifiP2pDevices.clear();
    }
  }

  private void hlpNulledMapWifiP2pDevices() {
    mapWifiP2pDevices = null;
    Log.i(LOG_TITLE, "hlpNulledOthers");
  }

  private Handler hlpCreateHandler(CallbackContext cb) {
    return new Handler(new Handler.Callback() {
      @Override
      public boolean handleMessage(Message msg) {
        switch (msg.what) {
          case MESSAGE_READ: 
            byte[] readBuff = (byte[]) msg.obj;
            String message = new String(readBuff, 0, msg.arg1);
  
            hlpSuccessMessage(ACTION_SEND_MESSAGE, "handleMessage", message, cb);
            break;
        }
        return true;
      }
    });
  }

  private void hlpSetMapPeerList(Collection<WifiP2pDevice> iPeerList) {
    mapWifiP2pDevices.clear();

    for (WifiP2pDevice device : iPeerList) {
      String key = device.deviceName + ":" + device.deviceAddress;
      mapWifiP2pDevices.put(key, device);
    }
  }

  private boolean hlpIsAllKeysExists(Collection<WifiP2pDevice> iPeerList) {

    boolean retval = true;
    for (WifiP2pDevice peer : iPeerList) {
      String key = peer.deviceName + ":" + peer.deviceAddress;
      retval = retval && (mapWifiP2pDevices.get(key) != null);
    }

    return retval;
  }
  
  private WifiP2pDevice hlpGetWifiP2pDevice(String peerName, String peerAddress) {
    String key = peerName + ":" + peerAddress;
    return mapWifiP2pDevices.get(key);
  }
  // end - Helpers methods

  // start - Shared Class
  private static abstract class WifiP2pManagerActionAdapter 
    implements WifiP2pManager.ActionListener {

    private CallbackContext ctx;
    public WifiP2pManagerActionAdapter(CallbackContext ctx) {
      this.ctx = ctx;
    }

    @Override
    public void onFailure(int id) {
      ctx.error("Discovery Failed " + id);
    }
  }
  // end - Shared Class

  // start - Override Lifecycle
  @Override
  public void onReset() {
    super.onReset();
    Log.i(LOG_TITLE, "onReset");
    mActivity.unregisterReceiver(mReceiver);
    mActivity.registerReceiver(mReceiver, mIntentFilter);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.i(LOG_TITLE, "onDestroy");
    mActivity.unregisterReceiver(mReceiver);
    hlpNulled();
  }

  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);

    Log.i(LOG_TITLE, "initialize");
    if (mActivity == null) {
      mActivity = cordova.getActivity();
    }

    if (mIntentFilter == null) {
      mIntentFilter = new IntentFilter();
      mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
      mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
      mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
      mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    if (mManager == null) {
      mManager = (WifiP2pManager) mActivity.getSystemService(Context.WIFI_P2P_SERVICE);
    }

    if (mChannel == null) {
      mChannel = mManager.initialize(mActivity, Looper.getMainLooper(), null);
    }

    if (mWifiManager == null) {
      mWifiManager = (WifiManager) mActivity.getSystemService(Context.WIFI_SERVICE);
    }

    if (mReceiver == null) {
      mReceiver = new WifiBroadcastReceiver();
      mActivity.registerReceiver(mReceiver, mIntentFilter);
    }

    if (mapWifiP2pDevices == null) {
      mapWifiP2pDevices = new HashMap<String, WifiP2pDevice>();
      Log.i(LOG_TITLE, "init mapWifiP2pDevices");
    }

    // actions = new Actions(this);
  }

  // private Actions actions;

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext ctx) throws JSONException {

    Log.i(LOG_TITLE, "execute");

    // switch (action) {
    //   case Constants.ACTION_WIFI_STATUS: 
    //     actions.actionWifiStatus(ctx);
    //     return true;
    //   case Constants.ACTION_ACTIVATE_WIFI:
    //     actions.actionActivateWifi(args, ctx);
    //     return true;
    //   case Constants.ACTION_DISCOVER_PEERS:
    //     actions.actionDiscoverPeers(ctx);
    //     return true;
    //   case Constants.ACTION_STOP_DISCOVER_PEERS:
    //     actions.actionStopDiscoverPeers(ctx);
    //     return true;
    //   case Constants.ACTION_CONNECT_TO_PEER:
    //     actions.actionConnectToPeer(args, ctx);
    //     return true;
    //   case Constants.ACTION_DISCONNECT_PEER:
    //     actions.actionDisconnectPeer(args, ctx);
    //     return true;
    //   case Constants.ACTION_SEND_MESSAGE:
    //     actions.actionSendMessage(args, ctx);
    //     return true;
    //   default: return true;
    // }

    switch (action) {
      case ACTION_WIFI_STATUS: 
        actionWifiStatus(ctx);
        return true;
      case ACTION_ACTIVATE_WIFI:
        actionActivateWifi(args, ctx);
        return true;
      case ACTION_DISCOVER_PEERS:
        actionDiscoverPeers(ctx);
        return true;
      case ACTION_STOP_DISCOVER_PEERS:
        actionStopDiscoverPeers(ctx);
        return true;
      case ACTION_CONNECT_TO_PEER:
        actionConnectToPeer(args, ctx);
        return true;
      case ACTION_DISCONNECT_PEER:
        actionDisconnectPeer(args, ctx);
        return true;
      case ACTION_SEND_MESSAGE:
        actionSendMessage(args, ctx);
        return true;
      default: return true;
    }
  }
  // end - Override Lifecycle

  // start - Wifi Status and Activation
  private void actionWifiStatus(CallbackContext ctx) {
    Log.i(LOG_TITLE, ACTION_WIFI_STATUS);
    hlpInit(ctx);
    Boolean isActive = mWifiManager.isWifiEnabled();
    hlpSuccessMessage(ACTION_WIFI_STATUS, "isActive", isActive, ctx);
  }

  private void actionActivateWifi(JSONArray args, CallbackContext ctx) throws JSONException {
    Log.i(LOG_TITLE, ACTION_ACTIVATE_WIFI);
    hlpInit(ctx);

    JSONObject opt = args.getJSONObject(0);
    Boolean activate = opt.getBoolean("activate");
    mWifiManager.setWifiEnabled(activate);
  }
  // start - Wifi Status and Activation

  // start - Peer Discovery and Stop Discovery
  private void actionDiscoverPeers(final CallbackContext ctx) {
    Log.i(LOG_TITLE, ACTION_DISCOVER_PEERS);
    hlpInit(ctx);

    Log.i(LOG_TITLE, (mManager == null) + "mManager");
    Log.i(LOG_TITLE, (mChannel == null) + "mChannel");
    Log.i(LOG_TITLE, (mapWifiP2pDevices == null) + "mapWifiP2pDevices");

    WifiP2pManager.ActionListener listener = new WifiP2pManagerActionAdapter(ctx) {
      @Override
      public void onSuccess() {
        hlpClearMapWifiP2pDevices();
        hlpSuccessMessage(ACTION_DISCOVER_PEERS, "onSuccess", "Discovery Started", ctx);
      }
    };

    // mManager.stopPeerDiscovery(mChannel, null);
    // cut all the connection
    // if (mChannel != null) {
    //   mManager.removeGroup(mChannel, null);
    // }

    mManager.discoverPeers(mChannel, listener);
  }

  private void actionStopDiscoverPeers(final CallbackContext ctx) {
    Log.i(LOG_TITLE, ACTION_STOP_DISCOVER_PEERS);
    hlpInit(ctx);

    mManager.stopPeerDiscovery(mChannel, new WifiP2pManagerActionAdapter(ctx) {
      @Override
      public void onSuccess() {
        hlpSuccessMessage(ACTION_STOP_DISCOVER_PEERS, "onSuccess", "Discovery Stopped", ctx);
      }
    });
  }
  // stop - Peer Discovery and Stop Discovery

  // start - Connect/Disconnect to Peer
  private void actionConnectToPeer(JSONArray args, CallbackContext ctx) throws JSONException {
    Log.i(LOG_TITLE, ACTION_CONNECT_TO_PEER);

    hlpInit(ctx);

    JSONObject opt = args.getJSONObject(0);
    String peerName = opt.getString("peerName");
    String peerAddress = opt.getString("peerAddress");

    WifiP2pConfig config = new WifiP2pConfig();
    config.deviceAddress = peerAddress;

    mManager.connect(mChannel, config, new WifiP2pManagerActionAdapter(ctx) {
      @Override
      public void onSuccess() {
        hlpSuccessMessage(ACTION_CONNECT_TO_PEER, "onSuccess", "Connected to " + peerName, ctx);
      } 

      @Override
      public void onFailure(int id) {
        ctx.error("Connection Failed " + id);
      }
    });
  }

  private void actionDisconnectPeer(JSONArray args, CallbackContext ctx) throws JSONException {
    Log.i(LOG_TITLE, ACTION_DISCONNECT_PEER);

    hlpInit(ctx);

    JSONObject opt = args.getJSONObject(0);
    String peerName = opt.getString("peerName");
    String peerAddress = opt.getString("peerAddress");

    WifiP2pManager.ActionListener listener  = new WifiP2pManagerActionAdapter(ctx) {
      @Override
      public void onSuccess() {
        hlpSuccessMessage(ACTION_DISCONNECT_PEER, "onSuccess", "Disconnect " + peerName, ctx);
      }

      @Override
      public void onFailure(int id) {
        ctx.error("Disconnect Peer Failed " + id + " @" + peerName);
      }
    };

    WifiP2pDevice mWifiP2pDevice = hlpGetWifiP2pDevice(peerName, peerAddress);

    if (mManager != null) {
      if ((mWifiP2pDevice == null)
        || (mWifiP2pDevice.status == WifiP2pDevice.CONNECTED)) {
        if (mChannel != null) {
          mManager.removeGroup(mChannel, listener);
        }
      } else if (mWifiP2pDevice.status == WifiP2pDevice.AVAILABLE
        || mWifiP2pDevice.status == WifiP2pDevice.INVITED) {
          mManager.cancelConnect(mChannel, listener);
      }
    }
  }
  // end - Connect/Disconnect to Peer

  // start - Peer List
  private void actionSendMessage(JSONArray args, CallbackContext ctx) throws JSONException {
    Log.i(LOG_TITLE, ACTION_SEND_MESSAGE);
    hlpInit(ctx);

    JSONObject opt = args.getJSONObject(0);
    String message = opt.getString("message");

    if (sendReceiveThread != null) {
      sendReceiveThread.write(message.getBytes());
    }
  }


  private class WifiBroadcastReceiver extends BroadcastReceiver {

    private CallbackContext cb;
    private PeerListListener peerListListener;

    public WifiBroadcastReceiver() {
      super();

      peerListListener = new PeerListListener() {

        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
  
          Collection<WifiP2pDevice> iPeerList = peerList.getDeviceList();
  
          if (!hlpIsAllKeysExists(iPeerList)) {
            hlpSetMapPeerList(iPeerList);
            reactionPeerListMessage();
          }
  
        }
      };
    }

    public void setCallbackContext(CallbackContext cb) {
      this.cb = cb;
    }

    private JSONObject hlpPutDevice(WifiP2pDevice device) throws JSONException {
      JSONObject retval = new JSONObject();
      retval.put("name", device.deviceName);
      retval.put("address", device.deviceAddress);
      retval.put("deviceTypePrimary", device.primaryDeviceType);
      retval.put("deviceTypeSecondary", device.secondaryDeviceType);
      retval.put("status", device.status);
      return retval;
    }

    private void reactionPeerListMessage() {
      try {
        JSONArray peers = new JSONArray();

        for (WifiP2pDevice device : mapWifiP2pDevices.values()) {
          JSONObject o = hlpPutDevice(device);
          peers.put(o);
        }
  
        hlpSuccessMessage(ACTION_DISCOVER_PEERS, "peerList", peers, cb);
        
      } catch (JSONException e) {
        cb.error("Error encountered: " + e.getMessage());
      }
    }

    private void reactionIsConnectedMessage(String message) {
      hlpSuccessMessage(ACTION_CONNECT_TO_PEER, "isConnected", message, cb);
    }

    private void reactionPutDevice(WifiP2pDevice device) {
      try {
        JSONObject o = hlpPutDevice(device);
        hlpSuccessMessage(ACTION_WIFI_STATUS, "device", o, cb);
        
      } catch (JSONException e) {
        cb.error("Error encountered: " + e.getMessage());
      }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();

      if (action.equals(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)) {
        Log.i(LOG_TITLE, "WIFI_P2P_STATE_CHANGED_ACTION");
        int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
        hlpSuccessMessage(ACTION_WIFI_STATUS, "state",  state == WifiP2pManager.WIFI_P2P_STATE_ENABLED, cb);
      }
      else if (action.equals(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)) {
        Log.i(LOG_TITLE, "WIFI_P2P_PEERS_CHANGED_ACTION");
        if (mManager == null) return;

        Log.i(LOG_TITLE, (mManager == null) + "WIFI_P2P_PEERS_CHANGED_ACTION mManager");
        Log.i(LOG_TITLE, (mChannel == null) + "WIFI_P2P_PEERS_CHANGED_ACTION mChannel");    

        mManager.requestPeers(mChannel, peerListListener);
      }
      else if (action.equals(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)) {
        Log.i(LOG_TITLE, "WIFI_P2P_CONNECTION_CHANGED_ACTION");
        if (mManager == null) {
          Log.i(LOG_TITLE, "WIFI_P2P_CONNECTION_CHANGED_ACTION mManager was null");
          return;
        }
        // Respond to new connection or disconnection
        NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
        if (networkInfo.isConnected()) {
          mManager.requestConnectionInfo(mChannel, new ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
              final InetAddress groupOwnerAddress = wifiP2pInfo.groupOwnerAddress;
              
              if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
                serverThread = new ServerThread();
                serverThread.start();
                reactionIsConnectedMessage("Host");         
              } else if (wifiP2pInfo.groupFormed) {
                clientThread = new ClientThread(groupOwnerAddress);
                clientThread.start();      
                reactionIsConnectedMessage("Client");
              }
            }
          });
        } else {
          // hlpNulledSockets();
          // clear all data here
          // if (mChannel != null) {
          //   mManager.removeGroup(mChannel, null);
          // }
      
          // hlpClearMapWifiP2pDevices();

          // reactionPeerListMessage();
          reactionIsConnectedMessage("Device disconnected");
        }
      }
      else if (action.equals(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)) {
        Log.i(LOG_TITLE, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
        WifiP2pDevice device = (WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
        reactionPutDevice(device);
      }
    }
  }


  public class ServerThread extends Thread {
    Socket socket;
    ServerSocket serverSocket;

    @Override
    public void run() {
      try {
        serverSocket = new ServerSocket(PORT);
        socket = serverSocket.accept();
        if (sendReceiveThread != null) {
          sendReceiveThread.off();
          sendReceiveThread = null;
        }
        sendReceiveThread = new SendReceiveThread(socket);
        sendReceiveThread.start();
      } catch (IOException e) {
        Log.i(LOG_TITLE, "ERROR: ServerThread" + e.getMessage());
        // try {
        //   socket.close();
        //   serverSocket.close();
        // } catch (IOException ioe) {

        // }
      } 
    }
  }

  public class ClientThread extends Thread {
    Socket socket;
    String hostAdd;

    public ClientThread(InetAddress hostAddress) {
      hostAdd = hostAddress.getHostAddress();
      socket = new Socket();
    }

    @Override
    public void run() {
      try {
        socket.connect(new InetSocketAddress(hostAdd, PORT), 500);
        if (sendReceiveThread != null) {
          sendReceiveThread.off();
          sendReceiveThread = null;
        }
        sendReceiveThread = new SendReceiveThread(socket);
        sendReceiveThread.start();
      } catch (IOException e) {
        Log.i(LOG_TITLE, "ERROR: ClientThread" + e.getMessage());
      } 
      // kkk
      // finally {
      //   if (socket != null) {
      //     if (socket.isConnected()) {
      //       try {
      //         socket.close();
      //       } catch (IOException e) {}
      //     }
      //   }
      // }
    }
  }

  public class SendReceiveThread extends Thread {
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;

    public SendReceiveThread(Socket socket) {
      this.socket = socket;
      try {
        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();

      } catch (IOException e) {
        Log.i(LOG_TITLE, "ERROR: SendReceiveThread " + e.getMessage());
      }
    }

    public void write(byte[] bytes) {
      try {
        Log.i(LOG_TITLE, "inputStream " + (inputStream == null));
        // Log.i(LOG_TITLE, "inputStream " + (inputStream.isClose()));
        outputStream.write(bytes);
      } catch (IOException e) {
        Log.i(LOG_TITLE, "ERROR: SendReceiveThread.write " + e.getMessage());
      }
    }

    public void off() {
      // try {
      //   outputStream.flush();
      //   outputStream.close();
      //   inputStream.close();
      // } catch(IOException e) {

      // } finally {
        // socket = null;
      // }

    }

    @Override
    public void run() {
      byte[] buffer = new byte[1024];
      int bytes;
      
      while (socket != null) {
        try {

            bytes = inputStream.read(buffer);
            // Log.i(LOG_TITLE, (bytes > 0) + "bytes > 0");
            // Log.i(LOG_TITLE, (handler == null) + "handler");
            if (bytes > 0 && handler != null) {
              Log.i(LOG_TITLE, "obtainMessage");
              handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
            }

        } catch (IOException e) {
          Log.i(LOG_TITLE, "ERROR: SendReceiveThread.run " + e.getMessage());
          // try {
          //   outputStream.flush();
          //   outputStream.close();
          //   inputStream.close();
          //   socket.close();
          //   socket = null;
          // } catch (IOException ioe) {}
          // off();
        } finally {
          
        }
      }

      Log.i(LOG_TITLE, "ERROR: SendReceiveThread.run[Stopped]");
    }
  }
}
