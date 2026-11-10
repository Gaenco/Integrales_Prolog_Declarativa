package org.example.screen.components;

import org.example.screen.Theme;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class TextField extends JTextField {

    private final String ayuda;

    public TextField(String ayuda, int columnas, int tamFuente, boolean centrado) {
        super(columnas);
        this.ayuda = ayuda;

        setOpaque(false);
        setFont(Theme.mono(Font.BOLD, tamFuente));
        setForeground(Theme.TEXT);
        setCaretColor(Theme.ORANGE);
        setSelectionColor(Theme.alfa(Theme.ORANGE, 150));
        setSelectedTextColor(Color.WHITE);
        setBorder(crearBorde(Theme.PANEL_LIGHT));
        if (centrado) {
            setHorizontalAlignment(CENTER);
        }

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                setBorder(crearBorde(Theme.ORANGE));
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                setBorder(crearBorde(Theme.PANEL_LIGHT));
                repaint();
            }
        });
    }

    private static CompoundBorder crearBorde(Color color) {
        return new CompoundBorder(
                new LineBorder(Theme.OUTLINE, 1, true),
                new CompoundBorder(new LineBorder(color, 2, true), new EmptyBorder(7, 11, 7, 11)));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Theme.FIELD);
        g2.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 12, 12);
        g2.setColor(new Color(0, 0, 0, 90));                       // sombra interior superior
        g2.fillRoundRect(3, 3, getWidth() - 6, 5, 4, 4);
        g2.dispose();

        super.paintComponent(g);

        if (getText().isEmpty()) {
            Graphics2D g3 = (Graphics2D) g.create();
            g3.setColor(Theme.alfa(Theme.MUTED, 140));
            g3.setFont(getFont().deriveFont(Font.PLAIN));
            FontMetrics fm = g3.getFontMetrics();
            int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
            int x = getHorizontalAlignment() == CENTER ? (getWidth() - fm.stringWidth(ayuda)) / 2 : getInsets().left;
            g3.drawString(ayuda, x, y);
            g3.dispose();
        }
    }
}
