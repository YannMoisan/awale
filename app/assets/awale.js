//http://en.wikipedia.org/wiki/Oware

// Board 11 10 9  8  7  6
//
// O  O  O  O  O  O O  O  O  O  O  O
//
// 0  1  2  3  4  5

var transitionEvent = whichTransitionEvent();


function refresh() {
    houses.forEach(function(i) {i.classList.remove("over");});//fix style issue
    //houses.forEach(function(element) {element.innerHTML=board[+element.id];});
    houses.forEach(function(element) {
        if (game.winner() !== undefined) {
            element.classList.add("inactive");
            document.getElementById("active").style.display = 'none';
            document.getElementById("passive").style.display = 'none';
        } else {
            if (game.curPlayer != game.owner(+element.id)) {
                element.classList.add("inactive");
            } else {
                element.classList.remove("inactive");
            }
        }
    });

    //document.getElementById("player").innerHTML = player;
    document.getElementById("score0").innerHTML = game.scores[0];
    document.getElementById("score1").innerHTML = game.scores[1];

    if (game.winner() === undefined) {
        if (game.noMoveLetOpponentPlay()) {
            console.log("noMoveLetOpponentPlay");
            var g = game.playNoMoveLetOpponentPlay();
            game = g;
            animateCapturing();
        }
    }

}

//http://davidwalsh.name/css-animation-callback
function animate(phase, clazz, callbackEnd) {
    var a = game.turn[phase];
    animationCount = a.length;

    // http://jslinterrors.com/dont-make-functions-within-a-loop/
    /*jshint -W083 */
    for (var i = 0; i < a.length; i++) {
        (function(j) { setTimeout(function() {
            var x = a[j].house;
            houses[x].classList.add(clazz);
            houses[x].innerHTML = a[j].value;
            if (transitionEvent) {
                houses[x].addEventListener(transitionEvent, callbackEnd);
            }
        }, 500 * i);
        })(i);
    }
}

function animateSowing() { animate('sowing', 'sowing', animateSowingEnd); }

function animateCapturing() { animate('capturing', 'capturing', animateCapturingEnd); }

function animateEnd(event, clazz, callbackEnd, callbackFinished) {
    event.target.classList.remove(clazz); //remove class to allow further anim on this house
    event.target.removeEventListener(transitionEvent, callbackEnd); //remove transitionEvent because there are two kinds of transiton on houses
    animationCount--;

    if (animationCount === 0) { // all animation finished ?
        callbackFinished();
    }
}

function animateSowingEnd(event) {
    animateEnd(event, "sowing", animateSowingEnd, function () {
        if (game.turn.capturing.length === 0) {
            refresh();
        } else {
            setTimeout(function() { animateCapturing(); }, 3000); // Why do i need to wait ?
        }
    });
}

function animateCapturingEnd(event) {
    animateEnd(event, "capturing", animateCapturingEnd, function() { 
        refresh(); 
        if (game.winner() !== undefined) {
            if (game.winner() === awale.playerId -1) {
                document.getElementById("message").innerHTML = "You win !";
            } else {
                document.getElementById("message").innerHTML = "You lose !";
            }
        }
    });
}

/* From Modernizr */
function whichTransitionEvent(){
    var t;
    var el = document.createElement('fakeelement');
    var transitions = {
      'animation':'animationend',
      'OAnimation':'oAnimationEnd',
      'MozAnimation':'animationend',
      'WebkitAnimation':'webkitAnimationEnd'
    };

    for(t in transitions){
        if( el.style[t] !== undefined ){
            return transitions[t];
        }
    }
}

function init2() {
     game = new Game();

    houses = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11].map(function(i) { return document.getElementById(i);}); // implicit global
    houses.forEach(function(e) {
        e.addEventListener("click", function() {
            if (!e.classList.contains("inactive") && game.curPlayer == awale.playerId -1) {
                var g = game.playWithValid(+e.id);
                if (g) { // to test that g != undefined
                    game = g;
                    doMove(+e.id);
                    animateSowing();

                }
            }
        });
        e.addEventListener("mouseover", function() { if (game.curPlayer == awale.playerId -1 && game.valid(+e.id)) this.classList.add("over"); });
        e.addEventListener("mouseout",  function() { if (game.curPlayer == awale.playerId -1 && game.valid(+e.id)) this.classList.remove("over");});
    });

    houses.forEach(function(element) {element.innerHTML=game.board[+element.id];});

    //document.getElementById("new").addEventListener("click", function() {
    //  game = new Game();
    //    houses.forEach(function(element) {element.innerHTML=game.board[+element.id];});
    //  refresh();
    //});

    refresh();

}

//syntaxic sugar for closure, no return

