cordova.define("com.mijzcx.cordova.plugin.wifidirect.wifidirect", function(require, exports, module) {
/*global cordova, module*/

module.exports = {
  AS_WIFI_STATUS_isActive : 'wifiStatus:isActive',
  AS_WIFI_STATUS_state: 'wifiStatus:state',
  AS_WIFI_STATUS_device: 'wifiStatus:device',
  AS_DISCOVER_PEERS_onSuccess: 'discoverPeers:onSuccess',
  AS_DISCOVER_PEERS_peerList: 'discoverPeers:peerList',
  AS_STOP_DISCOVER_PEERS_onSuccess: 'stopDiscoverPeers:onSuccess',
  AS_CONNECT_TO_PEER_onSuccess: 'connectToPeer:onSuccess',
  AS_CONNECT_TO_PEER_isConnected: 'connectToPeer:isConnected',
  AS_DISCONNECT_PEER_onSuccess: 'disconnectPeer:onSuccess',
  AS_SEND_MESSAGE_handleMessage: 'sendMessage:handleMessage',

  DEVICE_CONNECTED: 0,
  DEVICE_INVITED: 1,
  DEVICE_FAILED: 2,
  DEVICE_AVAILABLE: 3,
  DEVICE_UNAVAILABLE: 4,

  wifiStatus (cbSuccess, cbError) {
    cordova.exec(cbSuccess, cbError, 'WifiDirect', 'wifiStatus')
  },
  
  activateWifi (activate, cbSuccess, cbError) {
    const options = {activate}
    cordova.exec(cbSuccess, cbError, 'WifiDirect', 'activateWifi', [options])
  },

  discoverPeers (cbSuccess, cbError) {
    cordova.exec(cbSuccess, cbError, 'WifiDirect', 'discoverPeers')
  },

  stopDiscoverPeers(cbSuccess, cbError) {
    cordova.exec(cbSuccess, cbError, 'WifiDirect', 'stopDiscoverPeers')
  },

  connectToPeer (peerName, peerAddress, cbSuccess, cbError) {
    const options = {peerName, peerAddress}
    cordova.exec(cbSuccess, cbError, 'WifiDirect', 'connectToPeer', [options])
  },

  disconnectPeer (peerName, peerAddress, cbSuccess, cbError) {
    const options = {peerName, peerAddress}
    cordova.exec(cbSuccess, cbError, 'WifiDirect', 'disconnectPeer', [options])
  },

  sendMessage (message, cbSuccess, cbError) {
    const options = {message}
    cordova.exec(cbSuccess, cbError, 'WifiDirect', 'sendMessage', [options])
  }
}

});
