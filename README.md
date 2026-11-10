# Solución de integrales — Programación Declarativa, Proyecto Final 2026/02

Sistema experto que, dada una función f(x) y dos límites a y b, calcula la
**antiderivada** y el **área bajo la curva** entre a y b.

* **Base de conocimiento:** SWI-Prolog (`src/main/resources/calculo.pl`)
* **Interfaz:** Java + Swing (sin librerías externas)
* **Comunicación:** Java ejecuta el programa `swipl` y lee su respuesta.

## Qué necesitas instalar
1. **JDK 17 o superior**
2. **IntelliJ IDEA** (Community sirve)
3. **SWI-Prolog** → https://www.swi-prolog.org/Download.html
   * En Windows marca *"Add swipl to the system PATH"* durante la instalación.
   * Comprueba en una terminal: `swipl --version`

No hay dependencias de Maven que descargar.

## Cómo ejecutarlo
1. Descomprime el zip y en IntelliJ: **File → Open** → carpeta `integrales-prolog`.
2. Espera a que IntelliJ termine de indexar (si pide "Trust project", acepta).
3. Abre `src/main/java/org/example/Main.java` y pulsa ▶ junto a `main`.

Si SWI-Prolog no está en el PATH, define la variable de entorno `SWIPL_PATH`
con la ruta completa, p. ej. `C:\Program Files\swipl\bin\swipl.exe`, y reinicia IntelliJ.

## Cómo probar solo la base de conocimiento (sin Java)
```
cd src/main/resources
swipl calculo.pl
?- resolver_seguro('3*x^2 + sin(x)', '0', '2', R).
```
o como lo llama Java:
```
swipl -q -g ejecutar -t halt calculo.pl -- "3*x^2" 0 2
```
Pruebas automáticas (37 casos correctos + 15 de error, validados contra
integración numérica independiente):
```
cd pruebas
swipl -g correr -t halt pruebas.pl
```

## Las 15 integrales implementadas (rotuladas como en el enunciado)
| Letra | Integral | Observación |
|---|---|---|
| a | ∫ k dx | k = cualquier constante: `5`, `2.5`, `pi`, `sqrt(2)` |
| b | ∫ x dx | |
| c | ∫ k·x dx | |
| d | ∫ xⁿ dx, n entero | n = -1 da ln\|x\|; también acepta `1/x`, `3/x^2` |
| f | ∫ k·u dx | linealidad; también `u*k`, `u/k` y `-u` |
| g | ∫ (u + v) dx | u, v: cualquier combinación de las reglas |
| h | ∫ (u − v) dx | |
| i | ∫ aˣ dx, a > 0 | `2^x`, `e^x`, `exp(x)` |
| k | ∫ sin(x) dx | |
| l | ∫ cos(x) dx | |
| m | ∫ tan(x) dx = −ln\|cos x\| | el intervalo no puede contener x = π/2 + kπ |
| p | ∫ arctan(x) dx | por partes: x·arctan x − ½·ln(1+x²); se escribe `atan(x)` o `arctan(x)` |
| q | ∫ sinh(x) dx | también se acepta `sh(x)` |
| r | ∫ cosh(x) dx | |
| s | ∫ tanh(x) dx = ln(cosh x) | |

## La interfaz (estilo "mesa de cartas")
Diseño original inspirado en los videojuegos de cartas. **No usa imágenes, fuentes ni audio externos**:
todo se genera por código (texturas, tipografía de píxeles y sonidos).

* **Mesa de fieltro** con la integral tal como se escribe: ∫ (que flota y rebota si lo pulsas), límite
  superior *b*, límite inferior *a*, la función y `dx`. Vista previa "Se lee:" con la ecuación dibujada.
* **Teclado de botones 3D** (funciones, constantes, números y operadores). Las funciones dejan el cursor
  dentro del paréntesis; en `a^x` queda seleccionada la base.
* **CALCULAR**, **LIMPIAR TODO** (borra función, límites y resultados) y **COPIAR**.
* **Marcador estilo "fichas × multiplicador":** caja azul F(b), caja roja F(a) y caja dorada con el área,
  que cuenta desde cero con un "tic" por paso. Debajo, la ecuación exacta (fracciones apiladas).
* **SOPORTADAS:** diálogo modal donde las 15 integrales son cartas de póker (palo, índices, brillo foil e
  inclinación al pasar el mouse). La nota de cada carta aparece abajo.
* **SONIDO: SI/NO** silencia los efectos.
* Enter en cualquier recuadro = Calcular.

### Animaciones
Reparto de paneles y teclas al abrir, "juice" (rebote con giro) al pulsar, botones que se elevan al pasar el
mouse, sacudida de la tarjeta al error, título con ola, ∫ flotante, cartas que se reparten e inclinan, y un
fondo de remolino que se mueve lento con efecto de monitor CRT (líneas de barrido y viñeta).

### Sonidos
Sintetizados en tiempo real con `javax.sound` (clic, tick, carta, jugar, éxito, error, descartar, chip). Si
el equipo no tiene salida de audio, el programa sigue en silencio sin errores.

### Ecuaciones estilo calculadora TI
Fracciones apiladas, superíndices, raíces y ∫ con límites, dibujados con `Graphics2D` (`MathRender.java`).
Si una ecuación es muy ancha, se reduce sola para caber.

### Para quien quiera modificarlo
* Colores: `Theme.java`.   Tipografía de píxeles: `fx/PixelFont.java`.   Sonidos: `fx/Sfx.java`.
* Animaciones: `fx/Anim.java` (un solo reloj) y `fx/Juice.java`.
* Fondo y CRT: `BalatroBackground.java`, `CrtOverlay.java`.   Cartas: `CardTable.java`.

## Cómo escribir las expresiones
* Multiplicación explícita: `3*x^2` (no `3x^2`). Potencia: `^` o `**`.
* Decimales con punto. Constantes admitidas en coeficientes y límites: `pi`, `e`.
* Exponentes negativos: `x^(-2)` o `x^-2`.
* Si el intervalo [a,b] contiene x = 0 y hay potencias negativas, el sistema
  avisa que la integral es impropia en lugar de dar un número falso.

## Arquitectura
```
 Window (Swing) ──► Connection ──► swipl -g ejecutar calculo.pl -- "f" "a" "b"
      ▲                │                         │
      └── Resultado ◄──┴──── líneas clave=valor ◄┘
```
En Java: `Pretty` (texto plano) y `MathRender` (texto → árbol → cajas medidas → `Graphics2D`);
`MathView` dibuja varias líneas de ecuaciones y `PanelResult` las muestra en tarjetas.

En Prolog: `parsear` → `integral/2` (reglas) → `simplificar` → `evaluar_F`
→ F(b) − F(a). La antiderivada se representa como lista `Coeficiente-Base`,
así la linealidad se reduce a escalar y concatenar listas.
