import { MidiController } from './midiController.js';
import { Spatial3DAudioEngine, SPATIAL_PRESETS } from './spatialEngine.js';

const spatialAudio = new Spatial3DAudioEngine();

const state = {
  selectedSpatialSource: 0,
  activeSpatialPreset: 0
};

const midiLed = document.getElementById('midiLed');
const midiStatusText = document.getElementById('midiStatusText');
const midiDeviceSelect = document.getElementById('midiDeviceSelect');

const spatialPlayBtn = document.getElementById('spatialPlayBtn');
const spatialRadarCanvas = document.getElementById('spatialRadarCanvas');
const spatialCoordsReadout = document.getElementById('spatialCoordsReadout');
const spatialPadsGrid = document.getElementById('spatialPadsGrid');
const spatialSourcesList = document.getElementById('spatialSourcesList');
const selectedSourceHeader = document.getElementById('selectedSourceHeader');

const spatialXSlider = document.getElementById('spatialXSlider');
const spatialXVal = document.getElementById('spatialXVal');
const spatialYSlider = document.getElementById('spatialYSlider');
const spatialYVal = document.getElementById('spatialYVal');
const spatialZSlider = document.getElementById('spatialZSlider');
const spatialZVal = document.getElementById('spatialZVal');
const spatialOrbitSpeedSlider = document.getElementById('spatialOrbitSpeedSlider');
const spatialOrbitSpeedVal = document.getElementById('spatialOrbitSpeedVal');
const spatialOrbitRadiusSlider = document.getElementById('spatialOrbitRadiusSlider');
const spatialOrbitRadiusVal = document.getElementById('spatialOrbitRadiusVal');
const spatialOrbitModeSelect = document.getElementById('spatialOrbitModeSelect');
const spatialRoomSlider = document.getElementById('spatialRoomSlider');
const spatialRoomVal = document.getElementById('spatialRoomVal');
const spatialAudioFileInput = document.getElementById('spatialAudioFileInput');

function updateSpatialPlayBtnUI() {
  if (!spatialPlayBtn) return;
  spatialPlayBtn.classList.toggle('playing', spatialAudio.isRunning);
  spatialPlayBtn.textContent = spatialAudio.isRunning
    ? '⏹ Detener Escena 3D'
    : '▶ Iniciar Escena 3D';
}

spatialPlayBtn?.addEventListener('click', async () => {
  if (spatialAudio.isRunning) {
    spatialAudio.stopScene();
  } else {
    await spatialAudio.startScene();
  }
  updateSpatialPlayBtnUI();
});

function renderSpatialPads() {
  if (!spatialPadsGrid) return;
  spatialPadsGrid.innerHTML = '';
  SPATIAL_PRESETS.forEach((preset, idx) => {
    const padEl = document.createElement('div');
    padEl.className = 'minilab-pad';
    if (state.activeSpatialPreset === idx) padEl.classList.add('active');
    padEl.innerHTML = `
      <span class="pad-number">PAD ${preset.pad}</span>
      <span class="pad-chord-name" style="font-size:0.78rem;text-align:center;">${preset.name}</span>
      <span class="pad-midi-note">NOTE ${36 + idx}</span>
    `;
    padEl.addEventListener('pointerdown', async () => {
      applySpatialPadPreset(idx);
    });
    spatialPadsGrid.appendChild(padEl);
  });
}

function applySpatialPadPreset(idx) {
  const preset = SPATIAL_PRESETS[idx];
  if (!preset) return;
  state.activeSpatialPreset = idx;
  const src = spatialAudio.sources[state.selectedSpatialSource];
  if (src) {
    src.x = preset.x;
    src.y = preset.y;
    src.z = preset.z;
    src.orbitSpeed = preset.speed;
    src.orbitRadius = preset.radius;
    src.orbitMode = preset.mode;
  }
  renderSpatialPads();
  syncSpatialInspectorUI();
}

