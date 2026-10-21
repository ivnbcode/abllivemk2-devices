# OMNI-LAB MkII — Dispositivo Max for Live (`.amxd`) para Ableton Live

Este directorio contiene el dispositivo nativo **Max for Live** diseñado específicamente para **Ableton Live + Arturia MiniLab MkII**.

## Archivos incluidos

1. **[OMNI-LAB_MkII.amxd](file:///c:/Users/LENOVO/OneDrive/Documentos/omichord%20app/max-for-live/OMNI-LAB_MkII.amxd)**: El dispositivo Max for Live compilado (tipo *MIDI Effect*) listo para arrastrar y soltar en cualquier pista MIDI de Ableton Live.
2. **[omnilab_m4l.js](file:///c:/Users/LENOVO/OneDrive/Documentos/omichord%20app/max-for-live/omnilab_m4l.js)**: El motor de acordes, matriz de 16 cuerdas Sonic Strings, interpolador de rasgueo por `CC 1` y detector de tríadas/séptimas para el objeto `[js]` de Max.
3. **[OMNI-LAB_MkII.maxpat](file:///c:/Users/LENOVO/OneDrive/Documentos/omichord%20app/max-for-live/OMNI-LAB_MkII.maxpat)**: El patch fuente editable en Max 8 / Max 9.

---

## Cómo instalarlo y usarlo en Ableton Live

1. **Mantén `OMNI-LAB_MkII.amxd` y `omnilab_m4l.js` en la misma carpeta** (por ejemplo, en esta carpeta `max-for-live` o cópialos juntos a tu `User Library -> Presets -> MIDI Effects -> Max MIDI Effect`).
2. Abre **Ableton Live** y arrastra **`OMNI-LAB_MkII.amxd`** a una pista MIDI.
3. Justo después de `OMNI-LAB_MkII.amxd` en la misma pista, coloca el instrumento que quieras rasguear:
   - Para el sonido clásico del Omnichord en Ableton: coloca **Wavetable**, **Collision**, **Tension** o **Analog** con un poco de *Chorus-Ensemble* y *Hybrid Reverb*.
   - ¡También puedes colocar **Analog Lab** de tu Arturia o cualquier VST3!

---

## Controles en tu Arturia MiniLab MkII dentro de Ableton

* **Tira Táctil de Modulación (`CC 1`)**: Rasguea las **16 Sonic Strings** (4 octavas del acorde activo) de abajo hacia arriba con interpolación automática de velocidad.
* **Pads 1–8 (`Notas 36 a 43 / C1 a G1`)**: Cambian instantáneamente entre 8 acordes predefinidos (`C`, `G`, `Am`, `F`, `Dm`, `Em`, `G7`, `CM7`).
  > **Tip para Ableton:** Asegúrate de que tu MiniLab MkII esté en **Memory 1 (Shift + Pad 1)** o que los Pads envíen notas en el mismo canal de la pista.
* **25 Teclas (`C2 / Nota 48 en adelante`)**:
  - Toca **1 sola tecla** para cambiar la nota raíz manteniendo el tipo de acorde actual.
  - Toca **una tríada o acorde de 4 notas** en el MiniLab para que el dispositivo detecte automáticamente `Major`, `Minor`, `7th`, `Maj7`, `Min7`, `Dim`, `Aug` o `Sus4`.
* **Perillas (Knobs) Automatizables en Ableton**:
  1. **Strum Position (CC1)**: Te permite incluso dibujar o automatizar el rasgueo en los clips de Ableton.
  2. **Root**: Selección de nota raíz (`C` a `B`).
  3. **Quality**: Calidad del acorde (`Major`, `Minor`, `7th`, `Maj7`, `Min7`, `Dim`, `Aug`, `Sus4`).
  4. **Sustain**: Duración de las notas rasgueadas (`80 ms` a `3500 ms`).
  5. **Hold**: Activa o desactiva el colchón de acorde mantenido (Órgano + Bajo del Omnichord).
