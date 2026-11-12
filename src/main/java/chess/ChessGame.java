package chess;

final class ChessGame implements GameState, MoveValidator {
  private final Piece[][] board = new Piece[8][8];
  private boolean whiteToMove = true;
  private boolean whiteKingMoved;
  private boolean blackKingMoved;
  private boolean whiteKingRookMoved;
  private boolean whiteQueenRookMoved;
  private boolean blackKingRookMoved;
  private boolean blackQueenRookMoved;
  private int enPassantRow = -1;
  private int enPassantCol = -1;
  private String status = "White to move";
  private final MoveParser moveParser;
  private final PromotionPolicy promotionPolicy;

  ChessGame() {
    this(new CoordinateMoveParser(), new ChoicePromotionPolicy());
  }

  ChessGame(MoveParser moveParser, PromotionPolicy promotionPolicy) {
    this.moveParser = moveParser;
    this.promotionPolicy = promotionPolicy;
    reset();
  }

  @Override
  public synchronized String snapshot() {
    StringBuilder result = new StringBuilder("STATE|").append(whiteToMove ? "WHITE" : "BLACK")
        .append('|').append(status).append('|');
    for (Piece[] row : board) {
      for (Piece piece : row)
        result.append(piece.symbol);
    }
    return result.toString();
  }

  @Override
  public synchronized String move(String notation) {
    final Move move;
    try {
      move = moveParser.parse(notation);
    } catch (ChessException error) {
      return "ERROR|" + error.getMessage();
    }
    if (isGameOver())
      return "ERROR|The game is over";
    if (!inside(move.fromRow(), move.fromCol()) || !inside(move.toRow(), move.toCol())) {
      return "ERROR|That square is outside the board";
    }
    Piece moving = board[move.fromRow()][move.fromCol()];
    if (!moving.belongsTo(whiteToMove))
      return "ERROR|It is not your turn or that is not your piece";
    if (requiresPromotion(moving, move.toRow()) && move.promotion() == '\0') {
      return "ERROR|Choose a piece for pawn promotion";
    }
    if (!requiresPromotion(moving, move.toRow()) && move.promotion() != '\0') {
      return "ERROR|Only a pawn reaching the last rank can be promoted";
    }
    if (!isLegal(move, whiteToMove))
      return "ERROR|That move is illegal";

    apply(move, moving);
    whiteToMove = !whiteToMove;
    boolean check = isInCheck(whiteToMove);
    boolean hasMove = hasLegalMove(whiteToMove);
    if (!hasMove && check)
      status = (whiteToMove ? "Black" : "White") + " wins by checkmate";
    else if (!hasMove)
      status = "Draw by stalemate";
    else
      status = (whiteToMove ? "White" : "Black") + (check ? " is in check" : " to move");
    return "OK|" + move.notation();
  }

  private void reset() {
    for (int row = 0; row < 8; row++)
      for (int col = 0; col < 8; col++)
        board[row][col] = Piece.EMPTY;
    Piece[] back = { Piece.WHITE_ROOK, Piece.WHITE_KNIGHT, Piece.WHITE_BISHOP, Piece.WHITE_QUEEN,
        Piece.WHITE_KING, Piece.WHITE_BISHOP, Piece.WHITE_KNIGHT, Piece.WHITE_ROOK };
    Piece[] blackBack = { Piece.BLACK_ROOK, Piece.BLACK_KNIGHT, Piece.BLACK_BISHOP, Piece.BLACK_QUEEN,
        Piece.BLACK_KING, Piece.BLACK_BISHOP, Piece.BLACK_KNIGHT, Piece.BLACK_ROOK };
    for (int col = 0; col < 8; col++) {
      board[7][col] = back[col];
      board[6][col] = Piece.WHITE_PAWN;
      board[0][col] = blackBack[col];
      board[1][col] = Piece.BLACK_PAWN;
    }
  }