function renderSpatialSourcesList() {
  if (!spatialSourcesList) return;
  spatialSourcesList.innerHTML = '';

  spatialAudio.sources.forEach((src, idx) => {
    const card = document.createElement('div');
    card.className = 'source-item-card';
    if (state.selectedSpatialSource === idx) card.classList.add('selected');

    card.innerHTML = `
      <div class="source-left-info">
        <span class="source-color-dot" style="background:${src.color};box-shadow:0 0 8px ${src.color}"></span>
        <div>
          <div class="source-item-title">${src.name}</div>
          <div class="source-coords-mini" id="sourceMiniCoords-${idx}">
            X:${src.liveX.toFixed(1)} Y:${src.liveY.toFixed(1)} Z:${src.liveZ.toFixed(1)}
          </div>
        </div>
      </div>
      <input type="checkbox" ${src.enabled ? 'checked' : ''} title="Activar/Silenciar fuente 3D" />
    `;

    const checkbox = card.querySelector('input[type="checkbox"]');
    checkbox.addEventListener('click', (e) => e.stopPropagation());
    checkbox.addEventListener('change', (e) => {
      spatialAudio.setSourceEnabled(idx, e.target.checked);
    });

    card.addEventListener('click', () => {
      state.selectedSpatialSource = idx;
      renderSpatialSourcesList();
      syncSpatialInspectorUI();
    });

    spatialSourcesList.appendChild(card);
  });
}

function syncSpatialInspectorUI() {
  const src = spatialAudio.sources[state.selectedSpatialSource];
  if (!src) return;

  if (selectedSourceHeader) {
    selectedSourceHeader.textContent = `PARÁMETROS 3D: ${src.name.toUpperCase()}`;
  }
  if (spatialXSlider) {
    spatialXSlider.value = src.x;
    spatialXVal.textContent = `${src.x >= 0 ? '+' : ''}${src.x.toFixed(2)}m`;
  }
  if (spatialYSlider) {
    spatialYSlider.value = src.y;
    spatialYVal.textContent = `${src.y >= 0 ? '+' : ''}${src.y.toFixed(2)}m`;
  }
  if (spatialZSlider) {
    spatialZSlider.value = src.z;
    spatialZVal.textContent = `${src.z >= 0 ? '+' : ''}${src.z.toFixed(2)}m`;
  }
  if (spatialOrbitSpeedSlider) {
    spatialOrbitSpeedSlider.value = src.orbitSpeed;
    spatialOrbitSpeedVal.textContent = `${src.orbitSpeed.toFixed(2)} Hz`;
  }
  if (spatialOrbitRadiusSlider) {
    spatialOrbitRadiusSlider.value = src.orbitRadius;
    spatialOrbitRadiusVal.textContent = `${src.orbitRadius.toFixed(2)}m`;
  }
  if (spatialOrbitModeSelect) {
    spatialOrbitModeSelect.value = src.orbitMode;
  }
}

spatialXSlider?.addEventListener('input', (e) => {
  const src = spatialAudio.sources[state.selectedSpatialSource];
  src.x = parseFloat(e.target.value);
  spatialXVal.textContent = `${src.x >= 0 ? '+' : ''}${src.x.toFixed(2)}m`;
});

spatialYSlider?.addEventListener('input', (e) => {
  const src = spatialAudio.sources[state.selectedSpatialSource];
  src.y = parseFloat(e.target.value);
  spatialYVal.textContent = `${src.y >= 0 ? '+' : ''}${src.y.toFixed(2)}m`;
});

spatialZSlider?.addEventListener('input', (e) => {
  const src = spatialAudio.sources[state.selectedSpatialSource];
  src.z = parseFloat(e.target.value);
  spatialZVal.textContent = `${src.z >= 0 ? '+' : ''}${src.z.toFixed(2)}m`;
});

spatialOrbitSpeedSlider?.addEventListener('input', (e) => {
  const src = spatialAudio.sources[state.selectedSpatialSource];
  src.orbitSpeed = parseFloat(e.target.value);
  spatialOrbitSpeedVal.textContent = `${src.orbitSpeed.toFixed(2)} Hz`;
});

