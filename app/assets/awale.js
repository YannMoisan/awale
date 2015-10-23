//http://en.wikipedia.org/wiki/Oware

// Board 11 10 9  8  7  6
//
// O  O  O  O  O  O O  O  O  O  O  O
//
// 0  1  2  3  4  5

//var transitionEvent = whichTransitionEvent();

awale.view = {
    display: function(selector, cond) {
        document.querySelector(selector).style.display = cond ? "block" : "none";
    },
    text: function(selector, value) {
        if (value !== undefined)
            document.querySelector(selector).innerHTML = value;
    },
    formatTime: function(nbSeconds) {
      var min = Math.floor(nbSeconds/60);
      var sec = nbSeconds % 60;
      var formattedMin = min.toLocaleString('fr-FR', { minimumIntegerDigits: 2 });
      var formattedSec = sec.toLocaleString('fr-FR', { minimumIntegerDigits: 2 });
      return formattedMin + ":" + formattedSec;
    },
    refresh: function () {
        this.text("#nb-players", awale.metrics && awale.metrics.nbPlayers);
        this.text("#nb-games",   awale.metrics && awale.metrics.nbGames);

        this.display("#start", awale.status === "begin");
        this.display("#invitation", awale.status === "await");
        this.display("#game", awale.status === "started" || awale.status === "disconnected");
        this.display("#disconnected", awale.status === "disconnected");
        this.display("#error", awale.error);

        this.display("#active",  (!awale.game.winner()) && awale.game.curPlayer === awale.playerId);
        this.display("#passive", (!awale.game.winner()) && awale.game.curPlayer !== awale.playerId);

        this.text("#time0", awale.time && this.formatTime(awale.time.time0));
        this.text("#time1", awale.time && this.formatTime(awale.time.time1));

        this.text("#score0", awale.game.scores[0]);
        this.text("#score1", awale.game.scores[1]);

        this.text("#error", awale.error);
        this.text("#message", awale.game.winner() && (awale.game.winner() === awale.playerId ? "You Win !" : "You lose !"));

        document.getElementById("join-url").value = "http://" + document.location.host + "/game/" + awale.gameId;

        //console.log(this);
        this.houses.forEach(function (i) {
            i.classList.remove("over");
        });//fix style issue

        if (awale.game.winner() !== undefined) {
            this.houses.forEach(function (element) {
                element.classList.add("inactive");
            });
        } else {
                //awale.view.intervalID = window.setInterval(function() {
                //    if (awale.game.curPlayer === 0) {
                //        awale.time.time0 += 1;
                //        document.getElementById("time0").innerHTML = awale.view.formatTime(awale.time.time0);
                //    } else {
                //        awale.time.time0 += 1;
                //        document.getElementById("time1").innerHTML = awale.view.formatTime(awale.time.time0);
                //    }
                //}, 1000);
            this.houses.forEach(function (element) {
                if (awale.game.curPlayer != awale.game.owner(+element.id)) {
                    element.classList.add("inactive");
                } else {
                    element.classList.remove("inactive");
                }
            });
        }


        if (awale.game.winner() === undefined) {
            if (awale.game.noMoveLetOpponentPlay()) {
                console.log("noMoveLetOpponentPlay");
                var g = awale.game.playNoMoveLetOpponentPlay();
                awale.game = g;
                animateCapturing();
            }
        }

    },

    //http://davidwalsh.name/css-animation-callback
    animate: function (phase, clazz, callbackEnd) {
        var self=this;
        var a = awale.game.turn[phase];
        animationCount = a.length;

        // http://jslinterrors.com/dont-make-functions-within-a-loop/
        /*jshint -W083 */
        for (var i = 0; i < a.length; i++) {
            (function (j) {
                setTimeout(function () {
                    var x = a[j].house;
                    self.houses[x].classList.add(clazz);
                    self.houses[x].innerHTML = a[j].value;
                    if (self.transitionEvent) {
                        self.houses[x].addEventListener(self.transitionEvent, callbackEnd);
                    }
                }, 500 * i);
            })(i);
        }
    },

    animateSowing: function () {
        this.animate('sowing', 'sowing', this.animateSowingEnd);
    },

    animateCapturing: function () {
        this.animate('capturing', 'capturing', this.animateCapturingEnd);
    },

    animateEnd: function (event, clazz, callbackEnd, callbackFinished) {
        event.target.classList.remove(clazz); //remove class to allow further anim on this house
        event.target.removeEventListener(awale.view.transitionEvent, callbackEnd); //remove transitionEvent because there are two kinds of transiton on houses
        animationCount--;

        if (animationCount === 0) { // all animation finished ?
            callbackFinished();
        }
    },

    animateSowingEnd: function (event) {
        // call by a DOMElement. so this is a DOM element
        awale.view.animateEnd(event, "sowing", awale.view.animateSowingEnd, function () {
            if (awale.game.turn.capturing.length === 0) {
                awale.view.refresh();
            } else {
                setTimeout(function () {
                    awale.view.animateCapturing();
                }, 3000); // Why do i need to wait ?
            }
        });
    },

    animateCapturingEnd: function (event) {
        awale.view.animateEnd(event, "capturing", awale.view.animateCapturingEnd, function () {
            awale.view.refresh();
        });
    },

    /* From Modernizr */
    whichTransitionEvent: function () {
        var t;
        var el = document.createElement('fakeelement');
        var transitions = {
            'animation': 'animationend',
            'OAnimation': 'oAnimationEnd',
            'MozAnimation': 'animationend',
            'WebkitAnimation': 'webkitAnimationEnd'
        };

        for (t in transitions) {
            if (el.style[t] !== undefined) {
                return transitions[t];
            }
        }
    },
    //transitionEvent: awale.view.whichTransitionEvent(),

    // swap the board
    swapBoard : function() {
        this.swapElements(document.getElementById("0"), document.getElementById("6"));
        this.swapElements(document.getElementById("1"), document.getElementById("7"));
        this.swapElements(document.getElementById("2"), document.getElementById("8"));
        this.swapElements(document.getElementById("3"), document.getElementById("9"));
        this.swapElements(document.getElementById("4"), document.getElementById("10"));
        this.swapElements(document.getElementById("5"), document.getElementById("11"));
        this.swapElements(document.getElementById("score0"), document.getElementById("score1"));
        this.swapElements(document.getElementById("time0"), document.getElementById("time1"));
    },

    // from : http://stackoverflow.com/questions/10716986/swap-2-html-elements-and-preserve-event-listeners-on-them
    swapElements: function(obj1, obj2) {
        // create marker element and insert it where obj1 is
        var temp = document.createElement("div");
        obj1.parentNode.insertBefore(temp, obj1);

        // move obj1 to right before obj2
        obj2.parentNode.insertBefore(obj1, obj2);

        // move obj2 to right before where obj1 used to be
        temp.parentNode.insertBefore(obj2, temp);

        // remove temporary marker node
        temp.parentNode.removeChild(temp);
    },

    // intialize the view : register event handler, …
    init: function () {
        awale.game = new awale.Game();
        awale.status = "begin"; // begin, await, started

        this.houses = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11].map(function (i) {
            return document.getElementById(i);
        });
        this.transitionEvent = this.whichTransitionEvent();
        this.houses.forEach(function (e) {
            e.addEventListener("click", function () {
                var g = awale.game.playWithValid(+e.id);
                if (g) { // to test that g != undefined
                    awale.game = g;
                    awale.ctrl.doMove(+e.id);
                    awale.view.animateSowing();
                }
            });
            e.addEventListener("mouseover", function () {
                if (awale.game.valid(+e.id)) this.classList.add("over");
            });
            e.addEventListener("mouseout", function () {
                if (awale.game.valid(+e.id)) this.classList.remove("over");
            });
        });

        this.houses.forEach(function (element) {
            element.innerHTML = awale.game.board[+element.id];
        });

        // #invite
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


        this.refresh();

    }
};

