package net.arna.jcraft.api;

import net.arna.jcraft.api.attack.moves.AbstractMove;

public record MoveUsage(int timestamp, AbstractMove<?, ?> move) {
}
