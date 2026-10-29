# Two-Person Project Task List

This task list divides the multiplayer chess project between two people while
keeping the interfaces between their work explicit.

## Person 1 — Chess domain and server

| ID | Task | Deliverable | Depends on |
|---|---|---|---|
| 1.1 | Maintain the object model | `Piece`, `Move`, and `ChessGame` domain classes | None |
| 1.2 | Implement chess rules | Legal movement, turn validation, check, checkmate, stalemate, castling, en passant, and promotion | 1.1 |
| 1.3 | Maintain the server | Player pairing, game sessions, TCP protocol, and synchronized state changes | 1.1 |
| 1.4 | Add server-side tests | Tests for legal moves, illegal moves, special moves, and game-ending states | 1.2 |
| 1.5 | Add future persistence | Optional `GameRepository` abstraction and saved game history | 1.3 |

### Person 1 integration contract

The server exposes these messages to the client:

- `COLOR|WHITE` or `COLOR|BLACK`
- `STATE|<turn>|<status>|<64-piece board>`
- `MOVE|<source><destination>`
- `OK|<move>`
- `ERROR|<reason>`
- `WAITING|<message>` and `INFO|<message>`

## Person 2 — Client and documentation

| ID | Task | Deliverable | Depends on |
|---|---|---|---|
| 2.1 | Maintain the Swing client | Board rendering, piece selection, and move submission | 1.3 |
| 2.2 | Improve client UX | Highlight legal moves, show player color, display errors, and prevent invalid selections | 2.1 |
| 2.3 | Add connection handling | Reconnection message, opponent disconnect state, and clean shutdown | 2.1 |
| 2.4 | Maintain documentation | README setup guide, object-oriented methodology, protocol notes, and diagrams | 1.3, 2.1 |
| 2.5 | Add client/integration checks | Verify two clients receive the same board after each accepted move | 1.3, 2.1 |

## Shared tasks

Both people should participate in:

1. Agreeing on protocol changes before implementation.
2. Reviewing each other's changes.
3. Running compilation and integration checks before merging.
4. Testing invalid moves and disconnect behavior.
5. Demonstrating a complete two-player game.

## Suggested order

1. Person 1 completes the domain model and server protocol.
2. Person 2 connects the client to the agreed protocol.
3. Person 1 adds chess-rule tests while Person 2 improves the UI.
4. Both people run the two-client integration test.
5. Both people review the methodology and setup documentation.