spatialOrbitRadiusSlider?.addEventListener('input', (e) => {
  const src = spatialAudio.sources[state.selectedSpatialSource];
  src.orbitRadius = parseFloat(e.target.value);
  spatialOrbitRadiusVal.textContent = `${src.orbitRadius.toFixed(2)}m`;
});

spatialOrbitModeSelect?.addEventListener('change', (e) => {
  const src = spatialAudio.sources[state.selectedSpatialSource];
  src.orbitMode = e.target.value;
});

spatialRoomSlider?.addEventListener('input', (e) => {
  const v = parseFloat(e.target.value);
  spatialAudio.setRoomMix(v);
  spatialRoomVal.textContent = `${Math.round(v * 100)}%`;
});

spatialAudioFileInput?.addEventListener('change', async (e) => {
  const file = e.target.files?.[0];
  if (!file) return;
  await spatialAudio.loadAudioFileForSource(state.selectedSpatialSource, file);
  renderSpatialSourcesList();
  if (!spatialAudio.isRunning) {
    await spatialAudio.startScene();
    updateSpatialPlayBtnUI();
  }
});

// MIDI Integration for Arturia MiniLab MkII in SPATIAL-LAB 3D
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

  onRawMidi: () => {
    midiLed.classList.add('activity');
    setTimeout(() => midiLed.classList.remove('activity'), 90);
  },

  onTouchStripMove: (normValue) => {
    // Mod Strip (CC 1) rotates the selected 3D source 360 degrees around the listener's head
    const angle = normValue * Math.PI * 2;
    const src = spatialAudio.sources[state.selectedSpatialSource];
    if (src) {
      const radius = Math.max(0.95, Math.hypot(src.x, src.z));
      src.x = Number((Math.sin(angle) * radius).toFixed(2));
      src.z = Number((-Math.cos(angle) * radius).toFixed(2));
      syncSpatialInspectorUI();
    }
  },

  onPadTrigger: (padIndex) => {
    applySpatialPadPreset(padIndex);
  },

  shouldTreatNoteAsPad: () => true
});

midiDeviceSelect?.addEventListener('change', (e) => {
  midi.selectInput(e.target.value);
});

