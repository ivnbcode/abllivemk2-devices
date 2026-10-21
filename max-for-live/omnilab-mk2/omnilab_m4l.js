/*
 * OMNI-LAB MkII — Max for Live Core Engine (omnilab_m4l.js)
 * Diseñado para Ableton Live + Arturia MiniLab MkII
 *
 * Entradas (Inlets):
 *   Inlet 0: Mensajes de control y MIDI:
 *     - "note" <pitch> <velocity>       (desde [notein] / [stripnote])
 *     - "cc1" <0..127>                  (desde [ctlin 1] - Tira de Modulación MiniLab MkII)
 *     - "pitchbend" <0..127>            (desde [bendin] - Tira Pitch Bend MiniLab MkII)
 *     - "strum" <0.0..1.0>              (desde dial automatizable de Ableton)
 *     - "root" <0..11>                  (selección manual de nota raíz)
 *     - "quality" <0..7>                (Major, Minor, 7th, Maj7, Min7, Dim, Aug, Sus4)
 *     - "pad" <0..7>                    (disparo directo de Pad 1-8)
 *     - "assign_pad" <0..7>             (guardar acorde actual en Pad 1-8)
 *     - "autostrum"                     (rasguear todo el acorde automáticamente)
 *
 * Salidas (Outlets):
 *   Outlet 0: par [pitch, velocity] para las 16 Sonic Strings -> va a [makenote] -> Sintetizador / MIDI Out
 *   Outlet 1: par [pitch, velocity] para el Chord Hold (Órgano) -> va a [noteout] o sinte de acompañamiento
 *   Outlet 2: mensajes de UI para el panel de Ableton (nombre del acorde, notas, índice de cuerda)
 */

autowatch = 1;
inlets = 1;
outlets = 3;

var NOTE_NAMES = ['C', 'C#', 'D', 'D#', 'E', 'F', 'F#', 'G', 'G#', 'A', 'A#', 'B'];

var QUALITY_KEYS = ['major', 'minor', 'seventh', 'maj7', 'min7', 'dim', 'aug', 'sus4'];

var CHORD_TYPES = {
  major:   { name: 'Major', suffix: '',     intervals: [0, 4, 7] },
  minor:   { name: 'Minor', suffix: 'm',    intervals: [0, 3, 7] },
  seventh: { name: '7th',   suffix: '7',    intervals: [0, 4, 7, 10] },
  maj7:    { name: 'Maj7',  suffix: 'M7',   intervals: [0, 4, 7, 11] },
  min7:    { name: 'Min7',  suffix: 'm7',   intervals: [0, 3, 7, 10] },
  dim:     { name: 'Dim',   suffix: 'dim',  intervals: [0, 3, 6] },
  aug:     { name: 'Aug',   suffix: 'aug',  intervals: [0, 4, 8] },
  sus4:    { name: 'Sus4',  suffix: 'sus4', intervals: [0, 5, 7] }
};

// 8 Pads por defecto para Arturia MiniLab MkII (Notas 36 a 43: C1 a G1)
var padChords = [
  { root: 0,  type: 'major' },   // Pad 1: C
  { root: 7,  type: 'major' },   // Pad 2: G
  { root: 9,  type: 'minor' },   // Pad 3: Am
  { root: 5,  type: 'major' },   // Pad 4: F
  { root: 2,  type: 'minor' },   // Pad 5: Dm
  { root: 4,  type: 'minor' },   // Pad 6: Em
  { root: 7,  type: 'seventh' }, // Pad 7: G7
  { root: 0,  type: 'maj7' }     // Pad 8: CM7
];

var currentRoot = 0;          // 0 = C
var currentType = 'major';
var numStrings = 16;
var baseOctave = 3;           // C3 (MIDI 48)
var strumNotes = [];          // 16 MIDI note numbers
var lastStringIdx = -1;
var heldKeys = [];            // Notas presionadas en las 25 teclas del MiniLab
var activePadNotes = [];      // Notas activas del Chord Hold (Órgano)
var chordHoldEnabled = 1;
var padsLowOctaveEnabled = 1; // Si es 1, las notas 36..43 disparan los 8 Pads

