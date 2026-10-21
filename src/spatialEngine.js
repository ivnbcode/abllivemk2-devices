// SPATIAL-LAB 3D MkII — Multi-Source Web Audio 3D HRTF Binaural Engine
// Uses native PannerNode (panningModel = 'HRTF') + Room Reflections Convolver + Built-in Synth Loops & Custom Audio Loader

export const SPATIAL_PRESETS = [
  { id: 0, pad: 1, name: 'Frente Cercano',   x: 0.0,   y: 0.1,  z: -1.1, speed: 0.0,  radius: 0.0, mode: 'circle' },
  { id: 1, pad: 2, name: 'Detrás Nuca',      x: 0.0,   y: -0.1, z: 1.6,  speed: 0.0,  radius: 0.0, mode: 'circle' },
  { id: 2, pad: 3, name: 'Izquierda Lejos',  x: -2.2,  y: 0.2,  z: -0.4, speed: 0.0,  radius: 0.0, mode: 'circle' },
  { id: 3, pad: 4, name: 'Derecha Lejos',    x: 2.2,   y: 0.2,  z: -0.4, speed: 0.0,  radius: 0.0, mode: 'circle' },
  { id: 4, pad: 5, name: 'Cenit (Domo Sup)', x: 0.0,   y: 2.3,  z: -0.2, speed: 0.35, radius: 0.5, mode: 'circle' },
  { id: 5, pad: 6, name: 'Órbita 360°',      x: 0.0,   y: 0.2,  z: 0.0,  speed: 0.75, radius: 2.0, mode: 'circle' },
  { id: 6, pad: 7, name: 'Infinito 8',       x: 0.0,   y: 0.3,  z: 0.0,  speed: 0.9,  radius: 2.2, mode: 'figure8' },
  { id: 7, pad: 8, name: 'Espiral Domo 3D',  x: 0.0,   y: 0.0,  z: 0.0,  speed: 1.15, radius: 2.3, mode: 'spiral' }
];

export class Spatial3DAudioEngine {
  constructor() {
    this.ctx = null;
    this.isInitialized = false;
    this.isRunning = false;

    this.roomMix = 0.32;
    this.masterVolume = 0.85;

    // 4 Independent 3D Sound Sources in the room
    this.sources = [
      {
        id: 0,
        name: '1 · Arpegio Cristal',
        color: '#48f5c8',
        enabled: true,
        volume: 0.85,
        x: -1.2,
        y: 0.4,
        z: -1.1, // In WebAudio HRTF: negative Z is in front of listener
        orbitSpeed: 0.45,
        orbitRadius: 1.4,
        orbitMode: 'circle',
        phase: 0,
        liveX: -1.2,
        liveY: 0.4,
        liveZ: -1.1,
        customBuffer: null
      },
      {
        id: 1,
        name: '2 · Pulso Bajo Sub',
        color: '#ff8a47',
        enabled: true,
        volume: 0.8,
        x: 1.1,
        y: -0.3,
        z: -0.9,
        orbitSpeed: 0.0,
        orbitRadius: 0.0,
        orbitMode: 'circle',
        phase: 1.57,
        liveX: 1.1,
        liveY: -0.3,
        liveZ: -0.9,
        customBuffer: null
      },
      {
        id: 2,
        name: '3 · Pad Textura Aérea',
        color: '#b877ff',
        enabled: true,
        volume: 0.7,
        x: 0.0,
        y: 1.5,
        z: 1.2,
        orbitSpeed: 0.3,
        orbitRadius: 1.6,
        orbitMode: 'figure8',
        phase: 3.14,
        liveX: 0.0,
        liveY: 1.5,
        liveZ: 1.2,
        customBuffer: null
      },
      {
        id: 3,
        name: '4 · Percusión / Archivo',
        color: '#ffd24d',
        enabled: false,
        volume: 0.8,
        x: -0.8,
        y: 0.0,
        z: 1.3,
        orbitSpeed: 0.85,
        orbitRadius: 1.8,
        orbitMode: 'spiral',
        phase: 4.5,
        liveX: -0.8,
        liveY: 0.0,
        liveZ: 1.3,
        customBuffer: null
      }
    ];

    this.panners = [];
    this.sourceGains = [];
    this.customBufferNodes = [null, null, null, null];
    this.loopTimer = null;
    this.stepCounter = 0;
  }

