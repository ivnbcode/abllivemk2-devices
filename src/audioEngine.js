// Authentic Suzuki Omnichord Sound Engine (Web Audio API)
// Emulates the divided-oscillator Sonic Strings harp + Vintage Analog Chorus + Lush Reverb + Optional Chord Pad

export class OmnichordAudioEngine {
  constructor() {
    this.ctx = null;
    this.isInitialized = false;

    // Parameters controllable via UI or MIDI knobs
    this.params = {
      masterVolume: 0.85,
      harpVolume: 0.85,
      chordVolume: 0.45,
      sustain: 1.4,       // Release time in seconds (0.15s - 4.0s)
      toneCutoff: 3400,   // Lowpass filter frequency in Hz (600Hz - 9000Hz)
      chorusMix: 0.55,    // Vintage analog chorus depth
      reverbMix: 0.35,    // Lush hall reverb level
      vibratoDepth: 4.5,  // Vibrato in cents
      chordPadEnabled: true // Whether selecting a chord plays a gentle backing organ pad
    };

    // Nodes
    this.masterGain = null;
    this.harpBus = null;
    this.chordBus = null;
    this.toneFilter = null;
    this.chorusWet = null;
    this.reverbWet = null;
    this.analyser = null;

    // Active sustained chord pad oscillators
    this.activeChordVoices = [];
    this.currentChordKey = null;
  }

  async init() {
    if (this.isInitialized) {
      if (this.ctx.state === 'suspended') {
        await this.ctx.resume();
      }
      return;
    }

    const AudioCtx = window.AudioContext || window.webkitAudioContext;
    this.ctx = new AudioCtx();

    // Master chain
    this.masterGain = this.ctx.createGain();
    this.masterGain.gain.value = this.params.masterVolume;

    // Compressor to glue fast arpeggio strums smoothly without clipping
    this.compressor = this.ctx.createDynamicsCompressor();
    this.compressor.threshold.value = -16;
    this.compressor.knee.value = 18;
    this.compressor.ratio.value = 4;
    this.compressor.attack.value = 0.004;
    this.compressor.release.value = 0.18;

    // Analyser for visual waveform feedback
    this.analyser = this.ctx.createAnalyser();
    this.analyser.fftSize = 256;

    // Tone Filter (Classic Omnichord brightness control)
    this.toneFilter = this.ctx.createBiquadFilter();
    this.toneFilter.type = 'lowpass';
    this.toneFilter.frequency.value = this.params.toneCutoff;
    this.toneFilter.Q.value = 1.1;

    // Harp Bus & Chord Bus
    this.harpBus = this.ctx.createGain();
    this.harpBus.gain.value = this.params.harpVolume;

    this.chordBus = this.ctx.createGain();
    this.chordBus.gain.value = this.params.chordVolume;

    // Connect buses -> Tone Filter
    this.harpBus.connect(this.toneFilter);
    this.chordBus.connect(this.toneFilter);

    // Dry path
    this.dryGain = this.ctx.createGain();
    this.dryGain.gain.value = 0.85;
    this.toneFilter.connect(this.dryGain);
    this.dryGain.connect(this.compressor);

    // Stereo BBD Chorus Effect (crucial for the dreamy Omnichord shimmer)
    this._setupChorus();

    // Algorithmic Lush Reverb
    this._setupReverb();

    // Output routing
    this.compressor.connect(this.masterGain);
    this.masterGain.connect(this.analyser);
    this.analyser.connect(this.ctx.destination);

    this.isInitialized = true;
  }

  _setupChorus() {
    this.chorusWet = this.ctx.createGain();
    this.chorusWet.gain.value = this.params.chorusMix;

    const delayL = this.ctx.createDelay(0.05);
    const delayR = this.ctx.createDelay(0.05);
    delayL.delayTime.value = 0.014;
    delayR.delayTime.value = 0.019;

    const lfoL = this.ctx.createOscillator();
    const lfoR = this.ctx.createOscillator();
    lfoL.type = 'sine';
    lfoR.type = 'sine';
    lfoL.frequency.value = 0.85;
    lfoR.frequency.value = 1.12;

    const depthL = this.ctx.createGain();
    const depthR = this.ctx.createGain();
    depthL.gain.value = 0.0025;
    depthR.gain.value = 0.0028;

    lfoL.connect(depthL);
    lfoR.connect(depthR);
    depthL.connect(delayL.delayTime);
    depthR.connect(delayR.delayTime);

    const merger = this.ctx.createChannelMerger(2);
    this.toneFilter.connect(delayL);
    this.toneFilter.connect(delayR);

    delayL.connect(merger, 0, 0);
    delayR.connect(merger, 0, 1);

    merger.connect(this.chorusWet);
    this.chorusWet.connect(this.compressor);

    lfoL.start();
    lfoR.start();
  }