function loadbang() {
  rebuildStrumPlate();
  notifyUI();
}

function rebuildStrumPlate() {
  var def = CHORD_TYPES[currentType] || CHORD_TYPES.major;
  var intervals = def.intervals;
  strumNotes = [];

  var oct = 0;
  var idx = 0;
  var baseMidi = (baseOctave + 1) * 12 + currentRoot;

  while (strumNotes.length < numStrings) {
    var m = baseMidi + (oct * 12) + intervals[idx];
    if (m > 127) m = 127;
    strumNotes.push(m);
    idx++;
    if (idx >= intervals.length) {
      idx = 0;
      oct++;
    }
  }
}

function setChord(rootSemitone, typeKey) {
  currentRoot = ((rootSemitone % 12) + 12) % 12;
  currentType = CHORD_TYPES[typeKey] ? typeKey : 'major';
  rebuildStrumPlate();
  updateChordHoldVoices();
  notifyUI();
}

function updateChordHoldVoices() {
  // Apagar voces anteriores del Chord Hold
  for (var i = 0; i < activePadNotes.length; i++) {
    outlet(1, [activePadNotes[i], 0]);
  }
  activePadNotes = [];

  if (!chordHoldEnabled) return;

  var def = CHORD_TYPES[currentType] || CHORD_TYPES.major;
  var bassMidi = 36 + currentRoot; // C2 bass
  var chordBase = 48 + currentRoot; // C3 chord

  activePadNotes.push(bassMidi);
  outlet(1, [bassMidi, 72]);

  for (var j = 0; j < def.intervals.length; j++) {
    var n = chordBase + def.intervals[j];
    activePadNotes.push(n);
    outlet(1, [n, 64]);
  }
}

function notifyUI() {
  var def = CHORD_TYPES[currentType] || CHORD_TYPES.major;
  var rootName = NOTE_NAMES[currentRoot];
  var label = rootName + def.suffix;

  var noteNames = [];
  for (var i = 0; i < def.intervals.length; i++) {
    noteNames.push(NOTE_NAMES[(currentRoot + def.intervals[i]) % 12]);
  }

  outlet(2, ['chord_label', label]);
  outlet(2, ['chord_fullname', rootName + ' ' + def.name]);
  outlet(2, ['chord_notes', noteNames.join(' · ')]);
  outlet(2, ['strum_notes'].concat(strumNotes));
}

// --- Entrada de la tira táctil de Modulación (CC 1) del Arturia MiniLab MkII ---
function cc1(val) {
  var norm = Math.max(0, Math.min(127, val)) / 127.0;
  processStrumNormalized(norm);
}

function pitchbend(val) {
  var norm = Math.max(0, Math.min(127, val)) / 127.0;
  processStrumNormalized(norm);
}

function strum(normVal) {
  var norm = Math.max(0.0, Math.min(1.0, normVal));
  processStrumNormalized(norm);
}

function processStrumNormalized(norm) {
  if (strumNotes.length === 0) rebuildStrumPlate();

  var targetIdx = Math.floor(norm * numStrings);
  if (targetIdx >= numStrings) targetIdx = numStrings - 1;
  if (targetIdx < 0) targetIdx = 0;

  if (lastStringIdx === -1) {
    lastStringIdx = targetIdx;
    pluckString(targetIdx, 95);
    return;
  }

  if (targetIdx !== lastStringIdx) {
    var diff = Math.abs(targetIdx - lastStringIdx);
    var step = targetIdx > lastStringIdx ? 1 : -1;
    var vel = Math.min(124, Math.round(78 + diff * 6));

    // Interpolación de todas las cuerdas intermedias para glissando fluido
    for (var i = lastStringIdx + step; step > 0 ? i <= targetIdx : i >= targetIdx; i += step) {
      pluckString(i, vel);
    }
    lastStringIdx = targetIdx;
  }
}