  @Override
  public boolean isLegal(Move move, boolean side) {
    Piece piece = board[move.fromRow()][move.fromCol()];
    Piece target = board[move.toRow()][move.toCol()];
    if (!target.isEmpty() && target.belongsTo(side))
      return false;
    if (target == (side ? Piece.BLACK_KING : Piece.WHITE_KING))
      return false;
    int dr = move.toRow() - move.fromRow(), dc = move.toCol() - move.fromCol();
    boolean legalShape = switch (piece) {
      case WHITE_PAWN, BLACK_PAWN -> pawnMove(move, side, dr, dc);
      case WHITE_KNIGHT, BLACK_KNIGHT -> Math.abs(dr) * Math.abs(dc) == 2;
      case WHITE_BISHOP, BLACK_BISHOP -> Math.abs(dr) == Math.abs(dc) && clearPath(move);
      case WHITE_ROOK, BLACK_ROOK -> (dr == 0 || dc == 0) && clearPath(move);
      case WHITE_QUEEN, BLACK_QUEEN -> (dr == 0 || dc == 0 || Math.abs(dr) == Math.abs(dc)) && clearPath(move);
      case WHITE_KING, BLACK_KING -> kingMove(move, side, dr, dc);
      default -> false;
    };
    if (!legalShape)
      return false;
    Piece[][] copy = copyBoard();
    applyToBoard(move, piece);
    boolean safe = !isInCheck(side);
    restoreBoard(copy);
    return safe;
  }

  private boolean pawnMove(Move move, boolean side, int dr, int dc) {
    int direction = side ? -1 : 1;
    Piece target = board[move.toRow()][move.toCol()];
    if (dc == 0 && target.isEmpty() && dr == direction)
      return true;
    if (dc == 0 && target.isEmpty() && dr == 2 * direction
        && move.fromRow() == (side ? 6 : 1)
        && board[move.fromRow() + direction][move.fromCol()].isEmpty())
      return true;
    return Math.abs(dc) == 1 && dr == direction
        && (!target.isEmpty() || (move.toRow() == enPassantRow && move.toCol() == enPassantCol));
  }

  private boolean kingMove(Move move, boolean side, int dr, int dc) {
    if (Math.max(Math.abs(dr), Math.abs(dc)) == 1)
      return true;
    if (dr != 0 || Math.abs(dc) != 2 || isInCheck(side))
      return false;
    boolean kingSide = dc > 0;
    int row = side ? 7 : 0;
    int rookCol = kingSide ? 7 : 0;
    if (board[row][rookCol] != (side ? Piece.WHITE_ROOK : Piece.BLACK_ROOK)
        || (side ? (kingSide ? whiteKingRookMoved : whiteQueenRookMoved)
            : (kingSide ? blackKingRookMoved : blackQueenRookMoved)))
      return false;
    int step = kingSide ? 1 : -1;
    for (int col = move.fromCol() + step; col != rookCol; col += step)
      if (!board[row][col].isEmpty())
        return false;
    return !isSquareAttacked(row, move.fromCol() + step, !side)
        && !isSquareAttacked(row, move.fromCol() + 2 * step, !side);
  }

  private void apply(Move move, Piece piece) {
    applyToBoard(move, piece);
    int row = move.fromRow(), col = move.fromCol();
    if (piece == Piece.WHITE_KING)
      whiteKingMoved = true;
    if (piece == Piece.BLACK_KING)
      blackKingMoved = true;
    if (piece == Piece.WHITE_ROOK && row == 7 && col == 0)
      whiteQueenRookMoved = true;
    if (piece == Piece.WHITE_ROOK && row == 7 && col == 7)
      whiteKingRookMoved = true;
    if (piece == Piece.BLACK_ROOK && row == 0 && col == 0)
      blackQueenRookMoved = true;
    if (piece == Piece.BLACK_ROOK && row == 0 && col == 7)
      blackKingRookMoved = true;
    enPassantRow = Math.abs(move.toRow() - row) == 2 ? (move.toRow() + row) / 2 : -1;
    enPassantCol = enPassantRow >= 0 ? col : -1;
    if (piece == Piece.WHITE_KING && Math.abs(move.toCol() - col) == 2) {
      moveRookForCastle(7, move.toCol() > col);
    } else if (piece == Piece.BLACK_KING && Math.abs(move.toCol() - col) == 2) {
      moveRookForCastle(0, move.toCol() > col);
    }
  }

