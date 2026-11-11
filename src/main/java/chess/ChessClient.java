package chess;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public final class ChessClient {
    private final JButton[][] squares = new JButton[8][8];
    private final JLabel status = new JLabel("Connecting...");
    private Socket socket;
    private PrintWriter out;
    private boolean white;
    private boolean whiteToMove;
    private String position = "";
    private int selectedRow = -1, selectedCol = -1;

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 8080;
        SwingUtilities.invokeLater(() -> new ChessClient().connect(host, port));
    }

    private void connect(String host, int port) {
        JFrame frame = new JFrame("Multiplayer Chess");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        JPanel board = new JPanel(new GridLayout(8, 8));
        for (int row = 0; row < 8; row++) for (int col = 0; col < 8; col++) {
            JButton button = new JButton();
            button.setFont(new Font("SansSerif", Font.PLAIN, 36));
            button.setOpaque(true);
            button.setBorderPainted(false);
            final int r = row, c = col;
            button.addActionListener(event -> clickSquare(r, c));
            squares[row][col] = button;
            board.add(button);
        }
        status.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        frame.add(board, BorderLayout.CENTER); frame.add(status, BorderLayout.SOUTH);
        frame.setSize(640, 680); frame.setLocationByPlatform(true); frame.setVisible(true);

        try {
            socket = new Socket(host, port);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Thread listener = new Thread(() -> listen(in), "chess-client-listener");
            listener.setDaemon(true); listener.start();
        } catch (IOException error) {
            status.setText("Unable to connect: " + error.getMessage());
        }
    }

    private void listen(BufferedReader in) {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("COLOR|")) {
                    white = message.endsWith("WHITE");
                    setStatus("You are " + (white ? "White" : "Black") + " | Waiting for the board...");
                } else if (message.startsWith("STATE|")) {
                    updateBoard(message);
                } else if (message.startsWith("ERROR|")) {
                    setStatus(message.substring(6));
                } else if (message.startsWith("WAITING|") || message.startsWith("INFO|")) {
                    setStatus(message.substring(message.indexOf('|') + 1));
                }
            }
        } catch (IOException error) {
            setStatus("Disconnected from server");
        }
    }

    private void clickSquare(int row, int col) {
        if (selectedRow < 0) {
            selectedRow = row; selectedCol = col;
            squares[row][col].setBorder(BorderFactory.createLineBorder(Color.RED, 3));
        } else {
            String move = "" + (char) ('a' + selectedCol) + (8 - selectedRow)
                    + (char) ('a' + col) + (8 - row);
            if (isPromotion(selectedRow, selectedCol, row)) {
                Character choice = choosePromotion();
                if (choice == null) {
                    clearSelection();
                    return;
                }
                move += choice;
            }
            out.println("MOVE|" + move);
            clearSelection();
        }
    }

    private void updateBoard(String message) {
        String[] parts = message.split("\\|", 4);
        whiteToMove = parts[1].equals("WHITE");
        String position = parts[3];
        SwingUtilities.invokeLater(() -> {
            this.position = position;
            for (int row = 0; row < 8; row++) for (int col = 0; col < 8; col++) {
                JButton square = squares[row][col];
                square.setText(pieceGlyph(position.charAt(row * 8 + col)));
                square.setBackground((row + col) % 2 == 0 ? new Color(240, 217, 181) : new Color(181, 136, 99));
                square.setForeground(Character.isUpperCase(position.charAt(row * 8 + col))
                        ? Color.WHITE : Color.BLACK);
            }
            updateTurnStatus(parts[2]);
        });
    }

    private boolean isPromotion(int fromRow, int fromCol, int toRow) {
        if (position.length() != 64) return false;
        char piece = position.charAt(fromRow * 8 + fromCol);
        return (white && piece == 'P' || !white && piece == 'p')
                && (white && toRow == 0 || !white && toRow == 7);
    }

    private Character choosePromotion() {
        Object choice = JOptionPane.showInputDialog(null, "Promote pawn to:",
                "Pawn promotion", JOptionPane.QUESTION_MESSAGE, null,
                new Object[] {"Queen", "Rook", "Bishop", "Knight"}, "Queen");
        if (choice == null) return null;
        return switch (choice.toString()) {
            case "Queen" -> 'q';
            case "Rook" -> 'r';
            case "Bishop" -> 'b';
            case "Knight" -> 'n';
            default -> null;
        };
    }

    private void clearSelection() {
        if (selectedRow >= 0) squares[selectedRow][selectedCol].setBorderPainted(false);
        selectedRow = selectedCol = -1;
    }

    private void updateTurnStatus(String gameStatus) {
        boolean yourTurn = white == whiteToMove;
        status.setText("You are " + (white ? "White" : "Black") + " | "
                + (yourTurn ? "Your turn" : "Opponent's turn") + " | " + gameStatus);
    }

    private static String pieceGlyph(char piece) {
        return switch (Character.toLowerCase(piece)) {
            case 'p' -> "♟"; case 'n' -> "♞"; case 'b' -> "♝"; case 'r' -> "♜";
            case 'q' -> "♛"; case 'k' -> "♚"; default -> "";
        };
    }

    private void setStatus(String text) {
        SwingUtilities.invokeLater(() -> status.setText(text));
    }
}
