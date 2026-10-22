package com.omnilab.touch.audio

import kotlin.math.pow

/**
 * Zero-allocation Music Theory & Omnichord Chord Matrix table.
 * All MIDI pitches, frequencies, and chord intervals are pre-calculated at startup.
 */
object MusicTheory {

    const val NUM_STRINGS = 16
    const val NUM_ROOTS = 12
    const val NUM_CHORD_TYPES = 8

    val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    data class RootDef(
        val index: Int,
        val id: String,
        val name: String,
        val semitone: Int
    )

    // Authentic Suzuki Omnichord OM-27 / OM-84 Circle-of-Fifths horizontal layout
    val DISPLAY_ROOTS = arrayOf(
        RootDef(0, "Db", "Db", 1),
        RootDef(1, "Ab", "Ab", 8),
        RootDef(2, "Eb", "Eb", 3),
        RootDef(3, "Bb", "Bb", 10),
        RootDef(4, "F",  "F",  5),
        RootDef(5, "C",  "C",  0),
        RootDef(6, "G",  "G",  7),
        RootDef(7, "D",  "D",  2),
        RootDef(8, "A",  "A",  9),
        RootDef(9, "E",  "E",  4),
        RootDef(10, "B", "B",  11),
        RootDef(11, "F#", "F#", 6)
    )

    data class ChordTypeDef(
        val index: Int,
        val key: String,
        val rowLabel: String,
        val fullName: String,
        val suffix: String,
        val intervals: IntArray,
        val badgeColor: Int
    )

    val CHORD_TYPES = arrayOf(
        ChordTypeDef(0, "major",   "MAJOR", "Major", "",     intArrayOf(0, 4, 7),     0xFFE86A38.toInt()),
        ChordTypeDef(1, "minor",   "MINOR", "Minor", "m",    intArrayOf(0, 3, 7),     0xFFD9A738.toInt()),
        ChordTypeDef(2, "seventh", "7TH",   "7th",   "7",    intArrayOf(0, 4, 7, 10), 0xFF38B5A0.toInt()),
        ChordTypeDef(3, "maj7",    "MAJ7",  "Maj7",  "M7",   intArrayOf(0, 4, 7, 11), 0xFF5A95E8.toInt()),
        ChordTypeDef(4, "min7",    "MIN7",  "Min7",  "m7",   intArrayOf(0, 3, 7, 10), 0xFF9E6BE8.toInt()),
        ChordTypeDef(5, "dim",     "DIM",   "Dim",   "dim",  intArrayOf(0, 3, 6),     0xFFE0567E.toInt()),
        ChordTypeDef(6, "aug",     "AUG",   "Aug",   "aug",  intArrayOf(0, 4, 8),     0xFF52C466.toInt()),
        ChordTypeDef(7, "sus4",    "SUS4",  "Sus4",  "sus4", intArrayOf(0, 5, 7),     0xFF42B8D4.toInt())
    )

    // Pre-computed MIDI frequencies and note names (0..127)
    val MIDI_FREQS = FloatArray(128) { midi ->
        (440.0 * 2.0.pow((midi - 69) / 12.0)).toFloat()
    }

    val MIDI_NOTE_LABELS = Array(128) { midi ->
        val octave = (midi / 12) - 1
        val name = NOTE_NAMES[midi % 12]
        "$name$octave"
    }

    // Pre-computed chord labels: [rootIndex * NUM_CHORD_TYPES + typeIndex]
    val CHORD_SYMBOLS = Array(NUM_ROOTS * NUM_CHORD_TYPES) { idx ->
        val r = DISPLAY_ROOTS[idx / NUM_CHORD_TYPES]
        val t = CHORD_TYPES[idx % NUM_CHORD_TYPES]
        "${r.name}${t.suffix}"
    }

    val CHORD_FULL_NAMES = Array(NUM_ROOTS * NUM_CHORD_TYPES) { idx ->
        val r = DISPLAY_ROOTS[idx / NUM_CHORD_TYPES]
        val t = CHORD_TYPES[idx % NUM_CHORD_TYPES]
        "${r.name} ${t.fullName}"
    }

