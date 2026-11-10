package org.example.screen;

import org.example.Connection;
import org.example.Connection.Resultado;
import org.example.screen.components.*;
import org.example.screen.components.MathRender.Node;
import org.example.screen.components.MathView.Linea;
import org.example.screen.components.TextField;
import org.example.screen.fx.Sfx;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.example.screen.components.MathRender.*;

public class Window extends JFrame {

    private static final String[][] EJEMPLOS = {
            {"3x^2", "3*x^2", "0", "2"},
            {"x^2-3x+2", "x^2 - 3*x + 2", "0", "3"},
            {"sin+cos", "sin(x) + cos(x)", "0", "pi"},
            {"2^x", "2^x", "0", "3"},
            {"tan x", "tan(x)", "0", "1"},
            {"atan x", "atan(x)", "0", "1"}
    };

    private static final Pattern PERMITIDO = Pattern.compile("[0-9A-Za-z+\\-*/^().\\s]{1,200}");
    private static final Pattern CON_APROX = Pattern.compile("^(.*?)\\s*\\(aprox\\. (.*)\\)$");

    private final Connection motor;

    private final TextField txtFuncion = new TextField("f(x)   ej: 3*x^2 + sin(x)", 20, 20, false);
    private final TextField txtA = new TextField("a", 4, 17, true);
    private final TextField txtB = new TextField("b", 4, 17, true);
    private JTextField activo = txtFuncion;

    private final MathView vista = new MathView(19f, 40);
    private final PanelResult panelIntegral =
            new PanelResult("INTEGRAL INDEFINIDA", Theme.PURPLE, 178, 336, 21f, false);
    private final PanelResult panelArea =
            new PanelResult("ÁREA BAJO LA CURVA", Theme.ORANGE, 304, 336, 16f, true);
    private final PixelLabel lblEstado = new PixelLabel(" ", 2);

    private final Buttons btnCalcular = new Buttons("CALCULAR", Theme.BLUE, Theme.TEXT, 3, 26, 14);
    private final Buttons btnLimpiar = new Buttons("LIMPIAR TODO", Theme.RED, Theme.TEXT, 3, 20, 14);
    private final Buttons btnCopiar = new Buttons("COPIAR", Theme.PANEL_LIGHT, Theme.TEXT, 2, 14, 12);
    private final Buttons btnSoportadas = new Buttons("SOPORTADAS", Theme.ORANGE, Theme.TEXT, 2, 12, 10);
    private final Buttons btnSonido = new Buttons("SONIDO: SI", Theme.GREEN, Theme.TEXT, 2, 10, 10);

    private BCard banner;
    private BCard felt;
    private Keypad teclado;
    private boolean introHecha = false;

