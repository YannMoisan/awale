// immutable representation of a game in progress
function Game() {
    // Given the following board :
    // f e d b c a <- player 1
    // A B C D E F <- player 0
    // It is represented by an array : board = [A, B, C, D, E, F, a, b, c, d, e, f]
    this.board= [4,4,4,4,4,4,4,4,4,4,4,4];

    this.curPlayer = 0;
    this.scores = [0,0];
}

Game.prototype = {
    clone: function() {
        var newGame = new Game();
        newGame.board = this.board.slice(0);
        newGame.scores = this.scores.slice(0);
        return newGame;
    },
    update: function(house, value, phase) {
        this.board[house] = value;
        this.turn[phase].push({house: house, value: value});
    },
    noMoveLetOpponentPlay: function() {
        var ret = true;
        for (var i=0; i<12; i++) {
          if (this.curPlayer == this.owner(i) && this.canOpponentPlayAfterMove(i)) {
              ret = false;
          }
        }
        return ret;
    },
    canOpponentPlayAfterMove: function(i) {
        if (this.opponentsAllEmpty()) {
            var newGame = this.play(i);
            return !newGame.allEmpty(newGame.curPlayer);
        } else {
            return true;
        }
    },
    grandSlam: function(i) {
        if (!this.opponentsAllEmpty()) {
            var newGame = this.play(i);
            return newGame.allEmpty(newGame.curPlayer);
        } else {
            return false;
        }
    },
    playWithValid: function(i) {
        if (this.valid(i)) { 
            return this.play(i);
        }
    },
    play: function(i) { 
        var newGame = this.clone();
        newGame.turn = { sowing: [], capturing: [] };

        var nb = this.board[i];
        newGame.update(i, 0, "sowing");

        for (j = 1; j <= nb; j++) { 
            cur = (i + j) % 12;
            if (cur != i) {
                newGame.update(cur, newGame.board[cur] + 1, "sowing");
            } else {
                nb += 1; // jump over the initial house
            }
        }

        var tmp = newGame.board.slice(0);

        while (this.owner(cur) != this.curPlayer && [2, 3].indexOf(newGame.board[cur]) != -1) {
            newGame.scores[this.curPlayer] += newGame.board[cur];
            newGame.update(cur, 0, "capturing");
            cur -= 1;
        }

        // is this grandSlam
        // A grand slam is capturing all of an opponent's seeds in one turn.
        // Such a move is legal, but no capture results. International competitions often follow this rule.
        if (newGame.opponentsAllEmpty()) {
            // rollback
            newGame.scores[this.curPlayer] = this.scores[this.curPlayer];
            newGame.turn.capturing=[];
            newGame.board = tmp;
        }

        newGame.curPlayer = (this.curPlayer + 1) % 2;
        return newGame;
    },
    playNoMoveLetOpponentPlay: function() {
        var newGame = this.clone();
        newGame.turn = { sowing: [], capturing: [] };

        //var nb = this.board[i];
        //newGame.update(i, 0, "sowing");

        var PLAYER_HOUSES = [ [0, 1, 2, 3, 4, 5], [6, 7, 8, 9, 10, 11]];

        var curPlayerHouses = PLAYER_HOUSES[this.curPlayer];

        var curp = this.curPlayer;
        curPlayerHouses.forEach(function(i) {
            newGame.scores[curp] += newGame.board[i];
            newGame.update(i, 0, "capturing");
        });

        return newGame;
    },
    owner: function(i) {
        var PLAYER_HOUSES = [ [0, 1, 2, 3, 4, 5], [6, 7, 8, 9, 10, 11]];
        return ((PLAYER_HOUSES[0].indexOf(+i) == -1) ? 1 : 0);
    },
    valid: function(i) {
        this.noMoveLetOpponentPlay();
        // if there is a winner, all moves are invalid
        return this.winner() === undefined && this.curPlayer === this.owner(i) && this.board[i] !== 0 && this.canOpponentPlayAfterMove(i);
    },
    winner: function() {
        // need to conf babel for that, doesn't work with Jasmine test
        // var i = this.scores.findIndex(function(e) { return e > 24; });
        var i = -1;
        if (this.scores[0] > 24) {
            i=0;
        } else if (this.scores[1] > 24) {
            i=1;
        }
        return (i !== -1) ? i : undefined;
    },
    allEmpty: function(player) {
        var PLAYER_HOUSES = [ [0, 1, 2, 3, 4, 5], [6, 7, 8, 9, 10, 11]];
        var self = this;
        return PLAYER_HOUSES[player].map(function(i) {return self.board[i];}).every(function(e) { return e === 0; });
    },
    opponentsAllEmpty: function() { return this.allEmpty((this.curPlayer + 1) % 2); },

};
