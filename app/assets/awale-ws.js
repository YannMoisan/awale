function init() {
  awale.view.init();
  initWebSocket();
}

function initWebSocket() {
  var wsUri = "ws://" + document.location.host + "/ws";
  var websocket = new WebSocket(wsUri);
  awale.websocket = websocket;

  websocket.onopen    = function (evt) { onOpen(evt); };
  websocket.onclose   = function (evt) { onClose(evt); };
  websocket.onmessage = function (evt) { onMessage(evt); };
  websocket.onerror   = function (evt) { onError(evt); };
}

function onOpen(evt) {
   writeToScreen("CONNECTED");
   console.log(document.location.pathname);
  if (document.location.pathname.startsWith("/game/")) {
    awale.gameId = document.location.pathname.substring(6);
    doSend("join:" + awale.gameId);
  } else {
    doSend("connect:" + document.location.pathname);
  }
}

function onClose(evt) {
  writeToScreen("DISCONNECTED");
}

function onMessage(evt) {
  writeToScreen('<span style="color: blue;">RESPONSE: ' + evt.data + '</span>');
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
}

function onError(evt) {
  writeToScreen('<span style="color: red;">ERROR:</span> ' + evt.data);
}

function doSend(message) {
  writeToScreen("SENT: " + message);
  awale.websocket.send(message);
}

function doMove(move) {
  var message = "move:" + awale.gameId + ":" + move;
  doSend(message);
}

function writeToScreen(message) {
  console.log(message);
}

document.addEventListener("DOMContentLoaded", init, false);