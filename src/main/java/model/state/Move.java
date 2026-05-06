package model.state;

import model.Coordinate;
import player.Player;

public record Move(Player commencedBy, Coordinate target) {}