  async init() {
    if (this.isInitialized) {
      if (this.ctx.state === 'suspended') await this.ctx.resume();
      return;
    }

    const AudioCtx = window.AudioContext || window.webkitAudioContext;
    this.ctx = new AudioCtx();

    // Configure 3D Listener at center (0, 0, 0) looking forward (0, 0, -1)
    const listener = this.ctx.listener;
    if (listener.positionX) {
      listener.positionX.value = 0;
      listener.positionY.value = 0;
      listener.positionZ.value = 0;
      listener.forwardX.value = 0;
      listener.forwardY.value = 0;
      listener.forwardZ.value = -1;
      listener.upX.value = 0;
      listener.upY.value = 1;
      listener.upZ.value = 0;
    } else if (listener.setPosition) {
      listener.setPosition(0, 0, 0);
      listener.setOrientation(0, 0, -1, 0, 1, 0);
    }

    this.masterGain = this.ctx.createGain();
    this.masterGain.gain.value = this.masterVolume;

    this.analyser = this.ctx.createAnalyser();
    this.analyser.fftSize = 256;

    // 3D Room Reverb Convolver
    this.reverbNode = this.ctx.createConvolver();
    this.reverbNode.buffer = this._createRoomImpulse(2.1, 2.5);
    this.reverbWet = this.ctx.createGain();
    this.reverbWet.gain.value = this.roomMix;

    this.reverbNode.connect(this.reverbWet);
    this.reverbWet.connect(this.masterGain);
    this.masterGain.connect(this.analyser);
    this.analyser.connect(this.ctx.destination);

    // Create HRTF PannerNode for each of the 4 sources
    this.sources.forEach((src, idx) => {
      const panner = this.ctx.createPanner();
      panner.panningModel = 'HRTF';
      panner.distanceModel = 'inverse';
      panner.refDistance = 0.8;
      panner.maxDistance = 12;
      panner.rolloffFactor = 1.35;
      panner.coneInnerAngle = 360;

      const sGain = this.ctx.createGain();
      sGain.gain.value = src.enabled ? src.volume : 0.0;

      sGain.connect(panner);
      panner.connect(this.masterGain);
      panner.connect(this.reverbNode);

      this.panners[idx] = panner;
      this.sourceGains[idx] = sGain;
      this._applyPannerPosition(idx, src.x, src.y, src.z);
    });

    this.isInitialized = true;
  }

  _createRoomImpulse(duration, decay) {
    const rate = this.ctx.sampleRate;
    const len = rate * duration;
    const buf = this.ctx.createBuffer(2, len, rate);
    const L = buf.getChannelData(0);
    const R = buf.getChannelData(1);
    for (let i = 0; i < len; i++) {
      const env = Math.pow(1 - i / len, decay);
      L[i] = (Math.random() * 2 - 1) * env;
      R[i] = (Math.random() * 2 - 1) * env;
    }
    return buf;
  }

  _applyPannerPosition(idx, x, y, z) {
    const panner = this.panners[idx];
    if (!panner || !this.ctx) return;
    const now = this.ctx.currentTime;
    if (panner.positionX) {
      panner.positionX.setTargetAtTime(x, now, 0.02);
      panner.positionY.setTargetAtTime(y, now, 0.02);
      panner.positionZ.setTargetAtTime(z, now, 0.02);
    } else if (panner.setPosition) {
      panner.setPosition(x, y, z);
    }
  }

  async startScene() {
    await this.init();
    if (this.isRunning) return;
    this.isRunning = true;

    // Start any custom audio buffers loaded by user
    this.sources.forEach((src, idx) => {
      if (src.customBuffer) {
        this._startCustomBufferLoop(idx);
      }
    });

    // Procedural spatial synth sequencer (16th note ticks every 165ms)
    const arpNotes = [261.63, 329.63, 392.00, 493.88, 523.25, 659.25, 587.33, 392.00];
    const bassNotes = [65.41, 65.41, 87.31, 73.42];
    const padNotes = [196.00, 246.94, 293.66, 329.63];

    this.loopTimer = setInterval(() => {
      if (!this.isRunning || !this.isInitialized) return;
      const step = this.stepCounter++;

      // Source 0: Crystal Pluck Arpeggio
      if (this.sources[0].enabled && !this.sources[0].customBuffer) {
        const freq = arpNotes[step % arpNotes.length];
        this._triggerSynthTone(0, freq, 'triangle', 0.14, 0.24);
      }

      // Source 1: Warm Sub Bass Pulse (every 2 steps)
      if (this.sources[1].enabled && !this.sources[1].customBuffer && step % 2 === 0) {
        const freq = bassNotes[Math.floor(step / 4) % bassNotes.length];
        this._triggerSynthTone(1, freq, 'sawtooth', 0.28, 0.32, 650);
      }

      // Source 2: Ethereal Shimmer Chord Voice (every 4 steps)
      if (this.sources[2].enabled && !this.sources[2].customBuffer && step % 4 === 0) {
        const freq = padNotes[(step / 4) % padNotes.length] * 2;
        this._triggerSynthTone(2, freq, 'sine', 0.65, 0.2);
      }

      // Source 3: Metallic Hi-Hat / Woodblock click (if no custom audio loaded)
      if (this.sources[3].enabled && !this.sources[3].customBuffer) {
        this._triggerPercClick(3, step % 4 === 0 ? 1200 : 820);
      }
    }, 175);
  }

  stopScene() {
    this.isRunning = false;
    if (this.loopTimer) {
      clearInterval(this.loopTimer);
      this.loopTimer = null;
    }
    this.customBufferNodes.forEach((node, idx) => {
      if (node) {
        try { node.stop(); } catch (_) {}
        this.customBufferNodes[idx] = null;
      }
    });
  }

