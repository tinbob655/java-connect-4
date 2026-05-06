package ui;

import model.state.GameState;

public interface UI_API {

    //user input
    int getColumnInput();

    //user notification
    void sendMessage(String message);

    //game updates
    void displayState(GameState state);
}
