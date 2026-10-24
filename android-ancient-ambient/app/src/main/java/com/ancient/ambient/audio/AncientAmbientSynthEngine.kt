package com.ancient.ambient.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.Process
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

/**
 * 𓂀 ANCIENT AMBIENT MACHINE 𓋹 — Motor DSP Nativo de Baja Latencia (Zero-Allocation)
 * Recreación híbrida del Solar 42N + Sonicware Ambient Ø para Android (Touch + Arturia MiniLab MkII USB-OTG).
 */
class AncientAmbientSynthEngine(context: Context) {

    companion object {
        private const val TABLE_BITS = 11
        private const val TABLE_SIZE = 1 shl TABLE_BITS // 2048 muestras por tabla
        private const val TABLE_MASK = TABLE_SIZE - 1
        const val NUM_DRONES = 6
        const val NUM_SEQ_STEPS = 16
        const val NUM_KNOBS = 16
    }

    val sampleRate: Int
    val framesPerBlock: Int

    // 16 Perillas normalizadas (0f..1f) mapeadas 1:1 al Arturia MiniLab MkII (K1..K16)
    val knobNorms = floatArrayOf(
        0.80f, // K1:  𓂀 DRONE LVL (Click = Waveform)
        0.20f, // K2:  𓇳 ROOT HZ (32..146 Hz -> ~55 Hz)
        0.32f, // K3:  ≋ DRIFT
        0.76f, // K4:  𓋹 PAD LVL
        0.50f, // K5:  ☽↔☉ MORPH
        0.70f, // K6:  𓇼 V/OCT LVL
        0.32f, // K7:  ⋈ FM / GLIDE
        0.28f, // K8:  ⚡ DRIVE
        0.22f, // K9:  ⏷ LPF (80..9500 Hz -> ~1650 Hz, Click = VCF Mode)
        0.03f, // K10: ⏶ HPF (20..2400 Hz -> ~45 Hz)
        0.22f, // K11: ∆ RESO
        0.24f, // K12: ⏱ TEMPO (15..240 BPM -> ~68 BPM)
        0.35f, // K13: ◈ TREMOLO
        0.46f, // K14: ⧖ DELAY
        0.72f, // K15: 𓉴 REVERB
        0.85f  // K16: 𓇳 MASTER
    )

    // Estado de controles Arturia MiniLab MkII
    @Volatile var wavePresetIdx: Int = 0   // 0..5 (Ciclado con K1 Click)
    @Volatile var filterModeIdx: Int = 0   // 0: LP+HP Stereo, 1: LP, 2: HP, 3: BP Mono (Ciclado con K9 Click)
    @Volatile var scaleIdx: Int = 0        // 0: Just, 1: Phrygian, 2: Aeolian, 3: Pentatonic, 4: Lydian, 5: Microtonal
    @Volatile var presetIdx: Int = 0       // 0..7 (Escenas I..VIII)

    @Volatile var shiftSeqEdit: Boolean = false
    @Volatile var padBankB: Boolean = false
    @Volatile var octaveShift: Int = 0
    @Volatile var keyHoldLatch: Boolean = false
    @Volatile var droneMasterHold: Boolean = true
    @Volatile var autoOrbit: Boolean = true

    @Volatile var pitchStripNorm: Float = 0.5f
    @Volatile var modStripNorm: Float = 0.45f
    @Volatile var vecX: Float = 0.5f
    @Volatile var vecY: Float = 0.5f

    // 6 Cámaras de Drone
    val droneRatios = floatArrayOf(1.00f, 1.50f, 2.00f, 1.25f, 3.00f, 0.50f)
    val droneMuted = booleanArrayOf(false, false, false, false, true, false)

