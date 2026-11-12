package chess;

/**
 * Base exception for expected chess-domain failures.
 */
class ChessException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    ChessException(String message) {
        super(message);
    }
}

final class InvalidMoveException extends ChessException {
    private static final long serialVersionUID = 1L;

    InvalidMoveException(String message) {
        super(message);
    }
}

record Move(int fromRow, int fromCol, int toRow, int toCol, char promotion) {
    static Move parse(String value) {
        if (value == null || !value.matches("[a-h][1-8][a-h][1-8][qrbn]?")) {
            throw new InvalidMoveException("Use coordinate notation such as e2e4 or e7e8q");
        }
        return new Move(8 - (value.charAt(1) - '0'), value.charAt(0) - 'a',
                8 - (value.charAt(3) - '0'), value.charAt(2) - 'a',
                value.length() == 5 ? value.charAt(4) : '\0');
    }

    String notation() {
        return "" + (char) ('a' + fromCol) + (8 - fromRow)
                + (char) ('a' + toCol) + (8 - toRow)
                + (promotion == '\0' ? "" : String.valueOf(promotion));
    }
}

enum Piece {
    EMPTY('.', false), WHITE_PAWN('P', true), WHITE_KNIGHT('N', true), WHITE_BISHOP('B', true),
    WHITE_ROOK('R', true), WHITE_QUEEN('Q', true), WHITE_KING('K', true),
    BLACK_PAWN('p', false), BLACK_KNIGHT('n', false), BLACK_BISHOP('b', false),
    BLACK_ROOK('r', false), BLACK_QUEEN('q', false), BLACK_KING('k', false);

    final char symbol;
    final boolean white;

    Piece(char symbol, boolean white) {
        this.symbol = symbol;
        this.white = white;
    }

    boolean isEmpty() {
        return this == EMPTY;
    }

    boolean isWhite() {
        return !isEmpty() && white;
    }

    boolean isBlack() {
        return !isEmpty() && !white;
    }

    boolean belongsTo(boolean side) {
        return !isEmpty() && white == side;
    }

    Piece promoted(boolean side) {
        return side ? WHITE_QUEEN : BLACK_QUEEN;
    }

    static Piece fromSymbol(char symbol) {
        for (Piece piece : values()) {
            if (piece.symbol == symbol) return piece;
        }
        throw new IllegalArgumentException("Unknown piece: " + symbol);
    }
}

final class CoordinateMoveParser implements MoveParser {
    @Override
    public Move parse(String notation) {
        return Move.parse(notation);
    }
}

final class ChoicePromotionPolicy implements PromotionPolicy {
    @Override
    public Piece promote(Piece pawn, int destinationRow, char choice) {
        if ((pawn == Piece.WHITE_PAWN && destinationRow != 0)
                || (pawn == Piece.BLACK_PAWN && destinationRow != 7)) return pawn;
        if (choice == '\0') throw new InvalidMoveException("Choose a piece for pawn promotion: q, r, b, or n");
        boolean white = pawn == Piece.WHITE_PAWN;
        return switch (choice) {
            case 'q' -> white ? Piece.WHITE_QUEEN : Piece.BLACK_QUEEN;
            case 'r' -> white ? Piece.WHITE_ROOK : Piece.BLACK_ROOK;
            case 'b' -> white ? Piece.WHITE_BISHOP : Piece.BLACK_BISHOP;
            case 'n' -> white ? Piece.WHITE_KNIGHT : Piece.BLACK_KNIGHT;
            default -> throw new InvalidMoveException("Promotion must be q, r, b, or n");
        };
    }
}
