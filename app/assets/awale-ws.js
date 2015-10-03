awale.init = function() {
  awale.view.init();
  awale.ctrl.init();
};

awale.ctrl = {
  init: function () {
    var wsUri = "ws://" + document.location.host + "/ws";
    this.websocket = new WebSocket(wsUri);

    var self = this;
    this.websocket.onopen = function (evt) {
      self.onOpen(evt);
    };
    this.websocket.onclose = function (evt) {
      self.onClose(evt);
    };
    this.websocket.onmessage = function (evt) {
      self.onMessage(evt);
    };
    this.websocket.onerror = function (evt) {
      self.onError(evt);
    };
  },
  onOpen: function (evt) {
    console.log("CONNECTED");
    if (document.location.pathname.startsWith("/game/")) {
      awale.gameId = document.location.pathname.substring(6);
      this.doSend("join:" + awale.gameId);
    } else {
      this.doSend("connect:" + document.location.pathname);
    }
  },
  onClose: function (evt) {
    console.log("DISCONNECTED");
  },
  onMessage: function (evt) {
    console.log('RESPONSE: ' + evt.data);
    if (evt.data.startsWith("Stats")) {
      var arr = evt.data.split(":");
      awale.metrics={nbPlayers:arr[1], nbGames:arr[2]};
      awale.view.refresh();
    }
    if (evt.data.startsWith("active")) {
      var house = evt.data.substring(6);
      if (evt.data.length > 6) {
        awale.game = awale.game.play(+house);
        awale.view.animateSowing();
      }
    }
    if (evt.data == "close") {
      awale.status = "disconnected";
      awale.view.refresh();
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
    console.log('ERROR: ' + evt.data);
  },
  doSend: function(message) {
    console.log("SENT: " + message);
    this.websocket.send(message);
  },
  doMove: function(move) {
    var message = "move:" + awale.gameId + ":" + move;
    this.doSend(message);
  }
};

document.addEventListener("DOMContentLoaded", awale.init, false);