package org.example.screen.components;

import org.example.screen.Theme;
import org.example.screen.fx.Anim;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;


public class BalatroBackground extends JPanel implements Anim.Tick {

    private static final int LW = 112;
    private static final int LH = 70;

    private static final double[] A = {0.50, 0.12, 0.12};
    private static final double[] B = {0.05, 0.27, 0.40};
    private static final double[] C = {0.06, 0.07, 0.10};

    private final BufferedImage bajo = new BufferedImage(LW, LH, BufferedImage.TYPE_INT_RGB);
    private double t = 3.0;
    private double acumulado = 0;
    private BufferedImage lleno;

    public BalatroBackground(LayoutManager layout) {
        super(layout);
        render();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        Anim.add(this);
    }

    @Override
    public boolean tick(double dt) {
        t += dt;
        acumulado += dt;
        if (acumulado >= 1 / 15.0) {
            acumulado = 0;
            render();
            ampliar();
            repaint();
        }
        return isDisplayable();
    }

    private void render() {
        double velocidad = t * 0.22;
        for (int y = 0; y < LH; y++) {
            for (int x = 0; x < LW; x++) {
                double u = (x - LW / 2.0) / LH * 2.0;
                double v = (y - LH / 2.0) / LH * 2.0;
                double r = Math.sqrt(u * u + v * v);
                double ang = Math.atan2(v, u) + 2.6 * r - velocidad * 0.5;
                double sx = r * Math.cos(ang) * 3.4;
                double sy = r * Math.sin(ang) * 3.4;
                for (int i = 0; i < 4; i++) {
                    sx += 0.62 * Math.sin(sy * 1.25 + velocidad * 0.9 + i * 1.7);
                    sy += 0.62 * Math.cos(sx * 1.10 - velocidad * 0.7 + i * 2.3);
                }
                double k = Math.sin(sx * 1.15 + sy * 0.75) * 0.5 + 0.5;
                double m = Math.sin(sx * 0.80 - sy * 1.10 + 1.3) * 0.5 + 0.5;

                double aa = smooth(0.38, 0.62, k);
                double borde = 1 - Math.min(1, Math.abs(k - 0.5) * 9);
                double sombra = smooth(0.55, 0.9, m) * 0.55 + borde * 0.55;
                double rr = lerp(A[0], B[0], aa);
                double gg = lerp(A[1], B[1], aa);
                double bb = lerp(A[2], B[2], aa);
                rr = lerp(rr, C[0], sombra);
                gg = lerp(gg, C[1], sombra);
                bb = lerp(bb, C[2], sombra);
                bajo.setRGB(x, y, (to8(rr) << 16) | (to8(gg) << 8) | to8(bb));
            }
        }
    }

    private static double smooth(double a, double b, double x) {
        double u = Math.max(0, Math.min(1, (x - a) / (b - a)));
        return u * u * (3 - 2 * u);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static int to8(double v) {
        return (int) Math.max(0, Math.min(255, v * 255));
    }

    private void ampliar() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        if (lleno == null || lleno.getWidth() != w || lleno.getHeight() != h) {
            lleno = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        }
        Graphics2D g2 = lleno.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(bajo, 0, 0, w, h, null);
        g2.dispose();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (lleno == null || lleno.getWidth() != getWidth() || lleno.getHeight() != getHeight()) {
            ampliar();
        }
        if (lleno != null) {
            g.drawImage(lleno, 0, 0, null);
        }
    }
}
