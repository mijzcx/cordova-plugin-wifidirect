package com.mijzcx.cordova.plugin;

// import com.mijzcx.cordova.plugin.Constants;
// import com.mijzcx.cordova.plugin.WifiDirect;

public class Actions {

  // private WifiDirect wifiDirect;

  // public Actions(WifiDirect wifiDirect) {
  //   this.wifiDirect = wifiDirect;
  // }

  public void actionWifiStatus(CallbackContext ctx) {
    // Log.i(Constants.LOG_TITLE, Constants.ACTION_WIFI_STATUS);
    // hlpInit(ctx);
    // Boolean isActive = mWifiManager.isWifiEnabled();
    // hlpSuccessMessage(ACTION_WIFI_STATUS, "isActive", isActive, ctx);
  }

  public void actionActivateWifi(JSONArray args, CallbackContext ctx) throws JSONException {
    // Log.i(Constants.LOG_TITLE, Constants.ACTION_ACTIVATE_WIFI);
    // hlpInit(ctx);

    // JSONObject opt = args.getJSONObject(0);
    // Boolean activate = opt.getBoolean("activate");
    // mWifiManager.setWifiEnabled(activate);
  }

  public void actionDiscoverPeers(final CallbackContext ctx) {
    // Log.i(Constants.LOG_TITLE, Constants.ACTION_DISCOVER_PEERS);
    // hlpInit(ctx);

    // Log.i(LOG_TITLE, (mManager == null) + "mManager");
    // Log.i(LOG_TITLE, (mChannel == null) + "mChannel");
    // Log.i(LOG_TITLE, (mapWifiP2pDevices == null) + "mapWifiP2pDevices");

    // WifiP2pManager.ActionListener listener = new WifiP2pManagerActionAdapter(ctx) {
    //   @Override
    //   public void onSuccess() {
    //     hlpClearMapWifiP2pDevices();
    //     hlpSuccessMessage(ACTION_DISCOVER_PEERS, "onSuccess", "Discovery Started", ctx);
    //   }
    // };

    // mManager.stopPeerDiscovery(mChannel, null);
    // cut all the connection
    // if (mChannel != null) {
    //   mManager.removeGroup(mChannel, null);
    // }

    // mManager.discoverPeers(mChannel, listener);
  }

  public void actionStopDiscoverPeers(final CallbackContext ctx) {
    // Log.i(Constants.LOG_TITLE, Constants.ACTION_STOP_DISCOVER_PEERS);
    // hlpInit(ctx);

    // mManager.stopPeerDiscovery(mChannel, new WifiP2pManagerActionAdapter(ctx) {
    //   @Override
    //   public void onSuccess() {
    //     hlpSuccessMessage(ACTION_STOP_DISCOVER_PEERS, "onSuccess", "Discovery Stopped", ctx);
    //   }
    // });
  }

  public void actionConnectToPeer(JSONArray args, CallbackContext ctx) throws JSONException {
    // Log.i(Constants.LOG_TITLE, Constants.ACTION_CONNECT_TO_PEER);

    // hlpInit(ctx);

    // JSONObject opt = args.getJSONObject(0);
    // String peerName = opt.getString("peerName");
    // String peerAddress = opt.getString("peerAddress");

    // WifiP2pConfig config = new WifiP2pConfig();
    // config.deviceAddress = peerAddress;

    // mManager.connect(mChannel, config, new WifiP2pManagerActionAdapter(ctx) {
    //   @Override
    //   public void onSuccess() {
    //     hlpSuccessMessage(ACTION_CONNECT_TO_PEER, "onSuccess", "Connected to " + peerName, ctx);
    //   } 

    //   @Override
    //   public void onFailure(int id) {
    //     ctx.error("Connection Failed " + id);
    //   }
    // });
  }

  public void actionDisconnectPeer(JSONArray args, CallbackContext ctx) throws JSONException {
    // Log.i(Constants.LOG_TITLE, Constants.ACTION_DISCONNECT_PEER);

    // hlpInit(ctx);

    // JSONObject opt = args.getJSONObject(0);
    // String peerName = opt.getString("peerName");
    // String peerAddress = opt.getString("peerAddress");

    // WifiP2pManager.ActionListener listener  = new WifiP2pManagerActionAdapter(ctx) {
    //   @Override
    //   public void onSuccess() {
    //     hlpSuccessMessage(ACTION_DISCONNECT_PEER, "onSuccess", "Disconnect " + peerName, ctx);
    //   }

    //   @Override
    //   public void onFailure(int id) {
    //     ctx.error("Disconnect Peer Failed " + id + " @" + peerName);
    //   }
    // };

    // WifiP2pDevice mWifiP2pDevice = hlpGetWifiP2pDevice(peerName, peerAddress);

    // if (mManager != null) {
    //   if ((mWifiP2pDevice == null)
    //     || (mWifiP2pDevice.status == WifiP2pDevice.CONNECTED)) {
    //     if (mChannel != null) {
    //       mManager.removeGroup(mChannel, listener);
    //     }
    //   } else if (mWifiP2pDevice.status == WifiP2pDevice.AVAILABLE
    //     || mWifiP2pDevice.status == WifiP2pDevice.INVITED) {
    //       mManager.cancelConnect(mChannel, listener);
    //   }
    // }
  }

  public void actionSendMessage(JSONArray args, CallbackContext ctx) throws JSONException {
    // Log.i(Constants.LOG_TITLE, Constants.ACTION_SEND_MESSAGE);
    // hlpInit(ctx);

    // JSONObject opt = args.getJSONObject(0);
    // String message = opt.getString("message");

    // if (sendReceiveThread != null) {
    //   sendReceiveThread.write(message.getBytes());
    // }
  }
}