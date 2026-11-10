package org.example.screen.components;

import org.example.screen.components.MathRender.Formula;
import org.example.screen.components.MathRender.Node;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class MathView extends JComponent {

    public record Linea(Node nodo, float escala, Color color) {
    }

    private static final int MARGEN = 2;

    private final List<Linea> lineas = new ArrayList<>();
    private final float tam;
    private final int altoMinimo;
    private Color sombra = null;

    private Formula[] naturales = new Formula[0];
    private Formula[] ajustadas = new Formula[0];
    private int anchoAjuste = -1;

    public MathView(float tam, int altoMinimo) {
        this.tam = tam;
        this.altoMinimo = altoMinimo;
        setOpaque(false);
    }

    public void setSombra(Color c) {
        this.sombra = c;
    }

    public void setLineas(List<Linea> nuevas) {
        lineas.clear();
        lineas.addAll(nuevas);
        naturales = new Formula[lineas.size()];
        for (int i = 0; i < naturales.length; i++) {
            naturales[i] = MathRender.componer(lineas.get(i).nodo(), tam * lineas.get(i).escala());
        }
        anchoAjuste = -1;
        revalidate();
        repaint();
    }

    private float espacioEntreLineas() {
        return tam * 0.45f;
    }

    @Override
    public Dimension getPreferredSize() {
        float alto = 2 * MARGEN;
        for (int i = 0; i < naturales.length; i++) {
            alto += naturales[i].alto();
            if (i > 0) {
                alto += espacioEntreLineas();
            }
        }
        return new Dimension(200, Math.max(altoMinimo, (int) Math.ceil(alto)));
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(40, altoMinimo);
    }

    private void ajustar(int ancho) {
        if (anchoAjuste == ancho && ajustadas.length == naturales.length) {
            return;
        }
        anchoAjuste = ancho;
        ajustadas = new Formula[naturales.length];
        float disponible = ancho - 2 * MARGEN - 2;
        for (int i = 0; i < naturales.length; i++) {
            Formula f = naturales[i];
            if (f.ancho > disponible && disponible > 20) {
                float k = Math.max(0.55f, disponible / f.ancho);
                f = MathRender.componer(lineas.get(i).nodo(), tam * lineas.get(i).escala() * k);
            }
            ajustadas[i] = f;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        ajustar(getWidth());
        Graphics2D g2 = (Graphics2D) g.create();
        float y = MARGEN;
        for (int i = 0; i < ajustadas.length; i++) {
            Formula f = ajustadas[i];
            if (sombra != null) {
                g2.setColor(sombra);
                f.pintar(g2, MARGEN, y + f.asc + 2);
            }
            g2.setColor(lineas.get(i).color());
            f.pintar(g2, MARGEN, y + f.asc);
            y += f.alto() + espacioEntreLineas();
        }
        g2.dispose();
    }
}
