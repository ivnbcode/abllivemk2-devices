// Web MIDI Manager tailored for Arturia MiniLab MkII
// Handles:
// - Modulation Touch Strip (CC 1) as the primary Sonic String Strumplate
// - Optional Pitch Bend Strip or custom CC via MIDI Learn
// - 8 Pads (Notes 36-43 on Channel 10 or any channel) for instant preset chords
// - 25 Keys for live chord detection / root note selection

export class MidiController {
  constructor(callbacks = {}) {
    this.callbacks = callbacks;
    this.midiAccess = null;
    this.activeInput = null;
    this.inputs = [];

    // Strum configuration
    this.strumConfig = {
      source: 'cc',       // 'cc' | 'pitchbend' | 'both'
      ccNumber: 1,        // CC 1 = Arturia MiniLab MkII Modulation Touch Strip
      numStrings: 16,
      lastStringIndex: null,
      lastNormValue: null
    };

    // MIDI Learn state
    this.learningTarget = null; // e.g., 'strumStrip' or 'pad:0'

    // Currently held piano keys (MIDI note numbers)
    this.heldKeys = new Set();

    // Pad note mapping (default Arturia MiniLab MkII pads 1-8: 36..43)
    this.padNoteMap = new Map([
      [36, 0], [37, 1], [38, 2], [39, 3],
      [40, 4], [41, 5], [42, 6], [43, 7]
    ]);
  }

  async init() {
    if (!navigator.requestMIDIAccess) {
      this._notifyStatus({
        supported: false,
        connected: false,
        message: 'Web MIDI API no soportada en este navegador (usa Chrome o Edge).'
      });
      return false;
    }

    try {
      this.midiAccess = await navigator.requestMIDIAccess({ sysex: false });
      this.midiAccess.onstatechange = () => this.refreshDevices();
      this.refreshDevices();
      return true;
    } catch (err) {
      this._notifyStatus({
        supported: true,
        connected: false,
        message: 'Permiso MIDI denegado o error al acceder a dispositivos MIDI.'
      });
      return false;
    }
  }

  refreshDevices() {
    if (!this.midiAccess) return;

    this.inputs = Array.from(this.midiAccess.inputs.values());

    // Prefer Arturia MiniLab MkII automatically if connected
    let preferred = this.inputs.find(inp =>
      /minilab|arturia/i.test(inp.name || '') || /minilab|arturia/i.test(inp.manufacturer || '')
    );

    if (!preferred && this.inputs.length > 0) {
      preferred = this.inputs[0];
    }

    this.selectInput(preferred ? preferred.id : null);

    if (this.callbacks.onDevicesChanged) {
      this.callbacks.onDevicesChanged(this.inputs, this.activeInput);
    }
  }

  selectInput(inputId) {
    // Detach previous listeners
    this.inputs.forEach(inp => {
      inp.onmidimessage = null;
    });

    this.activeInput = this.inputs.find(inp => inp.id === inputId) || null;

    if (this.activeInput) {
      this.activeInput.onmidimessage = (e) => this._handleMidiMessage(e);
      const isMiniLab = /minilab|arturia/i.test(this.activeInput.name || '');
      this._notifyStatus({
        supported: true,
        connected: true,
        isMiniLab,
        deviceName: this.activeInput.name,
        message: isMiniLab
          ? `Conectado: ${this.activeInput.name} (Mod Strip CC1 listo)`
          : `Conectado: ${this.activeInput.name}`
      });
    } else {
      this._notifyStatus({
        supported: true,
        connected: false,
        isMiniLab: false,
        deviceName: null,
        message: 'Conecta tu Arturia MiniLab MkII por USB...'
      });
    }
  }

  startMidiLearn(target) {
    this.learningTarget = target;
    if (this.callbacks.onMidiLearnState) {
      this.callbacks.onMidiLearnState(target);
    }
  }

  cancelMidiLearn() {
    this.learningTarget = null;
    if (this.callbacks.onMidiLearnState) {
      this.callbacks.onMidiLearnState(null);
    }
  }

