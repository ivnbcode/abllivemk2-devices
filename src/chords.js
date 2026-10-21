// Music Theory & Omnichord Chord Matrix Module

export const NOTE_NAMES = ['C', 'C#', 'D', 'D#', 'E', 'F', 'F#', 'G', 'G#', 'A', 'A#', 'B'];

export const DISPLAY_ROOTS = [
  { id: 'Db', name: 'Db', semitone: 1, keyHint: '1' },
  { id: 'Ab', name: 'Ab', semitone: 8, keyHint: '2' },
  { id: 'Eb', name: 'Eb', semitone: 3, keyHint: '3' },
  { id: 'Bb', name: 'Bb', semitone: 10, keyHint: '4' },
  { id: 'F',  name: 'F',  semitone: 5, keyHint: '5' },
  { id: 'C',  name: 'C',  semitone: 0, keyHint: '6' },
  { id: 'G',  name: 'G',  semitone: 7, keyHint: '7' },
  { id: 'D',  name: 'D',  semitone: 2, keyHint: '8' },
  { id: 'A',  name: 'A',  semitone: 9, keyHint: '9' },
  { id: 'E',  name: 'E',  semitone: 4, keyHint: '0' },
  { id: 'B',  name: 'B',  semitone: 11, keyHint: '-' },
  { id: 'F#', name: 'F#', semitone: 6, keyHint: '=' }
];

export const CHORD_TYPES = {
  major: { name: 'Major', suffix: '', intervals: [0, 4, 7] },
  minor: { name: 'Minor', suffix: 'm', intervals: [0, 3, 7] },
  seventh: { name: '7th', suffix: '7', intervals: [0, 4, 7, 10] },
  maj7: { name: 'Maj7', suffix: 'M7', intervals: [0, 4, 7, 11] },
  min7: { name: 'Min7', suffix: 'm7', intervals: [0, 3, 7, 10] },
  dim: { name: 'Dim', suffix: 'dim', intervals: [0, 3, 6] },
  aug: { name: 'Aug', suffix: 'aug', intervals: [0, 4, 8] },
  sus4: { name: 'Sus4', suffix: 'sus4', intervals: [0, 5, 7] }
};

// Default 8 pads for Arturia MiniLab MkII Bank A (Notes 36-43) & Bank B (Notes 44-51)
export const DEFAULT_PAD_CHORDS = [
  { pad: 1, midiNote: 36, rootSemitone: 0, rootName: 'C', type: 'major' },
  { pad: 2, midiNote: 37, rootSemitone: 7, rootName: 'G', type: 'major' },
  { pad: 3, midiNote: 38, rootSemitone: 9, rootName: 'A', type: 'minor' },
  { pad: 4, midiNote: 39, rootSemitone: 5, rootName: 'F', type: 'major' },
  { pad: 5, midiNote: 40, rootSemitone: 2, rootName: 'D', type: 'minor' },
  { pad: 6, midiNote: 41, rootSemitone: 4, rootName: 'E', type: 'minor' },
  { pad: 7, midiNote: 42, rootSemitone: 7, rootName: 'G', type: 'seventh' },
  { pad: 8, midiNote: 43, rootSemitone: 0, rootName: 'C', type: 'maj7' }
];

export function midiToFreq(midi) {
  return 440 * Math.pow(2, (midi - 69) / 12);
}

export function midiToNoteName(midi) {
  const octave = Math.floor(midi / 12) - 1;
  const note = NOTE_NAMES[midi % 12];
  return `${note}${octave}`;
}

/**
 * Generates the 16 Sonic String pitches across ~4 octaves for a given chord.
 * Exactly like the Suzuki Omnichord OM-27 / OM-84 harp plate.
 */
export function generateStrumPlateNotes(rootSemitone, chordTypeKey, numStrings = 16, baseOctave = 3) {
  const chordDef = CHORD_TYPES[chordTypeKey] || CHORD_TYPES.major;
  const intervals = chordDef.intervals;
  const notes = [];

  let octaveOffset = 0;
  let intervalIdx = 0;

  while (notes.length < numStrings) {
    const baseMidi = (baseOctave + 1) * 12 + rootSemitone;
    const midi = baseMidi + octaveOffset * 12 + intervals[intervalIdx];
    notes.push({
      index: notes.length,
      midi,
      noteName: midiToNoteName(midi),
      freq: midiToFreq(midi)
    });

    intervalIdx++;
    if (intervalIdx >= intervals.length) {
      intervalIdx = 0;
      octaveOffset++;
    }
  }

  return notes;
}

