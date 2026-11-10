package org.example.screen.components;

import org.example.screen.Theme;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Keypad extends JPanel {

    public enum Accion { INSERTAR, BORRAR, LIMPIAR }

    private enum Tipo { NUMERO, OPERADOR, FUNCION, CONSTANTE, CONTROL }

    public record Tecla(String etiqueta, String ayuda, Accion accion, String texto,
                        int retroceso, boolean seleccionarPrimero) {
    }

    private record Def(Tecla tecla, Tipo tipo) {
    }

    private static Def ins(String et, String ayuda, String texto, Tipo t) {
        return new Def(new Tecla(et, ayuda, Accion.INSERTAR, texto, 0, false), t);
    }

    private static Def fun(String et, String ayuda, String texto, int retro) {
        return new Def(new Tecla(et, ayuda, Accion.INSERTAR, texto, retro, false), Tipo.FUNCION);
    }

    private static final Def[] FUNCIONES = {
            fun("sin", "Seno: sin(x)", "sin()", 1),
            fun("cos", "Coseno: cos(x)", "cos()", 1),
            fun("sinh", "Seno hiperbólico: sinh(x)", "sinh()", 1),
            fun("cosh", "Coseno hiperbólico: cosh(x)", "cosh()", 1),
            ins("e^x", "Exponencial natural: e^x", "e^x", Tipo.FUNCION),
            new Def(new Tecla("a^x", "Exponencial de base a: escribe el valor de a (ej: 2^x)",
                    Accion.INSERTAR, "2^x", 0, true), Tipo.FUNCION),
            ins("x^2", "x al cuadrado", "x^2", Tipo.FUNCION),
            fun("x^n", "x elevado a n: escribe el exponente entre paréntesis", "x^()", 1),
            ins("π", "Constante pi", "pi", Tipo.CONSTANTE),
            ins("e", "Constante de Euler", "e", Tipo.CONSTANTE),
            ins("1/x", "Uno entre x", "1/x", Tipo.FUNCION),
            ins("^", "Potencia", "^", Tipo.OPERADOR),
            ins("x", "Variable de integración", "x", Tipo.CONSTANTE),
            ins("(", "Abrir paréntesis", "(", Tipo.OPERADOR),
            ins(")", "Cerrar paréntesis", ")", Tipo.OPERADOR),
            new Def(new Tecla("C", "Limpiar el campo activo", Accion.LIMPIAR, "", 0, false), Tipo.CONTROL),
    };

    private static final Def[] NUMEROS = {
            ins("7", "", "7", Tipo.NUMERO), ins("8", "", "8", Tipo.NUMERO),
            ins("9", "", "9", Tipo.NUMERO), ins("÷", "Dividir", "/", Tipo.OPERADOR),
            ins("4", "", "4", Tipo.NUMERO), ins("5", "", "5", Tipo.NUMERO),
            ins("6", "", "6", Tipo.NUMERO), ins("×", "Multiplicar", "*", Tipo.OPERADOR),
            ins("1", "", "1", Tipo.NUMERO), ins("2", "", "2", Tipo.NUMERO),
            ins("3", "", "3", Tipo.NUMERO), ins("-", "Restar", "-", Tipo.OPERADOR),
            ins("0", "", "0", Tipo.NUMERO), ins(".", "Punto decimal", ".", Tipo.NUMERO),
            new Def(new Tecla("DEL", "Borrar un carácter", Accion.BORRAR, "", 0, false), Tipo.CONTROL),
            ins("+", "Sumar", "+", Tipo.OPERADOR),
    };

    private final List<Buttons> botones = new ArrayList<>();

    public Keypad(Consumer<Tecla> alPulsar) {
        super(new GridLayout(1, 2, 18, 0));
        setOpaque(false);
        add(bloque("FUNCIONES", FUNCIONES, alPulsar));
        add(bloque("NÚMEROS Y OPERADORES", NUMEROS, alPulsar));
    }

    public void entrar(double retrasoInicial) {
        for (int i = 0; i < botones.size(); i++) {
            botones.get(i).entrar(retrasoInicial + (i % 16) * 0.025);
        }
    }

    private JPanel bloque(String titulo, Def[] teclas, Consumer<Tecla> alPulsar) {
        PixelLabel lbl = new PixelLabel(titulo, 2);
        lbl.setColor(Theme.CREAM);

        JPanel grid = new JPanel(new GridLayout(4, 4, 2, 2));
        grid.setOpaque(false);
        for (Def d : teclas) {
            Buttons b = boton(d, alPulsar);
            botones.add(b);
            grid.add(b);
        }

        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.add(lbl, BorderLayout.NORTH);
        p.add(grid, BorderLayout.CENTER);
        return p;
    }

    private Buttons boton(Def d, Consumer<Tecla> alPulsar) {
        Color base;
        switch (d.tipo()) {
            case NUMERO -> base = Theme.PANEL_LIGHT;
            case OPERADOR -> base = Theme.ORANGE;
            case FUNCION -> base = Theme.BLUE;
            case CONSTANTE -> base = Theme.GREEN;
            default -> base = Theme.RED;
        }
        Buttons b = new Buttons(d.tecla().etiqueta(), base, Theme.TEXT, 2, 4, 9);
        if (!d.tecla().ayuda().isEmpty()) {
            b.setToolTipText(d.tecla().ayuda());
        }
        b.addActionListener(e -> alPulsar.accept(d.tecla()));
        return b;
    }
}
