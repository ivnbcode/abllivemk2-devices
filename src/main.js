import {
  DISPLAY_ROOTS,
  CHORD_TYPES,
  DEFAULT_PAD_CHORDS,
  NOTE_NAMES,
  generateStrumPlateNotes,
  generateChordVoicing,
  detectChordFromMidiNotes,
  formatChordLabel,
  midiToNoteName
} from './chords.js';
import { OmnichordAudioEngine } from './audioEngine.js';
import { MidiController } from './midiController.js';

const audio = new OmnichordAudioEngine();

// Application State
const state = {
  rootSemitone: 0,
  rootName: 'C',
  chordType: 'major',
  numStrings: 16,
  strumNotes: [],
  padChords: JSON.parse(localStorage.getItem('omnilab_pads')) || [...DEFAULT_PAD_CHORDS],
  activePadIndex: 0,
  assigningPadMode: false,
  padsAnyChannel: false,
  isPointerDownOnPlate: false,
  lastPointerStringIdx: null
};

// DOM Elements
const midiLed = document.getElementById('midiLed');
const midiStatusText = document.getElementById('midiStatusText');
const midiDeviceSelect = document.getElementById('midiDeviceSelect');
const strumStripSelect = document.getElementById('strumStripSelect');
const midiLearnBtn = document.getElementById('midiLearnBtn');
const midiActivityReadout = document.getElementById('midiActivityReadout');

const activeChordSymbol = document.getElementById('activeChordSymbol');
const activeChordFullName = document.getElementById('activeChordFullName');
const activeChordNotes = document.getElementById('activeChordNotes');
const chordMatrixGrid = document.getElementById('chordMatrixGrid');
const minilabPadsGrid = document.getElementById('minilabPadsGrid');
const assignPadBtn = document.getElementById('assignPadBtn');
const padsAnyChannelToggle = document.getElementById('padsAnyChannelToggle');
const heldKeysReadout = document.getElementById('heldKeysReadout');
const miniKeyboard = document.getElementById('miniKeyboard');

const sonicStringsList = document.getElementById('sonicStringsList');
const strumplateSurface = document.getElementById('strumplateSurface');
const touchStripIndicator = document.getElementById('touchStripIndicator');
const modStripBar = document.getElementById('modStripBar');
const modStripPercent = document.getElementById('modStripPercent');
const autoStrumBtn = document.getElementById('autoStrumBtn');
const openCatalogBtn = document.getElementById('openCatalogBtn');
const devicesHubSection = document.getElementById('devicesHubSection');
const waveformCanvas = document.getElementById('waveformCanvas');

if (openCatalogBtn && devicesHubSection) {
  openCatalogBtn.addEventListener('click', () => {
    devicesHubSection.scrollIntoView({ behavior: 'smooth' });
  });
}

// Ensure AudioContext starts on first user gesture
async function ensureAudioReady() {
  await audio.init();
}

window.addEventListener('pointerdown', ensureAudioReady, { once: true });
window.addEventListener('keydown', ensureAudioReady, { once: true });

// Set Active Chord & Update Sonic Strings Pitches
function setActiveChord(rootSemitone, rootName, chordType, sourceTag = 'MATRIX', triggerPad = true) {
  state.rootSemitone = rootSemitone;
  state.rootName = rootName;
  state.chordType = chordType;
  state.strumNotes = generateStrumPlateNotes(rootSemitone, chordType, state.numStrings);

  const chordDef = CHORD_TYPES[chordType] || CHORD_TYPES.major;
  const symbol = formatChordLabel(rootName, chordType);
  activeChordSymbol.textContent = symbol;
  activeChordFullName.textContent = `${rootName} ${chordDef.name}`;

  const pitchClassNames = chordDef.intervals.map(intv => NOTE_NAMES[(rootSemitone + intv) % 12]);
  activeChordNotes.textContent = pitchClassNames.join(' · ');

  // Highlight matching button in 4x12 matrix
  document.querySelectorAll('.chord-btn').forEach(btn => {
    const isMatch =
      Number(btn.dataset.semitone) === rootSemitone &&
      btn.dataset.type === chordType;
    btn.classList.toggle('active', isMatch);
  });

  // Update note labels on the 16 Sonic Strings
  // Note: top of vertical plate is high pitch (string 15), bottom is low pitch (string 0)
  // so sliding UP the Arturia Modulation Strip (0 -> 127) goes from bottom (low) to top (high)
  updateSonicStringsLabels();

  // Play or update warm Omnichord chord hold pad if enabled
  if (triggerPad && audio.isInitialized && audio.params.chordPadEnabled) {
    const voicing = generateChordVoicing(rootSemitone, chordType);
    audio.playChordPad(voicing, `${rootSemitone}:${chordType}`);
  }
}

