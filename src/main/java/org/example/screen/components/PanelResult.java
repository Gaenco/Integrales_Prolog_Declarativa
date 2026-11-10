package org.example.screen.components;

import org.example.screen.Theme;
import org.example.screen.components.MathView.Linea;

import javax.swing.*;
import java.awt.*;
import java.util.List;


public class PanelResult extends BCard {

    private static final String TEXTO = "texto";
    private static final String MATEMATICA = "math";

    private final PixelLabel lblTexto;
    private final MathView vista;
    private final ScoreView score;
    private final CardLayout cartas = new CardLayout();
    private final JPanel contenido = new JPanel(cartas);
    private String plano = "";

    public PanelResult(String titulo, Color colorTitulo, int alto, int anchoTexto, float tamMath, boolean conScore) {
        super(new BorderLayout(), Estilo.PANEL, titulo, colorTitulo);

        lblTexto = new PixelLabel("", 2);
        lblTexto.setAnchoEnvoltura(anchoTexto);
        lblTexto.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel panelTexto = new JPanel(new BorderLayout());
        panelTexto.setOpaque(false);
        panelTexto.add(lblTexto, BorderLayout.NORTH);

        vista = new MathView(tamMath, 40);
        vista.setSombra(Theme.alfa(Theme.OUTLINE, 220));
        score = conScore ? new ScoreView() : null;

        JPanel panelMath = new JPanel();
        panelMath.setLayout(new BoxLayout(panelMath, BoxLayout.Y_AXIS));
        panelMath.setOpaque(false);
        if (score != null) {
            score.setAlignmentX(Component.LEFT_ALIGNMENT);
            panelMath.add(score);
            panelMath.add(Box.createVerticalStrut(10));
        }
        vista.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelMath.add(vista);

        contenido.setOpaque(false);
        contenido.add(panelTexto, TEXTO);
        contenido.add(panelMath, MATEMATICA);
        add(contenido, BorderLayout.CENTER);

        setPreferredSize(new Dimension(100, alto));
        setMinimumSize(new Dimension(100, alto));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, alto));
    }

    public void setTexto(String texto, Color color) {
        plano = texto;
        lblTexto.setTexto(texto);
        lblTexto.setColor(color);
        cartas.show(contenido, TEXTO);
    }

    public void setMatematica(List<Linea> lineas, String textoPlano) {
        plano = textoPlano;
        vista.setLineas(lineas);
        cartas.show(contenido, MATEMATICA);
    }

    public ScoreView score() {
        return score;
    }

    public String getTexto() {
        return plano;
    }
}
