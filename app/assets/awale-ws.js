awale.init = function() {
  awale.view.init();
  awale.ctrl.init();
};

awale.ctrl = {
  init: function () {
    var wsUri = "ws://" + document.location.host + "/ws";
    var websocket = new WebSocket(wsUri);
    awale.websocket = websocket;

    var self = this;
    websocket.onopen = function (evt) {
      self.onOpen(evt);
    };
    websocket.onclose = function (evt) {
      self.onClose(evt);
    };
    websocket.onmessage = function (evt) {
      self.onMessage(evt);
    };
    websocket.onerror = function (evt) {
      self.onError(evt);
    };
  },
  onOpen: function (evt) {
    this.debug("CONNECTED");
    console.log(document.location.pathname);
    if (document.location.pathname.startsWith("/game/")) {
      awale.gameId = document.location.pathname.substring(6);
      this.doSend("join:" + awale.gameId);
    } else {
      this.doSend("connect:" + document.location.pathname);
    }
  },
  onClose: function (evt) {
    this.debug("DISCONNECTED");
  },
  onMessage: function (evt) {
    this.debug('<span style="color: blue;">RESPONSE: ' + evt.data + '</span>');
    if (evt.data.startsWith("Stats")) {
      var arr = evt.data.split(":");
      document.getElementById("nb-players").innerHTML = arr[1];
      document.getElementById("nb-games").innerHTML = arr[2];
    }
    if (evt.data.startsWith("active")) {
      var house = evt.data.substring(6);
      console.log(house);
      if (evt.data.length > 6) {
        awale.game = awale.game.play(+house);
        awale.view.animateSowing();
      }
    }
    if (evt.data == "close") {
      document.getElementById("message").innerHTML = 'Opponent disconnected';
    }
    if (evt.data == "passive") {
    }
    if (evt.data == "join1") {
      awale.status = "started";
      awale.view.refresh();
    }
    if (evt.data == "join2") {
      awale.playerId = 1;
      awale.status = "started";
      awale.view.swapBoard();
      awale.view.refresh();
    }
    if (evt.data.startsWith("Game:")) {
      awale.gameId = evt.data.substring(5);
      awale.playerId = 0;
      awale.status = "await";
      awale.view.refresh();
    }
  },
  onError: function (evt) {
    this.debug('ERROR: ' + evt.data);
  },
  doSend: function(message) {
    this.debug("SENT: " + message);
    awale.websocket.send(message);
  },
  doMove: function(move) {
    var message = "move:" + awale.gameId + ":" + move;
    this.doSend(message);
  },
  debug: function(message) {
    console.log(message);
  }
};

document.addEventListener("DOMContentLoaded", awale.init, false);