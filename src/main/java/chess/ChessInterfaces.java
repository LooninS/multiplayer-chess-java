package chess;

/**
 * Abstraction for a playable game state.
 */
interface GameState {
    String snapshot();

    String move(String notation);
}

/**
 * Strategy abstraction for validating a move against a game state.
 */
interface MoveValidator {
    boolean isLegal(Move move, boolean whiteSide);
}

/**
 * Small communication abstraction used by a game session.
 */
interface MessageSender {
    void send(String message);
}

interface MoveParser {
    Move parse(String notation);
}

/**
 * Extension point for promotion rules.
 */
interface PromotionPolicy {
    Piece promote(Piece pawn, int destinationRow, char choice);
}
