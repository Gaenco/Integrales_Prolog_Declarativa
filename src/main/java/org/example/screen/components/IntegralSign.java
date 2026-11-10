package org.example.screen.components;

import org.example.screen.Theme;
import org.example.screen.fx.Anim;
import org.example.screen.fx.Juice;
import org.example.screen.fx.Sfx;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;


public class IntegralSign extends JComponent implements Anim.Tick {

    private double fase = 0;
    private final Juice juice = new Juice();

    public IntegralSign(int ancho, int alto) {
        setPreferredSize(new Dimension(ancho, alto));
        setMinimumSize(new Dimension(ancho, alto));
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText("¡Pulsa la integral!");
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                juice.disparar(0.14, 0.10);
                Sfx.play(Sfx.Id.CARTA);
                Anim.add(IntegralSign.this);
            }
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        Anim.add(this);
    }

    @Override
    public boolean tick(double dt) {
        fase += dt;
        juice.tick(dt);
        repaint();
        return isDisplayable();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double w = getWidth();
        double h = getHeight();
        double flota = Math.sin(fase * 1.7) * 3.5;
        double giro = Math.sin(fase * 1.1) * 0.035 + juice.rotacion();
        AffineTransform at = new AffineTransform();
        at.translate(w / 2, h / 2 + flota);
        at.rotate(giro);
        at.scale(juice.escala(), juice.escala());
        at.translate(-w / 2, -h / 2);
        g2.transform(at);

        g2.setColor(new Color(0, 0, 0, 70));
        g2.fill(new Ellipse2D.Double(w * 0.18, h - 9 - flota * 0.4, w * 0.62, 8));

        Paint dorado = new GradientPaint(0, 0, Theme.aclarar(Theme.GOLD, 0.45), 0, (float) h, Theme.ORANGE);
        pintar(g2, 0, 0, (float) w, (float) h - 10, dorado, Theme.alfa(Theme.OUTLINE, 160), 0.10f);
        g2.dispose();
    }

    public static void pintar(Graphics2D g2, float x, float y, float w, float h,
                              Paint trazo, Color borde, float grosor) {
        g2 = (Graphics2D) g2.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        float sw = Math.max(1.3f, w * grosor);

        Path2D.Float p = new Path2D.Float();
        p.moveTo(x + 0.88f * w, y + 0.12f * h);
        p.curveTo(x + 0.84f * w, y + 0.02f * h, x + 0.68f * w, y + 0.02f * h, x + 0.64f * w, y + 0.19f * h);
        p.curveTo(x + 0.58f * w, y + 0.45f * h, x + 0.42f * w, y + 0.55f * h, x + 0.36f * w, y + 0.81f * h);
        p.curveTo(x + 0.32f * w, y + 0.98f * h, x + 0.16f * w, y + 0.98f * h, x + 0.12f * w, y + 0.88f * h);

        float r = sw * 0.95f;
        Ellipse2D.Float arriba = new Ellipse2D.Float(x + 0.88f * w - r, y + 0.12f * h - r, 2 * r, 2 * r);
        Ellipse2D.Float abajo = new Ellipse2D.Float(x + 0.12f * w - r, y + 0.88f * h - r, 2 * r, 2 * r);

        if (borde != null) {
            g2.setStroke(new BasicStroke(sw + 4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(borde);
            g2.draw(p);
            g2.fill(new Ellipse2D.Float(arriba.x - 2.2f, arriba.y - 2.2f, arriba.width + 4.4f, arriba.height + 4.4f));
            g2.fill(new Ellipse2D.Float(abajo.x - 2.2f, abajo.y - 2.2f, abajo.width + 4.4f, abajo.height + 4.4f));
        }
        g2.setPaint(trazo);
        g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(p);
        g2.fill(arriba);
        g2.fill(abajo);
        g2.dispose();
    }
}
