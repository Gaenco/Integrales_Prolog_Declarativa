package org.example.screen.fx;

import javax.swing.Timer;
import java.util.ArrayList;
import java.util.List;


public final class Anim {

    private Anim() {
    }

    public interface Tick {
        boolean tick(double dt);
    }

    private static final List<Tick> ACTIVOS = new ArrayList<>();
    private static final Timer TIMER = new Timer(16, e -> paso());
    private static long ultimo;

    static {
        TIMER.setCoalesce(true);
    }

    public static void add(Tick t) {
        if (!ACTIVOS.contains(t)) {
            ACTIVOS.add(t);
        }
        if (!TIMER.isRunning()) {
            ultimo = System.nanoTime();
            TIMER.start();
        }
    }

    private static void paso() {
        long ahora = System.nanoTime();
        double dt = Math.min(0.05, (ahora - ultimo) / 1e9);
        ultimo = ahora;
        for (Tick t : new ArrayList<>(ACTIVOS)) {
            if (!t.tick(dt)) {
                ACTIVOS.remove(t);
            }
        }
        if (ACTIVOS.isEmpty()) {
            TIMER.stop();
        }
    }

    public static double clamp(double v, double a, double b) {
        return Math.max(a, Math.min(b, v));
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public static double outCubic(double t) {
        double u = 1 - clamp(t, 0, 1);
        return 1 - u * u * u;
    }

    public static double outBack(double t) {
        t = clamp(t, 0, 1);
        double c1 = 2.2;
        double c3 = c1 + 1;
        return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
    }
}
