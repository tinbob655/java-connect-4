package player;

import model.Board;
import model.Coordinate;
import model.Pair;
import model.Slot;
import model.state.GameState;
import model.state.Move;
import ui.UIProxy;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Bot extends Player {

    private static final int MINIMAX_DEPTH = 11;
    private final ExecutorService threadPool = Executors.newWorkStealingPool();
    private static final UIProxy uiHandler = new UIProxy();
    private Player opponent;
    private final AtomicInteger globalAlpha = new AtomicInteger(Integer.MIN_VALUE);

    public Bot(Slot colour) {
        super(colour);
    }

    public void grantOpponent(Player p) {
        this.opponent = p;
    }

    @Override
    public Move turn(GameState state) {

        if (this.opponent == null) {
            throw new IllegalStateException("A bot requires an opponent to function");
        }

        uiHandler.sendMessage("Thinking...");

        //get a set of sorted moves for PVS
        PriorityQueue<Move> sortedMoves = new PriorityQueue<>(
                Comparator.comparingInt(move -> {

                    //pretend we did the move
                    Board newBoard = state.board().duplicate();
                    newBoard.setSlotAt(move.target(), move.commencedBy().getColour());
                    GameState newState = new GameState(newBoard, this.opponent);

                    //negative to get best first
                    return -this.score(newState);
                })
        );
        sortedMoves.addAll(state.getLegalMoves());

        //queue jobs on the thread pool
        List<Future<Pair<Move, Integer>>> tasks = new ArrayList<>();
        for (Move move : sortedMoves) {

            //create a job
            tasks.add(this.threadPool.submit(() -> {

                //pretend we did the move
                Board newBoard = state.board().duplicate();
                newBoard.setSlotAt(move.target(), move.commencedBy().getColour());
                GameState newState = new GameState(newBoard, opponent);

                int score = this.minimax(newState, MINIMAX_DEPTH -1, false, this.globalAlpha.get(), Integer.MAX_VALUE);
                this.globalAlpha.updateAndGet(current -> Math.max(current, score));
                return new Pair<>(move, score);
            }));
        }

        //execute the futures
        int best = Integer.MIN_VALUE;
        Move bestMove = sortedMoves.peek();
        try {
            for (Future<Pair<Move, Integer>> t : tasks) {
                Pair<Move, Integer> result = t.get();

                //if we found the new best move
                if (result.right() > best) {
                    bestMove = result.left();
                    best = result.right();
                }
            }
        }
        catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        return bestMove;
    }

    private int minimax(GameState state, int depth, boolean maximising, int alpha, int beta) {

        //we might be done
        if (depth == 0 || state.getWinner().isPresent()) {
            return this.score(state);
        }

        //otherwise do minimax
        Set<Move> legalMoves = state.getLegalMoves();
        if (maximising) {

            //we want the best possible score
            int best = Integer.MIN_VALUE;
            for (Move move : legalMoves) {

                //pretend we did the move
                Board newBoard = state.board().duplicate();
                newBoard.setSlotAt(move.target(), move.commencedBy().getColour());
                GameState newState = new GameState(newBoard, this.opponent);

                int rank = this.minimax(newState, depth -1, false, alpha, beta);
                best = Math.max(best, rank);
                alpha = Math.max(alpha, best);

                //pruning
                if (beta <= alpha) {
                    break;
                }
            }
            return best;
        }

        else {

            //we want the worst possible score
            int worst = Integer.MAX_VALUE;
            for (Move move : legalMoves) {

                //pretend we did the move
                Board newBoard = state.board().duplicate();
                newBoard.setSlotAt(move.target(), move.commencedBy().getColour());
                GameState newState = new GameState(newBoard, this);

                int rank = this.minimax(newState, depth -1, true, alpha, beta);
                worst = Math.min(worst, rank);
                beta = Math.min(beta, worst);

                //pruning
                if (beta <= alpha) {
                    break;
                }
            }
            return worst;
        }
    }

    private int score(GameState state) {

        Board board = state.board();
        Slot bot = this.getColour();
        Slot opponent = Slot.RED;
        int res = 0;

        //only need to scan in 4 directions. Avoids counting stuff twice
        int[][] directions = {{0,1}, {1,0}, {1,1}, {1,-1}};

        for (int row = 0; row < board.height(); row++) {
            for (int col = 0; col < board.width(); col++) {
                for (int[] dir : directions) {

                    //get 4 cells in this direction
                    List<Slot> window = new ArrayList<>();
                    boolean outOfBounds = false;
                    for (int i = 0; i < 4; i++) {
                        int r = row + dir[0] * i;
                        int c = col + dir[1] * i;

                        //check for out of bounds
                        if (r < 0 || r >= board.height() || c < 0 || c >= board.width()) {
                            outOfBounds = true;
                            break;
                        }
                        window.add(board.getSlotAt(new Coordinate(c, r)));
                    }

                    //if we are out of bounds then stop doing work
                    if (outOfBounds) {
                        continue;
                    }

                    res += scoreWindow(window, bot, opponent);
                }
            }
        }

        return res;
    }

    private int scoreWindow(List<Slot> window, Slot bot, Slot opponent) {
        int botCount = Collections.frequency(window, bot);
        int oppCount = Collections.frequency(window, opponent);

        //if we both have pieces then that is no-one's advantage: worthless
        if (botCount > 0 && oppCount > 0) return 0;

        //winning / loosing
        if (botCount == 4) return 10000;
        if (oppCount == 4) return -10000;

        //someone has 3 in a row
        if (botCount == 3) return 50;
        if (oppCount == 3) return -50;

        //someone has two in a row
        if (botCount == 2) return 10;
        if (oppCount == 2) return -10;

        return 0;
    }
}