    public Window(Connection motor) {
        super("Integrales en Prolog");
        this.motor = motor;

        txtFuncion.setText("x^2");
        txtA.setText("0");
        txtB.setText("3");

        for (TextField t : new TextField[]{txtFuncion, txtA, txtB}) {
            t.addActionListener(e -> calcular());
            t.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    activo = t;
                }
            });
        }
        txtFuncion.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                actualizarVista();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                actualizarVista();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                actualizarVista();
            }
        });

        btnCalcular.setSonido(Sfx.Id.JUGAR);
        btnLimpiar.setSonido(Sfx.Id.DESCARTAR);
        btnCalcular.addActionListener(e -> calcular());
        btnLimpiar.addActionListener(e -> limpiarTodo());
        btnCopiar.addActionListener(e -> copiarResultado());
        btnSoportadas.addActionListener(e -> new SoportadasDialog(this).setVisible(true));
        btnSonido.addActionListener(e -> {
            Sfx.setSilenciado(!Sfx.isSilenciado());
            actualizarBotonSonido();
        });
        btnLimpiar.setToolTipText("Borra la función, los límites y los resultados");
        btnSoportadas.setToolTipText("Las 15 integrales que sabe resolver");
        btnSonido.setToolTipText("Activar o silenciar los efectos de sonido");

        vista.setSombra(Theme.alfa(Theme.OUTLINE, 230));
        actualizarVista();

        JPanel izquierda = new JPanel();
        izquierda.setLayout(new BoxLayout(izquierda, BoxLayout.Y_AXIS));
        izquierda.setOpaque(false);
        agregar(izquierda, crearBanner(), 10);
        agregar(izquierda, panelIntegral, 10);
        agregar(izquierda, panelArea, 10);
        izquierda.add(Box.createVerticalGlue());
        agregar(izquierda, crearBotonesIzquierda(), 0);
        izquierda.setPreferredSize(new Dimension(372, 600));

        felt = crearMesa();

        BalatroBackground raiz = new BalatroBackground(new BorderLayout(18, 0));
        raiz.setBorder(new EmptyBorder(16, 20, 12, 20));
        raiz.add(izquierda, BorderLayout.WEST);
        raiz.add(felt, BorderLayout.CENTER);

        setContentPane(raiz);
        setGlassPane(new CrtOverlay());
        getGlassPane().setVisible(true);
        setMinimumSize(new Dimension(1120, 700));
        pack();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        mostrarInicial();
    }

    private BCard crearBanner() {
        banner = new BCard(new BorderLayout());
        banner.setBorder(new EmptyBorder(14, 16, 6 + 3 + 8, 16));
        PixelLabel titulo = new PixelLabel("INTEGRALES", 4);
        titulo.setColor(Theme.GOLD);
        titulo.setOla(true);
        PixelLabel sub = new PixelLabel("Reglas en SWI-Prolog", 2);
        sub.setColor(Theme.MUTED);
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setOpaque(false);
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        col.add(titulo);
        col.add(Box.createVerticalStrut(2));
        col.add(sub);
        banner.add(col, BorderLayout.CENTER);
        banner.setPreferredSize(new Dimension(100, 104));
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 104));
        return banner;
    }

    private JPanel crearBotonesIzquierda() {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.setOpaque(false);
        p.add(btnSoportadas, BorderLayout.WEST);
        p.add(btnSonido, BorderLayout.EAST);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height));
        return p;
    }

    private BCard crearMesa() {
        BCard mesa = new BCard(new BorderLayout(), BCard.Estilo.FIELTRO, null, null);
        mesa.setBorder(new EmptyBorder(18, 22, 6 + 3 + 14, 22));
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setOpaque(false);

        PixelLabel tu = new PixelLabel("TU INTEGRAL: escribe en los recuadros o usa el teclado", 2);
        tu.setColor(Theme.CREAM);
        agregar(col, tu, 6);
        agregar(col, crearFilaIntegral(), 4);
        agregar(col, vista, 4);
        agregar(col, crearEjemplos(), 12);

        teclado = new Keypad(this::pulsar);
        agregar(col, teclado, 12);
        agregar(col, crearBotonesAccion(), 6);
        lblEstado.setAnchoEnvoltura(640);
        agregar(col, lblEstado, 0);

        mesa.add(col, BorderLayout.CENTER);
        return mesa;
    }

    private JPanel crearFilaIntegral() {
        JPanel fila = new JPanel(new GridBagLayout());
        fila.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.VERTICAL;
        c.weighty = 1;
        c.gridx = 0;
        fila.add(new IntegralSign(70, 140), c);

        txtA.setToolTipText("Límite inferior a");
        txtB.setToolTipText("Límite superior b");
        for (TextField t : new TextField[]{txtA, txtB}) {
            t.setMaximumSize(t.getPreferredSize());
            t.setMinimumSize(t.getPreferredSize());
            t.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        JPanel lim = new JPanel();
        lim.setLayout(new BoxLayout(lim, BoxLayout.Y_AXIS));
        lim.setOpaque(false);
        lim.add(txtB);
        lim.add(Box.createVerticalGlue());
        lim.add(txtA);
        c.gridx = 1;
        c.insets = new Insets(0, 0, 0, 12);
        fila.add(lim, c);


        txtFuncion.setMinimumSize(new Dimension(200, txtFuncion.getPreferredSize().height));
        c.gridx = 2;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(0, 0, 0, 10);
        fila.add(txtFuncion, c);

        PixelLabel dx = new PixelLabel("dx", 5);
        dx.setColor(Theme.GOLD);
        c.gridx = 3;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 0, 0, 0);
        fila.add(dx, c);
        fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 148));
        return fila;
    }

    private JPanel crearEjemplos() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setOpaque(false);
        PixelLabel lbl = new PixelLabel("EJEMPLOS:", 2);
        lbl.setColor(Theme.CREAM);
        p.add(lbl);
        for (String[] ej : EJEMPLOS) {
            Buttons chip = new Buttons(ej[0], Theme.PANEL, Theme.TEXT, 2, 6, 5);
            chip.addActionListener(e -> {
                txtFuncion.setText(ej[1]);
                txtA.setText(ej[2]);
                txtB.setText(ej[3]);
                txtFuncion.requestFocusInWindow();
            });
            p.add(chip);
        }
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height + 4));
        return p;
    }

    private JPanel crearBotonesAccion() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.setOpaque(false);
        btnCalcular.setAlignmentY(Component.CENTER_ALIGNMENT);
        btnLimpiar.setAlignmentY(Component.CENTER_ALIGNMENT);
        btnCopiar.setAlignmentY(Component.CENTER_ALIGNMENT);
        p.add(btnCalcular);
        p.add(Box.createHorizontalStrut(6));
        p.add(btnLimpiar);
        p.add(Box.createHorizontalGlue());
        p.add(btnCopiar);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height + 2));
        return p;
    }

    @Override
    public void setVisible(boolean v) {
        super.setVisible(v);
        if (v && !introHecha) {
            introHecha = true;
            banner.entrar(0.00);
            panelIntegral.entrar(0.12);
            panelArea.entrar(0.24);
            felt.entrar(0.05);
            teclado.entrar(0.35);
            btnSoportadas.entrar(0.45);
            btnSonido.entrar(0.52);
            int[] n = {0};
            Timer t = new Timer(120, null);
            t.addActionListener(e -> {
                Sfx.play(Sfx.Id.CARTA);
                if (++n[0] >= 3) {
                    t.stop();
                }
            });
            t.start();
            txtFuncion.requestFocusInWindow();
        }
    }

    private void actualizarBotonSonido() {
        boolean on = !Sfx.isSilenciado();
        btnSonido.setText(on ? "SONIDO: SI" : "SONIDO: NO");
        btnSonido.setColores(on ? Theme.GREEN : Theme.PANEL_LIGHT, Theme.TEXT);
        btnSonido.rebotar();
    }

    private void pulsar(Keypad.Tecla t) {
        JTextField f = activo;
        f.requestFocusInWindow();
        switch (t.accion()) {
            case INSERTAR -> {
                f.replaceSelection(t.texto());
                int fin = f.getCaretPosition();
                if (t.seleccionarPrimero()) {
                    int ini = fin - t.texto().length();
                    f.select(ini, ini + 1);
                } else if (t.retroceso() > 0) {
                    f.setCaretPosition(fin - t.retroceso());
                }
            }
            case BORRAR -> borrar(f);
            case LIMPIAR -> f.setText("");
        }
    }

    private static void borrar(JTextField f) {
        try {
            int s = f.getSelectionStart();
            int e = f.getSelectionEnd();
            if (s != e) {
                f.getDocument().remove(s, e - s);
            } else {
                int p = f.getCaretPosition();
                if (p > 0) {
                    f.getDocument().remove(p - 1, 1);
                }
            }
        } catch (BadLocationException ignorada) {
        }
    }

    private void limpiarTodo() {
        txtFuncion.setText("");
        txtA.setText("");
        txtB.setText("");
        mostrarInicial();
        felt.sacudir();
        panelIntegral.pulso();
        panelArea.pulso();
        lblEstado.setColor(Theme.CREAM);
        lblEstado.setTexto("Todo limpio. Escribe una nueva integral.");
        txtFuncion.requestFocusInWindow();
    }

    private void actualizarVista() {
        String t = txtFuncion.getText().trim();
        if (t.isEmpty()) {
            vista.setLineas(List.of());
        } else {
            Node n = fila(texto("Se lee:"), esp(0.5f), parse(t));
            vista.setLineas(List.of(new Linea(n, 1f, Theme.TEXT)));
        }
    }

    private void calcular() {
        String f = txtFuncion.getText().trim();
        String a = txtA.getText().trim();
        String b = txtB.getText().trim();

        if (f.isEmpty() || a.isEmpty() || b.isEmpty()) {
            mostrarError("Escribe la función y los dos límites (a y b).");
            return;
        }
        if (!PERMITIDO.matcher(f).matches() || !PERMITIDO.matcher(a).matches()
                || !PERMITIDO.matcher(b).matches()) {
            mostrarError("Hay caracteres no permitidos. Usa punto (no coma) para los decimales.");
            return;
        }

        btnCalcular.setText("CALCULANDO...");
        btnCalcular.setEnabled(false);
        lblEstado.setColor(Theme.CREAM);
        lblEstado.setTexto("Consultando a SWI-Prolog...");

        new SwingWorker<Resultado, Void>() {
            @Override
            protected Resultado doInBackground() throws Exception {
                return motor.calcular(f, a, b);
            }

            @Override
            protected void done() {
                btnCalcular.setText("CALCULAR");
                btnCalcular.setEnabled(true);
                try {
                    mostrar(get(), a, b);
                } catch (Exception ex) {
                    Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
                    mostrarError("No se pudo consultar SWI-Prolog: " + causa.getMessage());
                }
            }
        }.execute();
    }

    private void mostrarInicial() {
        panelIntegral.setTitulo("INTEGRAL INDEFINIDA");
        panelIntegral.setColorTitulo(Theme.PURPLE);
        panelIntegral.setTexto("Aquí aparecerá la antiderivada:\n∫ f(x) dx = F(x) + C", Theme.MUTED);
        panelArea.setTitulo("ÁREA BAJO LA CURVA");
        panelArea.setTexto("Aquí verás el área entre a y b, calculada como F(b) - F(a).", Theme.MUTED);
        lblEstado.setColor(Theme.CREAM);
        lblEstado.setTexto("Pulsa CALCULAR o la tecla Enter.");
    }

    private void mostrar(Resultado r, String a, String b) {
        if (!r.ok()) {
            mostrarError(r.error());
            return;
        }
        Node f = parse(r.funcion());
        Node F = r.integral().equals("0")
                ? variable("C")
                : fila(parse(r.integral()), op("+"), variable("C"));
        Node linea1 = fila(integral(null, null), envolverSiSuma(f), dx());
        Node linea2 = fila(rel("="), F);
        String planoIntegral = "∫ (" + Pretty.formatear(r.funcion()) + ") dx = "
                + (r.integral().equals("0") ? "C" : Pretty.formatear(r.integral()) + " + C");

        panelIntegral.setTitulo("INTEGRAL INDEFINIDA");
        panelIntegral.setColorTitulo(Theme.PURPLE);
        panelIntegral.setMatematica(List.of(
                new Linea(linea1, 1f, Theme.TEXT),
                new Linea(linea2, 1f, Theme.TEXT)), planoIntegral);

        Node cabecera = fila(texto("Área"), rel("="), valorNodo(r.area()));
        Node geo = fila(texto("Área de |f| ≈ "), valorNodo(r.areaAbs()));
        String planoArea = "Área = " + valor(r.area()) + "\nF(" + Pretty.formatear(b) + ") = " + valor(r.fb())
                + "\nF(" + Pretty.formatear(a) + ") = " + valor(r.fa())
                + "\nÁrea geométrica de |f| ≈ " + Pretty.formatear(r.areaAbs());

        panelArea.setTitulo("ÁREA DE x = " + etiqueta(a) + " A x = " + etiqueta(b));
        panelArea.setMatematica(List.of(
                new Linea(cabecera, 1f, Theme.TEXT),
                new Linea(geo, 0.85f, Theme.MUTED)), planoArea);
        panelArea.score().setValores("F(" + etiqueta(b) + ")", r.fb(), "F(" + etiqueta(a) + ")", r.fa(), r.area(), true);

        panelIntegral.pulso();
        panelArea.pulso();
        lblEstado.setColor(Theme.GREEN);
        lblEstado.setTexto("¡Listo! Área calculada.");
    }

    private void mostrarError(String mensaje) {
        panelIntegral.setTitulo("NO SE PUDO RESOLVER");
        panelIntegral.setColorTitulo(Theme.RED);
        panelIntegral.setTexto(resumir(mensaje), Theme.AMBER);
        panelArea.setTitulo("ÁREA BAJO LA CURVA");
        panelArea.setTexto("Sin resultado. Corrige la integral y pulsa CALCULAR.", Theme.MUTED);
        panelIntegral.sacudir();
        Sfx.play(Sfx.Id.ERROR);
        lblEstado.setColor(Theme.AMBER);
        lblEstado.setTexto("Revisa la expresión o los límites.");
    }


    private static String resumir(String m) {
        int i = m.indexOf("Se admiten");
        if (i > 0) {
            m = m.substring(0, i).trim() + " Mira SOPORTADAS.";
        }
        return m.length() > 150 ? m.substring(0, 147) + "..." : m;
    }


    private static String etiqueta(String s) {
        String t = s.replaceAll("\\bpi\\b", "π").replace(" ", "");
        return t.length() > 7 ? t.substring(0, 6) + "." : t;
    }

    private static Node valorNodo(String s) {
        Matcher m = CON_APROX.matcher(s);
        if (m.matches()) {
            return fila(parse(m.group(1)), rel("≈"), parse(m.group(2)));
        }
        return parse(s);
    }

    private static String valor(String s) {
        return Pretty.formatear(s).replaceAll("\\s*\\(aprox\\. ([^)]*)\\)", " ≈ $1");
    }

    private void copiarResultado() {
        String t = panelIntegral.getTexto();
        if (t.isEmpty() || !t.startsWith("∫")) {
            return;
        }
        t += "\n" + panelArea.getTexto();
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(t), null);
        lblEstado.setColor(Theme.BLUE);
        lblEstado.setTexto("Copiado al portapapeles.");
    }

    private static void agregar(JComponent panel, JComponent c, int espacioDespues) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(c);
        if (espacioDespues > 0) {
            panel.add(Box.createVerticalStrut(espacioDespues));
        }
    }
}
