package com.matveevap.qrapp;

import javax.swing.*;
import java.awt.*;

public class QRPainter {
    public static void paintQRCode(boolean[][] matrix) {
        int size = matrix.length;
        int squareSize = 15;
        Drawer.qrOutput(size, squareSize, matrix);
    }

    private static int getSquareSize(int qrSize) {
        double scale = Math.max(3, Math.min(10, 500.0 / qrSize));

        int size = (int) (qrSize * scale);
        int minSize = 10;
        int maxSize = 600;

        return Math.max(minSize, Math.min(maxSize, size));
    }
}

class Drawer extends JPanel {
    private final int SIZE;
    private final int SQUARE_SIZE;
    private final boolean[][] QRMATRIX;

    private Drawer(int size, int squareSize, boolean[][] qrmatrix) {
        SIZE = size;
        SQUARE_SIZE = squareSize;
        QRMATRIX = qrmatrix;
    }

    static void qrOutput(int size, int squareSize, boolean[][] qrmatrix) {
        JFrame frame = new JFrame("QR Code");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new Drawer(size, squareSize, qrmatrix));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (int row = 0; row < SIZE + 8; row++) {
            for (int col = 0; col < SIZE + 8; col++) {
                if (row < 4 || row >= SIZE + 4 || col < 4 || col >= SIZE + 4) g.setColor(Color.WHITE);
                else if (QRMATRIX[row - 4][col - 4]) {
                    g.setColor(Color.BLACK);
                } else {
                    g.setColor(Color.WHITE);
                }
                g.fillRect(col * SQUARE_SIZE, row * SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);
            }
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension((SIZE + 8) * SQUARE_SIZE, (SIZE + 8) * SQUARE_SIZE);
    }
}