// Pluck a specific string (0 = lowest pitch at bottom, 15 = highest pitch at top)
function triggerSonicString(stringIndex, velocity = 0.85) {
  if (stringIndex < 0 || stringIndex >= state.strumNotes.length) return;
  const noteObj = state.strumNotes[stringIndex];
  if (!noteObj) return;

  if (!audio.isInitialized) {
    audio.init().then(() => {
      audio.pluckString(noteObj.freq, velocity, stringIndex);
    });
  } else {
    audio.pluckString(noteObj.freq, velocity, stringIndex);
  }

  // Visual glow on the plucked string
  const rowEl = document.querySelector(`.sonic-string-row[data-index="${stringIndex}"]`);
  if (rowEl) {
    rowEl.classList.remove('plucked');
    void rowEl.offsetWidth;
    rowEl.classList.add('plucked');
    setTimeout(() => {
      rowEl.classList.remove('plucked');
    }, 190);
  }
}

// Build 4x12 Omnichord Matrix Buttons
function renderChordMatrix() {
  chordMatrixGrid.innerHTML = '';
  const rowTypes = ['major', 'minor', 'seventh', 'maj7'];

  rowTypes.forEach(typeKey => {
    DISPLAY_ROOTS.forEach(root => {
      const btn = document.createElement('button');
      btn.className = `chord-btn type-${typeKey}`;
      btn.dataset.semitone = root.semitone;
      btn.dataset.rootName = root.name;
      btn.dataset.type = typeKey;
      btn.textContent = formatChordLabel(root.name, typeKey);

      btn.addEventListener('pointerdown', async () => {
        await ensureAudioReady();
        state.activePadIndex = null;
        renderMiniLabPads();
        setActiveChord(root.semitone, root.name, typeKey, 'MATRIX', true);
      });

      chordMatrixGrid.appendChild(btn);
    });
  });
}

// Build 8 Arturia MiniLab MkII Pads
function renderMiniLabPads() {
  minilabPadsGrid.innerHTML = '';
  state.padChords.forEach((padCfg, idx) => {
    const padEl = document.createElement('div');
    padEl.className = 'minilab-pad';
    if (state.activePadIndex === idx) padEl.classList.add('active');
    if (state.assigningPadMode) padEl.classList.add('assign-waiting');

    const chordLabel = formatChordLabel(padCfg.rootName, padCfg.type);
    padEl.innerHTML = `
      <span class="pad-number">PAD ${idx + 1}</span>
      <span class="pad-chord-name">${chordLabel}</span>
      <span class="pad-midi-note">NOTE ${padCfg.midiNote}</span>
    `;

    padEl.addEventListener('pointerdown', async () => {
      await ensureAudioReady();
      handlePadSelection(idx);
    });

    minilabPadsGrid.appendChild(padEl);
  });
}

function handlePadSelection(padIndex) {
  if (state.assigningPadMode) {
    // Save current chord into this pad
    state.padChords[padIndex] = {
      ...state.padChords[padIndex],
      rootSemitone: state.rootSemitone,
      rootName: state.rootName,
      type: state.chordType
    };
    localStorage.setItem('omnilab_pads', JSON.stringify(state.padChords));
    state.assigningPadMode = false;
    assignPadBtn.classList.remove('learning');
    assignPadBtn.textContent = 'Asignar Acorde Actual a un Pad';
  }

  state.activePadIndex = padIndex;
  const padCfg = state.padChords[padIndex];
  renderMiniLabPads();
  setActiveChord(padCfg.rootSemitone, padCfg.rootName, padCfg.type, `PAD ${padIndex + 1}`, true);
}

