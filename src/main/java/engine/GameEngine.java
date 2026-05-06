package engine;

import model.Board;
import model.Slot;
import model.state.GameState;
import model.state.Move;
import player.Player;

import java.util.ArrayList;
import java.util.List;

//SINGLETON
public final class GameEngine implements EngineAPI {

    private static final int BOARD_COLUMNS = 7;
    private static final int BOARD_ROWS = 6;

    private static volatile GameEngine instance;
    private static final Board board = new Board();
    private GameState gameState;
    private final List<Player> players = new ArrayList<>();
    private int turnIndex;

    public GameEngine() {
        this.turnIndex = 0;
    }

    public static GameEngine getInstance() {

        if (instance == null) {

            //need to make an instance
            synchronized (GameEngine.class) {
                if (instance == null) {
                    instance = new GameEngine();
                }
            }
        }
        return instance;
    }

    @Override
    public GameState getState() {
        return this.gameState;
    }

    @Override
    public void addPlayer(Player p) {
        this.players.add(p);
        this.gameState = this.recomputeState();
    }

    @Override
    public void turn() {

        //does a turn in the game

        //get the move we want to do
        Player currentPlayer = this.players.get(this.turnIndex);
        Move mv = currentPlayer.turn(this.gameState);

        //do the move
        this.advance(mv);
    }

    //takes a move and executes it
    @Override
    public void advance(Move move) {

        this.validateMove(move);

        //run the move
        board.setSlotAt(move.target(), move.commencedBy().getColour());
        this.turnIndex = (this.turnIndex + 1) % this.players.size();

        this.gameState = this.recomputeState();
    }

    private void validateMove(Move move) throws IllegalArgumentException {

        //make sure the move is on the board
        if (move.target().column() < 0 || move.target().column() >= BOARD_COLUMNS) throw new IllegalArgumentException("Move out of column range");
        if (move.target().row() < 0 || move.target().row() >= BOARD_ROWS) throw new IllegalArgumentException("Move out of row range");

        //make sure the move is being done by the right player
        Player currentPlayer = this.players.get(this.turnIndex);
        if (!move.commencedBy().equals(currentPlayer)) {
            throw new IllegalArgumentException("Move is not being played by the correct player");
        }

        //a player cannot place a token in a spot which already has a token in
        if (board.getSlotAt(move.target()) != Slot.EMPTY) {
            throw new IllegalArgumentException("A token cannot be placed at a slot which already contains a token");
        }
    }

    private GameState recomputeState() {
        Player currentPlayer = this.players.get(this.turnIndex);
        return new GameState(board, currentPlayer);
    }
}
