package org.example.screen.fx;


public final class Juice {

    private static final double DURACION = 0.4;

    private double restante;
    private double t;
    private double amp;
    private double rotAmp;

    public void disparar(double escala, double rotacion) {
        amp = escala;
        rotAmp = rotacion;
        restante = DURACION;
        t = 0;
    }

    public boolean tick(double dt) {
        if (restante <= 0) {
            return false;
        }
        t += dt;
        restante = Math.max(0, restante - dt);
        return restante > 0;
    }

    public double escala() {
        return 1 + amp * Math.sin(50.8 * t) * Math.pow(restante / DURACION, 3);
    }

    public double rotacion() {
        return rotAmp * Math.sin(40.8 * t) * Math.pow(restante / DURACION, 2);
    }

    public boolean activo() {
        return restante > 0;
    }
}
