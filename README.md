# Multiplayer Chess in Java

This is a dependency-free, server-authoritative chess game for Java 17:

- `ChessServer` pairs two TCP clients into a game.
- `ChessClient` opens a Swing chessboard and sends moves to the server.
- The server validates turns, ownership, legal moves, check, checkmate, stalemate, castling, en passant, and promotion.

The project follows an object-oriented methodology. The domain model, responsibilities,
relationships, design decisions, use cases, and message sequence are documented in
[`docs/OBJECT_ORIENTED_METHODOLOGY.md`](docs/OBJECT_ORIENTED_METHODOLOGY.md).
The two-person work split is documented in
[`docs/TASK_ASSIGNMENTS.md`](docs/TASK_ASSIGNMENTS.md).

## Setup guide

### Prerequisites

- Java Development Kit (JDK) 17 or newer.
- Maven 3.8 or newer is optional; direct `javac` compilation is also supported.

Check the installed tools:

```bash
java -version
javac -version
mvn -version
```

### Option A: Maven

From the project root, compile the project:

```bash
mvn compile
```

In separate terminals, start the server and two clients:

```bash
mvn exec:java -Dexec.mainClass=chess.ChessServer
mvn exec:java -Dexec.mainClass=chess.ChessClient
mvn exec:java -Dexec.mainClass=chess.ChessClient
```

### Option B: `javac`

From the project root:

```bash
rm -rf out
mkdir out
javac -d out $(find src/main/java -name '*.java')
```

Start the server:

```bash
java -cp out chess.ChessServer
```

Start two clients in separate terminals:

```bash
java -cp out chess.ChessClient
```

```bash
java -cp out chess.ChessClient
```

### Use a custom port

The default server port is `8080`. To use port `9000`:

```bash
java -cp out chess.ChessServer 9000
java -cp out chess.ChessClient localhost 9000
```

Run the client command twice. Replace `localhost` with the server's IP address
when connecting from another computer, and allow the TCP port through the firewall.

### How to play

1. Start the server.
2. Connect the first client; it becomes White.
3. Connect the second client; it becomes Black.
4. Click a source square and then a destination square.
5. The server validates the move and broadcasts the updated board to both clients.
6. Pawns promote automatically to queens.

### Troubleshooting

- **`mvn: command not found`**: use the direct `javac` setup or install Maven.
- **Connection refused**: start the server first and check the host and port.
- **Port already in use**: start the server on another port, such as `9000`.
- **Only one player is connected**: the first client waits until a second client joins.

## Object-oriented structure

`ChessGame` encapsulates the board and chess rules, `Move` models a move value,
`Piece` models the chess piece types, `ChessServer` coordinates network sessions,
`GameSession` owns one match, `Player` encapsulates a connection, and `ChessClient`
owns the Swing user interface. The server depends on the `ChessGame` abstraction
through its public operations rather than exposing the board directly.
