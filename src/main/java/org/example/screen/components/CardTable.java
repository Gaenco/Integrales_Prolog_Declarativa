package org.example.screen.components;

import org.example.screen.Theme;
import org.example.screen.components.MathRender.Formula;
import org.example.screen.components.MathRender.Node;
import org.example.screen.fx.Anim;
import org.example.screen.fx.Juice;
import org.example.screen.fx.PixelFont;
import org.example.screen.fx.Sfx;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;


public class CardTable extends JComponent implements Anim.Tick {

    public record CartaDef(String letra, java.util.List<Node> lineas, char palo, String nombrePalo, String nota) {
    }

    private static final int CW = 164;
    private static final int CH = 126;
    private static final int GX = 14;
    private static final int GY = 12;
    private static final int COLS = 5;

    private static final class Estado {
        double hover;
        double tilt;
        double shim = -0.5;
        double rotIni;
        boolean sono;
        final Juice juice = new Juice();
        Formula[] formulas;
    }

    private final CartaDef[] defs;
    private final Estado[] est;
    private double tiempo = 0;
    private int hoverIdx = -1;
    private double mx;
    private Consumer<CartaDef> alCambiar;

    public CardTable(java.util.List<CartaDef> cartas) {
        this.defs = cartas.toArray(new CartaDef[0]);
        this.est = new Estado[defs.length];
        for (int i = 0; i < est.length; i++) {
            est[i] = new Estado();
            est[i].rotIni = ((i * 37) % 11 - 5) * 0.07;
        }
        setOpaque(false);

        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mx = e.getX();
                int idx = buscar(e.getX(), e.getY());
                if (idx != hoverIdx) {
                    hoverIdx = idx;
                    setCursor(idx >= 0 ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                    if (idx >= 0) {
                        Sfx.play(Sfx.Id.TICK);
                        if (alCambiar != null) {
                            alCambiar.accept(defs[idx]);
                        }
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverIdx = -1;
            }

            @Override
            public void mousePressed(MouseEvent e) {
                int idx = buscar(e.getX(), e.getY());
                if (idx >= 0) {
                    est[idx].juice.disparar(0.12, 0.09);
                    Sfx.play(Sfx.Id.CARTA);
                }
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    public void setAlCambiar(Consumer<CartaDef> c) {
        this.alCambiar = c;
    }

    public void repartir() {
        tiempo = 0;
        for (Estado s : est) {
            s.sono = false;
        }
        Anim.add(this);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        repartir();
    }

    @Override
    public boolean tick(double dt) {
        tiempo += dt;
        for (int i = 0; i < est.length; i++) {
            Estado s = est[i];
            double obj = i == hoverIdx ? 1 : 0;
            s.hover += (obj - s.hover) * Math.min(1, dt * 14);
            double tiltObj = 0;
            if (i == hoverIdx) {
                tiltObj = Anim.clamp((mx - centroX(i)) / (CW / 2.0), -1, 1) * 0.13;
            }
            s.tilt += (tiltObj - s.tilt) * Math.min(1, dt * 12);
            double shimObj = i == hoverIdx ? Anim.clamp((mx - (centroX(i) - CW / 2.0)) / CW, 0, 1) * 1.5 - 0.25 : -0.5;
            s.shim += (shimObj - s.shim) * Math.min(1, dt * 8);
            s.juice.tick(dt);
            double p = Anim.clamp((tiempo - i * 0.05) / 0.5, 0, 1);
            if (p > 0.05 && !s.sono) {
                s.sono = true;
                if (i % 2 == 0) {
                    Sfx.play(Sfx.Id.CARTA);
                }
            }
        }
        repaint();
        return isDisplayable();
    }

    private double x0() {
        return (getWidth() - (COLS * CW + (COLS - 1) * GX)) / 2.0;
    }

    private double y0() {
        return 22;
    }

    private double centroX(int i) {
        return x0() + (i % COLS) * (CW + GX) + CW / 2.0;
    }

    private double centroY(int i) {
        return y0() + (i / COLS) * (CH + GY) + CH / 2.0;
    }

    private int buscar(int px, int py) {
        for (int i = defs.length - 1; i >= 0; i--) {
            if (Math.abs(px - centroX(i)) <= CW / 2.0 && Math.abs(py - centroY(i)) <= CH / 2.0) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public Dimension getPreferredSize() {
        int filas = (defs.length + COLS - 1) / COLS;
        return new Dimension(COLS * CW + (COLS - 1) * GX + 24, filas * CH + (filas - 1) * GY + 52);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int pasada = 0; pasada < 2; pasada++) {
            for (int i = 0; i < defs.length; i++) {
                boolean arriba = est[i].hover > 0.05;
                if ((pasada == 1) == arriba) {
                    pintarCarta(g2, i);
                }
            }
        }
        g2.dispose();
    }

    private void pintarCarta(Graphics2D g, int i) {
        Estado s = est[i];
        CartaDef d = defs[i];
        double p = Anim.clamp((tiempo - i * 0.05) / 0.5, 0, 1);
        if (p <= 0) {
            return;
        }
        double e = Anim.outBack(p);
        double sx = getWidth() / 2.0;
        double sy = getHeight() + 150;
        double x = Anim.lerp(sx, centroX(i), e);
        double y = Anim.lerp(sy, centroY(i), e);
        double rot = (1 - Anim.outCubic(p)) * s.rotIni + s.tilt + s.juice.rotacion()
                + Math.sin(tiempo * 2.2 + i) * 0.012 * s.hover;
        double esc = (1 + 0.10 * s.hover) * s.juice.escala();
        double lift = -12 * s.hover;

        Graphics2D c = (Graphics2D) g.create();
        AffineTransform at = new AffineTransform();
        at.translate(x, y + lift);
        at.rotate(rot);
        at.scale(esc, esc);
        at.translate(-CW / 2.0, -CH / 2.0);
        c.transform(at);
        c.setComposite(AlphaComposite.SrcOver.derive((float) Anim.clamp(p * 2, 0, 1)));

        boolean rojo = d.palo() == 'D' || d.palo() == 'H';
        Color colPalo = rojo ? new Color(0xD9372F) : Theme.INK;

        c.setColor(new Color(0, 0, 0, 100));
        c.fill(new RoundRectangle2D.Double(3, 6 + 10 * s.hover, CW - 6, CH, 16, 16));
        c.setColor(Theme.OUTLINE);
        c.fill(new RoundRectangle2D.Double(0, 0, CW, CH, 16, 16));
        RoundRectangle2D.Double papel = new RoundRectangle2D.Double(2.5, 2.5, CW - 5, CH - 5, 13, 13);
        c.setPaint(new GradientPaint(0, 0, Theme.aclarar(Theme.CREAM, 0.35), 0, CH, Theme.CREAM));
        c.fill(papel);
        c.setClip(papel);
        c.setPaint(new TexturePaint(BCard.grano(), new Rectangle(0, 0, 96, 96)));
        c.fill(papel);
        c.setClip(null);
        // filete interior del color del palo
        c.setColor(Theme.alfa(colPalo, 70));
        c.setStroke(new BasicStroke(2f));
        c.draw(new RoundRectangle2D.Double(8, 8, CW - 16, CH - 16, 8, 8));

        indice(c, d, colPalo);
        c.rotate(Math.PI, CW / 2.0, CH / 2.0);
        indice(c, d, colPalo);
        c.rotate(-Math.PI, CW / 2.0, CH / 2.0);

        prepararFormulas(s, d);
        float total = 0;
        for (Formula f : s.formulas) {
            total += f.alto();
        }
        total += 6 * (s.formulas.length - 1);
        float yy = (CH - total) / 2f + (s.formulas.length > 2 ? 4 : 0);
        c.setColor(Theme.INK);
        for (Formula f : s.formulas) {
            f.pintar(c, (CW - f.ancho) / 2f, yy + f.asc);
            yy += f.alto() + 6;
        }

        if (s.hover > 0.02) {
            c.setClip(papel);
            float cx = (float) (s.shim * CW);
            c.setComposite(AlphaComposite.SrcOver.derive((float) (0.38 * s.hover)));
            c.setPaint(new LinearGradientPaint(cx - 50, 0, cx + 50, CH * 0.55f,
                    new float[]{0f, 0.5f, 1f},
                    new Color[]{new Color(255, 255, 255, 0), new Color(255, 255, 255, 190), new Color(255, 255, 255, 0)}));
            c.fill(papel);
            c.setComposite(AlphaComposite.SrcOver.derive((float) (0.13 * s.hover)));
            c.setPaint(new LinearGradientPaint(0, 0, CW, CH,
                    new float[]{0f, 0.35f, 0.65f, 1f},
                    new Color[]{new Color(0x00E5FF), new Color(0xFF4DD2), new Color(0xFFE14D), new Color(0x4DFF88)}));
            c.fill(papel);
        }
        c.dispose();
    }

    private void prepararFormulas(Estado s, CartaDef d) {
        if (s.formulas != null) {
            return;
        }
        float tam = d.lineas().size() > 2 ? 12.5f : 15f;
        float maxW = CW - 34;
        Formula[] fs = new Formula[d.lineas().size()];
        float mayor = 0;
        for (int i = 0; i < fs.length; i++) {
            fs[i] = MathRender.componer(d.lineas().get(i), tam);
            mayor = Math.max(mayor, fs[i].ancho);
        }
        if (mayor > maxW) {
            float k = Math.max(0.65f, maxW / mayor);
            for (int i = 0; i < fs.length; i++) {
                fs[i] = MathRender.componer(d.lineas().get(i), tam * k);
            }
        }
        s.formulas = fs;
    }

    private void indice(Graphics2D c, CartaDef d, Color colPalo) {
        PixelFont.dibujar(c, d.letra(), 14, 13, 2, colPalo, null);
        int lw = PixelFont.ancho(d.letra(), 2);
        pip(c, d.palo(), 14 + lw + 10, 20, 7, colPalo);
    }

    static void pip(Graphics2D g, char palo, double cx, double cy, double r, Color color) {
        Graphics2D c = (Graphics2D) g.create();
        c.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        c.translate(cx, cy);
        c.scale(r, r);
        c.setColor(color);
        switch (palo) {
            case 'D' -> {
                Path2D.Double p = new Path2D.Double();
                p.moveTo(0, -1.1);
                p.lineTo(0.72, 0);
                p.lineTo(0, 1.1);
                p.lineTo(-0.72, 0);
                p.closePath();
                c.fill(p);
            }
            case 'H' -> c.fill(corazon(false));
            case 'S' -> {
                c.fill(corazon(true));
                Path2D.Double tallo = new Path2D.Double();
                tallo.moveTo(-0.14, 0.5);
                tallo.lineTo(0.14, 0.5);
                tallo.lineTo(0.42, 1.1);
                tallo.lineTo(-0.42, 1.1);
                tallo.closePath();
                c.fill(tallo);
            }
            default -> {            // trébol
                c.fill(new Ellipse2D.Double(-0.42, -1.05, 0.84, 0.84));
                c.fill(new Ellipse2D.Double(-0.98, -0.28, 0.84, 0.84));
                c.fill(new Ellipse2D.Double(0.14, -0.28, 0.84, 0.84));
                Path2D.Double tallo = new Path2D.Double();
                tallo.moveTo(-0.12, 0);
                tallo.lineTo(0.12, 0);
                tallo.lineTo(0.38, 1.1);
                tallo.lineTo(-0.38, 1.1);
                tallo.closePath();
                c.fill(tallo);
            }
        }
        c.dispose();
    }

    private static Path2D.Double corazon(boolean invertido) {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(0, -0.45);
        p.curveTo(-0.1, -0.95, -1.0, -1.0, -1.0, -0.28);
        p.curveTo(-1.0, 0.35, -0.25, 0.65, 0, 1.05);
        p.curveTo(0.25, 0.65, 1.0, 0.35, 1.0, -0.28);
        p.curveTo(1.0, -1.0, 0.1, -0.95, 0, -0.45);
        p.closePath();
        if (invertido) {
            p.transform(AffineTransform.getScaleInstance(1, -1));
            p.transform(AffineTransform.getTranslateInstance(0, -0.05));
        }
        return p;
    }
}
