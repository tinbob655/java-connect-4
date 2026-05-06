package ui;

import model.Coordinate;
import model.Slot;
import model.state.GameState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.SynchronousQueue;

// SINGLETON
public final class UI implements UI_API {


    //singleton stuff
    private static UI instance;
    static UI getInstance() {
        if (instance == null) {
            instance = new UI();
        }
        return instance;
    }


    private BoardPanel boardPanel;
    private JLabel messageLabel;
    private final SynchronousQueue<Integer> inputQueue = new SynchronousQueue<>();

    private UI() {
        try {
            SwingUtilities.invokeAndWait(this::buildUI);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialise GUI", e);
        }
    }

    private void buildUI() {

        JFrame frame = new JFrame("Connect Four");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(0, 8));
        frame.getContentPane().setBackground(new Color(28, 28, 36));

        //a label at the top
        messageLabel = new JLabel("Welcome to Connect Four!", SwingConstants.CENTER);
        messageLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        messageLabel.setForeground(Color.WHITE);
        messageLabel.setBorder(BorderFactory.createEmptyBorder(16, 16, 4, 16));

        //the actual board
        boardPanel = new BoardPanel();

        //numbers at bottom
        JPanel columnNumbers = buildColumnNumberStrip();

        //make frame
        frame.add(messageLabel, BorderLayout.NORTH);
        frame.add(boardPanel, BorderLayout.CENTER);
        frame.add(columnNumbers, BorderLayout.SOUTH);
        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    //puts numbers under each column
    private JPanel buildColumnNumberStrip() {
        JPanel strip = new JPanel(new GridLayout(1, BoardPanel.COLS));
        strip.setBackground(new Color(28, 28, 36));
        strip.setBorder(BorderFactory.createEmptyBorder(2, BoardPanel.PADDING, 12, BoardPanel.PADDING));

        for (int c = 0; c < BoardPanel.COLS; c++) {
            JLabel lbl = new JLabel(String.valueOf(c), SwingConstants.CENTER);
            lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
            lbl.setForeground(new Color(160, 160, 180));
            strip.add(lbl);
        }
        return strip;
    }

    @Override
    public int getColumnInput() {

        //block to wait for an input
        try {
            return inputQueue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        }
    }

    @Override
    public void sendMessage(String message) {
        SwingUtilities.invokeLater(() -> messageLabel.setText(message));
    }

    @Override
    public void displayState(GameState state) {
        SwingUtilities.invokeLater(() -> {
            boardPanel.setState(state);
            boardPanel.repaint();
        });
    }

    private class BoardPanel extends JPanel {

        static final int COLS = 7;
        static final int ROWS = 6;
        static final int CELL = 88;
        static final int PADDING = 14;

        private GameState state;

        //-1 indicates no hover
        private int hoveredColumn = -1;

        //colours for UI
        private static final Color BOARD_BLUE  = new Color(30,  100, 215);
        private static final Color BOARD_BLUE_H = new Color(60,  130, 245);
        private static final Color EMPTY_CELL = new Color(18,   18,  32);
        private static final Color RED_TOKEN = new Color(220,  45,  45);
        private static final Color YELLOW_TOKEN = new Color(245, 205,  20);
        private static final Color SHADOW = new Color(  0,   0,   0, 70);
        private static final Color SHINE = new Color(255, 255, 255, 55);

        BoardPanel() {
            setPreferredSize(new Dimension(
                    COLS * CELL + 2 * PADDING,
                    ROWS * CELL + 2 * PADDING));
            setBackground(BOARD_BLUE);

            // --- Click: publish the column to the game thread ---
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int col = xToColumn(e.getX());
                    if (col >= 0) {
                        try {
                            inputQueue.put(col);   // unblocks getColumnInput()
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            });

            // --- Mouse motion: highlight the column under the cursor ---
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int col = xToColumn(e.getX());
                    if (col != hoveredColumn) {
                        hoveredColumn = col;
                        repaint();
                    }
                }
            });

            // --- Clear highlight when mouse leaves the panel ---
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseExited(MouseEvent e) {
                    hoveredColumn = -1;
                    repaint();
                }
            });
        }

        //converts click coordinate to column coordinate
        private int xToColumn(int x) {
            int col = (x - PADDING) / CELL;
            return (col >= 0 && col < COLS) ? col : -1;
        }

        void setState(GameState state) {
            this.state = state;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            drawColumnHighlight(g2);
            drawCells(g2);
        }

        //highlight what the mouse is hovering over
        private void drawColumnHighlight(Graphics2D g2) {
            if (hoveredColumn < 0) return;
            g2.setColor(BOARD_BLUE_H);
            g2.fillRect(PADDING + hoveredColumn * CELL, 0, CELL, getHeight());
        }

        //draws each cell of the grid
        private void drawCells(Graphics2D g2) {
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLS; col++) {

                    Slot slot = (state == null)
                            ? Slot.EMPTY
                            : state.board().getSlotAt(new Coordinate(col, row));

                    int margin   = 7;
                    int x        = PADDING + col * CELL + margin;
                    int y        = PADDING + row * CELL + margin;
                    int diameter = CELL - margin * 2;

                    // Drop shadow (slightly offset circle drawn first)
                    g2.setColor(SHADOW);
                    g2.fillOval(x + 3, y + 3, diameter, diameter);

                    // Main token or empty hole
                    g2.setColor(tokenColour(slot));
                    g2.fillOval(x, y, diameter, diameter);

                    // Specular shine on placed tokens to give a 3-D feel
                    if (slot != Slot.EMPTY) {
                        g2.setColor(SHINE);
                        g2.fillOval(
                                x + diameter / 4,
                                y + diameter / 6,
                                diameter / 3,
                                diameter / 4);
                    }
                }
            }
        }

        private Color tokenColour(Slot slot) {
            return switch (slot) {
                case RED    -> RED_TOKEN;
                case YELLOW -> YELLOW_TOKEN;
                default     -> EMPTY_CELL;
            };
        }
    }
}