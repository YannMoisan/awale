function init() {
  awale.view.init();
  document.getElementById("game").style.display = 'none';
  document.getElementById("join").style.display = 'none';
  var copyTextareaBtn = document.querySelector('.copy-url');

  copyTextareaBtn.addEventListener('click', function(event) {
    var copyTextarea = document.querySelector('#join-url');
    copyTextarea.select();

    try {
      var successful = document.execCommand('copy');
      var msg = successful ? 'successful' : 'unsuccessful';
      console.log('Copying text command was ' + msg);
    } catch (err) {
      console.log('Oops, unable to copy');
    }
  });
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
    document.getElementById("active").style.display = 'block';
    document.getElementById("passive").style.display = 'none';
  }
  if (evt.data == "close") {
    document.getElementById("message").innerHTML = 'Opponent disconnected';
  }
  if (evt.data == "passive") {
    document.getElementById("active").style.display = 'none';
    document.getElementById("passive").style.display = 'block';
  }
  if (evt.data == "join1") {
    document.getElementById("game").style.display = 'block';
    document.getElementById("join").style.display = 'none';
  }
  if (evt.data == "join2") {
    awale.playerId = 1;

    // swap the board
    swapElements(document.getElementById("0"), document.getElementById("6"));
    swapElements(document.getElementById("1"), document.getElementById("7"));
    swapElements(document.getElementById("2"), document.getElementById("8"));
    swapElements(document.getElementById("3"), document.getElementById("9"));
    swapElements(document.getElementById("4"), document.getElementById("10"));
    swapElements(document.getElementById("5"), document.getElementById("11"));
    swapElements(document.getElementById("score0"), document.getElementById("score1"));
    document.getElementById("game").style.display = 'block';
  }
  if (evt.data.startsWith("Game:")) {
    awale.gameId = evt.data.substring(5);
    awale.playerId = 0;
    document.getElementById("join").style.display = 'block';
    document.getElementById("join-url").innerHTML = "http://" + document.location.host + "/game/" + awale.gameId;
  }
}

// from : http://stackoverflow.com/questions/10716986/swap-2-html-elements-and-preserve-event-listeners-on-them
function swapElements(obj1, obj2) {
  // create marker element and insert it where obj1 is
  var temp = document.createElement("div");
  obj1.parentNode.insertBefore(temp, obj1);

  // move obj1 to right before obj2
  obj2.parentNode.insertBefore(obj1, obj2);

  // move obj2 to right before where obj1 used to be
  temp.parentNode.insertBefore(obj2, temp);

  // remove temporary marker node
  temp.parentNode.removeChild(temp);
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