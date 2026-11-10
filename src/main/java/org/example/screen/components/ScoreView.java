package org.example.screen.components;

import org.example.screen.Theme;
import org.example.screen.fx.Anim;
import org.example.screen.fx.Juice;
import org.example.screen.fx.PixelFont;
import org.example.screen.fx.Sfx;

import javax.swing.*;
import java.awt.*;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ScoreView extends JComponent implements Anim.Tick {

    private static final Pattern APROX = Pattern.compile("^(.*?)\\s*\\(aprox\\. (.*)\\)$");
    private static final double DURACION = 1.0;

    private String etB = "F(b)";
    private String etA = "F(a)";
    private String txtB = "";
    private String txtA = "";
    private String txtArea = "";

    private double objetivo;
    private int decimales;
    private boolean numerico;
    private double progreso = 1;
    private int ultimoPaso = -1;
    private final Juice juiceOro = new Juice();
    private final Juice juiceB = new Juice();
    private final Juice juiceA = new Juice();
    private double retrasoB;
    private double retrasoA;

    public ScoreView() {
        setOpaque(false);
    }

    public static String corto(String v) {
        Matcher m = APROX.matcher(v);
        String d = m.matches() ? m.group(2) : v;
        try {
            double x = Double.parseDouble(d);
            if (Math.abs(x) >= 1e7 || (x != 0 && Math.abs(x) < 1e-4)) {
                return String.format(Locale.ROOT, "%.3e", x).replace("e+0", "e").replace("e-0", "e-");
            }
            String s = String.format(Locale.ROOT, "%.6f", x);
            s = s.contains(".") ? s.replaceAll("0+$", "").replaceAll("\\.$", "") : s;
            return s.equals("-0") ? "0" : s;
        } catch (NumberFormatException e) {
            return d;
        }
    }

    public void limpiar() {
        txtB = txtA = txtArea = "";
        progreso = 1;
        repaint();
    }

    public void setValores(String etiquetaB, String fb, String etiquetaA, String fa, String area, boolean animar) {
        etB = etiquetaB;
        etA = etiquetaA;
        txtB = corto(fb);
        txtA = corto(fa);
        txtArea = corto(area);
        try {
            objetivo = Double.parseDouble(txtArea);
            numerico = !txtArea.contains("e");
            int punto = txtArea.indexOf('.');
            decimales = punto < 0 ? 0 : txtArea.length() - punto - 1;
        } catch (NumberFormatException e) {
            numerico = false;
        }
        if (animar && numerico) {
            progreso = 0;
            ultimoPaso = -1;
            retrasoB = 0.05;
            retrasoA = 0.20;
            Anim.add(this);
        } else {
            progreso = 1;
        }
        repaint();
    }

    @Override
    public boolean tick(double dt) {
        boolean sigue = false;
        if (retrasoB > 0) {
            retrasoB -= dt;
            if (retrasoB <= 0) {
                juiceB.disparar(0.10, 0.02);
                Sfx.play(Sfx.Id.CARTA);
            }
            sigue = true;
        }
        if (retrasoA > 0) {
            retrasoA -= dt;
            if (retrasoA <= 0) {
                juiceA.disparar(0.10, -0.02);
                Sfx.play(Sfx.Id.CARTA);
            }
            sigue = true;
        }
        if (progreso < 1 && retrasoA <= 0.0) {
            progreso = Math.min(1, progreso + dt / DURACION);
            int paso = (int) (Anim.outCubic(progreso) * 16);
            if (paso != ultimoPaso && progreso < 1) {
                ultimoPaso = paso;
                Sfx.conteo(Math.min(11, paso * 12 / 16));
            }
            if (progreso >= 1) {
                juiceOro.disparar(0.10, 0.015);
                Sfx.play(Sfx.Id.CHIP);
                Sfx.play(Sfx.Id.EXITO);
            }
            sigue = true;
        }
        sigue |= juiceOro.tick(dt);
        sigue |= juiceB.tick(dt);
        sigue |= juiceA.tick(dt);
        repaint();
        return sigue;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(300, 156);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (txtArea.isEmpty()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int alto = 66;
        int signo = 30;
        int bw = (w - signo) / 2;

        caja(g2, 0, 0, bw, alto, Theme.BLUE, Theme.BLUE_DARK, etB, txtB, juiceB);
        caja(g2, w - bw, 0, bw, alto, Theme.RED, Theme.RED_DARK, etA, txtA, juiceA);
        PixelFont.dibujar(g2, "-", bw + (signo - PixelFont.ancho("-", 4)) / 2, alto / 2 - 14, 4, Theme.TEXT, Theme.OUTLINE);

        String valor;
        if (progreso < 1 && numerico) {
            valor = String.format(Locale.ROOT, "%." + decimales + "f", objetivo * Anim.outCubic(progreso));
        } else {
            valor = txtArea;
        }
        cajaOro(g2, 0, alto + 8, w, 78, valor);
        g2.dispose();
    }

    private void caja(Graphics2D g, int x, int y, int w, int h, Color c, Color oscuro, String etiqueta, String valor, Juice j) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(x + w / 2.0, y + h / 2.0);
        g2.rotate(j.rotacion());
        g2.scale(j.escala(), j.escala());
        g2.translate(-w / 2.0, -h / 2.0);
        int edge = 4;
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillRoundRect(0, edge + 3, w, h - edge, 12, 12);
        g2.setColor(Theme.OUTLINE);
        g2.fillRoundRect(-1, edge - 1, w + 2, h - edge + 2, 14, 14);
        g2.setColor(oscuro);
        g2.fillRoundRect(0, edge, w, h - edge, 12, 12);
        g2.setColor(Theme.OUTLINE);
        g2.fillRoundRect(-1, -1, w + 2, h - edge + 2, 14, 14);
        g2.setColor(c);
        g2.fillRoundRect(0, 0, w, h - edge, 12, 12);
        g2.setColor(new Color(255, 255, 255, 60));
        g2.fillRoundRect(4, 3, w - 8, 3, 3, 3);
        String et = etiqueta;
        PixelFont.dibujar(g2, et, (w - PixelFont.ancho(et, 2)) / 2, 5, 2, Theme.aclarar(c, 0.75), Theme.oscurecer(c, 0.6));
        int esc = PixelFont.ancho(valor, 3) <= w - 12 ? 3 : 2;
        PixelFont.dibujar(g2, valor, (w - PixelFont.ancho(valor, esc)) / 2, 30 + (esc == 2 ? 3 : 0), esc, Theme.TEXT, Theme.oscurecer(c, 0.6));
        g2.dispose();
    }

    private void cajaOro(Graphics2D g, int x, int y, int w, int h, String valor) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(x + w / 2.0, y + h / 2.0);
        g2.rotate(juiceOro.rotacion());
        g2.scale(juiceOro.escala(), juiceOro.escala());
        g2.translate(-w / 2.0, -h / 2.0);
        int edge = 5;
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillRoundRect(0, edge + 3, w, h - edge, 14, 14);
        g2.setColor(Theme.OUTLINE);
        g2.fillRoundRect(-1, edge - 1, w + 2, h - edge + 2, 16, 16);
        g2.setColor(Theme.GOLD_DARK);
        g2.fillRoundRect(0, edge, w, h - edge, 14, 14);
        g2.setColor(Theme.OUTLINE);
        g2.fillRoundRect(-1, -1, w + 2, h - edge + 2, 16, 16);
        g2.setPaint(new GradientPaint(0, 0, Theme.aclarar(Theme.GOLD, 0.15), 0, h, Theme.GOLD));
        g2.fillRoundRect(0, 0, w, h - edge, 14, 14);
        g2.setColor(new Color(255, 255, 255, 80));
        g2.fillRoundRect(5, 4, w - 10, 3, 3, 3);

        PixelFont.dibujar(g2, "ÁREA", 12, 9, 2, Theme.GOLD_DARK, new Color(255, 255, 255, 70));
        int esc = 5;
        while (esc > 2 && PixelFont.ancho(valor, esc) > w - 24) {
            esc--;
        }
        PixelFont.dibujar(g2, valor, (w - PixelFont.ancho(valor, esc)) / 2, 25 + (5 - esc) * 3, esc, Theme.TEXT, Theme.GOLD_DARK);
        g2.dispose();
    }
}
