package net.arna.jcraft.api;

public enum MoveSelectionResult {
    /**
     * Continues current move evaluation
     */
    PASS,
    /**
     * Stops the evaluation and uses the move
     */
    USE,
    /**
     * Skips to the next move evaluation
     */
    STOP
}
