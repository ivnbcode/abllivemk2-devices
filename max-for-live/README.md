# Catalogo de Dispositivos Max for Live (`max-for-live/`)

Esta carpeta agrupa todos los dispositivos **Max for Live (`.amxd`)** para **Ableton Live**, cada uno dentro de su propia subcarpeta con sus scripts de soporte (`.js`, `.maxpat`) y compilador.

---

## Dispositivos Disponibles

| Dispositivo | Carpeta | Tipo | Descripción |
| :--- | :--- | :--- | :--- |
| **OMNI-LAB MkII** | [`omnilab-mk2/`](./omnilab-mk2/) | MIDI Effect | Emulador de Strumplate (Sonic Strings de 16 cuerdas) y matriz de acordes del Suzuki Omnichord controlado con la tira táctil de Modulación (`CC 1`), Pads (`36-43`) y teclas del **Arturia MiniLab MkII**. |

---

## Cómo agregar un nuevo dispositivo Max for Live al repositorio

1. Crea una subcarpeta nueva con el nombre de tu dispositivo:
   ```text
   max-for-live/
   ├── omnilab-mk2/
   └── mi-nuevo-dispositivo/
       ├── MiDispositivo.amxd
       ├── MiDispositivo.maxpat
       ├── logica.js (opcional)
       └── README.md
   ```
2. Recuerda mantener siempre el archivo `.amxd` y sus dependencias `.js` en la misma subcarpeta para que Ableton Live los cargue automáticamente al arrastrar el dispositivo.
