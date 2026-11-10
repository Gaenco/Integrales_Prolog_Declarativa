package org.example;

import org.example.screen.Theme;
import org.example.screen.Window;

import javax.swing.*;
import java.awt.Font;

public class Main {
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "off");
        System.setProperty("swing.aatext", "false");

        Connection motor;
        try {
            motor = new Connection();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage(),
                    "No se pudo iniciar SWI-Prolog", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return;
        }
        final Connection m = motor;
        SwingUtilities.invokeLater(() -> {
            estilizarTooltips();
            new Window(m).setVisible(true);
        });
    }

    public static void estilizarTooltips() {
        UIManager.put("ToolTip.background", Theme.OUTLINE);
        UIManager.put("ToolTip.foreground", Theme.TEXT);
        UIManager.put("ToolTip.border", BorderFactory.createLineBorder(Theme.PANEL_LIGHT, 2));
        UIManager.put("ToolTip.font", Theme.sans(Font.BOLD, 12));
    }
}