  _setupReverb() {
    this.reverbNode = this.ctx.createConvolver();
    this.reverbNode.buffer = this._createImpulseResponse(2.4, 2.3);

    this.reverbWet = this.ctx.createGain();
    this.reverbWet.gain.value = this.params.reverbMix;

    this.toneFilter.connect(this.reverbNode);
    this.reverbNode.connect(this.reverbWet);
    this.reverbWet.connect(this.compressor);
  }

  _createImpulseResponse(duration, decay) {
    const sampleRate = this.ctx.sampleRate;
    const length = sampleRate * duration;
    const impulse = this.ctx.createBuffer(2, length, sampleRate);
    const left = impulse.getChannelData(0);
    const right = impulse.getChannelData(1);

    for (let i = 0; i < length; i++) {
      const n = i / length;
      const env = Math.pow(1 - n, decay);
      left[i] = (Math.random() * 2 - 1) * env;
      right[i] = (Math.random() * 2 - 1) * env;
    }
    return impulse;
  }

  /**
   * Plucks a single Sonic String on the Omnichord Strumplate.
   * Emulates the classic Suzuki OM-27/OM-84 brightness, attack chime, and warm tail.
   */
  pluckString(freq, velocity = 0.85, stringIndex = 0) {
    if (!this.isInitialized) return;
    if (this.ctx.state === 'suspended') {
      this.ctx.resume();
    }

    const now = this.ctx.currentTime;
    const sustain = this.params.sustain;

    // Voice envelope
    const voiceGain = this.ctx.createGain();
    const peakGain = Math.min(0.32, 0.24 * velocity);
    voiceGain.gain.setValueAtTime(0.0001, now);
    voiceGain.gain.linearRampToValueAtTime(peakGain, now + 0.005);
    voiceGain.gain.exponentialRampToValueAtTime(peakGain * 0.38, now + 0.12);
    voiceGain.gain.exponentialRampToValueAtTime(0.0001, now + sustain);

    // Per-voice dynamic filter (gives the characteristic metallic-to-warm Omnichord pluck)
    const pluckFilter = this.ctx.createBiquadFilter();
    pluckFilter.type = 'lowpass';
    const startFreq = Math.min(10000, freq * 7.5);
    const endFreq = Math.max(freq * 1.8, 700);
    pluckFilter.frequency.setValueAtTime(startFreq, now);
    pluckFilter.frequency.exponentialRampToValueAtTime(endFreq, now + Math.min(0.45, sustain * 0.5));
    pluckFilter.Q.value = 1.6;

    // Oscillator 1: Pulse/Square wave (classic top-octave divider tone)
    const osc1 = this.ctx.createOscillator();
    osc1.type = 'square';
    osc1.frequency.setValueAtTime(freq, now);

    // Oscillator 2: Sawtooth slightly detuned for richness
    const osc2 = this.ctx.createOscillator();
    osc2.type = 'sawtooth';
    osc2.frequency.setValueAtTime(freq, now);
    osc2.detune.setValueAtTime(5.5, now);

    // Oscillator 3: Octave harmonic chime (transient sparkle typical of Omnichord Sonic Strings)
    const oscChime = this.ctx.createOscillator();
    oscChime.type = 'triangle';
    oscChime.frequency.setValueAtTime(freq * 2, now);

    const chimeGain = this.ctx.createGain();
    chimeGain.gain.setValueAtTime(0.28, now);
    chimeGain.gain.exponentialRampToValueAtTime(0.001, now + 0.09);

    // Subtle pitch vibrato LFO
    if (this.params.vibratoDepth > 0) {
      const vibLfo = this.ctx.createOscillator();
      const vibGain = this.ctx.createGain();
      vibLfo.frequency.value = 5.5;
      vibGain.gain.value = this.params.vibratoDepth;
      vibLfo.connect(vibGain);
      vibGain.connect(osc1.detune);
      vibGain.connect(osc2.detune);
      vibLfo.start(now);
      vibLfo.stop(now + sustain + 0.05);
    }

    const osc1Gain = this.ctx.createGain();
    osc1Gain.gain.value = 0.55;

    const osc2Gain = this.ctx.createGain();
    osc2Gain.gain.value = 0.45;

    osc1.connect(osc1Gain);
    osc2.connect(osc2Gain);
    oscChime.connect(chimeGain);

    osc1Gain.connect(pluckFilter);
    osc2Gain.connect(pluckFilter);
    chimeGain.connect(pluckFilter);

    pluckFilter.connect(voiceGain);
    voiceGain.connect(this.harpBus);

    osc1.start(now);
    osc2.start(now);
    oscChime.start(now);

    const stopTime = now + sustain + 0.05;
    osc1.stop(stopTime);
    osc2.stop(stopTime);
    oscChime.stop(stopTime);
  }

