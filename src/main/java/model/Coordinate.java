package model;

import java.util.Objects;

public final class Coordinate {

    private final int x;
    private final int y;

    public Coordinate(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int column() {
        return this.x;
    }
    public int row() {
        return this.y;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Coordinate c)) {
            return false;
        }
        return c.x == this.x && c.y == this.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.x, this.y);
    }
}
