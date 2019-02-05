/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
var app = {
    // Application Constructor
  initialize: function() {
    document.addEventListener('deviceready', this.onDeviceReady.bind(this), false);

    const btnDiscovery = document.querySelector('#btnDiscovery')
    btnDiscovery.onclick = () => {
      this.discoverPeers()
    }

    const btnDisconnect = document.querySelector("#btnDisconnect")
    btnDisconnect.onclick = () => {
      this.disconnectPeer()
    }

    const btnSendMessage = document.querySelector('#btnSendMessage')
    btnSendMessage.onclick = () => {
      this.sendMessage()
    }
  },
  discoverPeers() {
    wifidirect.discoverPeers(app.onResponse, app.onError)
  },
  sendMessage() {

    const txtMessage = document.querySelector('#txtMessage')
    const val = txtMessage.value;
    
    wifidirect.sendMessage(val, app.onResponse, app.onError)

    txtMessage.value = ''
    txtMessage.focus()
  },
  disconnectPeer() {
    const selPeers = document.querySelector('#selPeers')
    const opt = selPeers.options[selPeers.selectedIndex]

    let peerAddress = ''
    let peerName = ''
    if (opt) {
      peerAddress = opt.value || ''
      peerName = opt.text || ''
    }

    wifidirect.disconnectPeer(peerName, peerAddress, app.onResponse, app.onError)
  },
  // deviceready Event Handler
  //
  // Bind any cordova events here. Common events are:
  // 'pause', 'resume', etc.
  onDeviceReady: function() {
    this.receivedEvent('deviceready');
  },
  onError(resp) {
    alert(resp)
  },
  onResponse (resp) {
    if (resp.type) {
      if (resp.type === wifidirect.AS_WIFI_STATUS_isActive) {
        const isActive = resp.message
        if (isActive) {
          // wifidirect.discoverPeers(app.onResponse)
        } else {
          wifidirect.activateWifi(true, app.onResponse, app.onError)
        }
      }
      else if (resp.type === wifidirect.AS_DISCOVER_PEERS_onSuccess) {
        alert(JSON.stringify(resp.message))
      }
      else if (resp.type === wifidirect.AS_DISCOVER_PEERS_peerList) {
        const selPeers = document.querySelector('#selPeers')
        const peers = resp.message
        if (Array.isArray(peers)) {
          const first = '<option val="">NONE</option>'
          const second = peers.map(peer => `<option value="${peer.address}">${peer.name} ${peer.status}</option>`).join('')
          selPeers.innerHTML = `${first} ${second}`
        } else {
          selPeers.innerHTML = ''
        }

        selPeers.onchange = function () {
          const opt = this.options[this.selectedIndex]
          const peerAddress = opt.value
          const peerName = opt.text

          wifidirect.connectToPeer(peerName, peerAddress, app.onResponse, app.onError)
        }
      }
      else if (resp.type === wifidirect.AS_STOP_DISCOVER_PEERS_onSuccess) {
        alert(JSON.stringify(resp.message))
      }
      else if (resp.type === wifidirect.AS_CONNECT_TO_PEER_onSuccess) {
        alert(JSON.stringify(resp.message))
      }
      else if (resp.type === wifidirect.AS_CONNECT_TO_PEER_isConnected) {
        alert(JSON.stringify(resp.message))
      }
      else if (resp.type === wifidirect.AS_DISCONNECT_PEER_onSuccess) {
        alert(JSON.stringify(resp.message))
      }
      else if (resp.type === wifidirect.AS_SEND_MESSAGE_handleMessage) {
        const spnMessage = document.querySelector('#spnMessage')
        spnMessage.innerHTML = resp.message
      }
    }
  },
  // Update DOM on a Received Event
  receivedEvent: function(id) {
    var parentElement = document.getElementById(id);
    var listeningElement = parentElement.querySelector('.listening');
    var receivedElement = parentElement.querySelector('.received');

    listeningElement.setAttribute('style', 'display:none;');
    receivedElement.setAttribute('style', 'display:block;');

    alert('Received Event: ' + id);

    wifidirect.wifiStatus(app.onResponse, app.onError)

    // hellokotlin.hello('test', (t) => {
    //   alert(t)
    // })
  }
};

app.initialize();