function pluckString(idx, velocity) {
  if (idx < 0 || idx >= strumNotes.length) return;
  var midiPitch = strumNotes[idx];
  outlet(0, [midiPitch, velocity]);
  outlet(2, ['string_hit', idx, midiPitch]);
}

function autostrum() {
  if (strumNotes.length === 0) rebuildStrumPlate();
  for (var i = 0; i < numStrings; i++) {
    pluckString(i, 92);
  }
}

// --- Selección manual de acorde desde perillas/botones de Ableton ---
function root(r) {
  setChord(r, currentType);
}

function quality(qIdx) {
  var key = QUALITY_KEYS[qIdx] || 'major';
  setChord(currentRoot, key);
}

function chord_hold(state) {
  chordHoldEnabled = state ? 1 : 0;
  updateChordHoldVoices();
}

function pad(padIdx) {
  if (padIdx >= 0 && padIdx < padChords.length) {
    var p = padChords[padIdx];
    setChord(p.root, p.type);
    outlet(2, ['active_pad', padIdx]);
  }
}

function assign_pad(padIdx) {
  if (padIdx >= 0 && padIdx < padChords.length) {
    padChords[padIdx] = { root: currentRoot, type: currentType };
    outlet(2, ['pad_assigned', padIdx, NOTE_NAMES[currentRoot] + CHORD_TYPES[currentType].suffix]);
  }
}

// --- Entrada de Notas MIDI del Arturia MiniLab MkII (Pads 36-43 y Teclado 48-72) ---
function note(pitch, velocity) {
  // Si son las notas 36..43 (C1..G1), corresponden a los 8 Pads del MiniLab MkII
  if (padsLowOctaveEnabled && pitch >= 36 && pitch <= 43) {
    if (velocity > 0) {
      pad(pitch - 36);
    }
    return;
  }

  // Teclas del MiniLab MkII: actualizar lista de notas mantenidas
  var existingIdx = heldKeys.indexOf(pitch);
  if (velocity > 0) {
    if (existingIdx === -1) heldKeys.push(pitch);
    detectChordFromHeldKeys();
  } else {
    if (existingIdx !== -1) heldKeys.splice(existingIdx, 1);
  }
}

function detectChordFromHeldKeys() {
  if (heldKeys.length === 0) return;

  var sorted = heldKeys.slice().sort(function(a, b) { return a - b; });
  var lowest = sorted[0];
  var rootSemi = lowest % 12;

  if (sorted.length === 1) {
    setChord(rootSemi, currentType);
    return;
  }

  // Clases de altura únicas
  var pcs = [];
  for (var i = 0; i < sorted.length; i++) {
    var pc = sorted[i] % 12;
    if (pcs.indexOf(pc) === -1) pcs.push(pc);
  }

  var rel = [];
  for (var j = 0; j < pcs.length; j++) {
    rel.push((pcs[j] - rootSemi + 12) % 12);
  }

  function has(interval) {
    return rel.indexOf(interval) !== -1;
  }

  var detectedType = currentType;
  if (has(3) && has(6)) detectedType = 'dim';
  else if (has(4) && has(8)) detectedType = 'aug';
  else if (has(4) && has(11)) detectedType = 'maj7';
  else if (has(4) && has(10)) detectedType = 'seventh';
  else if (has(3) && has(10)) detectedType = 'min7';
  else if (has(5) && has(7)) detectedType = 'sus4';
  else if (has(3) && has(7)) detectedType = 'minor';
  else if (has(4) && has(7)) detectedType = 'major';
  else if (has(3)) detectedType = 'minor';
  else if (has(4)) detectedType = 'major';

  setChord(rootSemi, detectedType);
}