  _triggerSynthTone(sourceIdx, freq, type, duration, peakGain, filterCutoff = 3800) {
    const now = this.ctx.currentTime;
    const osc = this.ctx.createOscillator();
    osc.type = type;
    osc.frequency.setValueAtTime(freq, now);

    const filter = this.ctx.createBiquadFilter();
    filter.type = 'lowpass';
    filter.frequency.setValueAtTime(filterCutoff, now);

    const env = this.ctx.createGain();
    env.gain.setValueAtTime(0.0001, now);
    env.gain.linearRampToValueAtTime(peakGain, now + 0.01);
    env.gain.exponentialRampToValueAtTime(0.0001, now + duration);

    osc.connect(filter);
    filter.connect(env);
    env.connect(this.sourceGains[sourceIdx]);

    osc.start(now);
    osc.stop(now + duration + 0.03);
  }

  _triggerPercClick(sourceIdx, pitch) {
    const now = this.ctx.currentTime;
    const osc = this.ctx.createOscillator();
    osc.type = 'square';
    osc.frequency.setValueAtTime(pitch, now);
    osc.frequency.exponentialRampToValueAtTime(180, now + 0.055);

    const env = this.ctx.createGain();
    env.gain.setValueAtTime(0.22, now);
    env.gain.exponentialRampToValueAtTime(0.0001, now + 0.06);

    osc.connect(env);
    env.connect(this.sourceGains[sourceIdx]);
    osc.start(now);
    osc.stop(now + 0.07);
  }

  async loadAudioFileForSource(sourceIdx, file) {
    await this.init();
    const arrayBuf = await file.arrayBuffer();
    const audioBuf = await this.ctx.decodeAudioData(arrayBuf);
    this.sources[sourceIdx].customBuffer = audioBuf;
    this.sources[sourceIdx].enabled = true;
    this.setSourceEnabled(sourceIdx, true);

    if (this.isRunning) {
      this._startCustomBufferLoop(sourceIdx);
    }
  }

  _startCustomBufferLoop(sourceIdx) {
    if (this.customBufferNodes[sourceIdx]) {
      try { this.customBufferNodes[sourceIdx].stop(); } catch (_) {}
    }
    const buf = this.sources[sourceIdx].customBuffer;
    if (!buf) return;

    const node = this.ctx.createBufferSource();
    node.buffer = buf;
    node.loop = true;
    node.connect(this.sourceGains[sourceIdx]);
    node.start();
    this.customBufferNodes[sourceIdx] = node;
  }

  setSourceEnabled(sourceIdx, enabled) {
    const src = this.sources[sourceIdx];
    if (!src) return;
    src.enabled = enabled;
    if (this.isInitialized && this.sourceGains[sourceIdx]) {
      this.sourceGains[sourceIdx].gain.setTargetAtTime(
        enabled ? src.volume : 0.0,
        this.ctx.currentTime,
        0.02
      );
    }
  }

  setRoomMix(val) {
    this.roomMix = val;
    if (this.isInitialized && this.reverbWet) {
      this.reverbWet.gain.setTargetAtTime(val, this.ctx.currentTime, 0.02);
    }
  }

  updateOrbits(dtSeconds) {
    this.sources.forEach((src, idx) => {
      if (src.orbitSpeed > 0.001 && src.orbitRadius > 0.001) {
        src.phase += src.orbitSpeed * dtSeconds * Math.PI * 2;
        if (src.orbitMode === 'figure8') {
          src.liveX = Math.max(-2.5, Math.min(2.5, src.x + Math.sin(src.phase) * src.orbitRadius));
          src.liveZ = Math.max(-2.5, Math.min(2.5, src.z + Math.sin(src.phase * 2) * src.orbitRadius * 0.75));
          src.liveY = Math.max(-2.5, Math.min(2.5, src.y + Math.cos(src.phase) * 0.5));
        } else if (src.orbitMode === 'spiral') {
          const r = src.orbitRadius * (0.35 + 0.65 * (0.5 + 0.5 * Math.sin(src.phase * 0.4)));
          src.liveX = Math.max(-2.5, Math.min(2.5, Math.sin(src.phase) * r));
          src.liveZ = Math.max(-2.5, Math.min(2.5, Math.cos(src.phase) * r));
          src.liveY = Math.max(-2.5, Math.min(2.5, Math.sin(src.phase * 0.5) * 1.8));
        } else {
          src.liveX = Math.max(-2.5, Math.min(2.5, src.x + Math.sin(src.phase) * src.orbitRadius));
          src.liveZ = Math.max(-2.5, Math.min(2.5, src.z + Math.cos(src.phase) * src.orbitRadius));
          src.liveY = src.y;
        }
      } else {
        src.liveX = src.x;
        src.liveY = src.y;
        src.liveZ = src.z;
      }

      if (this.isInitialized) {
        this._applyPannerPosition(idx, src.liveX, src.liveY, src.liveZ);
      }
    });
  }
}
