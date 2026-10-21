# SPATIAL-LAB 3D MkII — Procesador de Audio Espacial Binaural 3D para Ableton Live

Dispositivo **Max for Live Audio Effect (`.amxd`)** diseñado para espacializar cualquier pista de **Ableton Live** en un campo tridimensional (360° Azimut + Elevación + Distancia + Órbitas) controlado con el **Arturia MiniLab MkII**.

## Archivos incluidos
- **[SPATIAL-LAB_3D_MkII.amxd](./SPATIAL-LAB_3D_MkII.amxd)**: Efecto de audio Max for Live listo para arrastrar a cualquier pista de Audio o MIDI en Ableton Live.
- **[spatiallab_m4l.js](./spatiallab_m4l.js)**: Motor matemático binaural (ITD micro-delays L/R, ILD head-shadowing, filtrado de pabellón auricular Frente/Detrás/Elevación y generador de trayectorias orbitales).
- **[SPATIAL-LAB_3D_MkII.maxpat](./SPATIAL-LAB_3D_MkII.maxpat)**: Patch editable en Max 8 / Max 9.

## Controles en tu Arturia MiniLab MkII
1. **Knobs 1–3 (`X`, `Y`, `Z`)**: Mueven la fuente sonora en los ejes Izquierda/Derecha (`X`), Abajo/Arriba (`Y`) y Frente/Detrás (`Z`).
2. **Knobs 4–6 (`Orbit Hz`, `Radius`, `Room 3D`)**: Activan órbitas automáticas en 3D alrededor del oyente y ajustan la profundidad de sala.
3. **Tira Táctil de Modulación (`CC 1`)**: Gira manualmente la fuente sonora 360° alrededor de tu cabeza.
4. **Pads 1–8 (`Notas 36–43`)**: Saltan instantáneamente a 8 posiciones y trayectorias 3D preconfiguradas (*Frente Cercano*, *Detrás Nuca*, *Izquierda Lejos*, *Derecha Lejos*, *Cenit Arriba*, *Órbita 360*, *Infinito 8*, *Espiral Domo*).
