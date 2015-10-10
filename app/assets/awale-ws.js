awale.init = function() {
  awale.view.init();
  awale.ctrl.init();
};

awale.ctrl = {
  init: function () {
    var wsUri = "ws://" + document.location.host + "/ws";
    try {
      this.websocket = new WebSocket(wsUri);
    } catch(e) {
      console.log("Can't connect to : " + wsUri + " - " + e);
      awale.error = "Can't connect to : " + wsUri + " - " + e;
      awale.view.refresh();
    }

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
    awale.error = "DISCONNECTED";
    awale.view.refresh();
    console.log("DISCONNECTED");
  },
  onMessage: function (evt) {
    console.log('RESPONSE: ' + evt.data);
    var arr = evt.data.split(":");
    if (arr[0] === "stats") {
      awale.metrics={nbPlayers:arr[1], nbGames:arr[2]};
      awale.view.refresh();
    }
    if (arr[0] === "active") {
      awale.time={time0:+arr[2], time1:+arr[3]};
      if (arr[1] !== "") { // init case
        awale.game = awale.game.play(+arr[1]);
        awale.view.animateSowing();
      }
    }
    if (arr[0] === "close") {
      awale.status = "disconnected";
      awale.view.refresh();
    }
    if (arr[0] === "passive") {
      awale.time={time0:+arr[1], time1:+arr[2]};
      //awale.view.refresh();
    }
    if (arr[0] === "join1") {
      awale.status = "started";
      awale.view.refresh();
    }
    if (arr[0] === "join2") {
      awale.playerId = 1;
      awale.status = "started";
      awale.view.swapBoard();
      awale.view.refresh();
    }
    if (arr[0] === "game") {
      awale.gameId = arr[1];
      awale.playerId = 0;
      awale.status = "await";
      awale.view.refresh();
    }
    if (arr[0] === "error") {
      awale.error = arr[1];
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