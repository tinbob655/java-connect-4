package model;

import java.util.Arrays;

public class Board {

    private static final int GRID_COLUMNS = 7;
    private static final int GRID_ROWS = 6;
    private final Slot[][] grid;

    public Board() {
        this.grid = new Slot[GRID_ROWS][GRID_COLUMNS];
        for (int row = 0; row < GRID_ROWS; row++) {
            Arrays.fill(this.grid[row], Slot.EMPTY);
        }
    }

    //overload for creating a populated board
    Board(Slot[][] startingGrid) {

        //starting grid must have correct dimensions
        if (startingGrid.length == GRID_ROWS  && Arrays.stream(startingGrid).allMatch(col -> col.length == GRID_COLUMNS)) {

            //valid
            this.grid = startingGrid;
        }
        else throw new IllegalArgumentException("Starting grid must be " + GRID_ROWS + " x " + GRID_COLUMNS);
    }

    public int width() {
        return GRID_COLUMNS;
    }
    public int height() {
        return GRID_ROWS;
    }

    public Slot getSlotAt(Coordinate c) {
        return this.grid[c.row()][c.column()];
    }

    public void setSlotAt(Coordinate c, Slot s) {
        this.grid[c.row()][c.column()] = s;
    }

    public int getTopIndex(int column) {

        //iterate down the column until a token is found
        for (int row = 0; row < GRID_ROWS; row++) {
            if (this.grid[row][column] != Slot.EMPTY) {
                return row -1;
            }
        }

        //nothing was in this row
        return GRID_ROWS -1;
    }

    public Board duplicate() {
        Slot[][] gridCopy = new Slot[GRID_ROWS][GRID_COLUMNS];
        for (int row = 0; row < GRID_ROWS; row++) {
            gridCopy[row] = Arrays.copyOf(this.grid[row], GRID_COLUMNS);
        }
        return new Board(gridCopy);
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();

        // Print the board rows
        for (int i = 0; i < GRID_ROWS; i++) {

            //left border
            res.append("| ");

            //whole row
            for (int j = 0; j < GRID_COLUMNS; j++) {
                res.append(switch (this.grid[i][j]) {
                    case RED -> "R ";
                    case YELLOW -> "Y ";
                    default -> ". ";
                });
            }

            //right border
            res.append("|");

            //numbers to indicate row
            res.append(i);

            res.append('\n');
        }

        //create the bottom border
        res.append("+-");
        res.append("--".repeat(GRID_COLUMNS));
        res.append("+");

        //add column numbers for reference
        res.append('\n');
        res.append("  ");
        for (int j = 0; j < GRID_COLUMNS; j++) {
            res.append((j)).append(" ");
        }

        return res.toString();
    }

    //equality
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Board b)) {
            return false;
        }

        //check each cell of each board is equal
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int column = 0; column < GRID_COLUMNS; column++) {

                Coordinate c = new Coordinate(column, row);
                if (this.grid[row][column] != b.getSlotAt(c)) {

                    //missmatch
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(this.grid);
    }
}
