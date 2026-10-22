# OMNI-TOUCH MkII — App Nativa Android (Teléfonos y Tablets)

Versión nativa para **Android (Kotlin + AudioTrack PCM de Baja Latencia + Canvas Multitouch)** del **Suzuki Omnichord (OM-27 / OM-84)**, diseñada exclusivamente para **pantallas táctiles de teléfonos y tablets** (sin dependencias MIDI externas) y optimizada al extremo para correr con el mínimo consumo de CPU y RAM en cualquier procesador.

---

## Características de Optimización Extrema

1. **Motor DSP de Audio en Tiempo Real sin Asignaciones de Memoria (`OmnichordSynthEngine.kt`)**:
   - Hilo de audio dedicado con prioridad `THREAD_PRIORITY_URGENT_AUDIO` y `AudioTrack` en modo `PERFORMANCE_MODE_LOW_LATENCY`.
   - **0 bytes/segundo de basura en memoria (Zero-GC)** durante la síntesis: todas las voces, envolventes, filtros y líneas de retardo están pre-asignadas en arreglos primitivos (`FloatArray` / `BooleanArray`).
   - **Síntesis por Tabla de Ondas (Wavetables) de 2048 muestras**: evita llamadas costosas a `Math.sin()` o `Math.pow()` por muestra.
   - **Salto automático de cuerdas inactivas**: las 16 cuerdas *Sonic Strings* solo procesan muestras mientras su envolvente está activa (`< 2% CPU` en procesadores gama baja).
   - **Chorus Estéreo BBD + Reverb Algorítmica Schroeder ultraligera**: recrea el sonido cálido y espacial del Omnichord sin usar convolución FFT pesada.
   - **Caja de Ritmos Analógica OM-84 integrada**: 5 ritmos clásicos (*Rock*, *Bossa*, *Disco*, *Waltz*, *Slow*) generados por síntesis matemática pura (sin muestras de audio pesadas).

2. **Interfaz Táctil Multitouch por Hardware (`OmnichordInstrumentView.kt`)**:
   - **Cero jerarquía de vistas pesadas**: toda la interfaz se dibuja directamente sobre un único `Canvas` acelerado por GPU.
   - **Ahorro inteligente de batería (0% CPU/GPU en reposo)**: la pantalla solo se redibuja cuando hay interacción táctil o animación activa de cuerdas/onda.
   - **Multitouch real de 10 puntos con interpolación de barrido**: desliza uno o varios dedos por las 16 cuerdas *Sonic Strings* a cualquier velocidad sin saltar ninguna cuerda, mientras seleccionas acordes con la mano izquierda.

3. **Diseño Adaptable a Teléfonos y Tablets (Horizontal y Vertical)**:
   - **Modo Horizontal (Landscape — Postura ergonómica a dos manos)**:
     - **Mano izquierda**: Matriz de 48 acordes (ampliable a 96 acordes con `+ACORDES`) + 8 Memorias Rápidas programables (`+MEMORIA`).
     - **Mano derecha**: *Strumplate* vertical de 16 cuerdas de borde a borde para máximo recorrido táctil.
     - **Panel Desplegable (`⚙ SONIDO`)**: oculta los 8 controles deslizantes de síntesis (*Volumen Arpa, Sustain, Brillo Tone, Chorus BBD, Reverb, Volumen Órgano, Tempo BPM y Octava*) para maximizar el tamaño de los botones táctiles en teléfonos.
   - **Modo Vertical (Portrait — Adaptable con botón `🔄 VERT / HORIZ`)**:
     - Apila la pantalla OLED y la matriz arriba/centro y coloca el *Strumplate* ancho en la mitad inferior para tocar cómodamente en vertical.

---

## Compilación del APK

```bash
cd android-omnichord
./gradlew assembleRelease
```

El archivo APK optimizado y firmado listo para instalar en cualquier teléfono o tablet Android (Android 5.0 Lollipop / API 21 en adelante) se genera en:
- `app/build/outputs/apk/release/app-release.apk`
- Copia directa en la raíz del proyecto: `OMNI-TOUCH_MkII_Android.apk`
