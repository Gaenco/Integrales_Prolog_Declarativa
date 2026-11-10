package org.example.screen.fx;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;


public final class Sfx {

    private Sfx() {
    }

    public enum Id { CLICK, TICK, CARTA, JUGAR, EXITO, ERROR, DESCARTAR, CHIP }

    private static final float SR = 22050f;
    private static final AudioFormat FORMATO = new AudioFormat(SR, 16, 1, true, false);
    private static final Map<Id, byte[][]> SONIDOS = new EnumMap<>(Id.class);
    private static final byte[][] CONTEO = new byte[12][];
    private static final Random AZAR = new Random(7);
    private static final AtomicInteger SONANDO = new AtomicInteger();
    private static final ExecutorService HILO = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "sfx");
        t.setDaemon(true);
        return t;
    });

    private static volatile boolean silenciado = false;
    private static volatile boolean disponible = true;
    private static volatile long ultimoTick = 0;

    static {
        generar();
    }


    public static boolean isSilenciado() {
        return silenciado;
    }

    public static void setSilenciado(boolean s) {
        silenciado = s;
    }

    public static void play(Id id) {
        byte[][] variantes = SONIDOS.get(id);
        if (id == Id.TICK) {
            long ahora = System.nanoTime();
            if (ahora - ultimoTick < 70_000_000L) {
                return;
            }
            ultimoTick = ahora;
        }
        reproducir(variantes[AZAR.nextInt(variantes.length)]);
    }

    public static void conteo(int paso) {
        reproducir(CONTEO[Math.max(0, Math.min(CONTEO.length - 1, paso))]);
    }

    /** PCM crudo de un sonido (para pruebas). */
    public static byte[] datos(Id id, int variante) {
        byte[][] v = SONIDOS.get(id);
        return v[variante % v.length];
    }

    public static AudioFormat formato() {
        return FORMATO;
    }

    private static void reproducir(byte[] pcm) {
        if (silenciado || !disponible || SONANDO.get() > 8) {
            return;
        }
        HILO.execute(() -> {
            try {
                Clip clip = AudioSystem.getClip();
                clip.open(FORMATO, pcm, 0, pcm.length);
                SONANDO.incrementAndGet();
                clip.addLineListener(ev -> {
                    if (ev.getType() == LineEvent.Type.STOP) {
                        clip.close();
                        SONANDO.decrementAndGet();
                    }
                });
                clip.start();
            } catch (Exception | Error e) {
                disponible = false;
            }
        });
    }

    private static void generar() {
        // pop de botón, en tres tonos para que no suene repetitivo
        SONIDOS.put(Id.CLICK, new byte[][]{pop(0.90), pop(1.0), pop(1.12)});
        SONIDOS.put(Id.TICK, new byte[][]{tick()});
        SONIDOS.put(Id.CARTA, new byte[][]{carta(0.9), carta(1.0), carta(1.15)});
        SONIDOS.put(Id.JUGAR, new byte[][]{jugar()});
        SONIDOS.put(Id.EXITO, new byte[][]{exito()});
        SONIDOS.put(Id.ERROR, new byte[][]{error()});
        SONIDOS.put(Id.DESCARTAR, new byte[][]{descartar()});
        SONIDOS.put(Id.CHIP, new byte[][]{chip()});
        for (int i = 0; i < CONTEO.length; i++) {
            CONTEO[i] = conteoTick(440 * Math.pow(2, i / 12.0 * 1.0 + 0.0));
        }
    }

    private static float[] buf(double segundos) {
        return new float[(int) (SR * segundos)];
    }

    private static byte[] pcm(float[] x, double pico) {
        float max = 1e-6f;
        for (float v : x) {
            max = Math.max(max, Math.abs(v));
        }
        float k = (float) (pico / max);
        byte[] out = new byte[x.length * 2];
        for (int i = 0; i < x.length; i++) {
            // fundido de salida de 3 ms para evitar "clics" al final
            double fade = Math.min(1.0, (x.length - i) / (SR * 0.003));
            short s = (short) Math.round(Math.max(-1, Math.min(1, x[i] * k * fade)) * 32767);
            out[2 * i] = (byte) (s & 0xFF);
            out[2 * i + 1] = (byte) ((s >> 8) & 0xFF);
        }
        return out;
    }

    private static double tri(double fase) {
        double f = fase - Math.floor(fase);
        return 4 * Math.abs(f - 0.5) - 1;
    }

    private static double cuadrada(double fase) {
        return (fase - Math.floor(fase)) < 0.5 ? 1 : -1;
    }

    private static byte[] pop(double p) {
        float[] x = buf(0.09);
        double fase = 0;
        Random r = new Random(1);
        for (int i = 0; i < x.length; i++) {
            double t = i / SR;
            double f = (260 + 620 * Math.exp(-t * 48)) * p;
            fase += f / SR;
            double v = Math.sin(2 * Math.PI * fase) * Math.exp(-t * 36);
            if (t < 0.003) {
                v += (r.nextDouble() * 2 - 1) * 0.45;
            }
            x[i] = (float) v;
        }
        return pcm(x, 0.70);
    }

    private static byte[] tick() {
        float[] x = buf(0.035);
        for (int i = 0; i < x.length; i++) {
            double t = i / SR;
            x[i] = (float) (Math.sin(2 * Math.PI * 2300 * t) * Math.exp(-t * 110));
        }
        return pcm(x, 0.22);
    }

    private static byte[] carta(double p) {
        float[] x = buf(0.13);
        Random r = new Random(3);
        double y = 0;
        for (int i = 0; i < x.length; i++) {
            double t = i / SR;
            double a = Math.min(1, 0.04 + t * 7 * p);
            y += a * ((r.nextDouble() * 2 - 1) - y);
            double ataque = Math.min(1, t / 0.008);
            double v = y * ataque * Math.exp(-t * 26) + Math.sin(2 * Math.PI * 130 * p * t) * Math.exp(-t * 55) * 0.35;
            x[i] = (float) v;
        }
        return pcm(x, 0.60);
    }

    private static byte[] jugar() {
        float[] x = buf(0.26);
        Random r = new Random(5);
        double f1 = 0;
        for (int i = 0; i < x.length; i++) {
            double t = i / SR;
            f1 += (150 + 120 * Math.exp(-t * 30)) / SR;
            double golpe = Math.sin(2 * Math.PI * f1) * Math.exp(-t * 14);
            double acorde = (cuadrada(392 * t) * 0.10 + cuadrada(494 * t) * 0.08 + tri(587 * t) * 0.12) * Math.exp(-t * 11);
            double ruido = t < 0.02 ? (r.nextDouble() * 2 - 1) * 0.5 * (1 - t / 0.02) : 0;
            x[i] = (float) (golpe * 0.9 + acorde + ruido);
        }
        return pcm(x, 0.75);
    }

    private static byte[] exito() {
        float[] x = buf(0.75);
        double[] notas = {523.25, 659.25, 783.99, 1046.5};
        double[] inicio = {0.0, 0.075, 0.15, 0.225};
        for (int n = 0; n < notas.length; n++) {
            int i0 = (int) (inicio[n] * SR);
            for (int i = i0; i < x.length; i++) {
                double t = (i - i0) / SR;
                double env = Math.min(1, t / 0.004) * Math.exp(-t * (n == 3 ? 4.5 : 9));
                double v = tri(notas[n] * t) * 0.6 + cuadrada(notas[n] * t) * 0.14 + Math.sin(2 * Math.PI * notas[n] * 2 * t) * 0.12;
                x[i] += (float) (v * env);
            }
        }

        int i0 = (int) (0.27 * SR);
        for (int i = i0; i < x.length; i++) {
            double t = (i - i0) / SR;
            x[i] += (float) (Math.sin(2 * Math.PI * 3136 * t) * Math.exp(-t * 14) * 0.18);
        }
        return pcm(x, 0.72);
    }

    private static byte[] error() {
        float[] x = buf(0.34);
        double suave = 0;
        for (int i = 0; i < x.length; i++) {
            double t = i / SR;
            double f = 96 - 14 * Math.min(1, t / 0.3);
            double diente = 2 * ((f * t) - Math.floor(f * t + 0.5)) + 2 * ((f * 1.045 * t) - Math.floor(f * 1.045 * t + 0.5));
            suave += 0.28 * (diente - suave);                // filtro pasa-bajos
            double env = Math.min(1, t / 0.01) * (t < 0.13 ? 1 : (t < 0.16 ? 0.15 : 0.85)) * Math.exp(-t * 3.2);
            x[i] = (float) (suave * env);
        }
        return pcm(x, 0.70);
    }

    private static byte[] descartar() {
        float[] x = buf(0.28);
        Random r = new Random(11);
        double y = 0;
        double fase = 0;
        for (int i = 0; i < x.length; i++) {
            double t = i / SR;
            double a = Math.max(0.03, 0.55 * Math.exp(-t * 11));
            y += a * ((r.nextDouble() * 2 - 1) - y);
            fase += (620 * Math.exp(-t * 9) + 90) / SR;
            double env = Math.min(1, t / 0.006) * Math.exp(-t * 9);
            x[i] = (float) ((y * 0.9 + Math.sin(2 * Math.PI * fase) * 0.45) * env);
        }
        return pcm(x, 0.62);
    }

    private static byte[] chip() {
        float[] x = buf(0.42);
        double[] f = {1760, 2637, 3951, 5300};
        double[] a = {1.0, 0.62, 0.38, 0.20};
        for (int i = 0; i < x.length; i++) {
            double t = i / SR;
            double v = 0;
            for (int k = 0; k < f.length; k++) {
                v += a[k] * Math.sin(2 * Math.PI * f[k] * t) * Math.exp(-t * (9 + 5 * k));
            }
            x[i] = (float) (v * Math.min(1, t / 0.002));
        }
        return pcm(x, 0.55);
    }

    private static byte[] conteoTick(double f) {
        float[] x = buf(0.05);
        for (int i = 0; i < x.length; i++) {
            double t = i / SR;
            x[i] = (float) ((tri(f * t) * 0.8 + Math.sin(2 * Math.PI * f * 2 * t) * 0.2) * Math.exp(-t * 62) * Math.min(1, t / 0.002));
        }
        return pcm(x, 0.42);
    }
}
