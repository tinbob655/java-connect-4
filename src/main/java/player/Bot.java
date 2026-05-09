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

    private static final int MINIMAX_DEPTH = 12;
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
        this.globalAlpha.set(Integer.MIN_VALUE);

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
        int best = Integer.MIN_VALUE;
        Move bestMove = sortedMoves.peek();

        //queue jobs on the thread pool
        List<Future<Pair<Move, Integer>>> tasks = new ArrayList<>();
        while (!sortedMoves.isEmpty()) {

            Move move = sortedMoves.poll();

            //create a job
            tasks.add(this.threadPool.submit(() -> {

                //pretend we did the move
                Board newBoard = state.board().duplicate();
                newBoard.setSlotAt(move.target(), move.commencedBy().getColour());
                GameState newState = new GameState(newBoard, opponent);

                int score = this.minimax(newState, MINIMAX_DEPTH -1, false, Integer.MIN_VALUE, Integer.MAX_VALUE);
                this.globalAlpha.updateAndGet(current -> Math.max(current, score));
                return new Pair<>(move, score);
            }));
        }

        //execute the futures
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

        //need to keep original alpha and beta for later
        int originalAlpha = alpha;
        int originalBeta  = beta;

        if (depth == 0 || state.getWinner().isPresent()) {
            return this.score(state);
        }

        //we may have already computed this state as a sufficient depth
        TranspositionTableRecord ttEntry = this.transpositionTable.get(state);
        if (ttEntry != null && ttEntry.depth() >= depth) {
            switch (ttEntry.flag()) {

                //we have already computed this state
                case PERFECT -> {
                    return ttEntry.score;
                }

                case LOWER -> alpha = Math.max(alpha, ttEntry.score());
                case UPPER -> beta  = Math.min(beta,  ttEntry.score());
            }

            //might be able to prune
            if (alpha >= beta) {
                return ttEntry.score();
            }
        }

        Set<Move> legalMoves = state.getLegalMoves();
        int best;

        if (maximising) {
            best = Integer.MIN_VALUE;
            for (Move move : legalMoves) {

                //pretend we did the move
                Board newBoard = state.board().duplicate();
                newBoard.setSlotAt(move.target(), move.commencedBy().getColour());
                GameState newState = new GameState(newBoard, this.opponent);

                int rank = this.minimax(newState, depth - 1, false, alpha, beta);
                best  = Math.max(best, rank);
                alpha = Math.max(alpha, best);

                //prune
                if (beta <= alpha) {
                    break;
                }
            }
        }
        else {
            best = Integer.MAX_VALUE;
            for (Move move : legalMoves) {

                //pretend we did the move
                Board newBoard = state.board().duplicate();
                newBoard.setSlotAt(move.target(), move.commencedBy().getColour());
                GameState newState = new GameState(newBoard, this);

                int rank = this.minimax(newState, depth - 1, true, alpha, beta);
                best = Math.min(best, rank);
                beta = Math.min(beta, best);

                //prune
                if (beta <= alpha) {
                    break;
                }
            }
        }


        TranspositionTableFlag flag;

        //if we pruned due to alpha this pass
        if (best <= originalAlpha) {
            flag = TranspositionTableFlag.UPPER;
        }

        //if we pruned due to beta this pass
        else if (best >= originalBeta) {
            flag = TranspositionTableFlag.LOWER;
        }

        //if we fully computed this pass
        else {
            flag = TranspositionTableFlag.PERFECT;
        }

        this.transpositionTable.put(state, new TranspositionTableRecord(best, depth, flag));
        return best;
    }

    private int score(GameState state) {

        Board board = state.board();
        Slot bot = this.getColour();
        Slot opponent = this.opponent.getColour();

        //game might be over
        Optional<Slot> winner = state.getWinner();
        if (winner.isPresent()) {
            return winner.get() == bot ? 100000 : -100000;
        }

        //no moves available
        if (state.getLegalMoves().isEmpty()) return 0;

        //otherwise give the board a score
        int res = 0;
        int centreCol = board.width() / 2;

        for (int row = 0; row < board.height(); row++) {
            for (int col = 0; col < board.width(); col++) {

                Slot slot = board.getSlotAt(new Coordinate(col, row));
                int centreBonus = (board.width() / 2) - Math.abs(centreCol - col);

                //reward playing in the middle
                if (slot == bot) {
                    res += centreBonus * 3;
                }
                else if (slot == opponent) {
                    res -= centreBonus * 3;
                }
            }
        }

        //scan for tokens in a row in all directions
        int[][] directions = {{0,1}, {1,0}, {1,1}, {1,-1}};
        for (int row = 0; row < board.height(); row++) {
            for (int col = 0; col < board.width(); col++) {
                for (int[] dir : directions) {

                    List<Slot> window = new ArrayList<>();
                    boolean outOfBounds = false;
                    for (int i = 0; i < 4; i++) {
                        int r = row + dir[0] * i;
                        int c = col + dir[1] * i;

                        //stop if we are out of bounds
                        if (r < 0 || r >= board.height() || c < 0 || c >= board.width()) {
                            outOfBounds = true;
                            break;
                        }
                        window.add(board.getSlotAt(new Coordinate(c, r)));
                    }

                    //stop if we reach the end of the board
                    if (outOfBounds) continue;

                    //work if there is space after our window
                    Coordinate before = new Coordinate(col - dir[1], row - dir[0]);
                    Coordinate after = new Coordinate(col + dir[1] * 4, row + dir[0] * 4);

                    boolean leftOpen = (isInBounds(before, board)) && (board.getSlotAt(before) == Slot.EMPTY);
                    boolean rightOpen = (isInBounds(after, board)) && (board.getSlotAt(after) == Slot.EMPTY);

                    //score the window
                    res += scoreWindow(window, bot, opponent, leftOpen, rightOpen);
                }
            }
        }

        return res;
    }

    //works out if a position is in the bounds of the board
    private boolean isInBounds(Coordinate c, Board board) {
        return (c.column() >= 0 && c.column() < board.width()) && (c.row() >= 0 && c.row() < board.height());
    }

    private int scoreWindow(List<Slot> window, Slot bot, Slot opponent, boolean leftOpen, boolean rightOpen) {

        int botCount = Collections.frequency(window, bot);
        int oppCount = Collections.frequency(window, opponent);

        //no one has an advantage if both players have tokens
        if (botCount > 0 && oppCount > 0) return 0;

        boolean openEnded = leftOpen && rightOpen;

        //win condition
        if (botCount == 4) return 100000;
        if (oppCount == 4) return -100000;

        //3 in a row with both ends open : one end open
        if (botCount == 3) return openEnded ? 150 : 50;
        if (oppCount == 3) return openEnded ? -150 : -50;

        //2 in a row with both ends open : one end open
        if (botCount == 2) return openEnded ? 20 : 10;
        if (oppCount == 2) return openEnded ? -20 : -10;

        //only one piece
        if (botCount == 1) return 1;
        if (oppCount == 1) return -1;

        return 0;
    }


    //transposition table stuff
    private final Map<GameState, TranspositionTableRecord> transpositionTable = new ConcurrentHashMap<>();
    private enum TranspositionTableFlag {PERFECT, LOWER, UPPER}

    private record TranspositionTableRecord(int score, int depth, TranspositionTableFlag flag) {};
}
