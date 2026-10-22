package com.omnilab.touch.ui

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import com.omnilab.touch.audio.MusicTheory
import com.omnilab.touch.audio.OmnichordSynthEngine
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Hardware-accelerated, zero-allocation Multi-Touch Custom View for OMNI-TOUCH MkII.
 *
 * Adapts automatically to:
 * - Phone Landscape (Two-handed ergonomic split: Chords on left, full-height Strumplate on right, collapsible Synth Drawer)
 * - Tablet Landscape (Full studio workstation with expanded controls)
 * - Phone / Tablet Portrait (Stacked ergonomic layout: OLED + Chords top/center, wide Strumplate bottom)
 */
class OmnichordInstrumentView(
    context: Context,
    private val synth: OmnichordSynthEngine,
    private val onToggleOrientation: () -> Unit
) : View(context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("omnitouch_prefs", Context.MODE_PRIVATE)

    // Musical State
    private var activeRootIdx: Int = 5 // C (index 5 in Circle of Fifths)
    private var activeTypeIdx: Int = 0 // Major
    private var baseOctave: Int = 3
    private var chordHoldLatch: Boolean = true // Latch mode on by default
    private var showExtendedChords: Boolean = false // false = 4 rows (48 chords), true = 8 rows (96 chords)
    private var showSynthDrawer: Boolean = false
    private var assigningPadMode: Boolean = false

    // Pre-allocated audio frequency arrays
    private val stringMidiNotes = IntArray(MusicTheory.NUM_STRINGS)
    private val stringFreqs = FloatArray(MusicTheory.NUM_STRINGS)
    private val padVoicingFreqs = FloatArray(5)

    // 8 Programmable Quick-Access Chord Pads (RootIndex, TypeIndex)
    private val quickPadRoots = intArrayOf(5, 6, 8, 4, 7, 9, 6, 5) // C, G, Am, F, Dm, Em, G7, CM7
    private val quickPadTypes = intArrayOf(0, 0, 1, 0, 1, 1, 2, 3)
    private var activeQuickPadIdx: Int = -1

    // String visual glow levels (0f..1f)
    private val stringGlow = FloatArray(MusicTheory.NUM_STRINGS)
    private var lastFrameTimeMs: Long = SystemClock.uptimeMillis()

    // Multi-touch pointer tracking (up to 10 simultaneous fingers)
    private val maxPointers = 10
    private val pointerRole = IntArray(maxPointers) // 0=NONE, 1=STRUM, 2=MATRIX, 3=SLIDER
    private val pointerLastString = IntArray(maxPointers) { -1 }
    private val pointerLastY = FloatArray(maxPointers)
    private val pointerLastTime = LongArray(maxPointers)
    private val pointerActiveSlider = IntArray(maxPointers) { -1 }
    private var activeMatrixPointers = 0

    // Responsive Layout Metrics
    private var dp = resources.displayMetrics.density
    private var isLandscape = true
    private var isTablet = false

    // Pre-allocated UI Rectangles (Zero allocation in onDraw)
    private val topBarRect = RectF()
    private val oledRect = RectF()
    private val btnHoldRect = RectF()
    private val btnOrganRect = RectF()
    private val btnExtRect = RectF()
    private val btnRhythmRect = RectF()
    private val btnSynthRect = RectF()
    private val btnOrientRect = RectF()

    private val matrixAreaRect = RectF()
    private val chordBtnRects = Array(MusicTheory.NUM_CHORD_TYPES * MusicTheory.NUM_ROOTS) { RectF() }

    private val quickPadsBarRect = RectF()
    private val quickPadRects = Array(8) { RectF() }
    private val btnAssignPadRect = RectF()

    private val strumHousingRect = RectF()
    private val strumSurfaceRect = RectF()
    private val stringRects = Array(MusicTheory.NUM_STRINGS) { RectF() }
    private val btnAutoStrumRect = RectF()

    // Collapsible Synth Drawer Overlay Rectangles
    private val synthDrawerRect = RectF()
    private val synthCloseBtnRect = RectF()
    private val sliderRects = Array(8) { RectF() } // 0:HarpVol, 1:Sustain, 2:Tone, 3:Chorus, 4:Reverb, 5:OrganVol, 6:Tempo, 7:Octave

    private val waveformPath = Path()
    private val tempRect = RectF()

    // Rhythm pattern labels
    private val rhythmNames = arrayOf("RITMO: OFF", "RITMO: ROCK", "RITMO: BOSSA", "RITMO: DISCO", "RITMO: WALTZ", "RITMO: SLOW")

    // Paints
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF141210.toInt() }
    private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1E1B18.toInt() }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * dp
        color = 0xFF3A332C.toInt()
    }
    private val oledBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF071714.toInt() }
    private val oledWavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.0f * dp
        color = 0xFF48F5C8.toInt()
    }
    private val textBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.WHITE
    }
    private val textMonoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.MONOSPACE
        color = 0xFF48F5C8.toInt()
    }
    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stringPlatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE8DEC8.toInt() }
    private val stringTracePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Auto-strum state
    private var autoStrumActive = false
    private var autoStrumIndex = 0
    private var autoStrumNextMs = 0L

    init {
        isFocusable = true
        isClickable = true
        loadSavedState()
        recalculateChordAudio(triggerPad = false)
    }

    private fun loadSavedState() {
        activeRootIdx = prefs.getInt("rootIdx", 5).coerceIn(0, 11)
        activeTypeIdx = prefs.getInt("typeIdx", 0).coerceIn(0, 7)
        chordHoldLatch = prefs.getBoolean("chordHoldLatch", true)
        synth.chordPadEnabled = prefs.getBoolean("organEnabled", true)
        synth.harpVolume = prefs.getFloat("harpVol", 0.85f)
        synth.sustainSeconds = prefs.getFloat("sustain", 1.4f)
        synth.toneCutoffNorm = prefs.getFloat("tone", 0.65f)
        synth.chorusMix = prefs.getFloat("chorus", 0.55f)
        synth.reverbMix = prefs.getFloat("reverb", 0.35f)
        synth.chordVolume = prefs.getFloat("chordVol", 0.45f)
        synth.rhythmBpm = prefs.getFloat("bpm", 112f)
        baseOctave = prefs.getInt("baseOctave", 3).coerceIn(2, 4)

        for (i in 0 until 8) {
            quickPadRoots[i] = prefs.getInt("padRoot_$i", quickPadRoots[i]).coerceIn(0, 11)
            quickPadTypes[i] = prefs.getInt("padType_$i", quickPadTypes[i]).coerceIn(0, 7)
        }
    }

    private fun saveQuickPads() {
        val ed = prefs.edit()
        for (i in 0 until 8) {
            ed.putInt("padRoot_$i", quickPadRoots[i])
            ed.putInt("padType_$i", quickPadTypes[i])
        }
        ed.apply()
    }

    private fun saveSynthPrefs() {
        prefs.edit()
            .putInt("rootIdx", activeRootIdx)
            .putInt("typeIdx", activeTypeIdx)
            .putBoolean("chordHoldLatch", chordHoldLatch)
            .putBoolean("organEnabled", synth.chordPadEnabled)
            .putFloat("harpVol", synth.harpVolume)
            .putFloat("sustain", synth.sustainSeconds)
            .putFloat("tone", synth.toneCutoffNorm)
            .putFloat("chorus", synth.chorusMix)
            .putFloat("reverb", synth.reverbMix)
            .putFloat("chordVol", synth.chordVolume)
            .putFloat("bpm", synth.rhythmBpm)
            .putInt("baseOctave", baseOctave)
            .apply()
    }

    private fun recalculateChordAudio(triggerPad: Boolean) {
        val rootSemitone = MusicTheory.DISPLAY_ROOTS[activeRootIdx].semitone
        MusicTheory.fillStrumPlateNotes(
            rootSemitone,
            activeTypeIdx,
            baseOctave,
            stringMidiNotes,
            stringFreqs
        )
        val voiceCount = MusicTheory.fillChordVoicingFreqs(
            rootSemitone,
            activeTypeIdx,
            padVoicingFreqs
        )
        val gate = if (chordHoldLatch) triggerPad || synth.chordGateActive else (activeMatrixPointers > 0)
        synth.updateChordPad(padVoicingFreqs, voiceCount, gate)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        dp = resources.displayMetrics.density
        isLandscape = w >= h
        val smallestPx = min(w, h)
        isTablet = (smallestPx / dp) >= 580f

        computeResponsiveLayout(w.toFloat(), h.toFloat())
    }

    private fun computeResponsiveLayout(w: Float, h: Float) {
        val pad = 6f * dp

        if (isLandscape) {
            // LANDSCAPE: Ergonomic 2-Hand Split
            // Right side = Full-height Sonic Strings Strumplate (38% width on tablet, 36% on phone)
            val strumWidth = if (isTablet) w * 0.34f else w * 0.35f
            val leftWidth = w - strumWidth - pad * 3f

            strumHousingRect.set(w - strumWidth - pad, pad, w - pad, h - pad)
            layoutStrumplateInternal()

            // Top bar on the left workstation
            val topBarH = if (isTablet) 72f * dp else 48f * dp
            topBarRect.set(pad, pad, pad + leftWidth, pad + topBarH)
            layoutTopBarInternal()

            // Quick Pads bar at bottom-left
            val quickBarH = if (isTablet) 58f * dp else 42f * dp
            quickPadsBarRect.set(pad, h - pad - quickBarH, pad + leftWidth, h - pad)
            layoutQuickPadsInternal()

            // Main Omnichord Chord Matrix in center-left
            matrixAreaRect.set(
                pad,
                topBarRect.bottom + pad,
                pad + leftWidth,
                quickPadsBarRect.top - pad
            )
            layoutMatrixInternal()
        } else {
            // PORTRAIT: Ergonomic Stacked Layout for Phones & Tablets
            val topBarH = if (isTablet) 78f * dp else 68f * dp
            topBarRect.set(pad, pad, w - pad, pad + topBarH)
            layoutTopBarPortraitInternal()

            // Bottom 38% height = Full-width Sonic Strings Strumplate
            val strumH = h * 0.37f
            strumHousingRect.set(pad, h - pad - strumH, w - pad, h - pad)
            layoutStrumplateInternal()

            // Quick Pads strip above Strumplate
            val quickBarH = if (isTablet) 54f * dp else 44f * dp
            quickPadsBarRect.set(pad, strumHousingRect.top - pad - quickBarH, w - pad, strumHousingRect.top - pad)
            layoutQuickPadsInternal()

            // Chord Matrix in middle
            matrixAreaRect.set(pad, topBarRect.bottom + pad, w - pad, quickPadsBarRect.top - pad)
            layoutMatrixInternal()
        }

        // Synth Drawer Modal Centered over Matrix Area
        val drawerW = min(w - 24f * dp, if (isTablet) 520f * dp else w * 0.92f)
        val drawerH = min(h - 20f * dp, if (isTablet) 360f * dp else h * 0.88f)
        val dx = (w - drawerW) * 0.5f
        val dy = (h - drawerH) * 0.5f
        synthDrawerRect.set(dx, dy, dx + drawerW, dy + drawerH)
        synthCloseBtnRect.set(
            synthDrawerRect.right - 96f * dp,
            synthDrawerRect.top + 8f * dp,
            synthDrawerRect.right - 10f * dp,
            synthDrawerRect.top + 38f * dp
        )

        // 8 Synth Sliders in a 2x4 grid inside the drawer
        val gridTop = synthDrawerRect.top + 46f * dp
        val gridBottom = synthDrawerRect.bottom - 12f * dp
        val colW = (synthDrawerRect.width() - 36f * dp) / 2f
        val rowH = (gridBottom - gridTop) / 4f
        for (i in 0 until 8) {
            val col = i % 2
            val row = i / 2
            val sx = synthDrawerRect.left + 12f * dp + col * (colW + 12f * dp)
            val sy = gridTop + row * rowH + 4f * dp
            sliderRects[i].set(sx, sy, sx + colW, sy + rowH - 8f * dp)
        }
    }

    private fun layoutTopBarInternal() {
        val p = 4f * dp
        val oledW = topBarRect.width() * 0.36f
        oledRect.set(topBarRect.left + p, topBarRect.top + p, topBarRect.left + oledW, topBarRect.bottom - p)

        val btnsLeft = oledRect.right + p * 1.5f
        val btnsW = topBarRect.right - btnsLeft - p
        val btnW = (btnsW - p * 5f) / 6f
        val top = topBarRect.top + p
        val bot = topBarRect.bottom - p

        btnHoldRect.set(btnsLeft, top, btnsLeft + btnW, bot)
        btnOrganRect.set(btnHoldRect.right + p, top, btnHoldRect.right + p + btnW, bot)
        btnExtRect.set(btnOrganRect.right + p, top, btnOrganRect.right + p + btnW, bot)
        btnRhythmRect.set(btnExtRect.right + p, top, btnExtRect.right + p + btnW, bot)
        btnSynthRect.set(btnRhythmRect.right + p, top, btnRhythmRect.right + p + btnW, bot)
        btnOrientRect.set(btnSynthRect.right + p, top, topBarRect.right - p, bot)
    }

    private fun layoutTopBarPortraitInternal() {
        val p = 4f * dp
        val midY = topBarRect.centerY()
        oledRect.set(topBarRect.left + p, topBarRect.top + p, topBarRect.right - p, midY - p * 0.5f)

        val rowTop = midY + p * 0.5f
        val rowBot = topBarRect.bottom - p
        val btnsW = topBarRect.width() - p * 2f
        val btnW = (btnsW - p * 5f) / 6f
        var x = topBarRect.left + p

        btnHoldRect.set(x, rowTop, x + btnW, rowBot); x += btnW + p
        btnOrganRect.set(x, rowTop, x + btnW, rowBot); x += btnW + p
        btnExtRect.set(x, rowTop, x + btnW, rowBot); x += btnW + p
        btnRhythmRect.set(x, rowTop, x + btnW, rowBot); x += btnW + p
        btnSynthRect.set(x, rowTop, x + btnW, rowBot); x += btnW + p
        btnOrientRect.set(x, rowTop, topBarRect.right - p, rowBot)
    }

    private fun layoutMatrixInternal() {
        val p = 4f * dp
        val visibleRows = if (showExtendedChords) 8 else 4
        val labelColW = if (isTablet) 46f * dp else 32f * dp
        val gridLeft = matrixAreaRect.left + labelColW + p
        val gridW = matrixAreaRect.right - gridLeft - p
        val gridH = matrixAreaRect.height() - p * 2f

        val cellW = gridW / MusicTheory.NUM_ROOTS
        val cellH = gridH / visibleRows
        val gap = if (isTablet) 2.5f * dp else 1.8f * dp

        for (r in 0 until MusicTheory.NUM_CHORD_TYPES) {
            for (c in 0 until MusicTheory.NUM_ROOTS) {
                val idx = r * MusicTheory.NUM_ROOTS + c
                if (r < visibleRows) {
                    val left = gridLeft + c * cellW + gap
                    val top = matrixAreaRect.top + p + r * cellH + gap
                    chordBtnRects[idx].set(left, top, left + cellW - gap * 2f, top + cellH - gap * 2f)
                } else {
                    chordBtnRects[idx].setEmpty()
                }
            }
        }
    }

    private fun layoutQuickPadsInternal() {
        val p = 4f * dp
        val assignW = if (isTablet) 78f * dp else 56f * dp
        btnAssignPadRect.set(
            quickPadsBarRect.left + p,
            quickPadsBarRect.top + p,
            quickPadsBarRect.left + assignW,
            quickPadsBarRect.bottom - p
        )

        val padsLeft = btnAssignPadRect.right + p
        val padsW = quickPadsBarRect.right - padsLeft - p
        val singleW = (padsW - p * 7f) / 8f

        for (i in 0 until 8) {
            val l = padsLeft + i * (singleW + p)
            quickPadRects[i].set(l, quickPadsBarRect.top + p, l + singleW, quickPadsBarRect.bottom - p)
        }
    }

    private fun layoutStrumplateInternal() {
        val p = 6f * dp
        val headerH = if (isTablet) 26f * dp else 20f * dp
        val footerH = if (isTablet) 38f * dp else 32f * dp

        btnAutoStrumRect.set(
            strumHousingRect.left + p,
            strumHousingRect.bottom - footerH,
            strumHousingRect.right - p,
            strumHousingRect.bottom - p
        )

        strumSurfaceRect.set(
            strumHousingRect.left + p,
            strumHousingRect.top + headerH,
            strumHousingRect.right - p,
            btnAutoStrumRect.top - p
        )

        val sH = strumSurfaceRect.height() / MusicTheory.NUM_STRINGS
        // Top of Strumplate = highest pitch (S16), bottom = lowest pitch (S01), just like omnilab.html!
        for (i in 0 until MusicTheory.NUM_STRINGS) {
            val visualRow = (MusicTheory.NUM_STRINGS - 1) - i
            val top = strumSurfaceRect.top + visualRow * sH
            stringRects[i].set(strumSurfaceRect.left, top, strumSurfaceRect.right, top + sH)
        }
    }

    private fun triggerSonicString(stringIdx: Int, velocity: Float) {
        if (stringIdx !in 0 until MusicTheory.NUM_STRINGS) return
        synth.pluckString(stringIdx, stringFreqs[stringIdx], velocity)
        stringGlow[stringIdx] = 1.0f
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val now = SystemClock.uptimeMillis()
        val dt = ((now - lastFrameTimeMs).coerceIn(1L, 100L)) / 1000f
        lastFrameTimeMs = now

        // Handle Auto-Strum sequence if active
        var needsNextFrame = false
        if (autoStrumActive) {
            if (now >= autoStrumNextMs) {
                triggerSonicString(autoStrumIndex, 0.84f)
                autoStrumIndex++
                autoStrumNextMs = now + 26L
                if (autoStrumIndex >= MusicTheory.NUM_STRINGS) {
                    autoStrumActive = false
                }
            }
            needsNextFrame = true
        }

        // Background Chassis
        canvas.drawColor(0xFF12100E.toInt())

        // 1. Draw Top Control Bar & OLED
        drawTopBar(canvas)

        // 2. Draw Omnichord Chord Matrix
        drawChordMatrix(canvas)

        // 3. Draw 8 Quick-Access Chord Pads
        drawQuickPadsBar(canvas)

        // 4. Draw Sonic Strings Strumplate
        if (drawStrumplate(canvas, dt)) {
            needsNextFrame = true
        }

        // 5. Draw Collapsible Synth Controls Drawer if open
        if (showSynthDrawer) {
            drawSynthDrawer(canvas)
        }

        // Keep refreshing OLED waveform only while audio is active or strings are glowing
        if (needsNextFrame || (synth.chordPadEnabled && synth.chordGateActive) || synth.rhythmPatternIndex > 0) {
            postInvalidateOnAnimation()
        }
    }

    private fun drawTopBar(canvas: Canvas) {
        panelPaint.color = 0xFF1D1A17.toInt()
        canvas.drawRoundRect(topBarRect, 8f * dp, 8f * dp, panelPaint)
        canvas.drawRoundRect(topBarRect, 8f * dp, 8f * dp, borderPaint)

        // Retro OLED Screen
        oledBgPaint.color = 0xFF071A16.toInt()
        canvas.drawRoundRect(oledRect, 6f * dp, 6f * dp, oledBgPaint)

        // OLED Chord Readout
        val chordSym = MusicTheory.getChordSymbol(activeRootIdx, activeTypeIdx)
        val chordNotes = MusicTheory.getChordNotesSummary(activeRootIdx, activeTypeIdx)

        textBoldPaint.color = 0xFF48F5C8.toInt()
        textBoldPaint.textAlign = Paint.Align.LEFT
        textBoldPaint.textSize = (oledRect.height() * 0.56f).coerceIn(13f * dp, 26f * dp)
        canvas.drawText(
            chordSym,
            oledRect.left + 8f * dp,
            oledRect.centerY() + textBoldPaint.textSize * 0.35f,
            textBoldPaint
        )

        val symW = textBoldPaint.measureText(chordSym)
        textMonoPaint.color = 0xFFA2F5DF.toInt()
        textMonoPaint.textAlign = Paint.Align.LEFT
        textMonoPaint.textSize = (oledRect.height() * 0.28f).coerceIn(9f * dp, 13f * dp)
        canvas.drawText(
            chordNotes,
            oledRect.left + 14f * dp + symW,
            oledRect.centerY() + textMonoPaint.textSize * 0.35f,
            textMonoPaint
        )

        // Mini Oscilloscope Waveform on right side of OLED
        val waveLeft = oledRect.right - oledRect.width() * 0.35f
        val waveRight = oledRect.right - 6f * dp
        val waveMidY = oledRect.centerY()
        val waveAmp = oledRect.height() * 0.36f
        waveformPath.reset()
        val wBuf = synth.waveformBuffer
        val wStart = synth.waveformWriteIdx
        val steps = 32
        val stepX = (waveRight - waveLeft) / (steps - 1)
        for (i in 0 until steps) {
            val sample = wBuf[(wStart + i * 4) and 127]
            val x = waveLeft + i * stepX
            val y = waveMidY - sample * waveAmp
            if (i == 0) waveformPath.moveTo(x, y) else waveformPath.lineTo(x, y)
        }
        canvas.drawPath(waveformPath, oledWavePaint)

        // Control Buttons
        drawToggleBtn(
            canvas,
            btnHoldRect,
            if (chordHoldLatch) "HOLD: FIJO" else "HOLD: PULSO",
            chordHoldLatch,
            0xFFE86A38.toInt()
        )
        drawToggleBtn(
            canvas,
            btnOrganRect,
            if (synth.chordPadEnabled) "ÓRGANO: ON" else "ÓRGANO: OFF",
            synth.chordPadEnabled,
            0xFF38B5A0.toInt()
        )
        drawToggleBtn(
            canvas,
            btnExtRect,
            if (showExtendedChords) "96 ACORDES" else "+ACORDES",
            showExtendedChords,
            0xFF5A95E8.toInt()
        )
        drawToggleBtn(
            canvas,
            btnRhythmRect,
            rhythmNames[synth.rhythmPatternIndex],
            synth.rhythmPatternIndex > 0,
            0xFFD9A738.toInt()
        )
        drawToggleBtn(
            canvas,
            btnSynthRect,
            "⚙ SONIDO",
            showSynthDrawer,
            0xFF9E6BE8.toInt()
        )
        drawToggleBtn(
            canvas,
            btnOrientRect,
            if (isLandscape) "🔄 VERT" else "🔄 HORIZ",
            false,
            0xFF607D8B.toInt()
        )
    }

    private fun drawToggleBtn(
        canvas: Canvas,
        rect: RectF,
        label: String,
        active: Boolean,
        accentColor: Int
    ) {
        buttonPaint.color = if (active) accentColor else 0xFF2B2622.toInt()
        canvas.drawRoundRect(rect, 5f * dp, 5f * dp, buttonPaint)

        borderPaint.color = if (active) 0xFFFFFFFF.toInt() else 0xFF473F38.toInt()
        canvas.drawRoundRect(rect, 5f * dp, 5f * dp, borderPaint)
        borderPaint.color = 0xFF3A332C.toInt()

        textBoldPaint.color = if (active) 0xFF12100E.toInt() else 0xFFEAE2D6.toInt()
        textBoldPaint.textAlign = Paint.Align.CENTER
        textBoldPaint.textSize = (rect.height() * 0.30f).coerceIn(8.5f * dp, 12.5f * dp)
        canvas.drawText(
            label,
            rect.centerX(),
            rect.centerY() + textBoldPaint.textSize * 0.35f,
            textBoldPaint
        )
    }

    private fun drawChordMatrix(canvas: Canvas) {
        panelPaint.color = 0xFF1A1714.toInt()
        canvas.drawRoundRect(matrixAreaRect, 8f * dp, 8f * dp, panelPaint)
        canvas.drawRoundRect(matrixAreaRect, 8f * dp, 8f * dp, borderPaint)

        val visibleRows = if (showExtendedChords) 8 else 4
        val labelColW = if (isTablet) 46f * dp else 32f * dp
        val rowH = (matrixAreaRect.height() - 8f * dp) / visibleRows

        // Row labels on the left edge
        for (r in 0 until visibleRows) {
            val typeDef = MusicTheory.CHORD_TYPES[r]
            val cy = matrixAreaRect.top + 4f * dp + (r + 0.5f) * rowH
            textBoldPaint.color = typeDef.badgeColor
            textBoldPaint.textAlign = Paint.Align.CENTER
            textBoldPaint.textSize = (rowH * 0.28f).coerceIn(8f * dp, 12f * dp)
            canvas.drawText(
                typeDef.rowLabel,
                matrixAreaRect.left + labelColW * 0.52f,
                cy + textBoldPaint.textSize * 0.35f,
                textBoldPaint
            )
        }

        // Draw Chord Buttons
        for (r in 0 until visibleRows) {
            val typeDef = MusicTheory.CHORD_TYPES[r]
            for (c in 0 until MusicTheory.NUM_ROOTS) {
                val idx = r * MusicTheory.NUM_ROOTS + c
                val rect = chordBtnRects[idx]
                val isSelected = (c == activeRootIdx && r == activeTypeIdx)

                if (isSelected) {
                    buttonPaint.color = typeDef.badgeColor
                } else {
                    // Subtle vintage tinted dark button
                    buttonPaint.color = when (r % 4) {
                        0 -> 0xFF2D231E.toInt()
                        1 -> 0xFF2B261C.toInt()
                        2 -> 0xFF1D2927.toInt()
                        else -> 0xFF1E242E.toInt()
                    }
                }

                val corner = 5f * dp
                canvas.drawRoundRect(rect, corner, corner, buttonPaint)

                borderPaint.color = if (isSelected) Color.WHITE else (typeDef.badgeColor and 0x66FFFFFF)
                canvas.drawRoundRect(rect, corner, corner, borderPaint)

                textBoldPaint.color = if (isSelected) 0xFF12100E.toInt() else 0xFFF2ECE1.toInt()
                textBoldPaint.textAlign = Paint.Align.CENTER
                textBoldPaint.textSize = (min(rect.width(), rect.height()) * 0.37f).coerceIn(9f * dp, 15f * dp)

                val label = MusicTheory.getChordSymbol(c, r)
                canvas.drawText(
                    label,
                    rect.centerX(),
                    rect.centerY() + textBoldPaint.textSize * 0.35f,
                    textBoldPaint
                )
            }
        }
        borderPaint.color = 0xFF3A332C.toInt()
    }

    private fun drawQuickPadsBar(canvas: Canvas) {
        panelPaint.color = 0xFF1D1A17.toInt()
        canvas.drawRoundRect(quickPadsBarRect, 8f * dp, 8f * dp, panelPaint)
        canvas.drawRoundRect(quickPadsBarRect, 8f * dp, 8f * dp, borderPaint)

        // Assign Button
        drawToggleBtn(
            canvas,
            btnAssignPadRect,
            if (assigningPadMode) "TOCA PAD" else "+MEMORIA",
            assigningPadMode,
            0xFFE0567E.toInt()
        )

        // 8 Quick Memory Pads
        for (i in 0 until 8) {
            val rect = quickPadRects[i]
            val rIdx = quickPadRoots[i]
            val tIdx = quickPadTypes[i]
            val isCurrent = (rIdx == activeRootIdx && tIdx == activeTypeIdx)

            buttonPaint.color = if (assigningPadMode) {
                0xFF4A2030.toInt()
            } else if (isCurrent) {
                0xFF48F5C8.toInt()
            } else {
                0xFF28231F.toInt()
            }
            canvas.drawRoundRect(rect, 5f * dp, 5f * dp, buttonPaint)

            borderPaint.color = if (isCurrent) Color.WHITE else 0xFF4A4139.toInt()
            canvas.drawRoundRect(rect, 5f * dp, 5f * dp, borderPaint)

            val chordLabel = MusicTheory.getChordSymbol(rIdx, tIdx)
            textBoldPaint.color = if (isCurrent) 0xFF0E1A17.toInt() else 0xFFEAE2D6.toInt()
            textBoldPaint.textAlign = Paint.Align.CENTER
            textBoldPaint.textSize = (rect.height() * 0.40f).coerceIn(10f * dp, 15f * dp)
            canvas.drawText(
                chordLabel,
                rect.centerX(),
                rect.centerY() + textBoldPaint.textSize * 0.36f,
                textBoldPaint
            )
        }
        borderPaint.color = 0xFF3A332C.toInt()
    }

    private fun drawStrumplate(canvas: Canvas, dt: Float): Boolean {
        var anyGlowing = false

        // Outer Strumplate Housing
        panelPaint.color = 0xFF241F1B.toInt()
        canvas.drawRoundRect(strumHousingRect, 10f * dp, 10f * dp, panelPaint)
        canvas.drawRoundRect(strumHousingRect, 10f * dp, 10f * dp, borderPaint)

        // Header title
        textBoldPaint.color = 0xFFD9C7A3.toInt()
        textBoldPaint.textAlign = Paint.Align.CENTER
        textBoldPaint.textSize = if (isTablet) 12f * dp else 10f * dp
        canvas.drawText(
            "SONIC STRINGS (MULTITOUCH HARP)",
            strumHousingRect.centerX(),
            strumHousingRect.top + (if (isTablet) 17f * dp else 14f * dp),
            textBoldPaint
        )

        // Ivory / Cream Conductive Strumplate Surface
        canvas.drawRoundRect(strumSurfaceRect, 6f * dp, 6f * dp, stringPlatePaint)

        // 16 Individual Sonic Strings
        for (i in 0 until MusicTheory.NUM_STRINGS) {
            val rect = stringRects[i]
            val glow = stringGlow[i]
            if (glow > 0.01f) {
                anyGlowing = true
                val alpha = (glow * 215).toInt().coerceIn(0, 255)
                stringTracePaint.color = Color.argb(alpha, 56, 230, 185)
                canvas.drawRect(rect, stringTracePaint)

                stringGlow[i] = max(0f, glow - dt * 4.2f)
            }

            // Conductive Gold/Bronze PCB trace line in center of string slot
            val traceH = max(2f * dp, rect.height() * 0.22f)
            val cy = rect.centerY()
            stringTracePaint.color = if (glow > 0.15f) 0xFF0A332A.toInt() else 0xFFC29641.toInt()
            canvas.drawRect(
                rect.left + 8f * dp,
                cy - traceH * 0.5f,
                rect.right - 8f * dp,
                cy + traceH * 0.5f,
                stringTracePaint
            )

            // Divider line between strings
            stringTracePaint.color = 0x336E5A38
            canvas.drawLine(rect.left, rect.bottom, rect.right, rect.bottom, stringTracePaint)

            // String index & Note name labels
            val labelSize = (rect.height() * 0.48f).coerceIn(8.5f * dp, 13f * dp)
            textBoldPaint.textSize = labelSize
            textBoldPaint.color = if (glow > 0.2f) 0xFF06261E.toInt() else 0xFF3E3325.toInt()

            textBoldPaint.textAlign = Paint.Align.LEFT
            canvas.drawText(
                "S${i + 1}",
                rect.left + 12f * dp,
                cy + labelSize * 0.35f,
                textBoldPaint
            )

            textBoldPaint.textAlign = Paint.Align.RIGHT
            val noteName = MusicTheory.MIDI_NOTE_LABELS[stringMidiNotes[i]]
            canvas.drawText(
                noteName,
                rect.right - 12f * dp,
                cy + labelSize * 0.35f,
                textBoldPaint
            )
        }

        // Bottom Auto-Strum Bar Button
        drawToggleBtn(
            canvas,
            btnAutoStrumRect,
            "⚡ RASGUEAR ACORDE (AUTO-STRUM)",
            autoStrumActive,
            0xFF48F5C8.toInt()
        )

        return anyGlowing
    }

    private fun drawSynthDrawer(canvas: Canvas) {
        // Dim backdrop
        buttonPaint.color = 0xB8090807.toInt()
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), buttonPaint)

        // Modal Card
        panelPaint.color = 0xFF221E1A.toInt()
        canvas.drawRoundRect(synthDrawerRect, 12f * dp, 12f * dp, panelPaint)
        borderPaint.color = 0xFF48F5C8.toInt()
        canvas.drawRoundRect(synthDrawerRect, 12f * dp, 12f * dp, borderPaint)
        borderPaint.color = 0xFF3A332C.toInt()

        // Title & Close Button
        textBoldPaint.color = 0xFF48F5C8.toInt()
        textBoldPaint.textAlign = Paint.Align.LEFT
        textBoldPaint.textSize = 14f * dp
        canvas.drawText(
            "SÍNTESIS SONIC STRINGS & EFECTOS",
            synthDrawerRect.left + 14f * dp,
            synthDrawerRect.top + 28f * dp,
            textBoldPaint
        )

        drawToggleBtn(canvas, synthCloseBtnRect, "✕ CERRAR", true, 0xFFE86A38.toInt())

        // Draw the 8 Sliders
        drawSliderControl(canvas, sliderRects[0], "VOLUMEN ARPA", synth.harpVolume, "${(synth.harpVolume * 100).toInt()}%", 0xFF48F5C8.toInt())
        val susNorm = ((synth.sustainSeconds - 0.15f) / 3.65f).coerceIn(0f, 1f)
        drawSliderControl(canvas, sliderRects[1], "SUSTAIN (COLA)", susNorm, "${"%.1f".format(synth.sustainSeconds)}s", 0xFF48F5C8.toInt())
        drawSliderControl(canvas, sliderRects[2], "BRILLO (TONE)", synth.toneCutoffNorm, "${(synth.toneCutoffNorm * 100).toInt()}%", 0xFFD9A738.toInt())
        drawSliderControl(canvas, sliderRects[3], "CHORUS BBD", synth.chorusMix, "${(synth.chorusMix * 100).toInt()}%", 0xFF5A95E8.toInt())
        drawSliderControl(canvas, sliderRects[4], "REVERB SALA", synth.reverbMix, "${(synth.reverbMix * 100).toInt()}%", 0xFF9E6BE8.toInt())
        drawSliderControl(canvas, sliderRects[5], "VOL. ÓRGANO", synth.chordVolume, "${(synth.chordVolume * 100).toInt()}%", 0xFFE86A38.toInt())
        val bpmNorm = ((synth.rhythmBpm - 70f) / 90f).coerceIn(0f, 1f)
        drawSliderControl(canvas, sliderRects[6], "TEMPO RITMO", bpmNorm, "${synth.rhythmBpm.toInt()} BPM", 0xFF52C466.toInt())
        val octNorm = ((baseOctave - 2) / 2f).coerceIn(0f, 1f)
        drawSliderControl(canvas, sliderRects[7], "OCTAVA BASE", octNorm, "OCT $baseOctave", 0xFFE0567E.toInt())
    }

    private fun drawSliderControl(
        canvas: Canvas,
        rect: RectF,
        title: String,
        normValue: Float,
        valText: String,
        accentColor: Int
    ) {
        buttonPaint.color = 0xFF161311.toInt()
        canvas.drawRoundRect(rect, 6f * dp, 6f * dp, buttonPaint)

        // Fill bar
        val fillW = (rect.width() * normValue.coerceIn(0f, 1f)).coerceAtLeast(6f * dp)
        tempRect.set(rect.left, rect.top, rect.left + fillW, rect.bottom)
        buttonPaint.color = (accentColor and 0x00FFFFFF) or 0x55000000
        canvas.drawRoundRect(tempRect, 6f * dp, 6f * dp, buttonPaint)

        borderPaint.color = accentColor
        canvas.drawRoundRect(rect, 6f * dp, 6f * dp, borderPaint)

        val ts = (rect.height() * 0.34f).coerceIn(10f * dp, 13f * dp)
        textBoldPaint.textSize = ts
        textBoldPaint.color = Color.WHITE
        textBoldPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(title, rect.left + 10f * dp, rect.centerY() + ts * 0.35f, textBoldPaint)

        textBoldPaint.color = accentColor
        textBoldPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(valText, rect.right - 10f * dp, rect.centerY() + ts * 0.35f, textBoldPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val actionIdx = event.actionIndex
        val pointerId = event.getPointerId(actionIdx)

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val x = event.getX(actionIdx)
                val y = event.getY(actionIdx)
                handlePointerDown(pointerId, x, y)
            }

            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val pid = event.getPointerId(i)
                    if (pid in 0 until maxPointers) {
                        handlePointerMove(pid, event.getX(i), event.getY(i))
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                handlePointerUp(pointerId)
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    for (i in 0 until maxPointers) {
                        pointerRole[i] = 0
                        pointerLastString[i] = -1
                        pointerActiveSlider[i] = -1
                    }
                    activeMatrixPointers = 0
                    if (!chordHoldLatch) {
                        synth.chordGateActive = false
                    }
                }
            }
        }
        return true
    }

    private fun handlePointerDown(pid: Int, x: Float, y: Float) {
        if (pid !in 0 until maxPointers) return

        // If Synth Drawer is open, handle drawer touches first
        if (showSynthDrawer) {
            if (synthCloseBtnRect.contains(x, y) || !synthDrawerRect.contains(x, y)) {
                showSynthDrawer = false
                saveSynthPrefs()
                invalidate()
                return
            }
            for (s in sliderRects.indices) {
                if (sliderRects[s].contains(x, y)) {
                    pointerRole[pid] = 3
                    pointerActiveSlider[pid] = s
                    updateSliderValue(s, x)
                    return
                }
            }
            return
        }

        // 1. Check Strumplate first (Right hand priority)
        if (btnAutoStrumRect.contains(x, y)) {
            autoStrumActive = true
            autoStrumIndex = 0
            autoStrumNextMs = SystemClock.uptimeMillis()
            postInvalidateOnAnimation()
            return
        }

        if (strumSurfaceRect.contains(x, y)) {
            pointerRole[pid] = 1
            val sIdx = stringIndexAtY(y)
            pointerLastString[pid] = sIdx
            pointerLastY[pid] = y
            pointerLastTime[pid] = SystemClock.uptimeMillis()
            triggerSonicString(sIdx, 0.88f)
            return
        }

        // 2. Check Top Bar Buttons
        if (btnHoldRect.contains(x, y)) {
            chordHoldLatch = !chordHoldLatch
            synth.chordGateActive = chordHoldLatch
            saveSynthPrefs()
            invalidate()
            return
        }
        if (btnOrganRect.contains(x, y)) {
            synth.chordPadEnabled = !synth.chordPadEnabled
            if (synth.chordPadEnabled && chordHoldLatch) synth.chordGateActive = true
            saveSynthPrefs()
            invalidate()
            return
        }
        if (btnExtRect.contains(x, y)) {
            showExtendedChords = !showExtendedChords
            layoutMatrixInternal()
            invalidate()
            return
        }
        if (btnRhythmRect.contains(x, y)) {
            synth.rhythmPatternIndex = (synth.rhythmPatternIndex + 1) % rhythmNames.size
            postInvalidateOnAnimation()
            return
        }
        if (btnSynthRect.contains(x, y)) {
            showSynthDrawer = !showSynthDrawer
            invalidate()
            return
        }
        if (btnOrientRect.contains(x, y)) {
            onToggleOrientation()
            return
        }

        // 3. Check Quick Pads Bar
        if (btnAssignPadRect.contains(x, y)) {
            assigningPadMode = !assigningPadMode
            invalidate()
            return
        }
        for (i in 0 until 8) {
            if (quickPadRects[i].contains(x, y)) {
                if (assigningPadMode) {
                    quickPadRoots[i] = activeRootIdx
                    quickPadTypes[i] = activeTypeIdx
                    assigningPadMode = false
                    saveQuickPads()
                } else {
                    activeRootIdx = quickPadRoots[i]
                    activeTypeIdx = quickPadTypes[i]
                    activeQuickPadIdx = i
                    pointerRole[pid] = 2
                    activeMatrixPointers++
                    synth.chordGateActive = true
                    recalculateChordAudio(triggerPad = true)
                    saveSynthPrefs()
                }
                invalidate()
                return
            }
        }

        // 4. Check Chord Matrix Buttons
        if (matrixAreaRect.contains(x, y)) {
            val hit = findChordButtonAt(x, y)
            if (hit != -1) {
                pointerRole[pid] = 2
                activeMatrixPointers++
                activeRootIdx = hit % MusicTheory.NUM_ROOTS
                activeTypeIdx = hit / MusicTheory.NUM_ROOTS
                activeQuickPadIdx = -1
                synth.chordGateActive = true
                recalculateChordAudio(triggerPad = true)
                saveSynthPrefs()
            }
        }
    }

    private fun handlePointerMove(pid: Int, x: Float, y: Float) {
        when (pointerRole[pid]) {
            1 -> { // STRUMMING SONIC STRINGS
                if (!strumSurfaceRect.contains(x, y) && (x < strumHousingRect.left - 24f * dp || x > strumHousingRect.right + 24f * dp)) {
                    return
                }
                val currentStr = stringIndexAtY(y)
                val prevStr = pointerLastString[pid]
                val now = SystemClock.uptimeMillis()
                val dtMs = (now - pointerLastTime[pid]).coerceAtLeast(1L)
                val dyDp = abs(y - pointerLastY[pid]) / dp

                // Calculate dynamic velocity from finger swipe speed
                val speedDpPerMs = dyDp / dtMs
                val velocity = (0.55f + speedDpPerMs * 0.35f).coerceIn(0.45f, 1.0f)

                if (currentStr != prevStr && prevStr in 0 until MusicTheory.NUM_STRINGS) {
                    // Interpolate all strings crossed during fast glissando swipe so zero strings are skipped!
                    val step = if (currentStr > prevStr) 1 else -1
                    var s = prevStr + step
                    while (true) {
                        triggerSonicString(s, velocity)
                        if (s == currentStr) break
                        s += step
                    }
                    pointerLastString[pid] = currentStr
                    pointerLastY[pid] = y
                    pointerLastTime[pid] = now
                }
            }

            2 -> { // SLIDING ACROSS CHORD MATRIX
                if (matrixAreaRect.contains(x, y)) {
                    val hit = findChordButtonAt(x, y)
                    if (hit != -1) {
                        val newRoot = hit % MusicTheory.NUM_ROOTS
                        val newType = hit / MusicTheory.NUM_ROOTS
                        if (newRoot != activeRootIdx || newType != activeTypeIdx) {
                            activeRootIdx = newRoot
                            activeTypeIdx = newType
                            recalculateChordAudio(triggerPad = true)
                        }
                    }
                }
            }

            3 -> { // ADJUSTING SYNTH SLIDER
                val s = pointerActiveSlider[pid]
                if (s in sliderRects.indices) {
                    updateSliderValue(s, x)
                }
            }
        }
    }

    private fun handlePointerUp(pid: Int) {
        if (pid !in 0 until maxPointers) return
        if (pointerRole[pid] == 2) {
            activeMatrixPointers = max(0, activeMatrixPointers - 1)
            if (!chordHoldLatch && activeMatrixPointers == 0) {
                synth.chordGateActive = false
                invalidate()
            }
        }
        pointerRole[pid] = 0
        pointerLastString[pid] = -1
        pointerActiveSlider[pid] = -1
    }

    private fun stringIndexAtY(y: Float): Int {
        val relY = (y - strumSurfaceRect.top).coerceIn(0f, strumSurfaceRect.height() - 0.1f)
        val visualRow = ((relY / strumSurfaceRect.height()) * MusicTheory.NUM_STRINGS).toInt()
            .coerceIn(0, MusicTheory.NUM_STRINGS - 1)
        return (MusicTheory.NUM_STRINGS - 1) - visualRow
    }

    private fun findChordButtonAt(x: Float, y: Float): Int {
        val visibleRows = if (showExtendedChords) 8 else 4
        val maxIdx = visibleRows * MusicTheory.NUM_ROOTS
        for (i in 0 until maxIdx) {
            if (chordBtnRects[i].contains(x, y)) {
                return i
            }
        }
        return -1
    }

    private fun updateSliderValue(sliderIdx: Int, x: Float) {
        val rect = sliderRects[sliderIdx]
        val norm = ((x - rect.left) / rect.width()).coerceIn(0f, 1f)
        when (sliderIdx) {
            0 -> synth.harpVolume = norm
            1 -> synth.sustainSeconds = 0.15f + norm * 3.65f
            2 -> synth.toneCutoffNorm = norm
            3 -> synth.chorusMix = norm
            4 -> synth.reverbMix = norm
            5 -> synth.chordVolume = norm
            6 -> synth.rhythmBpm = (70f + norm * 90f)
            7 -> {
                val newOct = (2 + (norm * 2.99f).toInt()).coerceIn(2, 4)
                if (newOct != baseOctave) {
                    baseOctave = newOct
                    recalculateChordAudio(triggerPad = false)
                }
            }
        }
        invalidate()
    }
}