// Build 16 Sonic Strings UI (Top = String 15 High Pitch, Bottom = String 0 Low Pitch)
function renderSonicStrings() {
  sonicStringsList.innerHTML = '';
  for (let i = state.numStrings - 1; i >= 0; i--) {
    const row = document.createElement('div');
    row.className = 'sonic-string-row';
    row.dataset.index = i;
    row.innerHTML = `
      <span class="string-idx">S${String(i + 1).padStart(2, '0')}</span>
      <span class="string-note" id="stringNoteLabel-${i}">--</span>
    `;

    row.addEventListener('pointerdown', async (e) => {
      e.preventDefault();
      await ensureAudioReady();
      state.isPointerDownOnPlate = true;
      state.lastPointerStringIdx = i;
      triggerSonicString(i, 0.9);
    });

    row.addEventListener('pointerenter', () => {
      if (state.isPointerDownOnPlate && state.lastPointerStringIdx !== i) {
        state.lastPointerStringIdx = i;
        triggerSonicString(i, 0.85);
      }
    });

    sonicStringsList.appendChild(row);
  }

  window.addEventListener('pointerup', () => {
    state.isPointerDownOnPlate = false;
    state.lastPointerStringIdx = null;
  });
}

function updateSonicStringsLabels() {
  state.strumNotes.forEach((n, idx) => {
    const labelEl = document.getElementById(`stringNoteLabel-${idx}`);
    if (labelEl) {
      labelEl.textContent = n.noteName;
    }
  });
}

// Build Visual 25-Key MiniLab Keyboard (MIDI 48 C3 to 72 C5)
function renderMiniKeyboard() {
  miniKeyboard.innerHTML = '';
  const blackSemitones = new Set([1, 3, 6, 8, 10]);

  for (let midi = 48; midi <= 72; midi++) {
    const semitone = midi % 12;
    const isBlack = blackSemitones.has(semitone);
    const keyEl = document.createElement('div');
    keyEl.className = `mini-key ${isBlack ? 'black-key' : 'white-key'}`;
    keyEl.dataset.midi = midi;
    if (semitone === 0) {
      keyEl.textContent = midiToNoteName(midi);
    }

    // Allow clicking on virtual mini keys to test chord detection too
    keyEl.addEventListener('pointerdown', async () => {
      await ensureAudioReady();
      const rootObj = DISPLAY_ROOTS.find(r => r.semitone === semitone) || {
        name: NOTE_NAMES[semitone],
        semitone
      };
      setActiveChord(semitone, rootObj.name, state.chordType, 'KEY', true);
    });

    miniKeyboard.appendChild(keyEl);
  }
}

function updateMiniKeyboardHighlight(heldNotes) {
  const heldSet = new Set(heldNotes);
  document.querySelectorAll('.mini-key').forEach(keyEl => {
    const m = Number(keyEl.dataset.midi);
    // Highlight exact note or same pitch class if outside 48..72
    const isHeld = heldSet.has(m) || heldNotes.some(n => n % 12 === m % 12 && (n < 48 || n > 72));
    keyEl.classList.toggle('held', isHeld);
  });

  if (heldNotes.length === 0) {
    heldKeysReadout.textContent = 'Toca cualquier nota o tríada en el teclado...';
  } else {
    const names = heldNotes.map(n => midiToNoteName(n)).join(' + ');
    heldKeysReadout.textContent = `Notas activas: ${names}`;
  }
}

