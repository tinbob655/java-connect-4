package engine;

import model.state.GameState;
import model.state.Move;
import player.Player;

public interface EngineAPI {

    void addPlayer(Player p);

    GameState getState();

    void turn();
    void advance(Move move);
}