    // Secuenciador de 16 Steps (1..16)
    @Volatile var seqRunning: Boolean = true
    @Volatile var seqDir: Int = 0 // 0 = Forward, 1 = Pendulum, 2 = Random
    @Volatile var seqMultIdx: Int = 1 // 0 = 0.5x, 1 = 1x, 2 = 2x
    @Volatile var seqStepIdx: Int = 0
    private var seqPendulumDir: Int = 1
    val seqNotes = intArrayOf(0, 4, 7, 9, 5, 7, 11, 2, 0, 7, 10, 7, 5, 3, 7, 11)
    val seqGates = booleanArrayOf(
        true, true, true, false, true, true, true, false,
        true, true, true, false, true, true, true, true
    )

    // Teclado de 25 Mini-Teclas
    val heldKeys = BooleanArray(25)
    @Volatile var activeKeyIdx: Int = -1
    @Volatile var keyVelocity: Float = 0.78f

    // Buffer de visualización para el Osciloscopio CRT en la vista
    val scopeSamples = FloatArray(128)

    // 6 Bancos de Tablas de Onda precalculadas (0: SuperSaw, 1: Choir, 2: Organ, 3: Glass, 4: Triangle, 5: Pulse)
    private val waveTables = Array(6) { FloatArray(TABLE_SIZE) }
    private val sineTable = FloatArray(TABLE_SIZE)

    private val scaleRatios = arrayOf(
        floatArrayOf(1f, 16f/15f, 9f/8f, 6f/5f, 5f/4f, 4f/3f, 45f/32f, 3f/2f, 8f/5f, 5f/3f, 9f/5f, 15f/8f),
        IntArray(12) { intArrayOf(0, 1, 3, 5, 7, 8, 10, 12, 13, 15, 17, 19)[it] }.map { 2f.pow(it / 12f) }.toFloatArray(),
        IntArray(12) { intArrayOf(0, 2, 3, 5, 7, 8, 10, 12, 14, 15, 17, 19)[it] }.map { 2f.pow(it / 12f) }.toFloatArray(),
        IntArray(12) { intArrayOf(0, 2, 3, 7, 8, 12, 14, 15, 19, 20, 24, 26)[it] }.map { 2f.pow(it / 12f) }.toFloatArray(),
        IntArray(12) { intArrayOf(0, 2, 4, 6, 7, 9, 11, 12, 14, 16, 18, 19)[it] }.map { 2f.pow(it / 12f) }.toFloatArray(),
        floatArrayOf(0f, 1.35f, 2.75f, 4.4f, 6.0f, 7.25f, 8.85f, 10.3f, 11.75f, 13.4f, 15.0f, 16.8f).map { 2f.pow(it / 12f) }.toFloatArray()
    )

    // Fases de osciladores (Zero-GC)
    private val dronePhaseA = FloatArray(NUM_DRONES)
    private val dronePhaseB = FloatArray(NUM_DRONES)
    private var subPhase = 0f

    private val yinPhases = FloatArray(4)
    private val yangPhases = FloatArray(4)
    private val padIntervals = floatArrayOf(1.0f, 1.5f, 2.0f, 2.5f)

    private var vcoPhase = 0f
    private var vcoSubPhase = 0f
    private var vcoModPhase = 0f
    private var vcoCurrentFreq = 110f
    private var vcoTargetFreq = 110f
    private var vcoEnv = 0.12f

    private var lfoPhase = 0f
    private var tremPhase = 0f
    private var orbitPhase = 0f
    private var seqSampleCounter = 0

    // Filtros Estéreo SVF (HPF + LPF + Bandpass con resonancia)
    private var hpfStateL = 0f
    private var hpfStateR = 0f
    private var svfLowL = 0f
    private var svfBandL = 0f
    private var svfLowR = 0f
    private var svfBandR = 0f

    // Tape Delay Estéreo + Temple Shimmer Reverb (Schroeder + Pitch Shimmer)
    private val delayBufSize = 32768
    private val delayMask = delayBufSize - 1
    private val delayBufL = FloatArray(delayBufSize)
    private val delayBufR = FloatArray(delayBufSize)
    private var delayWritePos = 0
    private var delayLpL = 0f
    private var delayLpR = 0f

