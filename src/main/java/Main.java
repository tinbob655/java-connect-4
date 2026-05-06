import engine.GameEngine;
import model.Slot;
import player.Bot;
import player.Human;
import player.Player;
import ui.UIProxy;

public class Main {

    private static final UIProxy uiHandler = new UIProxy();

    public static void main(String[] args) {

        //setup
        doSetup();

        //gameplay
        doGameLoop();

        //resolution
        resolveGame();
    }

    private static void doSetup() {
        GameEngine engine = GameEngine.getInstance();

        Player human = new Human(Slot.RED);
        engine.addPlayer(human);

        Bot bot = new Bot(Slot.YELLOW);
        bot.grantOpponent(human);
        engine.addPlayer(bot);
    }

    private static void doGameLoop() {

        GameEngine engine = GameEngine.getInstance();
        while (engine.getState().getWinner().isEmpty()) {
            engine.turn();
        }
    }

    private static void resolveGame() {

        Slot winningColour = GameEngine.getInstance().getState().getWinner().orElseThrow();

        //since the human always plays red
        String winMsg = (winningColour == Slot.RED ? "You " : "The Bot ") + "won!";

        uiHandler.sendMessage(winMsg);
    }
}
