package org.example;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


public class Connection {

    public record Resultado(boolean ok, String error, String funcion, String integral,
                            String fb, String fa, String area, String areaAbs) {
    }

    private static final int TIMEOUT_SEG = 20;

    private final String swipl;
    private final Path carpeta;

    public Connection() throws IOException {
        this.carpeta = extraerScript();
        this.swipl = localizarSwipl();
    }

    public Resultado calcular(String funcion, String a, String b) throws Exception {
        List<String> cmd = List.of(swipl, "-q", "-g", "ejecutar", "-t", "halt",
                "calculo.pl", "--", funcion, a, b);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(carpeta.toFile());
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        Process p = pb.start();
        p.getOutputStream().close();

        CompletableFuture<String> salida = CompletableFuture.supplyAsync(() -> {
            try {
                return new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                return "";
            }
        });

        if (!p.waitFor(TIMEOUT_SEG, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new IOException("SWI-Prolog tardó demasiado en responder.");
        }
        return interpretar(salida.get(5, TimeUnit.SECONDS));
    }

    private static Resultado interpretar(String texto) {
        Map<String, String> datos = new HashMap<>();
        boolean ok = false;
        for (String linea : texto.split("\\R")) {
            if (linea.equals("OK")) {
                ok = true;
            } else {
                int i = linea.indexOf('=');
                if (i > 0) {
                    datos.put(linea.substring(0, i), linea.substring(i + 1));
                }
            }
        }
        if (datos.containsKey("ERROR")) {
            return error(datos.get("ERROR"));
        }
        if (!ok) {
            return error("Respuesta inesperada de SWI-Prolog:\n" + texto);
        }
        return new Resultado(true, "", datos.getOrDefault("funcion", ""),
                datos.getOrDefault("integral", ""), datos.getOrDefault("fb", ""),
                datos.getOrDefault("fa", ""), datos.getOrDefault("area", ""),
                datos.getOrDefault("area_abs", ""));
    }

    private static Resultado error(String mensaje) {
        return new Resultado(false, mensaje, "", "", "", "", "", "");
    }

    private Path extraerScript() throws IOException {
        Path dir = Files.createTempDirectory("integrales-");
        Path destino = dir.resolve("calculo.pl");
        try (InputStream in = getClass().getResourceAsStream("/calculo.pl")) {
            if (in == null) {
                throw new IllegalStateException("No se encontró calculo.pl en src/main/resources.");
            }
            Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Files.deleteIfExists(destino);
                Files.deleteIfExists(dir);
            } catch (IOException ignorado) {
            }
        }));
        return dir;
    }

    private static String localizarSwipl() {
        List<String> candidatos = new ArrayList<>();
        String prop = System.getProperty("swipl.path");
        String env = System.getenv("SWIPL_PATH");
        if (prop != null && !prop.isBlank()) candidatos.add(prop);
        if (env != null && !env.isBlank()) candidatos.add(env);
        candidatos.add("swipl");
        candidatos.add("C:\\Program Files\\swipl\\bin\\swipl.exe");
        candidatos.add("C:\\Program Files (x86)\\swipl\\bin\\swipl.exe");
        candidatos.add("/Applications/SWI-Prolog.app/Contents/MacOS/swipl");
        candidatos.add("/opt/homebrew/bin/swipl");
        candidatos.add("/usr/local/bin/swipl");
        candidatos.add("/usr/bin/swipl");

        for (String c : candidatos) {
            if (funciona(c)) {
                return c;
            }
        }
        throw new IllegalStateException(
                "No se encontró SWI-Prolog (swipl).\n\n"
                + "1) Instálalo desde https://www.swi-prolog.org/Download.html\n"
                + "2) Marca la opción \"Add swipl to the system PATH\" al instalar,\n"
                + "   o define la variable de entorno SWIPL_PATH con la ruta de swipl.exe.\n"
                + "3) Reinicia IntelliJ y vuelve a ejecutar.");
    }

    private static boolean funciona(String exe) {
        try {
            Process p = new ProcessBuilder(exe, "--version")
                    .redirectErrorStream(true).start();
            p.getOutputStream().close();
            p.getInputStream().readAllBytes();
            return p.waitFor(8, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
