# OMNI-LAB MkII & Colección Max for Live (Ableton Live + Arturia MiniLab MkII)

Instrumento musical virtual inspirado en el mítico **Suzuki Omnichord (OM-27 / OM-84)** y colección de dispositivos **Max for Live (`.amxd`)** diseñados para **Ableton Live** y controladores **Arturia MiniLab MkII**.

---

## Estructura del Repositorio

```text
├── index.html                  # Interfaz Web retro del OMNI-LAB MkII
├── package.json                # Configuración de Vite para la versión Web
├── src/                        # Código fuente de la aplicación Web (Web Audio + Web MIDI)
│   ├── audioEngine.js          # Síntesis Sonic Strings + Chorus BBD + Reverb + Chord Hold
│   ├── chords.js               # Matriz de 36+ acordes, voicings y detección MIDI en vivo
│   ├── midiController.js       # Integración con Arturia MiniLab MkII (CC1 Strumplate, Pads y Teclas)
│   ├── main.js                 # Controlador de UI y visualizador OLED
│   └── style.css               # Estética hardware vintage inspirada en Suzuki Omnichord
└── max-for-live/               # Colección de dispositivos Max for Live (.amxd) para Ableton Live
    ├── README.md               # Índice de dispositivos Max for Live
    └── omnilab-mk2/            # Dispositivo #1: OMNI-LAB MkII (Strumplate & Chord Matrix)
        ├── OMNI-LAB_MkII.amxd  # Dispositivo listo para arrastrar a una pista MIDI en Ableton Live
        ├── OMNI-LAB_MkII.maxpat# Patch fuente editable en Max 8 / Max 9
        ├── omnilab_m4l.js      # Motor JS para Max (16 Sonic Strings + detección de acordes)
        ├── build_amxd.py       # Script para empaquetar el .maxpat en .amxd
        └── README.md           # Guía de instalación y mapeo en Ableton Live
```

---

## 1. Uso en Ableton Live (Max for Live `.amxd`)

1. Entra en [`max-for-live/omnilab-mk2/`](./max-for-live/omnilab-mk2/).
2. Arrastra **`OMNI-LAB_MkII.amxd`** a una pista MIDI de **Ableton Live** (manteniendo `omnilab_m4l.js` en la misma carpeta).
3. Coloca cualquier sintetizador detrás del dispositivo en la misma pista (*Wavetable*, *Electric*, *Analog*, *Analog Lab*, etc.).
4. En tu **Arturia MiniLab MkII**:
   - **Tira táctil de Modulación (`CC 1`)**: Rasguea las 16 cuerdas **Sonic Strings** a lo largo de 4 octavas con velocidad dinámica.
   - **Pads 1–8 (`Notas 36–43 / C1–G1`)**: Disparan 8 acordes predefinidos al instante.
   - **25 Teclas (`C2 / Nota 48+`)**: Detectan acordes en tiempo real (toca 1 nota para elegir la raíz o toca una tríada/séptima para cambiar la calidad automáticamente).

---

## 2. Uso de la Aplicación Web Independiente (Web Audio + Web MIDI)

Si quieres tocar el Omnichord directamente en el navegador con su propio sintetizador integrado:

```bash
npm install
npm run dev
```

Abre `http://localhost:5173/` en Chrome o Edge con tu **Arturia MiniLab MkII** conectado por USB.