function initSpatialRadarCanvas() {
  if (!spatialRadarCanvas) return;
  const ctx = spatialRadarCanvas.getContext('2d');
  const W = spatialRadarCanvas.width;
  const H = spatialRadarCanvas.height;
  const cx = W / 2;
  const cy = H / 2;
  const scale = 80;

  let draggingSourceIdx = null;

  function canvasCoordsToMeters(clientX, clientY) {
    const rect = spatialRadarCanvas.getBoundingClientRect();
    const px = ((clientX - rect.left) / rect.width) * W;
    const py = ((clientY - rect.top) / rect.height) * H;
    const mx = Math.max(-2.5, Math.min(2.5, (px - cx) / scale));
    const mz = Math.max(-2.5, Math.min(2.5, (py - cy) / scale));
    return { mx, mz };
  }

  spatialRadarCanvas.addEventListener('pointerdown', (e) => {
    const { mx, mz } = canvasCoordsToMeters(e.clientX, e.clientY);
    let bestIdx = state.selectedSpatialSource;
    let bestDist = 999;
    spatialAudio.sources.forEach((src, idx) => {
      const d = Math.hypot(src.liveX - mx, src.liveZ - mz);
      if (d < bestDist && d < 0.55) {
        bestDist = d;
        bestIdx = idx;
      }
    });

    state.selectedSpatialSource = bestIdx;
    draggingSourceIdx = bestIdx;
    const src = spatialAudio.sources[bestIdx];
    src.x = Number(mx.toFixed(2));
    src.z = Number(mz.toFixed(2));
    renderSpatialSourcesList();
    syncSpatialInspectorUI();
  });

  window.addEventListener('pointermove', (e) => {
    if (draggingSourceIdx === null) return;
    const { mx, mz } = canvasCoordsToMeters(e.clientX, e.clientY);
    const src = spatialAudio.sources[draggingSourceIdx];
    src.x = Number(mx.toFixed(2));
    src.z = Number(mz.toFixed(2));
    syncSpatialInspectorUI();
  });

  window.addEventListener('pointerup', () => {
    draggingSourceIdx = null;
  });

  let lastTime = performance.now();

  function renderRadarFrame(now) {
    requestAnimationFrame(renderRadarFrame);
    const dt = Math.min(0.1, (now - lastTime) / 1000);
    lastTime = now;

    spatialAudio.updateOrbits(dt);

    ctx.clearRect(0, 0, W, H);

    ctx.strokeStyle = 'rgba(72, 245, 200, 0.14)';
    ctx.lineWidth = 1;
    [1, 2, 2.5].forEach(r => {
      ctx.beginPath();
      ctx.arc(cx, cy, r * scale, 0, Math.PI * 2);
      ctx.stroke();
    });

    ctx.beginPath();
    ctx.moveTo(cx, 12);
    ctx.lineTo(cx, H - 12);
    ctx.moveTo(12, cy);
    ctx.lineTo(W - 12, cy);
    ctx.stroke();

    ctx.fillStyle = 'rgba(160, 185, 200, 0.5)';
    ctx.font = '11px "JetBrains Mono", monospace';
    ctx.fillText('FRENTE (Z-)', cx - 34, 24);
    ctx.fillText('DETRÁS (Z+)', cx - 34, H - 12);
    ctx.fillText('IZQ (X-)', 14, cy - 8);
    ctx.fillText('DER (X+)', W - 68, cy - 8);

    ctx.fillStyle = '#253542';
    ctx.strokeStyle = '#48f5c8';
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.arc(cx, cy, 16, 0, Math.PI * 2);
    ctx.fill();
    ctx.stroke();

    ctx.beginPath();
    ctx.moveTo(cx - 5, cy - 14);
    ctx.lineTo(cx, cy - 23);
    ctx.lineTo(cx + 5, cy - 14);
    ctx.stroke();

    spatialAudio.sources.forEach((src, idx) => {
      const sx = cx + src.liveX * scale;
      const sz = cy + src.liveZ * scale;
      const isSelected = idx === state.selectedSpatialSource;

      ctx.strokeStyle = src.enabled ? `${src.color}55` : 'rgba(120,120,120,0.2)';
      ctx.lineWidth = isSelected ? 2 : 1;
      ctx.beginPath();
      ctx.moveTo(cx, cy);
      ctx.lineTo(sx, sz);
      ctx.stroke();

      const elevRadius = Math.max(8, 15 + src.liveY * 4.5);
      ctx.strokeStyle = src.color;
      ctx.lineWidth = isSelected ? 2.5 : 1.2;
      ctx.beginPath();
      ctx.arc(sx, sz, elevRadius + 5, 0, Math.PI * 2);
      ctx.stroke();

      ctx.fillStyle = src.enabled ? src.color : '#555';
      ctx.beginPath();
      ctx.arc(sx, sz, elevRadius, 0, Math.PI * 2);
      ctx.fill();

      ctx.fillStyle = '#090d11';
      ctx.font = 'bold 11px "Space Grotesk", sans-serif';
      ctx.fillText(String(idx + 1), sx - 3.5, sz + 4);

      const miniEl = document.getElementById(`sourceMiniCoords-${idx}`);
      if (miniEl) {
        miniEl.textContent = `X:${src.liveX.toFixed(1)} Y:${src.liveY.toFixed(1)} Z:${src.liveZ.toFixed(1)}`;
      }
    });

    const activeSrc = spatialAudio.sources[state.selectedSpatialSource];
    if (activeSrc && spatialCoordsReadout) {
      spatialCoordsReadout.textContent =
        `${activeSrc.name.toUpperCase()} · X: ${activeSrc.liveX.toFixed(2)}m · Y: ${activeSrc.liveY.toFixed(2)}m · Z: ${activeSrc.liveZ.toFixed(2)}m`;
    }
  }

  requestAnimationFrame(renderRadarFrame);
}

function boot() {
  renderSpatialPads();
  renderSpatialSourcesList();
  syncSpatialInspectorUI();
  initSpatialRadarCanvas();
  midi.init();
}

boot();
