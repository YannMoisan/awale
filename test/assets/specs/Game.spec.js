describe("A game", function() {
  it("should change player after a move", function() {
    var game = new Game();
    expect(game.play(0).curPlayer).toBe(1);
  });

  it("should update the board after a move", function() {
    var game = new Game();

    var newGame = game.play(0);

    expect(newGame.board).toEqual([0,5,5,5,5,4,4,4,4,4,4,4]);
    expect(newGame.scores).toEqual([0,0]);
    expect(game.board).toEqual([4,4,4,4,4,4,4,4,4,4,4,4]);
  });

  it("should update the board after a move with capture", function() {
    var game = new Game();
    game.board = [0,0,0,0,0,4,1,0,1,1,0,0];

    var newGame = game.play(5);

    expect(newGame.board).toEqual([0,0,0,0,0,0,2,1,0,0,0,0]);
    expect(newGame.scores).toEqual([4,0]);
  });

  it("should not capture seeds in our houses", function() {
    var game = new Game();
    game.board = [1,1,0,0,0,0,0,0,0,0,0,0];

    var newGame = game.play(0);

    expect(newGame.board).toEqual([0,2,0,0,0,0,0,0,0,0,0,0]);
    expect(newGame.scores).toEqual([0,0]);
  });

  it("should determine the owner of a house", function() {
    var game = new Game();
    expect(game.owner(0)).toBe(0);
    expect(game.owner(1)).toBe(0);
    expect(game.owner(2)).toBe(0);
    expect(game.owner(3)).toBe(0);
    expect(game.owner(4)).toBe(0);
    expect(game.owner(5)).toBe(0);
    expect(game.owner(6)).toBe(1);
    expect(game.owner(7)).toBe(1);
    expect(game.owner(8)).toBe(1);
    expect(game.owner(9)).toBe(1);
    expect(game.owner(10)).toBe(1);
    expect(game.owner(11)).toBe(1);
  });

  it("should determine if a move is valid", function() {
    var game = new Game();
    expect(game.valid(0)).toBe(true);
    expect(game.valid(1)).toBe(true);
    expect(game.valid(2)).toBe(true);
    expect(game.valid(3)).toBe(true);
    expect(game.valid(4)).toBe(true);
    expect(game.valid(5)).toBe(true);
    expect(game.valid(6)).toBe(false);
    expect(game.valid(7)).toBe(false);
    expect(game.valid(8)).toBe(false);
    expect(game.valid(9)).toBe(false);
    expect(game.valid(10)).toBe(false);
    expect(game.valid(11)).toBe(false);
  });

  it("should determine that a move from an empty house is invalid", function() {
    var game = new Game();
    game.board = [0,0,0,0,0,0,0,0,0,0,0,0];
    expect(game.valid(0)).toBe(false);
  });

  it("should return no winner at the beggining of the game", function() {
      var game = new Game();
      expect(game.winner()).toBe(undefined);
  });

  it("should return a winner if a player has a score greater than 24", function() {
      var game = new Game();

      game.scores = [25, 0];
      expect(game.winner()).toBe(0);

      game.scores = [0, 25];
      expect(game.winner()).toBe(1);
  });

  it("should determine if opponent's houses are all empty", function() {
    var game = new Game();
    expect(game.opponentsAllEmpty()).toBe(false);

    game.board = [4,4,4,4,4,4,0,0,0,0,0,0];
    expect(game.opponentsAllEmpty()).toBe(true);
  });

  it("should let the opponent play", function() {
    var game = new Game();
    game.board = [6,1,1,1,1,1,0,0,0,0,0,0];
    expect(game.canOpponentPlay(0)).toBe(true);
    expect(game.canOpponentPlay(1)).toBe(false);
    expect(game.canOpponentPlay(2)).toBe(false);
    expect(game.canOpponentPlay(3)).toBe(false);
    expect(game.canOpponentPlay(4)).toBe(false);
    expect(game.canOpponentPlay(5)).toBe(true);
  });
});

