package org.example.screen.components;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;


public class CrtOverlay extends JComponent {

    private BufferedImage capa;

    public CrtOverlay() {
        setOpaque(false);
    }

    @Override
    public boolean contains(int x, int y) {
        return false;
    }

    private void prepararCapa(int w, int h) {
        if (capa != null && capa.getWidth() == w && capa.getHeight() == h) {
            return;
        }
        capa = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_ARGB);
        int[] px = new int[w * h];
        for (int y = 0; y < h; y++) {
            double dy = (y - h / 2.0) / (h / 2.0);
            for (int x = 0; x < w; x++) {
                double dx = (x - w / 2.0) / (w / 2.0);
                double d = Math.sqrt(dx * dx * 0.8 + dy * dy);
                double v = Math.max(0, Math.min(1, (d - 0.72) / 0.75));
                int alfa = (int) (v * v * 150);
                if (y % 3 == 1) {
                    alfa = Math.min(255, alfa + 30);
                }
                px[y * w + x] = alfa << 24;
            }
        }
        capa.setRGB(0, 0, w, h, px, 0, w);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth();
        int h = getHeight();
        prepararCapa(w, h);
        g2.drawImage(capa, 0, 0, null);

        double fase = (System.nanoTime() / 1e9 * 55) % (h + 160);   // banda de refresco
        int by = (int) fase - 120;
        g2.setPaint(new GradientPaint(0, by, new Color(255, 255, 255, 0), 0, by + 60, new Color(255, 255, 255, 12)));
        g2.fillRect(0, by, w, 60);
        g2.setPaint(new GradientPaint(0, by + 60, new Color(255, 255, 255, 12), 0, by + 120, new Color(255, 255, 255, 0)));
        g2.fillRect(0, by + 60, w, 60);
        g2.dispose();
    }
}