    private val combLengths = intArrayOf(1557, 1617, 1491, 1422)
    private val combBufsL = Array(4) { FloatArray(combLengths[it]) }
    private val combBufsR = Array(4) { FloatArray(combLengths[it] + 23) }
    private val combIdxL = IntArray(4)
    private val combIdxR = IntArray(4)
    private val combLpL = FloatArray(4)
    private val combLpR = FloatArray(4)
    private var shimmerPhase1 = 0f
    private var shimmerPhase2 = 0f

    @Volatile private var running = false
    private var audioThread: Thread? = null

    init {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val nativeSr = am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.toIntOrNull() ?: 44100
        val nativeBuf = am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)?.toIntOrNull() ?: 256
        sampleRate = nativeSr.coerceIn(22050, 48000)
        framesPerBlock = max(256, nativeBuf)
        buildWavetables()
        updateTargetFreqFromStep()
    }

    private fun buildWavetables() {
        for (i in 0 until TABLE_SIZE) {
            val ph = (2.0 * PI * i) / TABLE_SIZE
            sineTable[i] = sin(ph).toFloat()
        }

        // 0: SuperSaw / Warm Strings
        for (h in 1..20) {
            val amp = (1f / h) * exp(-h * 0.035f)
            for (i in 0 until TABLE_SIZE) {
                val ph = (2.0 * PI * i * h) / TABLE_SIZE
                waveTables[0][i] += (amp * sin(ph) + 0.22f * amp * cos(ph * 0.5)).toFloat()
            }
        }
        // 1: Spectral Choir (Formantes vocales)
        for (h in 1..24) {
            val f1 = exp(-((h - 3f) / 1.8f).pow(2))
            val f2 = 0.65f * exp(-((h - 7f) / 2.2f).pow(2))
            val f3 = 0.30f * exp(-((h - 13f) / 2.5f).pow(2))
            val amp = (0.35f / h) + 0.55f * (f1 + f2 + f3)
            for (i in 0 until TABLE_SIZE) {
                waveTables[1][i] += (amp * sin((2.0 * PI * i * h) / TABLE_SIZE)).toFloat()
            }
        }
        // 2: Temple Pipe Organ
        val organHarmonics = intArrayOf(1, 2, 3, 4, 6, 8, 12, 16)
        for (idx in organHarmonics.indices) {
            val h = organHarmonics[idx]
            val amp = 0.78f.pow(idx)
            for (i in 0 until TABLE_SIZE) {
                waveTables[2][i] += (amp * sin((2.0 * PI * i * h) / TABLE_SIZE)).toFloat()
            }
        }
        // 3: Glass Bells
        for (h in 1..22) {
            val amp = (if (h % 2 == 1) 1f / h else 0.45f / h) * exp(-h * 0.075f)
            for (i in 0 until TABLE_SIZE) {
                waveTables[3][i] += (amp * sin((2.0 * PI * i * h) / TABLE_SIZE)).toFloat()
            }
        }
        // 4: Triangle
        for (h in 1..15 step 2) {
            val sign = if (((h - 1) / 2) % 2 == 0) 1f else -1f
            val amp = sign / (h * h)
            for (i in 0 until TABLE_SIZE) {
                waveTables[4][i] += (amp * sin((2.0 * PI * i * h) / TABLE_SIZE)).toFloat()
            }
        }
        // 5: Pulse / Square
        for (h in 1..19 step 2) {
            val amp = 1f / h
            for (i in 0 until TABLE_SIZE) {
                waveTables[5][i] += (amp * sin((2.0 * PI * i * h) / TABLE_SIZE)).toFloat()
            }
        }

        for (t in 0..5) {
            val maxAbs = waveTables[t].maxOf { kotlin.math.abs(it) }.coerceAtLeast(0.001f)
            for (i in 0 until TABLE_SIZE) waveTables[t][i] /= maxAbs
        }
    }

    fun getRootHz(): Float = 32f + knobNorms[1] * 114f
    fun getLpfHz(): Float = 80f * (9500f / 80f).pow(knobNorms[8])
    fun getHpfHz(): Float = 20f * (2400f / 20f).pow(knobNorms[9])
    fun getResoQ(): Float = 0.7f + knobNorms[10] * 11.5f
    fun getTempoBpm(): Float = 15f + knobNorms[11] * 225f

    fun triggerEncoderPush(knobIdx: Int) {
        if (knobIdx == 0) {
            wavePresetIdx = (wavePresetIdx + 1) % 6
        } else if (knobIdx == 8) {
            filterModeIdx = (filterModeIdx + 1) % 4
        }
    }

    fun triggerKey(keyIdx: Int, vel: Float = 0.8f) {
        val k = keyIdx.coerceIn(0, 24)
        heldKeys[k] = true
        activeKeyIdx = k
        keyVelocity = vel.coerceIn(0.2f, 1f)
        val ratios = scaleRatios[scaleIdx.coerceIn(0, 5)]
        val deg = k % 12
        val oct = (k / 12) + octaveShift
        vcoTargetFreq = (getRootHz() * 2f * ratios[deg] * 2f.pow(oct)).coerceIn(30f, 4200f)
    }

    fun releaseKey(keyIdx: Int) {
        val k = keyIdx.coerceIn(0, 24)
        if (keyHoldLatch) return
        heldKeys[k] = false
        var found = -1
        for (i in 24 downTo 0) {
            if (heldKeys[i]) {
                found = i
                break
            }
        }
        activeKeyIdx = found
    }

    fun releaseAllKeys() {
        keyHoldLatch = false
        for (i in 0..24) heldKeys[i] = false
        activeKeyIdx = -1
    }

    fun handlePadTrigger(padIdx: Int, isBankB: Boolean = padBankB) {
        val p = padIdx.coerceIn(0, 7)
        if (isBankB) {
            loadPreset(p)
            return
        }
        when {
            p < 6 -> droneMuted[p] = !droneMuted[p]
            p == 6 -> seqRunning = !seqRunning
            p == 7 -> mutateTexture()
        }
    }

    fun mutateTexture() {
        knobNorms[1] = Random.nextFloat() * 0.55f + 0.08f
        knobNorms[2] = Random.nextFloat() * 0.55f + 0.12f
        knobNorms[8] = Random.nextFloat() * 0.65f + 0.22f
        knobNorms[9] = Random.nextFloat() * 0.40f
        knobNorms[10] = Random.nextFloat() * 0.55f + 0.10f
        knobNorms[12] = Random.nextFloat() * 0.60f + 0.15f
        knobNorms[14] = Random.nextFloat() * 0.45f + 0.50f
    }

    fun randomizeSequencer() {
        for (i in 0 until NUM_SEQ_STEPS) {
            seqNotes[i] = Random.nextInt(12)
            seqGates[i] = Random.nextFloat() > 0.2f
        }
    }

    fun loadPreset(idx: Int) {
        presetIdx = idx.coerceIn(0, 7)
        when (presetIdx) {
            0 -> { // I · GIZA STARGATE
                knobNorms[1] = 0.20f; knobNorms[2] = 0.32f; knobNorms[8] = 0.64f; knobNorms[9] = 0.12f
                knobNorms[10] = 0.24f; knobNorms[11] = 0.24f; knobNorms[12] = 0.35f; knobNorms[14] = 0.72f
                scaleIdx = 0; wavePresetIdx = 0
            }
            1 -> { // II · ORION MONOLITH
                knobNorms[1] = 0.08f; knobNorms[2] = 0.65f; knobNorms[8] = 0.48f; knobNorms[9] = 0.28f
                knobNorms[10] = 0.68f; knobNorms[11] = 0.31f; knobNorms[12] = 0.62f; knobNorms[14] = 0.58f
                scaleIdx = 1; wavePresetIdx = 5
            }
            2 -> { // III · OSIRIS TOMB
                knobNorms[1] = 0.29f; knobNorms[2] = 0.18f; knobNorms[8] = 0.78f; knobNorms[9] = 0.32f
                knobNorms[10] = 0.15f; knobNorms[11] = 0.18f; knobNorms[12] = 0.28f; knobNorms[14] = 0.90f
                scaleIdx = 4; wavePresetIdx = 1
            }
            3 -> { // IV · SIRIUS TEMPLE
                knobNorms[1] = 0.15f; knobNorms[2] = 0.48f; knobNorms[8] = 0.70f; knobNorms[9] = 0.45f
                knobNorms[10] = 0.42f; knobNorms[11] = 0.36f; knobNorms[12] = 0.55f; knobNorms[14] = 0.76f
                scaleIdx = 2; wavePresetIdx = 3
            }
            4 -> { // V · ANUBIS VOID
                knobNorms[1] = 0.23f; knobNorms[2] = 0.25f; knobNorms[8] = 0.58f; knobNorms[9] = 0.16f
                knobNorms[10] = 0.32f; knobNorms[11] = 0.21f; knobNorms[12] = 0.40f; knobNorms[14] = 0.78f
                scaleIdx = 1; wavePresetIdx = 2
            }
            5 -> { // VI · THOTH EMERALD
                knobNorms[1] = 0.36f; knobNorms[2] = 0.14f; knobNorms[8] = 0.74f; knobNorms[9] = 0.36f
                knobNorms[10] = 0.20f; knobNorms[11] = 0.27f; knobNorms[12] = 0.30f; knobNorms[14] = 0.68f
                scaleIdx = 3; wavePresetIdx = 3
            }
            6 -> { // VII · HORUS SKYSHIP
                knobNorms[1] = 0.10f; knobNorms[2] = 0.55f; knobNorms[8] = 0.66f; knobNorms[9] = 0.10f
                knobNorms[10] = 0.34f; knobNorms[11] = 0.15f; knobNorms[12] = 0.22f; knobNorms[14] = 0.84f
                scaleIdx = 2; wavePresetIdx = 0
            }
            7 -> { // VIII · DUAT PORTAL
                knobNorms[1] = 0.03f; knobNorms[2] = 0.52f; knobNorms[8] = 0.52f; knobNorms[9] = 0.05f
                knobNorms[10] = 0.52f; knobNorms[11] = 0.11f; knobNorms[12] = 0.70f; knobNorms[14] = 0.94f
                scaleIdx = 5; wavePresetIdx = 2
            }
        }
    }

    private fun updateTargetFreqFromStep() {
        val st = seqStepIdx.coerceIn(0, NUM_SEQ_STEPS - 1)
        if (seqGates[st] && activeKeyIdx == -1) {
            val ratios = scaleRatios[scaleIdx.coerceIn(0, 5)]
            val note = seqNotes[st].coerceIn(0, 11)
            val octMult = 2f.pow(octaveShift * 0.5f)
            vcoTargetFreq = (getRootHz() * 2f * ratios[note] * octMult).coerceIn(30f, 4200f)
        }
    }

    private fun advanceStep() {
        when (seqDir) {
            0 -> seqStepIdx = (seqStepIdx + 1) % NUM_SEQ_STEPS
            1 -> {
                seqStepIdx += seqPendulumDir
                if (seqStepIdx >= NUM_SEQ_STEPS - 1) {
                    seqStepIdx = NUM_SEQ_STEPS - 1
                    seqPendulumDir = -1
                } else if (seqStepIdx <= 0) {
                    seqStepIdx = 0
                    seqPendulumDir = 1
                }
            }
            else -> seqStepIdx = Random.nextInt(NUM_SEQ_STEPS)
        }
        updateTargetFreqFromStep()
        if (seqGates[seqStepIdx] && activeKeyIdx == -1) {
            vcoEnv = max(vcoEnv, 0.42f)
        }
    }

    fun start() {
        if (running) return
        running = true
        audioThread = Thread({ runAudioLoop() }, "AncientAmbientDSP").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun stop() {
        running = false
        audioThread?.join(300)
        audioThread = null
    }

    private fun runAudioLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        val minBufBytes = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(framesPerBlock * 4)

        val track = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(minBufBytes)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .build()
        } else {
            @Suppress("DEPRECATION")
            AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufBytes,
                AudioTrack.MODE_STREAM
            )
        }

        val outPcm = ShortArray(framesPerBlock * 2)
        val invSr = 1f / sampleRate
        track.play()

        while (running) {
            val mult = if (seqMultIdx == 0) 0.5f else if (seqMultIdx == 2) 2.0f else 1.0f
            val effectiveBpm = max(15f, getTempoBpm() * mult)
            val samplesPerStep = ((60f / effectiveBpm) * 0.5f * sampleRate).toInt().coerceAtLeast(512)

            if (autoOrbit) {
                orbitPhase += framesPerBlock * invSr * 0.42f
                vecX = 0.5f + 0.36f * sin(orbitPhase)
                vecY = 0.5f + 0.36f * cos(orbitPhase * 0.73f + 0.5f)
            }

            val pitchSemis = (pitchStripNorm - 0.5f) * 4f
            val octMult = 2f.pow(octaveShift * 0.5f + pitchSemis / 12f)
            val rootHz = getRootHz() * octMult
            val spread = knobNorms[2]
            val mod = modStripNorm

            val wDrone = (1f - vecX * 0.5f) * (1f - vecY * 0.5f)
            val wPad = (0.5f + vecX * 0.5f) * (1f - vecY * 0.5f)
            val wAtmos = (1f - vecX * 0.5f) * (0.5f + vecY * 0.5f)

            val droneAmp = if (droneMasterHold) knobNorms[0] * wDrone * 0.38f else 0f
            val padAmp = knobNorms[3] * wPad * 0.34f
            val morph = (knobNorms[4] * 0.7f + mod * 0.3f).coerceIn(0f, 1f)
            val atmosAmp = knobNorms[5] * wAtmos * 0.42f
            val fmDepth = knobNorms[6]
            val glideCoeff = 1f - exp(-1f / (sampleRate * (0.012f + fmDepth * 0.25f)))
            val drive = 1f + knobNorms[7] * 6.5f

            val lpfCutoff = (getLpfHz() * (0.55f + mod * 1.15f)).coerceIn(80f, sampleRate * 0.42f)
            val hpfCutoff = getHpfHz().coerceIn(20f, sampleRate * 0.25f)
            val svfF = (2f * sin(PI * lpfCutoff * invSr)).toFloat().coerceIn(0.01f, 0.88f)
            val svfQ = (1f / getResoQ()).coerceIn(0.12f, 1.2f)
            val hpfAlpha = (1f / (1f + 2f * PI.toFloat() * hpfCutoff * invSr)).coerceIn(0.05f, 0.999f)

            val tremDepth = knobNorms[12] * 0.48f
            val tremHz = 0.4f + knobNorms[12] * 6.5f
            val tremInc = tremHz * invSr * TABLE_SIZE

            val delayMix = knobNorms[13] * 0.55f
            val delayFb = 0.32f + knobNorms[13] * 0.42f
            val delaySamplesL = (sampleRate * 0.36f).toInt().coerceIn(256, delayBufSize - 1)
            val delaySamplesR = (sampleRate * 0.48f).toInt().coerceIn(256, delayBufSize - 1)

            val revMix = knobNorms[14] * 0.65f
            val master = knobNorms[15]

            val mainWave = waveTables[wavePresetIdx.coerceIn(0, 5)]
            val yinWave = waveTables[1] // Choir
            val yangWave = waveTables[3] // Glass

            for (frame in 0 until framesPerBlock) {
                if (seqRunning) {
                    seqSampleCounter++
                    if (seqSampleCounter >= samplesPerStep) {
                        seqSampleCounter = 0
                        advanceStep()
                    }
                }

                // 1. 6 Voces Drone + Sub-Osc
                var sigL = 0f
                var sigR = 0f
                if (droneAmp > 0.001f) {
                    for (d in 0 until NUM_DRONES) {
                        if (droneMuted[d]) continue
                        val baseF = rootHz * droneRatios[d]
                        val detune = (d - 2.5f) * spread * 1.3f
                        val incA = max(18f, baseF - detune * 0.5f) * invSr * TABLE_SIZE
                        val incB = max(18f, baseF + detune * 0.5f + 0.17f) * invSr * TABLE_SIZE
                        dronePhaseA[d] = (dronePhaseA[d] + incA) % TABLE_SIZE
                        dronePhaseB[d] = (dronePhaseB[d] + incB) % TABLE_SIZE
                        val s = (mainWave[dronePhaseA[d].toInt() and TABLE_MASK] +
                                mainWave[dronePhaseB[d].toInt() and TABLE_MASK]) * 0.09f
                        val pan = d / 5f
                        sigL += s * (1f - pan * 0.6f)
                        sigR += s * (0.4f + pan * 0.6f)
                    }
                    subPhase = (subPhase + (rootHz * 0.5f * invSr * TABLE_SIZE)) % TABLE_SIZE
                    val sub = sineTable[subPhase.toInt() and TABLE_MASK] * 0.22f
                    sigL += sub
                    sigR += sub
                    sigL *= droneAmp
                    sigR *= droneAmp
                }

                // 2. Pad Yin-Yang
                if (padAmp > 0.001f) {
                    var pSumL = 0f
                    var pSumR = 0f
                    for (p in 0..3) {
                        val f = rootHz * 2f * padIntervals[p]
                        val incY = f * (1f - spread * 0.003f) * invSr * TABLE_SIZE
                        val incYang = f * (1f + spread * 0.0035f) * invSr * TABLE_SIZE
                        yinPhases[p] = (yinPhases[p] + incY) % TABLE_SIZE
                        yangPhases[p] = (yangPhases[p] + incYang) % TABLE_SIZE
                        val vYin = yinWave[yinPhases[p].toInt() and TABLE_MASK] * (1f - morph)
                        val vYang = yangWave[yangPhases[p].toInt() and TABLE_MASK] * morph
                        pSumL += (vYin + vYang * 0.8f) * 0.2f
                        pSumR += (vYin * 0.8f + vYang) * 0.2f
                    }
                    sigL += pSumL * padAmp
                    sigR += pSumR * padAmp
                }

                // 3. Voz Melódica V/Oct + FM (25 Keys + 16-Step Sequencer)
                vcoCurrentFreq += (vcoTargetFreq - vcoCurrentFreq) * glideCoeff
                val targetEnv = if (activeKeyIdx >= 0) (0.48f + keyVelocity * 0.42f) else 0.10f
                vcoEnv += (targetEnv - vcoEnv) * 0.0015f

                vcoModPhase = (vcoModPhase + vcoCurrentFreq * 1.5f * invSr * TABLE_SIZE) % TABLE_SIZE
                val fmMod = sineTable[vcoModPhase.toInt() and TABLE_MASK] * fmDepth * 140f
                val instFreq = max(24f, vcoCurrentFreq + fmMod)
                vcoPhase = (vcoPhase + instFreq * invSr * TABLE_SIZE) % TABLE_SIZE
                vcoSubPhase = (vcoSubPhase + instFreq * 0.5f * invSr * TABLE_SIZE) % TABLE_SIZE

                val vcoSig = (mainWave[vcoPhase.toInt() and TABLE_MASK] * 0.7f +
                        sineTable[vcoSubPhase.toInt() and TABLE_MASK] * 0.35f) * vcoEnv * atmosAmp
                sigL += vcoSig
                sigR += vcoSig

                // 4. Filtro Dual Pasa-Altos (HPF) + Pasa-Bajos/Banda (LPF/BP)
                val hpL = sigL - hpfStateL
                hpfStateL += hpL * (1f - hpfAlpha)
                val hpR = sigR - hpfStateR
                hpfStateR += hpR * (1f - hpfAlpha)

                val inL = if (filterModeIdx == 1) sigL else hpL
                val inR = if (filterModeIdx == 1) sigR else hpR

                val highL = inL - svfLowL - svfQ * svfBandL
                svfBandL += svfF * highL
                svfLowL += svfF * svfBandL

                val highR = inR - svfLowR - svfQ * svfBandR
                svfBandR += svfF * highR
                svfLowR += svfF * svfBandR

                var fltL = when (filterModeIdx) {
                    2 -> hpL
                    3 -> svfBandL
                    else -> svfLowL
                }
                var fltR = when (filterModeIdx) {
                    2 -> hpR
                    3 -> svfBandR
                    else -> svfLowR
                }

                // Saturación Polivoks + Trémolo Estéreo
                fltL = tanh(fltL * drive)
                fltR = tanh(fltR * drive)

                tremPhase = (tremPhase + tremInc) % TABLE_SIZE
                val tOsc = sineTable[tremPhase.toInt() and TABLE_MASK]
                val tL = 1f - tremDepth * 0.5f + tOsc * tremDepth
                val tR = if (filterModeIdx == 3) tL else (1f - tremDepth * 0.5f - tOsc * tremDepth)
                fltL *= tL
                fltR *= tR

                // 5. Tape Delay Estéreo
                val rPosL = (delayWritePos - delaySamplesL + delayBufSize) and delayMask
                val rPosR = (delayWritePos - delaySamplesR + delayBufSize) and delayMask
                delayLpL += 0.35f * (delayBufL[rPosL] - delayLpL)
                delayLpR += 0.35f * (delayBufR[rPosR] - delayLpR)

                delayBufL[delayWritePos] = fltL + delayLpR * delayFb
                delayBufR[delayWritePos] = fltR + delayLpL * delayFb
                delayWritePos = (delayWritePos + 1) and delayMask

                val wetDelayL = fltL + delayLpL * delayMix
                val wetDelayR = fltR + delayLpR * delayMix

                // 6. Temple Shimmer Reverb
                shimmerPhase1 = (shimmerPhase1 + rootHz * 4f * invSr * TABLE_SIZE) % TABLE_SIZE
                shimmerPhase2 = (shimmerPhase2 + vcoCurrentFreq * 2f * invSr * TABLE_SIZE) % TABLE_SIZE
                val shimIn = (sineTable[shimmerPhase1.toInt() and TABLE_MASK] +
                        yinWave[shimmerPhase2.toInt() and TABLE_MASK]) * 0.035f * revMix

                val revInL = (wetDelayL + shimIn) * 0.35f
                val revInR = (wetDelayR + shimIn) * 0.35f
                var revOutL = 0f
                var revOutR = 0f

                for (c in 0..3) {
                    val bufL = combBufsL[c]
                    val idxL = combIdxL[c]
                    val delL = bufL[idxL]
                    combLpL[c] += 0.42f * (delL - combLpL[c])
                    bufL[idxL] = revInL + combLpL[c] * 0.84f
                    combIdxL[c] = if (idxL + 1 >= bufL.size) 0 else idxL + 1
                    revOutL += delL

                    val bufR = combBufsR[c]
                    val idxR = combIdxR[c]
                    val delR = bufR[idxR]
                    combLpR[c] += 0.42f * (delR - combLpR[c])
                    bufR[idxR] = revInR + combLpR[c] * 0.84f
                    combIdxR[c] = if (idxR + 1 >= bufR.size) 0 else idxR + 1
                    revOutR += delR
                }

                val outL = tanh((wetDelayL + revOutL * revMix) * master)
                val outR = tanh((wetDelayR + revOutR * revMix) * master)

                if ((frame and 3) == 0) {
                    val sIdx = (frame shr 2) and 127
                    scopeSamples[sIdx] = (outL + outR) * 0.5f
                }

                val pcmIdx = frame shl 1
                outPcm[pcmIdx] = (outL * 32000f).toInt().coerceIn(-32767, 32767).toShort()
                outPcm[pcmIdx + 1] = (outR * 32000f).toInt().coerceIn(-32767, 32767).toShort()
            }

            track.write(outPcm, 0, outPcm.size)
        }

        track.stop()
        track.release()
    }
}