  private void applyToBoard(Move move, Piece piece) {
    boolean side = piece.isWhite();
    if ((piece == Piece.WHITE_PAWN || piece == Piece.BLACK_PAWN)
        && move.toCol() != move.fromCol() && board[move.toRow()][move.toCol()].isEmpty()) {
      board[move.fromRow()][move.toCol()] = Piece.EMPTY;
    }
    board[move.fromRow()][move.fromCol()] = Piece.EMPTY;
    char choice = move.promotion() == '\0' ? 'q' : move.promotion();
    board[move.toRow()][move.toCol()] = promotionPolicy.promote(piece, move.toRow(), choice);
  }

  private void moveRookForCastle(int row, boolean kingSide) {
    int from = kingSide ? 7 : 0, to = kingSide ? 5 : 3;
    board[row][to] = board[row][from];
    board[row][from] = Piece.EMPTY;
  }

  private boolean clearPath(Move move) {
    int rowStep = Integer.compare(move.toRow(), move.fromRow());
    int colStep = Integer.compare(move.toCol(), move.fromCol());
    int row = move.fromRow() + rowStep, col = move.fromCol() + colStep;
    while (row != move.toRow() || col != move.toCol()) {
      if (!board[row][col].isEmpty())
        return false;
      row += rowStep;
      col += colStep;
    }
    return true;
  }

  private boolean isInCheck(boolean side) {
    for (int row = 0; row < 8; row++)
      for (int col = 0; col < 8; col++) {
        if (board[row][col] == (side ? Piece.WHITE_KING : Piece.BLACK_KING)) {
          return isSquareAttacked(row, col, !side);
        }
      }
    return true;
  }

  private boolean isSquareAttacked(int row, int col, boolean byWhite) {
    for (int fromRow = 0; fromRow < 8; fromRow++)
      for (int fromCol = 0; fromCol < 8; fromCol++) {
        Piece piece = board[fromRow][fromCol];
        if (!piece.belongsTo(byWhite))
          continue;
        int dr = row - fromRow, dc = col - fromCol;
        boolean attacked = switch (piece) {
          case WHITE_PAWN, BLACK_PAWN -> dr == (byWhite ? -1 : 1) && Math.abs(dc) == 1;
          case WHITE_KNIGHT, BLACK_KNIGHT -> Math.abs(dr) * Math.abs(dc) == 2;
          case WHITE_BISHOP, BLACK_BISHOP -> Math.abs(dr) == Math.abs(dc)
              && clearPath(new Move(fromRow, fromCol, row, col, '\0'));
          case WHITE_ROOK, BLACK_ROOK -> (dr == 0 || dc == 0)
              && clearPath(new Move(fromRow, fromCol, row, col, '\0'));
          case WHITE_QUEEN, BLACK_QUEEN -> (dr == 0 || dc == 0 || Math.abs(dr) == Math.abs(dc))
              && clearPath(new Move(fromRow, fromCol, row, col, '\0'));
          case WHITE_KING, BLACK_KING -> Math.max(Math.abs(dr), Math.abs(dc)) == 1;
          default -> false;
        };
        if (attacked)
          return true;
      }
    return false;
  }

  private boolean hasLegalMove(boolean side) {
    for (int fromRow = 0; fromRow < 8; fromRow++)
      for (int fromCol = 0; fromCol < 8; fromCol++)
        for (int toRow = 0; toRow < 8; toRow++)
          for (int toCol = 0; toCol < 8; toCol++)
            if (board[fromRow][fromCol].belongsTo(side)
                && isLegal(new Move(fromRow, fromCol, toRow, toCol, '\0'), side))
              return true;
    return false;
  }

  private boolean isGameOver() {
    return status.contains("wins") || status.startsWith("Draw");
  }

  private static boolean requiresPromotion(Piece piece, int destinationRow) {
    return (piece == Piece.WHITE_PAWN && destinationRow == 0)
        || (piece == Piece.BLACK_PAWN && destinationRow == 7);
  }

  private Piece[][] copyBoard() {
    Piece[][] copy = new Piece[8][8];
    for (int row = 0; row < 8; row++)
      copy[row] = board[row].clone();
    return copy;
  }

  private void restoreBoard(Piece[][] copy) {
    for (int row = 0; row < 8; row++)
      System.arraycopy(copy[row], 0, board[row], 0, 8);
  }

  private static boolean inside(int row, int col) {
    return row >= 0 && row < 8 && col >= 0 && col < 8;
  }
}
