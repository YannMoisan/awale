//http://en.wikipedia.org/wiki/Oware

// Board 11 10 9  8  7  6
//
// O  O  O  O  O  O O  O  O  O  O  O
//
// 0  1  2  3  4  5

var transitionEvent = whichTransitionEvent();

awale.view = {};
awale.view.refresh = function() {
    console.log(this);
    awale.view.houses.forEach(function(i) {i.classList.remove("over");});//fix style issue

    if (awale.game.winner() !== undefined) {
        awale.view.houses.forEach(function(element) {
            element.classList.add("inactive");
        });
        document.getElementById("active").style.display = 'none';
        document.getElementById("passive").style.display = 'none';
        if (awale.game.winner() === awale.playerId) {
            document.getElementById("message").innerHTML = "You win !";
        } else {
            document.getElementById("message").innerHTML = "You lose !";
        }
    } else {
        awale.view.houses.forEach(function(element) {
            if (awale.game.curPlayer != awale.game.owner(+element.id)) {
                element.classList.add("inactive");
            } else {
                element.classList.remove("inactive");
            }
        });
    }

    document.getElementById("score0").innerHTML = awale.game.scores[0];
    document.getElementById("score1").innerHTML = awale.game.scores[1];

    if (awale.game.winner() === undefined) {
        if (awale.game.noMoveLetOpponentPlay()) {
            console.log("noMoveLetOpponentPlay");
            var g = awale.game.playNoMoveLetOpponentPlay();
            awale.game = g;
            animateCapturing();
        }
    }

};

//http://davidwalsh.name/css-animation-callback
awale.view.animate = function(phase, clazz, callbackEnd) {
    var a = awale.game.turn[phase];
    animationCount = a.length;

    // http://jslinterrors.com/dont-make-functions-within-a-loop/
    /*jshint -W083 */
    for (var i = 0; i < a.length; i++) {
        (function(j) { setTimeout(function() {
            var x = a[j].house;
            awale.view.houses[x].classList.add(clazz);
            awale.view.houses[x].innerHTML = a[j].value;
            if (transitionEvent) {
                awale.view.houses[x].addEventListener(transitionEvent, callbackEnd);
            }
        }, 500 * i);
        })(i);
    }
};

awale.view.animateSowing = function() { this.animate('sowing', 'sowing', this.animateSowingEnd); };

awale.view.animateCapturing = function() { this.animate('capturing', 'capturing', this.animateCapturingEnd); };

awale.view.animateEnd = function(event, clazz, callbackEnd, callbackFinished) {
    event.target.classList.remove(clazz); //remove class to allow further anim on this house
    event.target.removeEventListener(transitionEvent, callbackEnd); //remove transitionEvent because there are two kinds of transiton on houses
    animationCount--;

    if (animationCount === 0) { // all animation finished ?
        callbackFinished();
    }
};

awale.view.animateSowingEnd = function(event) {
    awale.view.animateEnd(event, "sowing", awale.view.animateSowingEnd, function () {
        if (awale.game.turn.capturing.length === 0) {
            awale.view.refresh();
        } else {
            setTimeout(function() { this.animateCapturing(); }, 3000); // Why do i need to wait ?
        }
    });
};

awale.view.animateCapturingEnd = function(event) {
    awale.view.animateEnd(event, "capturing", awale.view.animateCapturingEnd, function() {
        refresh(); 
    });
};

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

awale.view.init = function() {
    awale.game = new awale.Game();

    awale.view.houses = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11].map(function(i) { return document.getElementById(i);}); // implicit global
    awale.view.houses.forEach(function(e) {
        e.addEventListener("click", function() {
            var g = awale.game.playWithValid(+e.id);
            if (g) { // to test that g != undefined
                awale.game = g;
                doMove(+e.id);
                awale.view.animateSowing();
            }
        });
        e.addEventListener("mouseover", function() { if (awale.game.valid(+e.id)) this.classList.add("over"); });
        e.addEventListener("mouseout",  function() { if (awale.game.valid(+e.id)) this.classList.remove("over");});
    });

    awale.view.houses.forEach(function(element) {element.innerHTML=awale.game.board[+element.id];});

    awale.view.refresh();

};
