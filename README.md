# ABLLIVEMK2-DEVICES — Suite de Instrumentos Web MIDI, Max for Live (`.amxd`) y App Nativa Android (`.apk`)

Colección de instrumentos musicales virtuales y procesadores de audio diseñados para **Ableton Live (Max for Live)**, navegadores **Web (Web Audio + Web MIDI)** con integración directa para **Arturia MiniLab MkII**, y **dispositivos móviles Android (Teléfonos y Tablets)** con síntesis nativa de ultra-baja latencia.

🌐 **Portal Web / Hub en Vivo (GitHub Pages):** [https://ivnbcode.github.io/abllivemk2-devices/](https://ivnbcode.github.io/abllivemk2-devices/)

---

## Catálogo de Dispositivos y Aplicaciones

| # | Dispositivo / App | Plataformas | Descripción Principal |
| :- | :--- | :--- | :--- |
| **01** | **OMNI-LAB MkII** | Web (`omnilab.html`) + Ableton Live (`.amxd`) | Emulador del mítico **Suzuki Omnichord (OM-27 / OM-84)**. Convierte la tira táctil de Modulación (`CC 1`) del **Arturia MiniLab MkII** en las 16 cuerdas metálicas *Sonic Strings* a lo largo de 4 octavas, con detección de acordes en tiempo real desde las 25 teclas y 8 acordes asignables a los Pads. |
| **01-B** | **OMNI-TOUCH MkII (Android)** | Android Nativo (`OMNI-TOUCH_MkII_Android.apk` · **47 KB**) | Versión nativa para **teléfonos y tablets Android** en **Kotlin + AudioTrack PCM de baja latencia**, diseñada 100% para pantalla táctil multitouch (sin MIDI) y optimizada para correr con `< 2% CPU` y `0 GC` en cualquier procesador. |
| **02** | **SPATIAL-LAB 3D MkII** | Web (`spatiallab.html`) + Ableton Live (`.amxd`) | Posicionador de **Audio Espacial Binaural 360°** con radar 3D interactivo, hasta 4 fuentes simultáneas, micro-retardos interaurales (ITD), sombra acústica de cabeza (ILD), órbitas LFO 3D, reflexiones de sala y control por tira Mod (`CC 1`) y Pads del **Arturia MiniLab MkII**. |

---

## Estructura del Repositorio

```text
├── index.html                          # Portal Hub de dispositivos y centro de descargas (.amxd, .zip y .apk)
├── omnilab.html                        # Interfaz Web de 01 · OMNI-LAB MkII (Suzuki Omnichord + MiniLab MkII)
├── spatiallab.html                     # Interfaz Web de 02 · SPATIAL-LAB 3D MkII (Radar Binaural 360°)
├── OMNI-TOUCH_MkII_Android.apk         # APK Android nativo compilado, minificado y firmado (47 KB)
├── package.json                        # Configuración de Vite (multi-página)
├── vite.config.js                      # Entradas de compilación para index, omnilab y spatiallab
├── src/                                # Código fuente de los dispositivos Web (Web Audio + Web MIDI)
│   ├── audioEngine.js                  # Motor de síntesis Sonic Strings + Chorus BBD + Reverb + Chord Hold
│   ├── chords.js                       # Matriz de acordes, voicings y detección MIDI de tríadas/séptimas
│   ├── midiController.js               # Integración Web MIDI con Arturia MiniLab MkII (CC1, Pitch, Pads, Teclas)
│   ├── omnilabApp.js                   # Controlador de interfaz y osciloscopio OLED para OMNI-LAB MkII
│   ├── spatialEngine.js                # Motor DSP Binaural 3D (HRTF, ITD/ILD, Doppler, Sala y 4 fuentes)
│   ├── spatiallabApp.js                # Radar 3D en Canvas y controlador MIDI para SPATIAL-LAB 3D MkII
│   └── style.css                       # Sistema de diseño hardware retro / consola de estudio
├── android-omnichord/                  # Proyecto Android Nativo (Kotlin) — OMNI-TOUCH MkII
│   ├── README.md                       # Documentación técnica de la versión Android
│   ├── OMNI-TOUCH_MkII_Android.apk     # Copia del APK Release listo para instalar
│   └── app/src/main/java/com/omnilab/touch/
│       ├── MainActivity.kt             # Actividad full-screen inmersiva con cambio rápido Horizontal/Vertical
│       ├── audio/
│       │   ├── MusicTheory.kt          # Tablas precalculadas de frecuencias MIDI, 96 acordes y 16 cuerdas
│       │   └── OmnichordSynthEngine.kt # Motor PCM en tiempo real sin asignaciones (0 GC) + Ritmos OM-84
│       └── ui/
│           └── OmnichordInstrumentView.kt # Vista Canvas multitouch (10 dedos) adaptable a teléfono y tablet
└── max-for-live/                       # Colección de dispositivos Max for Live (.amxd) para Ableton Live
    ├── README.md                       # Índice de dispositivos Max for Live
    ├── omnilab-mk2/                    # Dispositivo #1: OMNI-LAB MkII (MIDI Effect .amxd + .js + .zip)
    └── spatiallab-3d/                  # Dispositivo #2: SPATIAL-LAB 3D MkII (Audio Effect .amxd + .js + .zip)
```

---

## 1. App Nativa Android: OMNI-TOUCH MkII (Teléfonos y Tablets)

Diseñada específicamente para tocar el Omnichord directamente en la **pantalla táctil de cualquier teléfono o tablet Android** sin necesidad de controladores MIDI externos:

- **Optimización Extrema para Cualquier Procesador (`47 KB` APK / `0 GC`)**:
  - Construida en **Kotlin puro sobre el SDK nativo de Android** sin librerías pesadas externas.
  - Motor de síntesis PCM en tiempo real ([`OmnichordSynthEngine.kt`](./android-omnichord/app/src/main/java/com/omnilab/touch/audio/OmnichordSynthEngine.kt)) corriendo en un hilo `THREAD_PRIORITY_URGENT_AUDIO` con `AudioTrack` en modo `PERFORMANCE_MODE_LOW_LATENCY`.
  - **Cero recolección de basura (0 bytes/s)** en el hilo de audio: tablas de onda precalculadas de 2048 muestras, filtros dinámicos de 1 polo por cuerda, **Chorus Estéreo BBD** y **Reverb Algorítmica Schroeder** sobre buffers circulares estáticos.
  - **0% de uso de CPU/GPU en reposo**: las voces inactivas se saltan en una sola instrucción y el `Canvas` detiene su bucle de redibujado cuando no hay audio ni toques activos.
- **Ergonomía Táctil Multitouch (Horizontal y Vertical)**:
  - **Modo Horizontal (Landscape)**: Postura ergonómica a dos manos con la matriz de acordes a la izquierda y el **Strumplate vertical de 16 cuerdas** de borde a borde a la derecha.
  - **Modo Vertical (Portrait)**: Conmutable al instante mediante el botón **`🔄 VERT / HORIZ`** sin cortar el sonido.
  - **Interpolación continua de rasgueo (10 dedos)**: desliza uno o varios dedos por las 16 cuerdas *Sonic Strings* a cualquier velocidad sin saltar ninguna cuerda, con sensibilidad dinámica a la velocidad del barrido.
  - **Matriz de 48 a 96 Acordes + 8 Pads de Memoria Rápida**: incluye acordes *Major, Minor, 7th, Maj7* y modo extendido (`+ACORDES`) con *Min7, Dim, Aug, Sus4*.
  - **Modo `HOLD: FIJO / PULSO`** + **Panel desplegable `⚙ SONIDO`** + **Caja de Ritmos Analógica OM-84 integrada** (*Rock, Bossa, Disco, Waltz, Slow*).

### Instalación y Compilación en Android
1. Descarga e instala directamente [**`OMNI-TOUCH_MkII_Android.apk`**](./OMNI-TOUCH_MkII_Android.apk) en tu teléfono o tablet Android (compatible desde Android 5.0 / API 21 hasta Android 14+).
2. Para compilar desde el código fuente:
   ```bash
   cd android-omnichord
   ./gradlew assembleRelease
   ```

---

## 2. Dispositivos para Ableton Live (Max for Live `.amxd`)

### 01 · OMNI-LAB MkII (`max-for-live/omnilab-mk2/`)
1. Descarga [`OMNI-LAB_MkII_Ableton_M4L.zip`](./max-for-live/omnilab-mk2/OMNI-LAB_MkII_Ableton_M4L.zip) o entra en [`max-for-live/omnilab-mk2/`](./max-for-live/omnilab-mk2/).
2. Arrastra **`OMNI-LAB_MkII.amxd`** a una pista MIDI de **Ableton Live** (manteniendo `omnilab_m4l.js` en la misma carpeta) delante de cualquier sintetizador (*Wavetable*, *Drift*, *Electric*, *Analog Lab*, etc.).
3. Controles en **Arturia MiniLab MkII** (y parámetros automatizables en Ableton Live):
   - **Tira de Modulación (`CC 1`) / Dial `Strum`**: Rasguea las 16 cuerdas *Sonic Strings* a través de 4 octavas.
   - **Pads 1–8 (`Notas 36–43`)**: Disparan 8 acordes predefinidos al instante.
   - **25 Teclas (`Nota 48+`)**: Detectan acordes y tríadas/séptimas en tiempo real.

### 02 · SPATIAL-LAB 3D MkII (`max-for-live/spatiallab-3d/`)
1. Descarga [`SPATIAL-LAB_3D_MkII_Ableton_M4L.zip`](./max-for-live/spatiallab-3d/SPATIAL-LAB_3D_MkII_Ableton_M4L.zip) o entra en [`max-for-live/spatiallab-3d/`](./max-for-live/spatiallab-3d/).
2. Arrastra **`SPATIAL-LAB_3D_MkII.amxd`** como efecto de audio en cualquier pista de **Ableton Live**.
3. Posiciona el sonido en 360° mediante las perillas **Pos X, Pos Y, Elev Z, Orbit Rate, Orbit Depth y Room Size**, o usa la **Tira Mod (`CC 1`)** para girar la fuente 360° alrededor del oyente.

---

## 3. Desarrollo Local de la Suite Web (Vite)

```bash
npm install
npm run dev
```

Abre `http://localhost:5173/` en un navegador compatible con Web MIDI (Chrome, Edge, Brave u Opera) para acceder al **Hub de Dispositivos**, abrir **OMNI-LAB MkII** (`omnilab.html`) o **SPATIAL-LAB 3D MkII** (`spatiallab.html`).
