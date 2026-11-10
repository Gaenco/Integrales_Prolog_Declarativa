package org.example.screen.components;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public final class MathRender {

    private MathRender() {
    }


    public sealed interface Node permits Sym, Op, Esp, Row, Sup, Sub, Frac, Sqrt, Paren, IntegralN {
    }

    public record Sym(String t, boolean cursiva) implements Node {
    }

    public record Op(String t, boolean relacion) implements Node {
    }

    public record Esp(float em) implements Node {
    }

    public record Row(List<Node> items) implements Node {
    }

    public record Sup(Node base, Node exp) implements Node {
    }

    public record Sub(Node base, Node sub) implements Node {
    }

    public record Frac(Node num, Node den) implements Node {
    }

    public record Sqrt(Node arg) implements Node {
    }

    public record Paren(Node inner, String abre, String cierra) implements Node {
    }

    public record IntegralN(Node inf, Node sup) implements Node {
    }

    public static Node texto(String s) {
        return new Sym(s, false);
    }

    public static Node variable(String s) {
        return new Sym(s, true);
    }

    public static Node op(String s) {
        return new Op(s, false);
    }

    public static Node rel(String s) {
        return new Op(s, true);
    }

    public static Node esp(float em) {
        return new Esp(em);
    }

    public static Node fila(Node... n) {
        return new Row(List.of(n));
    }

    public static Node integral(Node inf, Node sup) {
        return new IntegralN(inf, sup);
    }

    public static Node dx() {
        return fila(esp(0.12f), texto("d"), variable("x"));
    }

    public static Node envolverSiSuma(Node n) {
        if (n instanceof Row r) {
            for (Node it : r.items()) {
                if (it instanceof Op o && !o.relacion() && (o.t().equals("+") || o.t().equals("−"))) {
                    return new Paren(n, "(", ")");
                }
            }
        }
        return n;
    }

    public static Node parse(String s) {
        String limpio = s == null ? "" : s;
        try {
            return new Parser(limpio).todo();
        } catch (RuntimeException e) {
            return texto(limpio);
        }
    }

    private static final class Parser {
        private final String s;
        private int i = 0;

        Parser(String s) {
            this.s = s;
        }

        Node todo() {
            Node n = relacion();
            ws();
            if (i < s.length()) {
                throw new IllegalArgumentException("sobra texto");
            }
            return n;
        }

        private void ws() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
                i++;
            }
        }

        private boolean peek(char c) {
            ws();
            return i < s.length() && s.charAt(i) == c;
        }

        private void esperar(char c) {
            if (!peek(c)) {
                throw new IllegalArgumentException("falta " + c);
            }
            i++;
        }

        private boolean esMenos() {
            return peek('-') || peek('−');
        }

        Node relacion() {
            List<Node> it = new ArrayList<>();
            it.add(expresion());
            while (peek('=') || peek('≈')) {
                it.add(rel(String.valueOf(s.charAt(i++))));
                it.add(expresion());
            }
            return it.size() == 1 ? it.get(0) : new Row(it);
        }

        Node expresion() {
            List<Node> it = new ArrayList<>();
            if (esMenos()) {
                i++;
                it.add(texto("−"));
            } else if (peek('+')) {
                i++;
            }
            it.add(termino());
            for (;;) {
                if (peek('+')) {
                    i++;
                    it.add(op("+"));
                    it.add(termino());
                } else if (esMenos()) {
                    i++;
                    it.add(op("−"));
                    it.add(termino());
                } else {
                    break;
                }
            }
            return it.size() == 1 ? it.get(0) : new Row(it);
        }

        Node termino() {
            Node acc = factor();
            for (;;) {
                if (peek('*') || peek('·')) {
                    i++;
                    acc = mult(acc, factor());
                } else if (peek('/')) {
                    i++;
                    acc = new Frac(desenvolver(acc), desenvolver(factor()));
                } else {
                    break;
                }
            }
            return acc;
        }

        Node factor() {
            if (esMenos()) {
                i++;
                return new Row(List.of(texto("−"), potencia()));
            }
            return potencia();
        }

        Node potencia() {
            Node base = atomo();
            if (peek('^')) {
                i++;
                return new Sup(base, exponente());
            }
            if (peek('_')) {
                i++;
                return new Sub(base, exponente());
            }
            return base;
        }

        Node exponente() {
            if (esMenos()) {
                i++;
                return new Row(List.of(texto("−"), exponente()));
            }
            return desenvolver(potencia());
        }

        Node atomo() {
            ws();
            if (i >= s.length()) {
                throw new IllegalArgumentException("fin inesperado");
            }
            char c = s.charAt(i);
            if (c == '(') {
                i++;
                Node n = relacion();
                esperar(')');
                return new Paren(n, "(", ")");
            }
            if (c == '|') {
                i++;
                Node n = expresion();
                esperar('|');
                return new Paren(n, "|", "|");
            }
            if (Character.isDigit(c) || c == '.') {
                return numero();
            }
            if (Character.isLetter(c)) {
                return identificador();
            }
            throw new IllegalArgumentException("símbolo no válido: " + c);
        }

        Node numero() {
            int ini = i;
            while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) {
                i++;
            }
            return texto(s.substring(ini, i));
        }

        Node identificador() {
            int ini = i;
            while (i < s.length() && Character.isLetter(s.charAt(i))) {
                i++;
            }
            String nombre = s.substring(ini, i);
            int fin = i;

            if (peek('(')) {
                i++;
                Node arg = relacion();
                esperar(')');
                if (nombre.equals("sqrt")) {
                    return new Sqrt(arg);
                }
                if (nombre.equals("abs")) {
                    return new Paren(arg, "|", "|");
                }
                return new Row(List.of(nombreFuncion(nombre), new Paren(arg, "(", ")")));
            }
            i = fin;
            if (nombre.length() > 1 && peek('|')) { // ln|x|
                i++;
                Node arg = expresion();
                esperar('|');
                return new Row(List.of(nombreFuncion(nombre), new Paren(arg, "|", "|")));
            }
            i = fin;
            return switch (nombre) {
                case "pi" -> variable("π");
                case "e" -> texto("e");
                default -> new Sym(nombre, nombre.length() == 1);
            };
        }

        Node nombreFuncion(String n) {
            String m = switch (n) {
                case "atan" -> "arctan";
                case "asin" -> "arcsin";
                case "acos" -> "arccos";
                case "log" -> "ln";
                default -> n;
            };
            return new Sym(m, m.length() == 1);
        }

        Node desenvolver(Node n) {
            return n instanceof Paren p && p.abre().equals("(") ? p.inner() : n;
        }

        Node mult(Node a, Node b) {
            boolean implicita = (esNumero(a) || esVariable(a)) && !requiereSeparador(b);
            if (!implicita) {
                return new Row(List.of(a, op("·"), b));
            }
            float hueco = empiezaConNombre(b) ? 0.14f : 0.04f;
            return new Row(List.of(a, esp(hueco), b));
        }

        boolean esNumero(Node n) {
            return n instanceof Sym s && !s.cursiva() && !s.t().isEmpty() && Character.isDigit(s.t().charAt(0));
        }

        boolean esVariable(Node n) {
            return n instanceof Sym s && s.cursiva();
        }

        boolean requiereSeparador(Node n) {
            if (n instanceof Sym s) {
                return !s.t().isEmpty() && (Character.isDigit(s.t().charAt(0)) || s.t().charAt(0) == '.'
                        || s.t().charAt(0) == '−');
            }
            if (n instanceof Sup p) {
                return requiereSeparador(p.base());
            }
            if (n instanceof Row r && !r.items().isEmpty()) {
                return requiereSeparador(r.items().get(0));
            }
            return false;
        }

        boolean empiezaConNombre(Node n) {
            if (n instanceof Sym s) {
                return !s.cursiva() && s.t().length() > 1 && Character.isLetter(s.t().charAt(0));
            }
            if (n instanceof Row r && !r.items().isEmpty()) {
                return empiezaConNombre(r.items().get(0));
            }
            return false;
        }
    }

    private static final FontRenderContext FRC = new FontRenderContext(null, true, true);

    private static Font fuente(boolean cursiva, float tam) {
        return new Font(Font.SERIF, cursiva ? Font.ITALIC : Font.PLAIN, 1).deriveFont(tam);
    }

    private abstract static class Box {
        float w;
        float asc;
        float desc;
        boolean relacion;

        abstract void paint(Graphics2D g, float x, float y);
    }

    private static final class Space extends Box {
        Space(float w) {
            this.w = w;
        }

        @Override
        void paint(Graphics2D g, float x, float y) {
        }
    }

    private static final class TextBox extends Box {
        final String t;
        final Font f;

        TextBox(String t, boolean cursiva, float tam) {
            this.t = t;
            this.f = fuente(cursiva, tam);
            this.w = (float) f.getStringBounds(t, FRC).getWidth() + (cursiva ? 0.07f : 0.02f) * tam;
            boolean parentesis = t.equals("(") || t.equals(")");
            boolean mayusculaAcentuada = t.matches(".*[ÁÉÍÓÚÑ].*");
            this.asc = (mayusculaAcentuada ? 0.95f : parentesis ? 0.80f : 0.72f) * tam;
            boolean conCola = parentesis || t.matches(".*[gjpqy,;].*");
            this.desc = conCola ? 0.24f * tam : 0.0f;
        }

        @Override
        void paint(Graphics2D g, float x, float y) {
            g.setFont(f);
            g.drawString(t, x, y);
        }
    }

    private static final class HBox extends Box {
        final List<Box> hijos;

        HBox(List<Box> hijos) {
            this.hijos = hijos;
            for (Box b : hijos) {
                w += b.w;
                asc = Math.max(asc, b.asc);
                desc = Math.max(desc, b.desc);
            }
        }

        @Override
        void paint(Graphics2D g, float x, float y) {
            float cx = x;
            for (Box b : hijos) {
                b.paint(g, cx, y);
                cx += b.w;
            }
        }
    }

    private static final class SupBox extends Box {
        final Box base;
        final Box sup;
        final float subir;
        final float hueco;

        SupBox(Box base, Box sup, float tam) {
            this.base = base;
            this.sup = sup;
            this.hueco = 0.03f * tam;
            this.subir = Math.max(0.40f * tam, base.asc - 0.28f * tam);
            this.w = base.w + hueco + sup.w;
            this.asc = Math.max(base.asc, subir + sup.asc);
            this.desc = base.desc;
        }

        @Override
        void paint(Graphics2D g, float x, float y) {
            base.paint(g, x, y);
            sup.paint(g, x + base.w + hueco, y - subir);
        }
    }

    private static final class SubBox extends Box {
        final Box base;
        final Box sub;
        final float bajar;
        final float hueco;

        SubBox(Box base, Box sub, float tam) {
            this.base = base;
            this.sub = sub;
            this.bajar = 0.20f * tam;
            this.hueco = 0.03f * tam;
            this.w = base.w + hueco + sub.w;
            this.asc = base.asc;
            this.desc = Math.max(base.desc, bajar + sub.desc);
        }

        @Override
        void paint(Graphics2D g, float x, float y) {
            base.paint(g, x, y);
            sub.paint(g, x + base.w + hueco, y + bajar);
        }
    }

    private static final class FracBox extends Box {
        final Box num;
        final Box den;
        final float eje;
        final float hueco;
        final float grosor;
        final float margen;

        FracBox(Box num, Box den, float tam) {
            this.num = num;
            this.den = den;
            this.eje = 0.26f * tam;
            this.hueco = 0.13f * tam;
            this.grosor = Math.max(1f, 0.058f * tam);
            this.margen = 0.12f * tam;
            this.w = Math.max(num.w, den.w) + 2 * margen;
            this.asc = eje + grosor / 2 + hueco + num.desc + num.asc;
            this.desc = Math.max(0f, grosor / 2 + hueco + den.asc + den.desc - eje);
        }

        @Override
        void paint(Graphics2D g, float x, float y) {
            float barra = y - eje;
            g.fill(new Rectangle2D.Float(x + margen * 0.4f, barra - grosor / 2, w - margen * 0.8f, grosor));
            num.paint(g, x + (w - num.w) / 2, barra - grosor / 2 - hueco - num.desc);
            den.paint(g, x + (w - den.w) / 2, barra + grosor / 2 + hueco + den.asc);
        }
    }

    private static final class SqrtBox extends Box {
        final Box arg;
        final float tam;
        final float pico;

        SqrtBox(Box arg, float tam) {
            this.arg = arg;
            this.tam = tam;
            this.pico = 0.62f * tam;
            this.w = pico + arg.w + 0.10f * tam;
            this.asc = arg.asc + 0.22f * tam;
            this.desc = arg.desc + 0.04f * tam;
        }

        @Override
        void paint(Graphics2D g, float x, float y) {
            float arriba = y - asc + 0.03f * tam;
            float abajo = y + desc;
            float h = abajo - arriba;
            Path2D.Float p = new Path2D.Float();
            p.moveTo(x + 0.02f * tam, arriba + 0.58f * h);
            p.lineTo(x + 0.12f * tam, arriba + 0.52f * h);
            p.lineTo(x + 0.27f * tam, abajo);
            p.lineTo(x + pico - 0.04f * tam, arriba);
            p.lineTo(x + w, arriba);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setStroke(new BasicStroke(Math.max(1f, 0.065f * tam), BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
            g2.draw(p);
            g2.dispose();
            arg.paint(g, x + pico, y);
        }
    }

    private static final class ParenBox extends Box {
        final Box inner;
        final float tam;
        final float pw;

        ParenBox(Box inner, float tam) {
            this.inner = inner;
            this.tam = tam;
            this.pw = 0.24f * tam;
            this.w = inner.w + 2 * pw;
            this.asc = inner.asc + 0.12f * tam;
            this.desc = inner.desc + 0.12f * tam;
        }

        @Override
        void paint(Graphics2D g, float x, float y) {
            float arriba = y - asc + 0.04f * tam;
            float abajo = y + desc - 0.04f * tam;
            float medio = (arriba + abajo) / 2;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setStroke(new BasicStroke(Math.max(1f, 0.06f * tam), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D.Float izq = new Path2D.Float();
            izq.moveTo(x + pw * 0.85f, arriba);
            izq.quadTo(x - pw * 0.15f, medio, x + pw * 0.85f, abajo);
            g2.draw(izq);
            Path2D.Float der = new Path2D.Float();
            der.moveTo(x + w - pw * 0.85f, arriba);
            der.quadTo(x + w + pw * 0.15f, medio, x + w - pw * 0.85f, abajo);
            g2.draw(der);
            g2.dispose();
            inner.paint(g, x + pw, y);
        }
    }

    private static final class BarBox extends Box {
        final Box inner;
        final float tam;

        BarBox(Box inner, float tam) {
            this.inner = inner;
            this.tam = tam;
            this.w = inner.w + 0.36f * tam;
            this.asc = Math.max(inner.asc, 0.80f * tam) + 0.06f * tam;
            this.desc = Math.max(inner.desc, 0.24f * tam) + 0.04f * tam;
        }

        @Override
        void paint(Graphics2D g, float x, float y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setStroke(new BasicStroke(Math.max(1f, 0.06f * tam), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            g2.draw(new java.awt.geom.Line2D.Float(x + 0.09f * tam, y - asc, x + 0.09f * tam, y + desc));
            g2.draw(new java.awt.geom.Line2D.Float(x + w - 0.09f * tam, y - asc, x + w - 0.09f * tam, y + desc));
            g2.dispose();
            inner.paint(g, x + 0.18f * tam, y);
        }
    }

    private static final class IntBox extends Box {
        final float tam;
        final Box inf;
        final Box sup;
        float arriba;
        float abajo;
        float signoW;

        IntBox(float tam, Box inf, Box sup) {
            this.tam = tam;
            this.inf = inf;
            this.sup = sup;
            ajustar(0, 0);
        }

        void ajustar(float ascContenido, float descContenido) {
            arriba = Math.max(1.12f * tam, ascContenido + 0.10f * tam);
            abajo = Math.max(0.62f * tam, descContenido + 0.10f * tam);
            float h = arriba + abajo;
            signoW = 0.40f * h;
            float ancho = signoW;
            if (sup != null) {
                ancho = Math.max(ancho, signoW * 0.98f + sup.w);
            }
            if (inf != null) {
                ancho = Math.max(ancho, signoW * 0.55f + inf.w);
            }
            w = ancho + 0.04f * tam;
            asc = arriba;
            desc = abajo;
        }

        @Override
        void paint(Graphics2D g, float x, float y) {
            float h = arriba + abajo;
            IntegralSign.pintar(g, x, y - arriba, signoW, h, g.getPaint(), null, 0.075f);
            if (sup != null) {
                sup.paint(g, x + signoW * 0.98f, y - arriba + sup.asc);
            }
            if (inf != null) {
                inf.paint(g, x + signoW * 0.55f, y + abajo - inf.desc);
            }
        }
    }

    private static Box build(Node n, float tam, float min) {
        if (n instanceof Sym s) {
            return new TextBox(s.t(), s.cursiva(), tam);
        }
        if (n instanceof Op o) {
            return new TextBox(o.t(), false, tam);
        }
        if (n instanceof Esp e) {
            return new Space(e.em() * tam);
        }
        if (n instanceof Row r) {
            return fila(r, tam, min);
        }
        if (n instanceof Sup p) {
            Box base = build(p.base(), tam, min);
            Box exp = build(p.exp(), Math.max(min, tam * 0.70f), min);
            return new SupBox(base, exp, tam);
        }
        if (n instanceof Sub p) {
            Box base = build(p.base(), tam, min);
            Box sub = build(p.sub(), Math.max(min, tam * 0.70f), min);
            return new SubBox(base, sub, tam);
        }
        if (n instanceof Frac f) {
            float tf = Math.max(min, tam * 0.92f);
            return new FracBox(build(f.num(), tf, min), build(f.den(), tf, min), tam);
        }
        if (n instanceof Sqrt q) {
            return new SqrtBox(build(q.arg(), tam, min), tam);
        }
        if (n instanceof Paren p) {
            Box in = build(p.inner(), tam, min);
            if (p.abre().equals("|")) {
                return new BarBox(in, tam);
            }
            if (in.asc + in.desc > 1.45f * tam) {
                return new ParenBox(in, tam);
            }
            return new HBox(List.of(new TextBox("(", false, tam), in, new TextBox(")", false, tam)));
        }
        IntegralN it = (IntegralN) n;
        float tl = Math.max(min, tam * 0.62f);
        return new IntBox(tam,
                it.inf() == null ? null : build(it.inf(), tl, min),
                it.sup() == null ? null : build(it.sup(), tl, min));
    }

    private static Box fila(Row r, float tam, float min) {
        List<Box> bs = new ArrayList<>();
        for (Node it : r.items()) {
            if (it instanceof Op o) {
                float pad = o.relacion() ? 0.34f * tam : o.t().equals("·") ? 0.10f * tam : 0.24f * tam;
                if (tam <= min * 1.45f) {
                    pad *= 0.45f;
                }
                bs.add(new Space(pad));
                Box ob = new TextBox(o.t(), false, tam);
                ob.relacion = o.relacion();
                bs.add(ob);
                bs.add(new Space(pad));
            } else {
                bs.add(build(it, tam, min));
                if (it instanceof IntegralN) {
                    bs.add(new Space(0.10f * tam));
                }
            }
        }

        for (int k = 0; k < bs.size(); k++) {
            if (bs.get(k) instanceof IntBox ib) {
                float a = 0;
                float d = 0;
                for (int j = k + 1; j < bs.size() && !bs.get(j).relacion; j++) {
                    if (bs.get(j) instanceof IntBox) {
                        break;
                    }
                    a = Math.max(a, bs.get(j).asc);
                    d = Math.max(d, bs.get(j).desc);
                }
                ib.ajustar(a, d);
            }
        }
        return new HBox(bs);
    }

    public static final class Formula {
        private final Box box;
        public final float ancho;
        public final float asc;
        public final float desc;

        private Formula(Box box) {
            this.box = box;
            this.ancho = box.w;
            this.asc = box.asc;
            this.desc = box.desc;
        }

        public float alto() {
            return asc + desc;
        }

        public void pintar(Graphics2D g, float x, float baseY) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            box.paint(g, x, baseY);
        }
    }

    public static Formula componer(Node n, float tam) {
        return new Formula(build(n, tam, tam * 0.5f));
    }
}
