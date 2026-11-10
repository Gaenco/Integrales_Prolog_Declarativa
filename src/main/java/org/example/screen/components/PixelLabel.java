package org.example.screen.components;

import org.example.screen.Theme;
import org.example.screen.fx.Anim;
import org.example.screen.fx.PixelFont;

import javax.swing.*;
import java.awt.*;
import java.util.List;


public class PixelLabel extends JComponent implements Anim.Tick {

    private String texto;
    private final int escala;
    private Color color = Theme.TEXT;
    private Color sombra = Theme.OUTLINE;
    private float alineacion = 0f;
    private int anchoEnvoltura = 0;
    private boolean ola = false;
    private double fase = 0;

    public PixelLabel(String texto, int escala) {
        this.texto = texto;
        this.escala = escala;
        setOpaque(false);
    }

    public void setTexto(String t) {
        this.texto = t;
        revalidate();
        repaint();
    }

    public String getTexto() {
        return texto;
    }

    public void setColor(Color c) {
        this.color = c;
        repaint();
    }

    public void setSombra(Color c) {
        this.sombra = c;
    }

    public void setAlineacion(float a) {
        this.alineacion = a;
    }

    public void setAnchoEnvoltura(int px) {
        this.anchoEnvoltura = px;
        revalidate();
    }

    public void setOla(boolean o) {
        this.ola = o;
        if (o) {
            Anim.add(this);
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (ola) {
            Anim.add(this);
        }
    }

    @Override
    public boolean tick(double dt) {
        fase += dt;
        repaint();
        return ola && isDisplayable();
    }

    private List<String> lineas() {
        return anchoEnvoltura > 0 ? PixelFont.envolver(texto, anchoEnvoltura, escala) : List.of(texto);
    }

    private int altoLinea() {
        return PixelFont.alto(escala) + escala + escala;
    }

    @Override
    public Dimension getPreferredSize() {
        List<String> ls = lineas();
        int w = anchoEnvoltura > 0 ? anchoEnvoltura : PixelFont.ancho(texto, escala) + escala;
        int h = ls.size() * altoLinea() - escala + (ola ? 3 * escala : 0);
        return new Dimension(w, Math.max(h, PixelFont.alto(escala) + escala));
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(Math.min(60, getPreferredSize().width), getPreferredSize().height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        int y = 0;
        int idx = 0;
        for (String l : lineas()) {
            int tw = PixelFont.ancho(l, escala);
            int x = (int) ((getWidth() - tw) * alineacion);
            if (ola) {
                int cx = x;
                for (int i = 0; i < l.length(); i++) {
                    char ch = l.charAt(i);
                    int dy = (int) Math.round(Math.sin(fase * 5 + (idx + i) * 0.55) * escala * 1.2);
                    PixelFont.dibujar(g2, String.valueOf(ch), cx, y + dy + escala * 3 / 2, escala, color, sombra);
                    cx += PixelFont.avance(ch) * escala;
                }
            } else {
                PixelFont.dibujar(g2, l, x, y, escala, color, sombra);
            }
            idx += l.length();
            y += altoLinea();
        }
        g2.dispose();
    }
}
