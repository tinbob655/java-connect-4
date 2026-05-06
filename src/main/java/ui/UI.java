package ui;

import engine.GameEngine;
import model.state.GameState;

import java.util.Scanner;

//SINGLETON
public final class UI implements UI_API {

    private static final Scanner scanner = new Scanner(System.in);

    //singleton methods
    private static UI instance;
    static UI getInstance() {

        if (instance == null) {
            instance = new UI();
        }
        return instance;
    }

    private UI() {
        super();
    }

    @Override
    public int getColumnInput() {

        while (true) {

            //try to get an int
            try {
                int input = scanner.nextInt();
                if (input < 0 || input > GameEngine.getInstance().getState().board().width()) {
                    throw new IllegalArgumentException();
                }

                //valid column received
                return input;
            }
            catch (Exception e) {
                scanner.nextLine();
                this.sendMessage("Invalid column, please try again...");
            }
        }
    }

    @Override
    public void sendMessage(String message) {
        System.out.println(message);
    }

    @Override
    public void displayState(GameState state) {
        this.sendMessage(state.board().toString());
    }
}