// Initialize MIDI Controller for Arturia MiniLab MkII
const midi = new MidiController({
  onStatusChange: (status) => {
    midiStatusText.textContent = status.message;
    midiLed.classList.toggle('connected', status.connected);
  },

  onDevicesChanged: (inputs, active) => {
    midiDeviceSelect.innerHTML = '';
    if (inputs.length === 0) {
      const opt = document.createElement('option');
      opt.value = '';
      opt.textContent = 'Sin dispositivos MIDI USB';
      midiDeviceSelect.appendChild(opt);
      return;
    }

    inputs.forEach(inp => {
      const opt = document.createElement('option');
      opt.value = inp.id;
      opt.textContent = inp.name || `Dispositivo MIDI (${inp.id})`;
      if (active && active.id === inp.id) {
        opt.selected = true;
      }
      midiDeviceSelect.appendChild(opt);
    });
  },

  onRawMidi: ({ status, channel, data1, data2 }) => {
    // Flash activity LED
    midiLed.classList.add('activity');
    setTimeout(() => midiLed.classList.remove('activity'), 90);

    if (status === 0xb0) {
      midiActivityReadout.textContent = `CC${data1}: ${String(data2).padStart(3, '0')}`;
    } else if (status === 0xe0) {
      const val = ((data2 << 7) | data1) >> 7;
      midiActivityReadout.textContent = `PITCH: ${String(val).padStart(3, '0')}`;
    } else if (status === 0x90 && data2 > 0) {
      midiActivityReadout.textContent = `NOTE: ${midiToNoteName(data1)} (Ch${channel})`;
    }
  },

  onTouchStripMove: (normValue) => {
    // Update visual cursor on the Strumplate (0 = bottom, 1 = top)
    const pct = Math.round(normValue * 100);
    modStripBar.style.width = `${pct}%`;
    modStripPercent.textContent = `${pct}%`;

    touchStripIndicator.classList.add('visible');
    const topPercent = (1 - normValue) * 96;
    touchStripIndicator.style.top = `${topPercent}%`;
  },

  onStrumString: (stringIdx, velocity) => {
    triggerSonicString(stringIdx, velocity);
  },

  onPadTrigger: (padIndex) => {
    handlePadSelection(padIndex);
  },

  shouldTreatNoteAsPad: () => state.padsAnyChannel,

  onKeysChanged: (heldNotes, changedNote, isNoteOn) => {
    updateMiniKeyboardHighlight(heldNotes);
    if (isNoteOn && heldNotes.length > 0) {
      const detected = detectChordFromMidiNotes(heldNotes, state.chordType);
      if (detected) {
        state.activePadIndex = null;
        renderMiniLabPads();
        setActiveChord(detected.rootSemitone, detected.rootName, detected.type, 'MINILAB KEYS', true);
      }
    }
  },

  onStrumConfigChanged: (cfg) => {
    midiLearnBtn.classList.remove('learning');
    if (cfg.source === 'cc') {
      midiLearnBtn.textContent = `Aprendido: CC ${cfg.ccNumber}`;
    } else {
      midiLearnBtn.textContent = 'Aprendido: Pitch Bend';
    }
  }
});

// UI Controls & Event Listeners
midiDeviceSelect.addEventListener('change', (e) => {
  midi.selectInput(e.target.value);
});

strumStripSelect.addEventListener('change', (e) => {
  const val = e.target.value;
  if (val === 'cc:1') {
    midi.strumConfig.source = 'cc';
    midi.strumConfig.ccNumber = 1;
  } else if (val === 'pitchbend') {
    midi.strumConfig.source = 'pitchbend';
  } else if (val === 'both') {
    midi.strumConfig.source = 'both';
    midi.strumConfig.ccNumber = 1;
  }
});

midiLearnBtn.addEventListener('click', () => {
  if (midi.learningTarget === 'strumStrip') {
    midi.cancelMidiLearn();
    midiLearnBtn.classList.remove('learning');
    midiLearnBtn.textContent = 'MIDI Learn Strum';
  } else {
    midi.startMidiLearn('strumStrip');
    midiLearnBtn.classList.add('learning');
    midiLearnBtn.textContent = 'Mueve tu tira táctil...';
  }
});

assignPadBtn.addEventListener('click', () => {
  state.assigningPadMode = !state.assigningPadMode;
  assignPadBtn.classList.toggle('learning', state.assigningPadMode);
  assignPadBtn.textContent = state.assigningPadMode
    ? 'Toca un Pad (1-8) para guardar...'
    : 'Asignar Acorde Actual a un Pad';
  renderMiniLabPads();
});

padsAnyChannelToggle.addEventListener('change', (e) => {
  state.padsAnyChannel = e.target.checked;
});

// Synth Parameter Sliders
const harpVolSlider = document.getElementById('harpVolSlider');
const harpVolVal = document.getElementById('harpVolVal');
harpVolSlider.addEventListener('input', (e) => {
  const v = parseFloat(e.target.value);
  audio.setParam('harpVolume', v);
  harpVolVal.textContent = `${Math.round(v * 100)}%`;
});

const sustainSlider = document.getElementById('sustainSlider');
const sustainVal = document.getElementById('sustainVal');
sustainSlider.addEventListener('input', (e) => {
  const v = parseFloat(e.target.value);
  audio.setParam('sustain', v);
  sustainVal.textContent = `${v.toFixed(1)}s`;
});

