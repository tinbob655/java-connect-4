package player;

import engine.GameEngine;
import model.Coordinate;
import model.Slot;
import model.state.GameState;
import model.state.Move;
import ui.UIProxy;
import java.util.Set;

public final class Human extends Player {

    private static final UIProxy uiHandler = new UIProxy();

    public Human(Slot colour) {
        super(colour);
    }

    @Override
    public Move turn(GameState state) {

        Set<Move> legalMoves = state.getLegalMoves();
        uiHandler.sendMessage("Please choose which column to place a token in (0 - 6):");

        int chosenColumn = uiHandler.getColumnInput();
        int topIndex = GameEngine.getInstance().getState().board().getTopIndex(chosenColumn);
        Coordinate target = new Coordinate(chosenColumn, topIndex);
        Move move = new Move(this, target);

        //make sure the target is valid
        if (!legalMoves.contains(move)) {

            //invalid
            uiHandler.sendMessage("Invalid move, please try again...");
            return this.turn(state);
        }
        return move;
    }
}
