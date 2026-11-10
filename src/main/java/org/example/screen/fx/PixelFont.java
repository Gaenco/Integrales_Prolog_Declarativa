package org.example.screen.fx;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public final class PixelFont {

    private PixelFont() {
    }

    public static final int FILAS = 9;
    private static final int TOPE = 2;

    private static final String[] ASCII = {
            "00 00 00 00 00", "00 00 5F 00 00", "00 07 00 07 00", "14 7F 14 7F 14", "24 2A 7F 2A 12",
            "23 13 08 64 62", "36 49 55 22 50", "00 05 03 00 00", "00 1C 22 41 00", "00 41 22 1C 00",
            "14 08 3E 08 14", "08 08 3E 08 08", "00 50 30 00 00", "08 08 08 08 08", "00 60 60 00 00",
            "20 10 08 04 02", "3E 51 49 45 3E", "00 42 7F 40 00", "42 61 51 49 46", "21 41 45 4B 31",
            "18 14 12 7F 10", "27 45 45 45 39", "3C 4A 49 49 30", "01 71 09 05 03", "36 49 49 49 36",
            "06 49 49 29 1E", "00 36 36 00 00", "00 56 36 00 00", "08 14 22 41 00", "14 14 14 14 14",
            "00 41 22 14 08", "02 01 51 09 06", "32 49 79 41 3E", "7E 11 11 11 7E", "7F 49 49 49 36",
            "3E 41 41 41 22", "7F 41 41 22 1C", "7F 49 49 49 41", "7F 09 09 09 01", "3E 41 49 49 7A",
            "7F 08 08 08 7F", "00 41 7F 41 00", "20 40 41 3F 01", "7F 08 14 22 41", "7F 40 40 40 40",
            "7F 02 0C 02 7F", "7F 04 08 10 7F", "3E 41 41 41 3E", "7F 09 09 09 06", "3E 41 51 21 5E",
            "7F 09 19 29 46", "46 49 49 49 31", "01 01 7F 01 01", "3F 40 40 40 3F", "1F 20 40 20 1F",
            "3F 40 38 40 3F", "63 14 08 14 63", "07 08 70 08 07", "61 51 49 45 43", "00 7F 41 41 00",
            "02 04 08 10 20", "00 41 41 7F 00", "04 02 01 02 04", "40 40 40 40 40", "00 01 02 04 00",
            "20 54 54 54 78", "7F 48 44 44 38", "38 44 44 44 20", "38 44 44 48 7F", "38 54 54 54 18",
            "08 7E 09 01 02", "08 14 54 54 3C", "7F 08 04 04 78", "00 44 7D 40 00", "20 40 44 3D 00",
            "00 7F 10 28 44", "00 41 7F 40 00", "7C 04 18 04 78", "7C 08 04 04 78", "38 44 44 44 38",
            "7C 14 14 14 08", "08 14 14 18 7C", "7C 08 04 04 08", "48 54 54 54 20", "04 3F 44 40 20",
            "3C 40 40 20 7C", "1C 20 40 20 1C", "3C 40 30 40 3C", "44 28 10 28 44", "0C 50 50 50 3C",
            "44 64 54 4C 44", "00 08 36 41 00", "00 00 7F 00 00", "00 41 36 08 00", "10 08 08 10 08"
    };

    private static final Map<Character, int[]> GLIFOS = new HashMap<>();

    static {
        for (int i = 0; i < ASCII.length; i++) {
            String[] hex = ASCII[i].split(" ");
            int[] cols = new int[hex.length];
            for (int c = 0; c < hex.length; c++) {
                cols[c] = Integer.parseInt(hex[c], 16) << TOPE;
            }
            GLIFOS.put((char) (32 + i), cols);
        }

        GLIFOS.put('¡', volteado(GLIFOS.get('!')));
        GLIFOS.put('¿', volteado(GLIFOS.get('?')));

        // vocales con tilde, diéresis y eñe
        String vocales = "aeiouAEIOU";
        String acentos = "áéíóúÁÉÍÓÚ";
        for (int i = 0; i < vocales.length(); i++) {
            GLIFOS.put(acentos.charAt(i), conMarca(vocales.charAt(i), '\'', Character.isUpperCase(vocales.charAt(i))));
        }
        GLIFOS.put('ñ', conMarca('n', '~', false));
        GLIFOS.put('Ñ', conMarca('N', '~', true));
        GLIFOS.put('ü', conMarca('u', '"', false));
        GLIFOS.put('Ü', conMarca('U', '"', true));


        GLIFOS.put('π', filas(4, "XXXXX", ".X.X.", ".X.X.", ".X.X.", ".X..X"));
        GLIFOS.put('×', filas(3, "X...X", ".X.X.", "..X..", ".X.X.", "X...X"));
        GLIFOS.put('÷', filas(3, "..X..", ".....", "XXXXX", ".....", "..X.."));
        GLIFOS.put('·', filas(5, "X"));
        GLIFOS.put('°', filas(0, ".X.", "X.X", ".X."));
        GLIFOS.put('∫', filas(0, "...XX", "..X..", "..X..", "..X..", "..X..", "..X..", "..X..", "..X..", "XX..."));
        GLIFOS.put('−', GLIFOS.get('-'));
        GLIFOS.put('≈', GLIFOS.get('~'));
    }

    private static int[] volteado(int[] g) {
        int[] r = new int[g.length];
        for (int c = 0; c < g.length; c++) {
            int v = g[c] >> TOPE;
            int inv = 0;
            for (int b = 0; b < 7; b++) {
                if ((v & (1 << b)) != 0) {
                    inv |= 1 << (6 - b);
                }
            }
            r[c] = inv << TOPE;
        }
        return r;
    }

    private static int[] filas(int fila0, String... rows) {
        int[] cols = new int[rows[0].length()];
        for (int r = 0; r < rows.length; r++) {
            for (int c = 0; c < cols.length; c++) {
                if (rows[r].charAt(c) == 'X') {
                    cols[c] |= 1 << (fila0 + r);
                }
            }
        }
        return cols;
    }

    private static int[] conMarca(char base, char marca, boolean mayus) {
        int[] g = GLIFOS.get(base).clone();
        if (base == 'i' || base == 'j') {
            for (int c = 0; c < g.length; c++) {
                g[c] &= ~(1 << TOPE);
            }
        }
        int f = mayus ? 0 : 1;
        int[][] px = switch (marca) {
            case '\'' -> new int[][]{{3, f}, {2, f + 1}};
            case '~' -> new int[][]{{1, f}, {2, f}, {4, f}, {0, f + 1}, {3, f + 1}};
            default -> new int[][]{{1, f + 1}, {3, f + 1}};
        };
        for (int[] p : px) {
            g[p[0]] |= 1 << p[1];
        }
        return g;
    }

    private static int[] glifo(char c) {
        int[] g = GLIFOS.get(c);
        return g != null ? g : GLIFOS.get('?');
    }


    private static int[] extremos(int[] g) {
        int ini = -1;
        int fin = -1;
        for (int c = 0; c < g.length; c++) {
            if (g[c] != 0) {
                if (ini < 0) {
                    ini = c;
                }
                fin = c;
            }
        }
        return new int[]{ini, fin};
    }

    public static int avance(char c) {
        if (c == ' ') {
            return 3;
        }
        int[] e = extremos(glifo(c));
        return e[0] < 0 ? 3 : (e[1] - e[0] + 1) + 1;
    }

    public static int ancho(String s, int esc) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            n += avance(s.charAt(i));
        }
        return Math.max(0, n - 1) * esc;
    }

    public static int alto(int esc) {
        return FILAS * esc;
    }

    private static void pintar(Graphics2D g, String s, int x, int y, int esc, Color color) {
        g.setColor(color);
        int cx = x;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            int[] gl = glifo(ch);
            int[] e = extremos(gl);
            if (e[0] >= 0) {
                for (int c = e[0]; c <= e[1]; c++) {
                    for (int r = 0; r < FILAS; r++) {
                        if ((gl[c] & (1 << r)) != 0) {
                            g.fillRect(cx + (c - e[0]) * esc, y + r * esc, esc, esc);
                        }
                    }
                }
            }
            cx += avance(ch) * esc;
        }
    }

    private static final Map<String, BufferedImage> CACHE = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> e) {
            return size() > 600;
        }
    };

    private static BufferedImage imagen(String s, int esc, Color color, Color sombra) {
        String clave = esc + "|" + color.getRGB() + "|" + (sombra == null ? 0 : sombra.getRGB()) + "|" + s;
        BufferedImage im = CACHE.get(clave);
        if (im == null) {
            int w = Math.max(1, ancho(s, esc));
            int h = FILAS * esc + (sombra != null ? esc : 0);
            im = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = im.createGraphics();
            if (sombra != null) {
                pintar(g, s, 0, esc, esc, sombra);
            }
            pintar(g, s, 0, 0, esc, color);
            g.dispose();
            CACHE.put(clave, im);
        }
        return im;
    }

    public static void dibujar(Graphics2D g, String s, int x, int y, int esc, Color color, Color sombra) {
        if (s == null || s.isEmpty()) {
            return;
        }
        g.drawImage(imagen(s, esc, color, sombra), x, y, null);
    }

    public static List<String> envolver(String texto, int anchoMax, int esc) {
        List<String> lineas = new ArrayList<>();
        for (String parrafo : texto.split("\n", -1)) {
            StringBuilder actual = new StringBuilder();
            for (String palabra : parrafo.split(" ")) {
                String prueba = actual.length() == 0 ? palabra : actual + " " + palabra;
                if (actual.length() > 0 && ancho(prueba, esc) > anchoMax) {
                    lineas.add(actual.toString());
                    actual = new StringBuilder(palabra);
                } else {
                    actual = new StringBuilder(prueba);
                }
            }
            lineas.add(actual.toString());
        }
        return lineas;
    }
}