const toneSlider = document.getElementById('toneSlider');
const toneVal = document.getElementById('toneVal');
toneSlider.addEventListener('input', (e) => {
  const v = parseFloat(e.target.value);
  audio.setParam('toneCutoff', v);
  toneVal.textContent = `${(v / 1000).toFixed(1)}kHz`;
});

const chorusSlider = document.getElementById('chorusSlider');
const chorusVal = document.getElementById('chorusVal');
chorusSlider.addEventListener('input', (e) => {
  const v = parseFloat(e.target.value);
  audio.setParam('chorusMix', v);
  chorusVal.textContent = `${Math.round(v * 100)}%`;
});

const reverbSlider = document.getElementById('reverbSlider');
const reverbVal = document.getElementById('reverbVal');
reverbSlider.addEventListener('input', (e) => {
  const v = parseFloat(e.target.value);
  audio.setParam('reverbMix', v);
  reverbVal.textContent = `${Math.round(v * 100)}%`;
});

const chordVolSlider = document.getElementById('chordVolSlider');
const chordVolVal = document.getElementById('chordVolVal');
chordVolSlider.addEventListener('input', (e) => {
  const v = parseFloat(e.target.value);
  audio.setParam('chordVolume', v);
  chordVolVal.textContent = `${Math.round(v * 100)}%`;
});

const chordPadToggle = document.getElementById('chordPadToggle');
chordPadToggle.addEventListener('change', (e) => {
  const enabled = e.target.checked;
  audio.setParam('chordPadEnabled', enabled);
  if (enabled && audio.isInitialized) {
    const voicing = generateChordVoicing(state.rootSemitone, state.chordType);
    audio.playChordPad(voicing, `${state.rootSemitone}:${state.chordType}`);
  }
});

// Demo Auto-Strum Button & Spacebar shortcut
async function performAutoStrum() {
  await ensureAudioReady();
  for (let i = 0; i < state.numStrings; i++) {
    setTimeout(() => {
      triggerSonicString(i, 0.82);
    }, i * 28);
  }
}

autoStrumBtn.addEventListener('click', performAutoStrum);

// PC Keyboard Shortcuts for quick chord testing
window.addEventListener('keydown', async (e) => {
  if (e.repeat) return;
  if (e.code === 'Space') {
    e.preventDefault();
    performAutoStrum();
    return;
  }

  // Keys 1..0, -, = select roots in the Omnichord circle-of-fifths order
  const matchedRoot = DISPLAY_ROOTS.find(r => r.keyHint === e.key);
  if (matchedRoot) {
    await ensureAudioReady();
    let type = state.chordType;
    if (e.shiftKey) type = 'minor';
    else if (e.altKey) type = 'seventh';
    setActiveChord(matchedRoot.semitone, matchedRoot.name, type, 'PC KEYBOARD', true);
  }
});

// Real-time OLED Waveform Visualizer
function startWaveformLoop() {
  const ctx2d = waveformCanvas.getContext('2d');
  const width = waveformCanvas.width;
  const height = waveformCanvas.height;
  const buffer = new Uint8Array(128);

  function draw() {
    requestAnimationFrame(draw);
    ctx2d.clearRect(0, 0, width, height);

    ctx2d.strokeStyle = '#48f5c8';
    ctx2d.lineWidth = 2;
    ctx2d.beginPath();

    if (audio.isInitialized && audio.analyser) {
      audio.analyser.getByteTimeDomainData(buffer);
      const sliceWidth = width / buffer.length;
      let x = 0;
      for (let i = 0; i < buffer.length; i++) {
        const v = buffer[i] / 128.0;
        const y = (v * height) / 2;
        if (i === 0) ctx2d.moveTo(x, y);
        else ctx2d.lineTo(x, y);
        x += sliceWidth;
      }
    } else {
      ctx2d.moveTo(0, height / 2);
      ctx2d.lineTo(width, height / 2);
    }

    ctx2d.stroke();
  }

  draw();
}

// Boot Application
function boot() {
  renderChordMatrix();
  renderMiniLabPads();
  renderSonicStrings();
  renderMiniKeyboard();
  setActiveChord(0, 'C', 'major', 'INIT', false);
  midi.init();
  startWaveformLoop();
}

boot();
