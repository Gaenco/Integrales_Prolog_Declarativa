package org.example.screen;

import java.awt.Color;
import java.awt.Font;


public final class Theme {

    private Theme() {
    }


    public static final Color OUTLINE = new Color(0x1B2426);
    public static final Color PANEL = new Color(0x374244);
    public static final Color PANEL_DARK = new Color(0x2A3436);
    public static final Color PANEL_LIGHT = new Color(0x4F6367);
    public static final Color FELT = new Color(0x2F6B58);
    public static final Color FELT_DARK = new Color(0x1F4A3C);
    public static final Color FIELD = new Color(0x161E20);
    public static final Color CREAM = new Color(0xF2EBD8);
    public static final Color INK = new Color(0x2B2D3A);

    public static final Color RED = new Color(0xFE5F55);
    public static final Color RED_DARK = new Color(0xA5322B);
    public static final Color BLUE = new Color(0x009DFF);
    public static final Color BLUE_DARK = new Color(0x0A5E96);
    public static final Color ORANGE = new Color(0xFF9A00);
    public static final Color ORANGE_DARK = new Color(0xA45F00);
    public static final Color GOLD = new Color(0xF3B958);
    public static final Color GOLD_DARK = new Color(0x9C6F1E);
    public static final Color GREEN = new Color(0x4BC292);
    public static final Color GREEN_DARK = new Color(0x2A7A58);
    public static final Color PURPLE = new Color(0x8867A5);
    public static final Color PURPLE_DARK = new Color(0x54406A);

    public static final Color TEXT = Color.WHITE;
    public static final Color MUTED = new Color(0xA9BDBD);
    public static final Color AMBER = new Color(0xFFD866);

    public static Color alfa(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    public static Color oscurecer(Color c, double f) {
        return new Color((int) (c.getRed() * (1 - f)), (int) (c.getGreen() * (1 - f)), (int) (c.getBlue() * (1 - f)));
    }

    public static Color aclarar(Color c, double f) {
        return new Color((int) (c.getRed() + (255 - c.getRed()) * f), (int) (c.getGreen() + (255 - c.getGreen()) * f),
                (int) (c.getBlue() + (255 - c.getBlue()) * f));
    }

    public static Font sans(int estilo, int tamano) {
        return new Font(Font.SANS_SERIF, estilo, tamano);
    }

    public static Font mono(int estilo, int tamano) {
        return new Font(Font.MONOSPACED, estilo, tamano);
    }

    public static Font serif(int estilo, int tamano) {
        return new Font(Font.SERIF, estilo, tamano);
    }
}
