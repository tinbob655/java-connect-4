package player;

import model.state.GameState;
import model.Slot;
import model.state.Move;

import java.util.Objects;

public abstract class Player {

    private final Slot colour;

    public Player(Slot colour) {

        //player cannot play as "empty"
        if (colour == Slot.EMPTY) {
            throw new IllegalArgumentException("Cannot play as 'empty'");
        }
        else {
            this.colour = colour;
        }
    }

    public Slot getColour() {
        return this.colour;
    }


    //abstract methods
    public abstract Move turn(GameState state);


    //equality
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Player p)) {
            return false;
        }
        return p.getColour() == this.colour;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.colour);
    }
}
