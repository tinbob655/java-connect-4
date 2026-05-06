package ui;

import model.state.GameState;

public final class UIProxy implements UI_API {

    private final UI ui;

    public UIProxy() {
        this.ui = UI.getInstance();
    }

    @Override
    public int getColumnInput() {
        return this.ui.getColumnInput();
    }

    @Override
    public void sendMessage(String message) {
        this.ui.sendMessage(message);
    }

    @Override
    public void displayState(GameState state) {
        this.ui.displayState(state);
    }
}
