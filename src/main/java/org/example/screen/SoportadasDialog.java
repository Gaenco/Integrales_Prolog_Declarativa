package org.example.screen;

import org.example.screen.components.*;
import org.example.screen.components.CardTable.CartaDef;
import org.example.screen.components.MathRender.Node;
import org.example.screen.fx.Sfx;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;

import static org.example.screen.components.MathRender.*;

public class SoportadasDialog extends JDialog {

    private final PixelLabel lblTitulo = new PixelLabel("", 3);
    private final PixelLabel lblNota = new PixelLabel("", 2);

    public SoportadasDialog(Frame owner) {
        super(owner, "Integrales soportadas", true);

        BalatroBackground raiz = new BalatroBackground(new BorderLayout(0, 12));
        raiz.setBorder(new EmptyBorder(14, 22, 14, 22));
        raiz.add(crearEncabezado(), BorderLayout.NORTH);

        CardTable mesa = new CardTable(cartas());
        mesa.setAlCambiar(this::mostrarNota);
        BCard felt = new BCard(new BorderLayout(), BCard.Estilo.FIELTRO, null, null);
        felt.setBorder(new EmptyBorder(8, 10, 16, 10));
        felt.add(mesa, BorderLayout.CENTER);
        raiz.add(felt, BorderLayout.CENTER);
        raiz.add(crearPie(), BorderLayout.SOUTH);

        setContentPane(raiz);
        setGlassPane(new CrtOverlay());
        getGlassPane().setVisible(true);
        setSize(960, 730);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        mostrarNota(null);
    }

    private static List<CartaDef> cartas() {
        String basicas = "Básicas";
        String lineal = "Linealidad";
        String expo = "Exponencial e hiperbólicas";
        String trigo = "Trigonométricas";
        return List.of(
                carta("a", "k", "k*x", 'D', basicas, "k es cualquier constante numérica: 5, 2.5, π, e, sqrt(2)..."),
                carta("b", "x", "x^2/2", 'D', basicas, "Caso particular de x^n con n = 1."),
                carta("c", "k*x", "k*x^2/2", 'D', basicas, "k es una constante numérica."),
                carta("d", "x^n", "x^(n+1)/(n+1)", 'D', basicas,
                        "n entero. Si n = -1 el resultado es ln|x|. Con n negativo el intervalo no puede contener x = 0."),
                new CartaDef("f", List.of(fila(integral(null, null), parse("k*u"), dx()),
                        fila(rel("="), variable("k"), esp(0.1f), integral(null, null), variable("u"), dx())),
                        'C', lineal, "La constante sale de la integral. También se admiten u*k, u/k y -u."),
                suma("g", "+"),
                suma("h", "−"),
                carta("i", "a^x", "a^x/ln(a)", 'H', expo, "a > 0 y a distinto de 1. También se acepta e^x y exp(x)."),
                carta("k", "sin(x)", "-cos(x)", 'S', trigo, "Seno."),
                carta("l", "cos(x)", "sin(x)", 'S', trigo, "Coseno."),
                carta("m", "tan(x)", "-ln|cos(x)|", 'S', trigo,
                        "El intervalo no puede contener x = π/2 + kπ, donde tan(x) tiene asíntotas."),
                new CartaDef("p", List.of(fila(integral(null, null), parse("atan(x)"), dx()),
                        fila(rel("="), parse("x*atan(x)")),
                        fila(texto("−"), esp(0.15f), parse("ln(1 + x^2)/2"), op("+"), variable("C"))),
                        'S', trigo, "Se resuelve por partes. Se puede escribir atan(x) o arctan(x)."),
                carta("q", "sinh(x)", "cosh(x)", 'H', expo, "Seno hiperbólico. También se escribe sh(x)."),
                carta("r", "cosh(x)", "sinh(x)", 'H', expo, "Coseno hiperbólico."),
                carta("s", "tanh(x)", "ln(cosh(x))", 'H', expo, "Tangente hiperbólica."));
    }

    private static CartaDef carta(String letra, String f, String F, char palo, String nombrePalo, String nota) {
        Node l1 = fila(integral(null, null), envolverSiSuma(parse(f)), dx());
        Node l2 = fila(rel("="), parse(F), op("+"), variable("C"));
        return new CartaDef(letra, List.of(l1, l2), palo, nombrePalo, nota);
    }

    private static CartaDef suma(String letra, String signo) {
        Node l1 = fila(integral(null, null), envolverSiSuma(fila(variable("u"), op(signo), variable("v"))), dx());
        Node l2 = fila(rel("="), integral(null, null), variable("u"), dx(), op(signo),
                integral(null, null), variable("v"), dx());
        return new CartaDef(letra, List.of(l1, l2), 'C', "Linealidad", "u y v pueden ser cualquier combinación de las reglas.");
    }

    private JPanel crearEncabezado() {
        PixelLabel titulo = new PixelLabel("COLECCIÓN: 15 INTEGRALES", 4);
        titulo.setColor(Theme.GOLD);
        titulo.setOla(true);
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        PixelLabel sub = new PixelLabel("Pasa el mouse sobre una carta. k y a son constantes; u y v, cualquier combinación de las reglas.", 2);
        sub.setColor(Theme.CREAM);
        sub.setAnchoEnvoltura(880);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.add(titulo);
        p.add(Box.createVerticalStrut(6));
        p.add(sub);
        return p;
    }

    private JPanel crearPie() {
        BCard nota = new BCard(new BorderLayout(0, 6));
        nota.setBorder(new EmptyBorder(12, 18, 6 + 3 + 10, 18));
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblNota.setAnchoEnvoltura(640);
        lblNota.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel textos = new JPanel();
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        textos.setOpaque(false);
        textos.add(lblTitulo);
        textos.add(Box.createVerticalStrut(6));
        textos.add(lblNota);
        nota.add(textos, BorderLayout.CENTER);

        Buttons cerrar = new Buttons("CERRAR", Theme.ORANGE, Theme.TEXT, 3, 22, 14);
        cerrar.addActionListener(e -> dispose());

        JPanel p = new JPanel(new BorderLayout(16, 0));
        p.setOpaque(false);
        p.add(nota, BorderLayout.CENTER);
        p.add(cerrar, BorderLayout.EAST);
        p.setPreferredSize(new Dimension(100, 104));
        return p;
    }

    private void mostrarNota(CartaDef d) {
        if (d == null) {
            lblTitulo.setTexto("Elige una carta");
            lblTitulo.setColor(Theme.GOLD);
            lblNota.setTexto("Cada carta es una integral que el sistema sabe resolver.");
            lblNota.setColor(Theme.TEXT);
            return;
        }
        lblTitulo.setTexto("(" + d.letra() + ")  " + d.nombrePalo().toUpperCase());
        lblTitulo.setColor(d.palo() == 'D' || d.palo() == 'H' ? Theme.RED : Theme.BLUE);
        lblNota.setTexto(d.nota());
        lblNota.setColor(Theme.TEXT);
    }
}