  /**
   * Starts or updates the warm vintage Omnichord accompaniment chord pad
   */
  playChordPad(voicing, chordKey) {
    if (!this.isInitialized) return;
    if (!this.params.chordPadEnabled) {
      this.stopChordPad();
      return;
    }
    if (this.currentChordKey === chordKey && this.activeChordVoices.length > 0) {
      return;
    }

    this.stopChordPad(0.08);
    this.currentChordKey = chordKey;

    const now = this.ctx.currentTime;
    const voices = [];

    // Warm analog organ-like filter for accompaniment pad
    const padFilter = this.ctx.createBiquadFilter();
    padFilter.type = 'lowpass';
    padFilter.frequency.value = 1150;
    padFilter.Q.value = 0.7;

    const padEnv = this.ctx.createGain();
    padEnv.gain.setValueAtTime(0.0001, now);
    padEnv.gain.linearRampToValueAtTime(0.18, now + 0.06);

    padFilter.connect(padEnv);
    padEnv.connect(this.chordBus);

    // Bass root note (triangle + soft square)
    if (voicing.bass) {
      const bassOsc = this.ctx.createOscillator();
      bassOsc.type = 'triangle';
      bassOsc.frequency.setValueAtTime(voicing.bass.freq, now);
      const bassGain = this.ctx.createGain();
      bassGain.gain.value = 0.45;
      bassOsc.connect(bassGain);
      bassGain.connect(padFilter);
      bassOsc.start(now);
      voices.push(bassOsc);
    }

    // Chord triad/7th voices
    voicing.voices.forEach((v, idx) => {
      const osc = this.ctx.createOscillator();
      osc.type = idx % 2 === 0 ? 'sawtooth' : 'square';
      osc.frequency.setValueAtTime(v.freq, now);
      osc.detune.setValueAtTime((idx - 1) * 4, now);

      const vGain = this.ctx.createGain();
      vGain.gain.value = 0.22;
      osc.connect(vGain);
      vGain.connect(padFilter);
      osc.start(now);
      voices.push(osc);
    });

    this.activeChordVoices.push({ voices, padEnv });
  }

  stopChordPad(releaseTime = 0.22) {
    if (!this.isInitialized || this.activeChordVoices.length === 0) return;
    const now = this.ctx.currentTime;

    const toStop = [...this.activeChordVoices];
    this.activeChordVoices = [];
    this.currentChordKey = null;

    toStop.forEach(({ voices, padEnv }) => {
      try {
        padEnv.gain.cancelScheduledValues(now);
        padEnv.gain.setValueAtTime(Math.max(0.0001, padEnv.gain.value), now);
        padEnv.gain.exponentialRampToValueAtTime(0.0001, now + releaseTime);
        voices.forEach(osc => {
          try {
            osc.stop(now + releaseTime + 0.03);
          } catch (_) {}
        });
      } catch (_) {}
    });
  }

  setParam(name, value) {
    this.params[name] = value;
    if (!this.isInitialized) return;
    const now = this.ctx.currentTime;

    switch (name) {
      case 'masterVolume':
        this.masterGain.gain.setTargetAtTime(value, now, 0.015);
        break;
      case 'harpVolume':
        this.harpBus.gain.setTargetAtTime(value, now, 0.015);
        break;
      case 'chordVolume':
        this.chordBus.gain.setTargetAtTime(value, now, 0.015);
        break;
      case 'toneCutoff':
        this.toneFilter.frequency.setTargetAtTime(value, now, 0.02);
        break;
      case 'chorusMix':
        this.chorusWet.gain.setTargetAtTime(value, now, 0.02);
        break;
      case 'reverbMix':
        this.reverbWet.gain.setTargetAtTime(value, now, 0.02);
        break;
      case 'chordPadEnabled':
        if (!value) this.stopChordPad();
        break;
    }
  }
}
