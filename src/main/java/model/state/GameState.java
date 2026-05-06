package model.state;

import model.Board;
import model.Coordinate;
import model.Slot;
import player.Player;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class GameState {

    private final Board board;
    private final Player currentPlayer;
    private Optional<Slot> winner;

    public GameState(Board board, Player currentPlayer) {

        this.board = board;
        this.currentPlayer = currentPlayer;
        this.winner = Optional.of(Slot.EMPTY);
    }

    public Board board() {
        return this.board;
    }

    public Set<Move> getLegalMoves() {
        Set<Move> res = new HashSet<>();

        int columnCount = this.board.width();
        for (int column = 0; column < columnCount; column++) {

            //we can only add to the top of each column
            int firstFreeCell = board.getTopIndex(column);

            //the column may be full
            if (firstFreeCell == -1) {
                continue;
            }

            //if the column is not full then add it to the set of moves
            Coordinate target = new Coordinate(column, firstFreeCell);
            res.add(new Move(currentPlayer, target));
        }

        return res;
    }

    public Optional<Slot> getWinner() {

        //if we have not calculated a winner yet for this slot then calculate one
        if (this.winner.equals(Optional.of(Slot.EMPTY))) {
            this.winner = this.calculateWinner();
        }

        return this.winner;
    }

    private Optional<Slot> calculateWinner() {

        //only need to scan in +ve directions
        int[][] directions = {{0,1}, {1,0}, {1,1}, {1,-1}};

        for (int row = 0; row < board.height(); row++) {
            for (int col = 0; col < board.width(); col++) {

                Slot start = board.getSlotAt(new Coordinate(col, row));

                //stop doing work if we see an empty slot
                if (start == Slot.EMPTY) {
                    continue;
                }

                for (int[] dir : directions) {
                    boolean fourInARow = true;
                    for (int i = 1; i < 4; i++) {
                        int r = row + dir[0] * i;
                        int c = col + dir[1] * i;

                        //check for out-of-bounds
                        if (r < 0 || r >= board.height() || c < 0 || c >= board.width()) {
                            fourInARow = false;
                            break;
                        }

                        //we found a different colour so stop
                        if (board.getSlotAt(new Coordinate(c, r)) != start) {
                            fourInARow = false;
                            break;
                        }
                    }

                    if (fourInARow) {
                        return Optional.of(start);
                    }
                }
            }
        }
        return Optional.empty();
    }
}
