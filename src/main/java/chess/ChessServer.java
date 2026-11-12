package chess;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public final class ChessServer {
    private static final int DEFAULT_PORT = 8080;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final BlockingQueue<Player> waiting = new LinkedBlockingQueue<>();

    public static void main(String[] args) throws IOException {
        int port = args.length == 0 ? DEFAULT_PORT : Integer.parseInt(args[0]);
        new ChessServer().start(port);
    }

    private void start(int port) throws IOException {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Chess server listening on port " + port);
            while (!server.isClosed()) {
                Socket socket = server.accept();
                pool.submit(() -> addPlayer(socket));
            }
        }
    }

    private void addPlayer(Socket socket) {
        try {
            Player player = new Player(socket);
            synchronized (waiting) {
                Player opponent = waiting.poll();
                if (opponent == null) {
                    waiting.offer(player);
                    player.send("WAITING|Waiting for an opponent...");
                } else {
                    new GameSession(opponent, player).start();
                }
            }
        } catch (IOException error) {
            close(socket);
        }
    }

    private static void close(Closeable closeable) {
        try { closeable.close(); } catch (IOException ignored) { }
    }

    private final class GameSession {
        private final Player white, black;
        private final MessageSender[] participants;
        private final GameState game;

        GameSession(Player white, Player black) {
            this(white, black, new ChessGame());
        }

        GameSession(Player white, Player black, GameState game) {
            this.white = white; this.black = black;
            this.participants = new MessageSender[] {white, black};
            this.game = game;
        }

        void start() {
            white.send("COLOR|WHITE"); black.send("COLOR|BLACK");
            broadcast(game.snapshot());
            pool.submit(() -> readMoves(white, true));
            pool.submit(() -> readMoves(black, false));
        }

        private void readMoves(Player player, boolean whiteSide) {
            try {
                String line;
                while ((line = player.in.readLine()) != null) {
                    if (!line.startsWith("MOVE|")) continue;
                    String result;
                    synchronized (game) {
                        boolean turnMatches = game.snapshot().startsWith("STATE|" + (whiteSide ? "WHITE" : "BLACK"));
                        result = turnMatches ? game.move(line.substring(5)) : "ERROR|It is not your turn";
                    }
                    player.send(result);
                    if (result.startsWith("OK|")) broadcast(game.snapshot());
                }
            } catch (IOException ignored) {
                broadcast("INFO|" + (whiteSide ? "White" : "Black") + " disconnected");
            } finally {
                close(player.socket);
            }
        }

        private void broadcast(String message) {
            for (MessageSender participant : participants) participant.send(message);
        }
    }

    private static final class Player implements MessageSender {
        final Socket socket;
        final BufferedReader in;
        final PrintWriter out;

        Player(Socket socket) throws IOException {
            this.socket = socket;
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        }

        @Override
        public synchronized void send(String message) {
            out.println(message);
        }
    }
}