  _handleMidiMessage(event) {
    const data = event.data;
    if (!data || data.length < 2) return;

    const status = data[0] & 0xf0;
    const channel = (data[0] & 0x0f) + 1; // 1-16
    const data1 = data[1];
    const data2 = data.length > 2 ? data[2] : 0;

    if (this.callbacks.onRawMidi) {
      this.callbacks.onRawMidi({ status, channel, data1, data2 });
    }

    // Handle MIDI Learn
    if (this.learningTarget) {
      if (this.learningTarget === 'strumStrip') {
        if (status === 0xb0) {
          this.strumConfig.source = 'cc';
          this.strumConfig.ccNumber = data1;
          this.cancelMidiLearn();
          if (this.callbacks.onStrumConfigChanged) {
            this.callbacks.onStrumConfigChanged(this.strumConfig);
          }
          return;
        } else if (status === 0xe0) {
          this.strumConfig.source = 'pitchbend';
          this.cancelMidiLearn();
          if (this.callbacks.onStrumConfigChanged) {
            this.callbacks.onStrumConfigChanged(this.strumConfig);
          }
          return;
        }
      }
    }

    // 1. Control Change (0xB0) -> Modulation Touch Strip (CC 1) or Knobs
    if (status === 0xb0) {
      const cc = data1;
      const val = data2; // 0..127

      if (
        (this.strumConfig.source === 'cc' || this.strumConfig.source === 'both') &&
        cc === this.strumConfig.ccNumber
      ) {
        this._processStrumValue(val / 127);
        return;
      }

      // Optional Arturia MiniLab MkII Knobs for live sound shaping
      if (this.callbacks.onControlChange) {
        this.callbacks.onControlChange(cc, val, channel);
      }
      return;
    }

    // 2. Pitch Bend (0xE0) -> If user selected Pitch Bend strip or Both
    if (status === 0xe0) {
      if (this.strumConfig.source === 'pitchbend' || this.strumConfig.source === 'both') {
        const pb14 = (data2 << 7) | data1; // 0..16383 (center 8192)
        const norm = pb14 / 16383;
        this._processStrumValue(norm);
      }
      return;
    }

    // 3. Note On (0x90) & Note Off (0x80)
    if (status === 0x90 && data2 > 0) {
      this._handleNoteOn(data1, data2, channel);
    } else if (status === 0x80 || (status === 0x90 && data2 === 0)) {
      this._handleNoteOff(data1, channel);
    }
  }

  /**
   * Interpolates fast movements across the Arturia MiniLab MkII touch strip
   * so every Sonic String in between is plucked smoothly like a real Omnichord harp.
   */
  _processStrumValue(normalizedValue) {
    const clamped = Math.max(0, Math.min(1, normalizedValue));
    const numStrings = this.strumConfig.numStrings;
    const targetIdx = Math.min(numStrings - 1, Math.floor(clamped * numStrings));

    if (this.callbacks.onTouchStripMove) {
      this.callbacks.onTouchStripMove(clamped, targetIdx);
    }

    if (this.strumConfig.lastStringIndex === null) {
      this.strumConfig.lastStringIndex = targetIdx;
      this.strumConfig.lastNormValue = clamped;
      if (this.callbacks.onStrumString) {
        this.callbacks.onStrumString(targetIdx, 0.85);
      }
      return;
    }

    const prevIdx = this.strumConfig.lastStringIndex;
    if (targetIdx !== prevIdx) {
      const diff = Math.abs(targetIdx - prevIdx);
      const step = targetIdx > prevIdx ? 1 : -1;
      // Calculate dynamic velocity from swipe speed
      const velocity = Math.min(1.0, 0.68 + diff * 0.06);

      // Trigger intermediate strings with micro-stagger for authentic glissando
      let delayStep = 0;
      for (let i = prevIdx + step; step > 0 ? i <= targetIdx : i >= targetIdx; i += step) {
        const stringIdx = i;
        if (delayStep === 0) {
          this.callbacks.onStrumString?.(stringIdx, velocity);
        } else {
          setTimeout(() => {
            this.callbacks.onStrumString?.(stringIdx, velocity);
          }, delayStep * 14);
        }
        delayStep++;
      }

      this.strumConfig.lastStringIndex = targetIdx;
    }

    this.strumConfig.lastNormValue = clamped;
  }

  _handleNoteOn(note, velocity, channel) {
    // Check if it's an Arturia MiniLab MkII Pad
    // On MiniLab MkII default preset, Pads send on Channel 10 or notes 36-43
    if (channel === 10 && this.padNoteMap.has(note)) {
      const padIndex = this.padNoteMap.get(note);
      this.callbacks.onPadTrigger?.(padIndex, note, velocity / 127);
      return;
    }

    // If pad mode is active or note is in 36-43 range and user configured pads on any channel
    if (this.padNoteMap.has(note) && this.callbacks.shouldTreatNoteAsPad?.(note, channel)) {
      const padIndex = this.padNoteMap.get(note);
      this.callbacks.onPadTrigger?.(padIndex, note, velocity / 127);
      return;
    }

    // Otherwise treat as MiniLab MkII 25-key keyboard note for chord selection
    this.heldKeys.add(note);
    this.callbacks.onKeysChanged?.(Array.from(this.heldKeys), note, true);
  }

  _handleNoteOff(note, channel) {
    if (channel === 10) return;
    if (this.heldKeys.has(note)) {
      this.heldKeys.delete(note);
      this.callbacks.onKeysChanged?.(Array.from(this.heldKeys), note, false);
    }
  }

  _notifyStatus(statusObj) {
    if (this.callbacks.onStatusChange) {
      this.callbacks.onStatusChange(statusObj);
    }
  }
}
