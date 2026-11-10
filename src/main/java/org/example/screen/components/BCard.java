package org.example.screen.components;

import org.example.screen.Theme;
import org.example.screen.fx.Anim;
import org.example.screen.fx.Juice;
import org.example.screen.fx.PixelFont;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;


public class BCard extends JPanel implements Anim.Tick {

    public enum Estilo { PANEL, FIELTRO }

    private static final int SOMBRA = 6;
    private static final int BORDE = 3;
    private static final int ALTO_CINTA = 28;

    private static final BufferedImage GRANO = crearGrano();

    private final Estilo estilo;
    private String titulo;
    private Color colorTitulo;
    private BufferedImage chrome;

    private double aparecer = 1;
    private double retraso = 0;
    private double sacudida = 0;
    private final Juice juice = new Juice();

    public BCard(LayoutManager layout, Estilo estilo, String titulo, Color colorTitulo) {
        super(layout);
        this.estilo = estilo;
        this.titulo = titulo;
        this.colorTitulo = colorTitulo;
        setOpaque(false);
        actualizarBorde();
    }

    public BCard(LayoutManager layout) {
        this(layout, Estilo.PANEL, null, null);
    }

    private void actualizarBorde() {
        int arriba = BORDE + (titulo != null ? ALTO_CINTA : 0) + 12;
        setBorder(new EmptyBorder(arriba, 16, SOMBRA + BORDE + 12, 16));
    }

    public void setTitulo(String t) {
        this.titulo = t;
        chrome = null;
        actualizarBorde();
        repaint();
    }

    public void setColorTitulo(Color c) {
        this.colorTitulo = c;
        chrome = null;
        repaint();
    }

    public void entrar(double retrasoSeg) {
        aparecer = 0;
        retraso = retrasoSeg;
        Anim.add(this);
    }

    public void pulso() {
        juice.disparar(0.05, 0.012);
        Anim.add(this);
    }

    public void sacudir() {
        sacudida = 0.5;
        Anim.add(this);
    }

    @Override
    public boolean tick(double dt) {
        if (retraso > 0) {
            retraso -= dt;
        } else if (aparecer < 1) {
            aparecer = Math.min(1, aparecer + dt / 0.55);
        }
        boolean j = juice.tick(dt);
        if (sacudida > 0) {
            sacudida = Math.max(0, sacudida - dt);
        }
        repaint();
        return j || sacudida > 0 || aparecer < 1 || retraso > 0;
    }

    private boolean animando() {
        return aparecer < 1 || retraso > 0 || sacudida > 0 || juice.activo();
    }

    @Override
    public void paint(Graphics g) {
        if (!animando()) {
            super.paint(g);
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        double w = getWidth();
        double h = getHeight();
        double esc = juice.escala();
        double dy = 0;
        float alfa = 1f;
        if (aparecer < 1 || retraso > 0) {
            double p = retraso > 0 ? 0 : aparecer;
            esc *= 0.90 + 0.10 * Anim.outBack(p);
            dy = (1 - Anim.outCubic(p)) * 36;
            alfa = (float) Anim.clamp(p * 1.8, 0, 1);
        }
        double dx = sacudida > 0 ? 8 * Math.sin(sacudida * 70) * Math.pow(sacudida / 0.5, 2) : 0;
        g2.translate(w / 2 + dx, h / 2 + dy);
        g2.rotate(juice.rotacion());
        g2.scale(esc, esc);
        g2.translate(-w / 2, -h / 2);
        if (alfa < 1f) {
            g2.setComposite(AlphaComposite.SrcOver.derive(alfa));
        }
        super.paint(g2);
        g2.dispose();
    }

    @Override
    protected void paintComponent(Graphics g) {
        int w = getWidth();
        int hTotal = getHeight();
        if (w <= 0 || hTotal <= 0) {
            return;
        }
        if (chrome == null || chrome.getWidth() != w || chrome.getHeight() != hTotal) {
            chrome = new BufferedImage(w, hTotal, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gi = chrome.createGraphics();
            dibujarMarco(gi, w, hTotal);
            gi.dispose();
        }
        g.drawImage(chrome, 0, 0, null);
    }

    private void dibujarMarco(Graphics2D g2, int w, int hTotal) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int h = hTotal - SOMBRA;
        int arco = 18;

        // sombra proyectada
        g2.setColor(new Color(0, 0, 0, 95));
        g2.fill(new RoundRectangle2D.Float(2, SOMBRA, w - 4, h, arco, arco));

        // contorno
        g2.setColor(Theme.OUTLINE);
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, arco, arco));

        RoundRectangle2D.Float interior = new RoundRectangle2D.Float(BORDE, BORDE, w - 2 * BORDE, h - 2 * BORDE, arco - 5, arco - 5);
        if (estilo == Estilo.FIELTRO) {
            g2.setPaint(new GradientPaint(0, 0, Theme.FELT, 0, h, Theme.FELT_DARK));
        } else {
            g2.setColor(Theme.PANEL);
        }
        g2.fill(interior);

        g2.setClip(interior);
        g2.setPaint(new TexturePaint(GRANO, new Rectangle(0, 0, GRANO.getWidth(), GRANO.getHeight())));
        g2.fill(interior);
        if (estilo == Estilo.FIELTRO) {                      // marco interior más oscuro, como el borde de la mesa
            g2.setStroke(new BasicStroke(10f));
            g2.setColor(new Color(0, 0, 0, 55));
            g2.draw(interior);
        }

        if (titulo != null) {
            g2.setColor(colorTitulo != null ? colorTitulo : Theme.PURPLE);
            g2.fillRect(BORDE, BORDE, w - 2 * BORDE, ALTO_CINTA);
            g2.setColor(new Color(0, 0, 0, 70));
            g2.fillRect(BORDE, BORDE + ALTO_CINTA - 3, w - 2 * BORDE, 3);
            g2.setColor(new Color(255, 255, 255, 60));
            g2.fillRect(BORDE, BORDE, w - 2 * BORDE, 2);
        }
        g2.setClip(null);

        g2.setColor(new Color(255, 255, 255, 30));
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(arco / 2 + 2, BORDE + (titulo != null ? ALTO_CINTA : 0) + 1, w - arco / 2 - 2, BORDE + (titulo != null ? ALTO_CINTA : 0) + 1);

        if (titulo != null) {
            int ty = BORDE + (ALTO_CINTA - PixelFont.alto(2)) / 2 - 1;
            PixelFont.dibujar(g2, titulo, BORDE + 14, ty, 2, Theme.TEXT, Theme.oscurecer(colorTitulo != null ? colorTitulo : Theme.PURPLE, 0.55));
        }
    }

    private static BufferedImage crearGrano() {
        BufferedImage im = new BufferedImage(96, 96, BufferedImage.TYPE_INT_ARGB);
        Random r = new Random(42);
        for (int y = 0; y < 96; y++) {
            for (int x = 0; x < 96; x++) {
                int a = r.nextInt(26);
                int c = r.nextBoolean() ? 255 : 0;
                im.setRGB(x, y, (a << 24) | (c << 16) | (c << 8) | c);
            }
        }
        return im;
    }

    public static BufferedImage grano() {
        return GRANO;
    }
}