/**
 * Generates warm mid-range voicing for the Chord Accompaniment / Hold pad
 */
export function generateChordVoicing(rootSemitone, chordTypeKey) {
  const chordDef = CHORD_TYPES[chordTypeKey] || CHORD_TYPES.major;
  const baseMidi = 48 + rootSemitone; // C3 area
  const bassMidi = 36 + rootSemitone; // C2 bass root
  return {
    bass: { midi: bassMidi, freq: midiToFreq(bassMidi), noteName: midiToNoteName(bassMidi) },
    voices: chordDef.intervals.map(interval => {
      const m = baseMidi + interval;
      return { midi: m, freq: midiToFreq(m), noteName: midiToNoteName(m) };
    })
  };
}

/**
 * Detects a chord from an array of held MIDI notes on the Arturia MiniLab MkII keys.
 * Supports:
 * - Single note: sets root with current chord quality (or Major)
 * - 2 notes: root + interval detection (minor 3rd -> minor, major 3rd -> major, 7th -> 7th)
 * - 3+ notes: full triad / seventh recognition
 */
export function detectChordFromMidiNotes(midiNotes, fallbackQuality = 'major') {
  if (!midiNotes || midiNotes.length === 0) return null;

  const sorted = [...midiNotes].sort((a, b) => a - b);
  const lowestNote = sorted[0];
  const rootSemitone = lowestNote % 12;

  const rootObj = DISPLAY_ROOTS.find(r => r.semitone === rootSemitone) || {
    name: NOTE_NAMES[rootSemitone],
    semitone: rootSemitone
  };

  if (sorted.length === 1) {
    return {
      rootSemitone,
      rootName: rootObj.name,
      type: fallbackQuality
    };
  }

  // Unique pitch classes relative to lowest note
  const pitchClasses = [...new Set(sorted.map(n => n % 12))];

  // Try every note as candidate root to handle inversions, prioritizing the lowest note
  const candidates = [rootSemitone, ...pitchClasses.filter(p => p !== rootSemitone)];

  for (const candidateRoot of candidates) {
    const relIntervals = pitchClasses
      .map(p => (p - candidateRoot + 12) % 12)
      .sort((a, b) => a - b);

    const has = (intv) => relIntervals.includes(intv);
    const cRootObj = DISPLAY_ROOTS.find(r => r.semitone === candidateRoot) || {
      name: NOTE_NAMES[candidateRoot],
      semitone: candidateRoot
    };

    if (has(3) && has(6)) {
      return { rootSemitone: candidateRoot, rootName: cRootObj.name, type: 'dim' };
    }
    if (has(4) && has(8)) {
      return { rootSemitone: candidateRoot, rootName: cRootObj.name, type: 'aug' };
    }
    if (has(4) && has(11)) {
      return { rootSemitone: candidateRoot, rootName: cRootObj.name, type: 'maj7' };
    }
    if (has(4) && has(10)) {
      return { rootSemitone: candidateRoot, rootName: cRootObj.name, type: 'seventh' };
    }
    if (has(3) && has(10)) {
      return { rootSemitone: candidateRoot, rootName: cRootObj.name, type: 'min7' };
    }
    if (has(5) && has(7) && !has(3) && !has(4)) {
      return { rootSemitone: candidateRoot, rootName: cRootObj.name, type: 'sus4' };
    }
    if (has(3) && has(7)) {
      return { rootSemitone: candidateRoot, rootName: cRootObj.name, type: 'minor' };
    }
    if (has(4) && has(7)) {
      return { rootSemitone: candidateRoot, rootName: cRootObj.name, type: 'major' };
    }
  }

  // Two-note dyad fallback relative to lowest note
  const relToLowest = pitchClasses.map(p => (p - rootSemitone + 12) % 12);
  if (relToLowest.includes(3)) {
    return { rootSemitone, rootName: rootObj.name, type: 'minor' };
  }
  if (relToLowest.includes(10)) {
    return { rootSemitone, rootName: rootObj.name, type: 'seventh' };
  }
  if (relToLowest.includes(11)) {
    return { rootSemitone, rootName: rootObj.name, type: 'maj7' };
  }

  return {
    rootSemitone,
    rootName: rootObj.name,
    type: 'major'
  };
}

export function formatChordLabel(rootName, typeKey) {
  const def = CHORD_TYPES[typeKey] || CHORD_TYPES.major;
  return `${rootName}${def.suffix}`;
}
