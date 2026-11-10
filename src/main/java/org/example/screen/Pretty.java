package org.example.screen;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class Pretty {

    private Pretty() {
    }

    private static final Pattern POT = Pattern.compile("\\^(\\(([^()]*)\\)|-?[0-9]+|[xn])");
    private static final Pattern PI = Pattern.compile("\\bpi\\b");
    private static final Pattern IMPLICITO = Pattern.compile("(\\d)\\*(?=[a-z(])");

    private static final String NORMAL = "0123456789-+xn()";
    private static final String SUPER = "⁰¹²³⁴⁵⁶⁷⁸⁹⁻⁺ˣⁿ⁽⁾";

    public static String formatear(String s) {
        if (s == null) {
            return "";
        }
        s = s.replace("**", "^");

        Matcher m = POT.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String g = m.group(1);
            String interior = g.startsWith("(") ? m.group(2) : g;
            String sup = superindice(interior);
            m.appendReplacement(sb, Matcher.quoteReplacement(sup != null ? sup : m.group()));
        }
        m.appendTail(sb);
        s = sb.toString();

        s = PI.matcher(s).replaceAll("π");
        s = IMPLICITO.matcher(s).replaceAll("$1");
        return s.replace("*", "·").replace('-', '−');
    }

    private static String superindice(String t) {
        if (t.isEmpty()) {
            return null;
        }
        StringBuilder r = new StringBuilder();
        for (char c : t.toCharArray()) {
            int i = NORMAL.indexOf(c);
            if (i < 0) {
                return null;
            }
            r.append(SUPER.charAt(i));
        }
        return r.toString();
    }
}
