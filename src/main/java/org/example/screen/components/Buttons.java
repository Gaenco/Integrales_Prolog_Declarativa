package org.example.screen.components;

import org.example.screen.Theme;
import org.example.screen.fx.Anim;
import org.example.screen.fx.Juice;
import org.example.screen.fx.PixelFont;
import org.example.screen.fx.Sfx;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


public class Buttons extends JButton implements Anim.Tick {

    private static final int MARGEN = 4;
    private static final int BORDE = 4;

    private Color base;
    private Color colorTexto;
    private final int escala;
    private final int padH;
    private final int padV;
    private Sfx.Id sonido = Sfx.Id.CLICK;

    private double hover;
    private double hoverObjetivo;
    private double presionado;
    private double aparecer = 1;
    private double retraso;
    private final Juice juice = new Juice();

    public Buttons(String texto, Color base, Color colorTexto, int escala, int padH, int padV) {
        super(texto);
        this.base = base;
        this.colorTexto = colorTexto;
        this.escala = escala;
        this.padH = padH;
        this.padV = padV;

        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!isEnabled()) {
                    return;
                }
                hoverObjetivo = 1;
                Sfx.play(Sfx.Id.TICK);
                Anim.add(Buttons.this);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverObjetivo = 0;
                Anim.add(Buttons.this);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled()) {
                    Anim.add(Buttons.this);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                Anim.add(Buttons.this);
            }
        });
        addActionListener(e -> {
            Sfx.play(sonido);
            juice.disparar(0.09, 0.035);
            Anim.add(this);
        });
    }

    public void setSonido(Sfx.Id id) {
        this.sonido = id;
    }

    public void setColores(Color base, Color texto) {
        this.base = base;
        this.colorTexto = texto;
        repaint();
    }

    public void entrar(double retrasoSeg) {
        aparecer = 0;
        retraso = retrasoSeg;
        Anim.add(this);
    }

    public void rebotar() {
        juice.disparar(0.09, 0.035);
        Anim.add(this);
    }

    @Override
    public boolean tick(double dt) {
        hover += (hoverObjetivo - hover) * Math.min(1, dt * 18);
        if (Math.abs(hoverObjetivo - hover) < 0.01) {
            hover = hoverObjetivo;
        }
        double objP = getModel().isPressed() ? 1 : 0;
        presionado += (objP - presionado) * Math.min(1, dt * 34);
        if (Math.abs(objP - presionado) < 0.02) {
            presionado = objP;
        }
        if (retraso > 0) {
            retraso -= dt;
        } else if (aparecer < 1) {
            aparecer = Math.min(1, aparecer + dt / 0.45);
        }
        boolean j = juice.tick(dt);
        repaint();
        return j || retraso > 0 || aparecer < 1 || hover != hoverObjetivo || presionado != objP;
    }

    @Override
    public Dimension getPreferredSize() {
        int tw = PixelFont.ancho(getText(), escala);
        return new Dimension(tw + 2 * padH + 2 * MARGEN, 7 * escala + 2 * padV + BORDE + 2 * MARGEN);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double w = getWidth();
        double h = getHeight();

        double p = retraso > 0 ? 0 : aparecer;
        double esc = (1 + 0.05 * hover) * juice.escala() * (0.85 + 0.15 * Anim.outBack(p));
        double dy = (1 - Anim.outCubic(p)) * 34;
        g2.translate(w / 2, h / 2 + dy);
        g2.rotate(juice.rotacion());
        g2.scale(esc, esc);
        g2.translate(-w / 2, -h / 2);
        if (p < 1) {
            g2.setComposite(AlphaComposite.SrcOver.derive((float) Anim.clamp(p * 1.8, 0, 1)));
        }

        boolean activo = isEnabled();
        Color cara = activo ? base : Theme.PANEL_LIGHT;
        Color canto = Theme.oscurecer(cara, 0.45);

        int x = MARGEN;
        int bw = (int) w - 2 * MARGEN;
        int alturaCara = (int) h - 2 * MARGEN - BORDE;
        int yCanto = MARGEN + BORDE;
        int yCara = MARGEN + (int) Math.round(BORDE * presionado - 2 * hover);
        int arco = 12;

        g2.setColor(new Color(0, 0, 0, 85));
        g2.fillRoundRect(x, yCanto + 3, bw, alturaCara, arco, arco);
        g2.setColor(Theme.OUTLINE);
        g2.fillRoundRect(x - 1, yCanto - 1, bw + 2, alturaCara + 2, arco + 2, arco + 2);
        g2.setColor(canto);
        g2.fillRoundRect(x, yCanto, bw, alturaCara, arco, arco);
        g2.setColor(Theme.OUTLINE);
        g2.fillRoundRect(x - 1, yCara - 1, bw + 2, alturaCara + 2, arco + 2, arco + 2);
        g2.setColor(hover > 0 ? Theme.aclarar(cara, 0.10 * hover) : cara);
        g2.fillRoundRect(x, yCara, bw, alturaCara, arco, arco);
        g2.setColor(new Color(255, 255, 255, 70));
        g2.fillRoundRect(x + 4, yCara + 3, bw - 8, 3, 3, 3);

        String t = getText();
        int tw = PixelFont.ancho(t, escala);
        int tx = x + (bw - tw) / 2;
        int ty = yCara + (int) Math.round(alturaCara / 2.0 - 5.5 * escala);
        Color txt = activo ? colorTexto : Theme.MUTED;
        PixelFont.dibujar(g2, t, tx, ty, escala, txt, Theme.oscurecer(cara, 0.6));
        g2.dispose();
    }
}