    val CHORD_NOTES_SUMMARY = Array(NUM_ROOTS * NUM_CHORD_TYPES) { idx ->
        val r = DISPLAY_ROOTS[idx / NUM_CHORD_TYPES]
        val t = CHORD_TYPES[idx % NUM_CHORD_TYPES]
        val sb = StringBuilder()
        for (i in t.intervals.indices) {
            if (i > 0) sb.append(" · ")
            sb.append(NOTE_NAMES[(r.semitone + t.intervals[i]) % 12])
        }
        sb.toString()
    }

    fun getChordSymbol(rootIdx: Int, typeIdx: Int): String {
        val safeR = rootIdx.coerceIn(0, NUM_ROOTS - 1)
        val safeT = typeIdx.coerceIn(0, NUM_CHORD_TYPES - 1)
        return CHORD_SYMBOLS[safeR * NUM_CHORD_TYPES + safeT]
    }

    fun getChordFullName(rootIdx: Int, typeIdx: Int): String {
        val safeR = rootIdx.coerceIn(0, NUM_ROOTS - 1)
        val safeT = typeIdx.coerceIn(0, NUM_CHORD_TYPES - 1)
        return CHORD_FULL_NAMES[safeR * NUM_CHORD_TYPES + safeT]
    }

    fun getChordNotesSummary(rootIdx: Int, typeIdx: Int): String {
        val safeR = rootIdx.coerceIn(0, NUM_ROOTS - 1)
        val safeT = typeIdx.coerceIn(0, NUM_CHORD_TYPES - 1)
        return CHORD_NOTES_SUMMARY[safeR * NUM_CHORD_TYPES + safeT]
    }

    /**
     * Fills the 16 Sonic String MIDI note numbers and frequencies in-place without allocating memory.
     */
    fun fillStrumPlateNotes(
        rootSemitone: Int,
        chordTypeIdx: Int,
        baseOctave: Int,
        outMidi: IntArray,
        outFreqs: FloatArray
    ) {
        val chordDef = CHORD_TYPES[chordTypeIdx.coerceIn(0, NUM_CHORD_TYPES - 1)]
        val intervals = chordDef.intervals
        val baseMidi = (baseOctave + 1) * 12 + rootSemitone

        var octaveOffset = 0
        var intervalIdx = 0

        for (s in 0 until NUM_STRINGS) {
            val midi = (baseMidi + octaveOffset * 12 + intervals[intervalIdx]).coerceIn(24, 115)
            outMidi[s] = midi
            outFreqs[s] = MIDI_FREQS[midi]

            intervalIdx++
            if (intervalIdx >= intervals.size) {
                intervalIdx = 0
                octaveOffset++
            }
        }
    }

    /**
     * Fills the Chord Pad accompaniment frequencies in-place:
     * outFreqs[0] = Sub-bass root (C2 area)
     * outFreqs[1..4] = Chord triad / 7th voices (C3-C4 area)
     * Returns the number of active voices (4 for triads, 5 for 7th chords).
     */
    fun fillChordVoicingFreqs(
        rootSemitone: Int,
        chordTypeIdx: Int,
        outFreqs: FloatArray
    ): Int {
        val chordDef = CHORD_TYPES[chordTypeIdx.coerceIn(0, NUM_CHORD_TYPES - 1)]
        val intervals = chordDef.intervals
        val bassMidi = (36 + rootSemitone).coerceIn(24, 96)
        val baseMidi = (48 + rootSemitone).coerceIn(36, 96)

        outFreqs[0] = MIDI_FREQS[bassMidi]
        for (i in intervals.indices) {
            val m = (baseMidi + intervals[i]).coerceIn(24, 108)
            outFreqs[i + 1] = MIDI_FREQS[m]
        }
        return intervals.size + 1
    }
}
