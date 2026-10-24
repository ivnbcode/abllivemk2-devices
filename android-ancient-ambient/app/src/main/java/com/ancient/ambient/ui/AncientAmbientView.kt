package com.ancient.ambient.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import com.ancient.ambient.audio.AncientAmbientSynthEngine
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * 𓂀 ANCIENT AMBIENT MACHINE 𓋹 — Superficie Táctil Multitouch 1:1 Arturia MiniLab MkII
 * Incluye:
 * - 16 Encoders Rotatorios (K1 y K9 Clickables + Modo SHIFT para editar los 16 Steps)
 * - 2 Tiras Táctiles Verticales (Pitch con retorno al centro + Mod CC1 retenida)
 * - 8 Pads RGB con 2 Bancos (1–8 Drones/Play/Mutate, 9–16 Escenas I–VIII)
 * - Secuenciador de 16 Steps (1–16)
 * - Pantalla Stargate Pirámide 3D XY + Osciloscopio en Tiempo Real
 * - Teclado de 25 Mini-Teclas con Glissando Multitouch
 */
class AncientAmbientView(
    context: Context,
    private val synth: AncientAmbientSynthEngine,
    private val onToggleOrientation: () -> Unit
) : View(context) {

    private val colObsidian = Color.parseColor("#060503")
    private val colBasalt = Color.parseColor("#110D08")
    private val colInset = Color.parseColor("#090704")
    private val colGold = Color.parseColor("#E6B84C")
    private val colGoldBright = Color.parseColor("#FFE494")
    private val colGoldDim = Color.parseColor("#55E6B84C")
    private val colCream = Color.parseColor("#F5EBD6")
    private val colMuted = Color.parseColor("#9E8F73")
    private val colCyan = Color.parseColor("#00F5D4")
    private val colCrimson = Color.parseColor("#E63946")
    private val colBlue = Color.parseColor("#00BBF9")
    private val colPurple = Color.parseColor("#C77DFF")

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }
    private val path = Path()

    private val knobNames = arrayOf(
        "DRONE", "ROOT HZ", "DRIFT", "PAD LVL", "MORPH", "V/OCT", "FM/GLIDE", "DRIVE",
        "LPF", "HPF", "RESO", "TEMPO", "TREMOLO", "DELAY", "REVERB", "MASTER"
    )
    private val waveNames = arrayOf("SUPERSAW", "CHOIR", "ORGAN", "GLASS", "TRIANGLE", "PULSE")
    private val filterNames = arrayOf("LP+HP·ST", "LP·ST", "HP·ST", "BP·MONO")
    private val presetNames = arrayOf(
        "I·GIZA", "II·ORION", "III·OSIRIS", "IV·SIRIUS",
        "V·ANUBIS", "VI·THOTH", "VII·HORUS", "VIII·DUAT"
    )
    private val scaleNames = arrayOf("JUST", "PHRYGIAN", "AEOLIAN", "PENTA", "LYDIAN", "MICRO")
    private val noteNames = arrayOf("I", "IIb", "II", "IIIb", "III", "IV", "Vb", "V", "VIb", "VI", "VIIb", "VII")
    private val isBlackSemitone = booleanArrayOf(false, true, false, true, false, false, true, false, true, false, true, false)

    // Rectángulos de hit-testing
    private val btnPresetRect = RectF()
    private val btnScaleRect = RectF()
    private val btnHoldRect = RectF()
    private val btnDirRect = RectF()
    private val btnRndRect = RectF()
    private val btnOrientRect = RectF()

    private val crtVectorRect = RectF()
    private val crtScopeRect = RectF()

    private val sysBtnRects = Array(4) { RectF() } // SHIFT, PAD BANK, OCT-, OCT+
    private val pitchStripRect = RectF()
    private val modStripRect = RectF()

    private val knobRects = Array(16) { RectF() }
    private val padRects = Array(8) { RectF() }
    private val stepRects = Array(16) { RectF() }
    private val whiteKeyRects = Array(15) { RectF() }
    private val whiteKeyNoteIdx = IntArray(15)
    private val blackKeyRects = Array(10) { RectF() }
    private val blackKeyNoteIdx = IntArray(10)

    // Seguimiento Multitouch por pointerId
    private val pointerRole = HashMap<Int, Int>() // -1=none, 0..15=knob, 100=pitch, 101=mod, 102=vector, 200=keybed
    private val pointerStartY = HashMap<Int, Float>()
    private val pointerStartVal = HashMap<Int, Float>()
    private val pointerKeyMap = HashMap<Int, Int>()

    private var animPhase = 0f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        animPhase += 0.03f
        canvas.drawColor(colObsidian)

        val pad = w * 0.01f
        val topH = h * 0.14f
        val keybedH = h * 0.25f
        val midH = h - topH - keybedH - pad * 4f

        // 1. BARRA SUPERIOR + PANTALLAS CRT STARGATE Y OSCILOSCOPIO
        drawTopBarAndCRTs(canvas, pad, pad, w - pad * 2f, topH)

        // 2. SUPERFICIE CENTRAL ARTURIA MINILAB MkII (ALA IZQUIERDA + ALA DERECHA)
        val midTop = topH + pad * 2f
        val leftWingW = (w - pad * 3f) * 0.21f
        val rightDeckW = (w - pad * 3f) - leftWingW
        drawLeftWing(canvas, pad, midTop, leftWingW, midH)
        drawRightDeck(canvas, pad * 2f + leftWingW, midTop, rightDeckW, midH)

        // 3. TECLADO INFERIOR DE 25 MINI-TECLAS
        val keyTop = midTop + midH + pad
        draw25Keybed(canvas, pad, keyTop, w - pad * 2f, keybedH)

        postInvalidateOnAnimation()
    }

    private fun drawTopBarAndCRTs(c: Canvas, x: Float, y: Float, w: Float, h: Float) {
        panelPaint.color = colBasalt
        val r = RectF(x, y, x + w, y + h)
        c.drawRoundRect(r, 10f, 10f, panelPaint)
        strokePaint.color = colGold
        strokePaint.strokeWidth = 2f
        c.drawRoundRect(r, 10f, 10f, strokePaint)

        // Título y 6 botones compactos a la izquierda
        val leftW = w * 0.52f
        textPaint.color = colGoldBright
        textPaint.textSize = h * 0.22f
        c.drawText("𓂀 ANCIENT AMBIENT MACHINE 𓋹", x + 14f, y + h * 0.32f, textPaint)

        val btnY = y + h * 0.45f
        val btnH = h * 0.44f
        val gap = 6f
        val bw = (leftW - 20f - gap * 5f) / 6f

        val labels = arrayOf(
            presetNames[synth.presetIdx],
            scaleNames[synth.scaleIdx],
            if (synth.keyHoldLatch) "HOLD:ON" else "HOLD:OFF",
            when (synth.seqDir) { 0 -> "DIR:→"; 1 -> "DIR:⇄"; else -> "DIR:🎲" },
            "🎲 STEP",
            "🔄 VIEW"
        )
        val rects = arrayOf(btnPresetRect, btnScaleRect, btnHoldRect, btnDirRect, btnRndRect, btnOrientRect)
        for (i in 0..5) {
            val bx = x + 12f + i * (bw + gap)
            rects[i].set(bx, btnY, bx + bw, btnY + btnH)
            panelPaint.color = if (i == 2 && synth.keyHoldLatch) colCyan else colInset
            c.drawRoundRect(rects[i], 6f, 6f, panelPaint)
            strokePaint.color = colGoldDim
            strokePaint.strokeWidth = 1.5f
            c.drawRoundRect(rects[i], 6f, 6f, strokePaint)

            textPaint.color = if (i == 2 && synth.keyHoldLatch) colObsidian else colGoldBright
            textPaint.textSize = btnH * 0.36f
            val tw = textPaint.measureText(labels[i])
            c.drawText(labels[i], rects[i].centerX() - tw / 2f, rects[i].centerY() + textPaint.textSize * 0.35f, textPaint)
        }

        // CRT 1: Stargate Pirámide 3D XY
        val crtW = (w - leftW - 24f) * 0.48f
        crtVectorRect.set(x + leftW + 4f, y + 6f, x + leftW + 4f + crtW, y + h - 6f)
        panelPaint.color = colObsidian
        c.drawRoundRect(crtVectorRect, 8f, 8f, panelPaint)
        strokePaint.color = colGold
        c.drawRoundRect(crtVectorRect, 8f, 8f, strokePaint)

        val cx = crtVectorRect.centerX()
        val cy = crtVectorRect.centerY()
        val rad = min(crtVectorRect.width(), crtVectorRect.height()) * 0.40f
        strokePaint.color = colGoldDim
        c.drawCircle(cx, cy, rad, strokePaint)

        // Pirámide 3D en el centro del Stargate
        strokePaint.color = colGold
        path.reset()
        path.moveTo(cx, cy - rad * 0.75f)
        path.lineTo(cx - rad * 0.85f, cy + rad * 0.65f)
        path.lineTo(cx + rad * 0.85f, cy + rad * 0.65f)
        path.close()
        c.drawPath(path, strokePaint)

        val px = crtVectorRect.left + synth.vecX * crtVectorRect.width()
        val py = crtVectorRect.top + synth.vecY * crtVectorRect.height()
        panelPaint.color = colCyan
        c.drawCircle(px, py, 7f, panelPaint)

        // CRT 2: Osciloscopio + Ventana LPF/HPF
        crtScopeRect.set(crtVectorRect.right + 8f, y + 6f, x + w - 8f, y + h - 6f)
        panelPaint.color = colObsidian
        c.drawRoundRect(crtScopeRect, 8f, 8f, panelPaint)
        strokePaint.color = colGold
        c.drawRoundRect(crtScopeRect, 8f, 8f, strokePaint)

        strokePaint.color = colCyan
        strokePaint.strokeWidth = 2f
        path.reset()
        val sw = crtScopeRect.width()
        val sh = crtScopeRect.height()
        for (i in 0 until 128) {
            val sx = crtScopeRect.left + (i / 127f) * sw
            val sy = crtScopeRect.centerY() - synth.scopeSamples[i].coerceIn(-1f, 1f) * (sh * 0.42f)
            if (i == 0) path.moveTo(sx, sy) else path.lineTo(sx, sy)
        }
        c.drawPath(path, strokePaint)
    }

    private fun drawLeftWing(c: Canvas, x: Float, y: Float, w: Float, h: Float) {
        val wingRect = RectF(x, y, x + w, y + h)
        panelPaint.color = colBasalt
        c.drawRoundRect(wingRect, 10f, 10f, panelPaint)
        strokePaint.color = colGold
        strokePaint.strokeWidth = 1.8f
        c.drawRoundRect(wingRect, 10f, 10f, strokePaint)

        val pad = 8f
        val btnH = h * 0.13f
        val bw = (w - pad * 3f) / 2f
        val sysLabels = arrayOf(
            if (synth.shiftSeqEdit) "SHIFT:SEQ" else "SHIFT",
            if (synth.padBankB) "PAD 9-16" else "PAD 1-8",
            "OCT −",
            "OCT +"
        )

        for (i in 0..3) {
            val col = i % 2
            val row = i / 2
            val bx = x + pad + col * (bw + pad)
            val by = y + pad + row * (btnH + 5f)
            sysBtnRects[i].set(bx, by, bx + bw, by + btnH)

            panelPaint.color = when {
                i == 0 && synth.shiftSeqEdit -> colBlue
                i == 1 && synth.padBankB -> colPurple
                else -> colInset
            }
            c.drawRoundRect(sysBtnRects[i], 6f, 6f, panelPaint)
            strokePaint.color = colGoldDim
            c.drawRoundRect(sysBtnRects[i], 6f, 6f, strokePaint)

            textPaint.color = if ((i == 0 && synth.shiftSeqEdit) || (i == 1 && synth.padBankB)) colObsidian else colCream
            textPaint.textSize = btnH * 0.36f
            val tw = textPaint.measureText(sysLabels[i])
            c.drawText(sysLabels[i], sysBtnRects[i].centerX() - tw / 2f, sysBtnRects[i].centerY() + textPaint.textSize * 0.35f, textPaint)
        }

        // 2 Tiras Táctiles Verticales (PITCH y MOD CC1)
        val stripsTop = y + pad * 2f + btnH * 2f + 12f
        val stripsBottom = y + h - pad - 14f
        val stripW = (w - pad * 3f) * 0.42f

        pitchStripRect.set(x + pad * 1.3f, stripsTop, x + pad * 1.3f + stripW, stripsBottom)
        modStripRect.set(x + w - pad * 1.3f - stripW, stripsTop, x + w - pad * 1.3f, stripsBottom)

        drawVerticalStrip(c, pitchStripRect, synth.pitchStripNorm, "↕ PITCH", true)
        drawVerticalStrip(c, modStripRect, synth.modStripNorm, "☼ MOD", false)
    }

    private fun drawVerticalStrip(c: Canvas, r: RectF, norm: Float, label: String, centerTick: Boolean) {
        panelPaint.color = colInset
        c.drawRoundRect(r, 8f, 8f, panelPaint)

        val fillH = r.height() * norm.coerceIn(0f, 1f)
        val fillRect = RectF(r.left + 2f, r.bottom - fillH, r.right - 2f, r.bottom - 2f)
        panelPaint.shader = LinearGradient(r.left, r.bottom, r.left, r.top, colGold, colCyan, Shader.TileMode.CLAMP)
        c.drawRoundRect(fillRect, 6f, 6f, panelPaint)
        panelPaint.shader = null

        strokePaint.color = colGold
        strokePaint.strokeWidth = 1.8f
        c.drawRoundRect(r, 8f, 8f, strokePaint)

        if (centerTick) {
            strokePaint.color = colGoldBright
            c.drawLine(r.left + 5f, r.centerY(), r.right - 5f, r.centerY(), strokePaint)
        }

        textPaint.color = colGoldBright
        textPaint.textSize = max(11f, r.width() * 0.24f)
        val tw = textPaint.measureText(label)
        c.drawText(label, r.centerX() - tw / 2f, r.bottom + 12f, textPaint)
    }

    private fun drawRightDeck(c: Canvas, x: Float, y: Float, w: Float, h: Float) {
        val deckRect = RectF(x, y, x + w, y + h)
        panelPaint.color = colBasalt
        c.drawRoundRect(deckRect, 10f, 10f, panelPaint)
        strokePaint.color = colGold
        strokePaint.strokeWidth = 1.8f
        c.drawRoundRect(deckRect, 10f, 10f, strokePaint)

        val pad = 7f
        val knobRowH = h * 0.30f
        val padsRowH = h * 0.19f
        val seqRowH = h - knobRowH * 2f - padsRowH - pad * 5f

        val kw = (w - pad * 9f) / 8f

        // 16 Perillas Rotatorias (2 filas x 8)
        for (i in 0 until 16) {
            val row = i / 8
            val col = i % 8
            val kx = x + pad + col * (kw + pad)
            val ky = y + pad + row * (knobRowH + pad)
            val kr = knobRects[i]
            kr.set(kx, ky, kx + kw, ky + knobRowH)

            val isClickable = (i == 0 || i == 8)
            panelPaint.color = colInset
            c.drawRoundRect(kr, 7f, 7f, panelPaint)
            strokePaint.color = if (isClickable) colCyan else colGoldDim
            strokePaint.strokeWidth = if (isClickable) 2.2f else 1.2f
            c.drawRoundRect(kr, 7f, 7f, strokePaint)

            val cx = kr.centerX()
            val cy = kr.top + kr.height() * 0.43f
            val radius = min(kr.width(), kr.height()) * 0.28f

            panelPaint.color = Color.parseColor("#1D1710")
            c.drawCircle(cx, cy, radius, panelPaint)
            strokePaint.color = if (isClickable) colCyan else colGold
            strokePaint.strokeWidth = 2f
            c.drawCircle(cx, cy, radius, strokePaint)

            // Indicador de ángulo
            val normVal = if (synth.shiftSeqEdit) (synth.seqNotes[i] / 11f) else synth.knobNorms[i]
            val angRad = ((-135f + normVal * 270f) - 90f) * (PI.toFloat() / 180f)
            strokePaint.color = if (isClickable) colCyan else colGoldBright
            strokePaint.strokeWidth = 2.8f
            c.drawLine(
                cx + cos(angRad) * (radius * 0.25f),
                cy + sin(angRad) * (radius * 0.25f),
                cx + cos(angRad) * (radius * 0.88f),
                cy + sin(angRad) * (radius * 0.88f),
                strokePaint
            )

            // Nombre y Valor (o Nota de Step si SHIFT está activo)
            textPaint.textSize = kr.height() * 0.14f
            textPaint.color = colCream
            val title = if (synth.shiftSeqEdit) "STEP ${i + 1}" else knobNames[i]
            val tw = textPaint.measureText(title)
            c.drawText(title, cx - tw / 2f, kr.bottom - kr.height() * 0.18f, textPaint)

            textPaint.color = if (isClickable) colCyan else colGoldBright
            val subText = when {
                synth.shiftSeqEdit -> noteNames[synth.seqNotes[i]]
                i == 0 -> waveNames[synth.wavePresetIdx]
                i == 8 -> filterNames[synth.filterModeIdx]
                i == 1 -> "${synth.getRootHz().toInt()}Hz"
                i == 11 -> "${synth.getTempoBpm().toInt()}BPM"
                else -> "${(synth.knobNorms[i] * 100).toInt()}%"
            }
            val vw = textPaint.measureText(subText)
            c.drawText(subText, cx - vw / 2f, kr.bottom - 4f, textPaint)
        }

        // Fila de 8 Pads RGB (Banco 1–8 Drones/Play/Mutate, Banco 9–16 Escenas)
        val padsTop = y + pad * 3f + knobRowH * 2f
        val pw = (w - pad * 9f) / 8f
        val padColors = intArrayOf(colCrimson, colGoldBright, colBlue, colGoldBright, colCyan, colPurple, colBlue, colCrimson)

        for (p in 0..7) {
            val px = x + pad + p * (pw + pad)
            val pr = padRects[p]
            pr.set(px, padsTop, px + pw, padsTop + padsRowH)

            val active = if (!synth.padBankB) {
                when {
                    p < 6 -> !synth.droneMuted[p]
                    p == 6 -> synth.seqRunning
                    else -> true
                }
            } else {
                synth.presetIdx == p
            }

            panelPaint.color = if (active) Color.argb(65, Color.red(padColors[p]), Color.green(padColors[p]), Color.blue(padColors[p])) else colInset
            c.drawRoundRect(pr, 7f, 7f, panelPaint)
            strokePaint.color = if (active) padColors[p] else colGoldDim
            strokePaint.strokeWidth = if (active) 2.5f else 1.2f
            c.drawRoundRect(pr, 7f, 7f, strokePaint)

            textPaint.color = if (active) colGoldBright else colMuted
            textPaint.textSize = pr.height() * 0.26f
            val lbl = if (!synth.padBankB) {
                when {
                    p < 6 -> "DRONE ${p + 1}"
                    p == 6 -> if (synth.seqRunning) "▶ SEQ ON" else "⏸ SEQ OFF"
                    else -> "🎲 MUTATE"
                }
            } else {
                presetNames[p]
            }
            val lw = textPaint.measureText(lbl)
            c.drawText(lbl, pr.centerX() - lw / 2f, pr.centerY() + textPaint.textSize * 0.35f, textPaint)
        }

        // Fila del Secuenciador de 16 Steps (1–16)
        val seqTop = padsTop + padsRowH + pad
        val sw = (w - pad * 17f) / 16f
        for (s in 0 until 16) {
            val sx = x + pad + s * (sw + pad)
            val sr = stepRects[s]
            sr.set(sx, seqTop, sx + sw, seqTop + max(18f, seqRowH))

            val isCurrent = (s == synth.seqStepIdx)
            val gateOn = synth.seqGates[s]

            panelPaint.color = when {
                isCurrent -> Color.parseColor("#3800F5D4")
                gateOn -> Color.parseColor("#22E6B84C")
                else -> colInset
            }
            c.drawRoundRect(sr, 5f, 5f, panelPaint)
            strokePaint.color = when {
                isCurrent -> colCyan
                gateOn -> colGold
                else -> colGoldDim
            }
            strokePaint.strokeWidth = if (isCurrent) 2.2f else 1f
            c.drawRoundRect(sr, 5f, 5f, strokePaint)

            textPaint.color = if (isCurrent) colCyan else if (gateOn) colGoldBright else colMuted
            textPaint.textSize = min(sr.width() * 0.38f, sr.height() * 0.38f)
            val stTxt = noteNames[synth.seqNotes[s]]
            val tw = textPaint.measureText(stTxt)
            c.drawText(stTxt, sr.centerX() - tw / 2f, sr.centerY() + textPaint.textSize * 0.35f, textPaint)
        }
    }

    private fun draw25Keybed(c: Canvas, x: Float, y: Float, w: Float, h: Float) {
        val kbRect = RectF(x, y, x + w, y + h)
        panelPaint.color = colObsidian
        c.drawRoundRect(kbRect, 8f, 8f, panelPaint)

        val gap = 3f
        val whiteW = (w - gap * 16f) / 15f
        var wIdx = 0
        val whiteXMap = FloatArray(25)

        // 15 Teclas Blancas
        for (k in 0 until 25) {
            if (!isBlackSemitone[k % 12]) {
                val kx = x + gap + wIdx * (whiteW + gap)
                whiteXMap[k] = kx
                val wr = whiteKeyRects[wIdx]
                wr.set(kx, y + 4f, kx + whiteW, y + h - 4f)
                whiteKeyNoteIdx[wIdx] = k

                val held = synth.heldKeys[k]
                panelPaint.color = if (held) colGold else Color.parseColor("#2A2115")
                c.drawRoundRect(wr, 6f, 6f, panelPaint)
                strokePaint.color = if (held) colCyan else colGold
                strokePaint.strokeWidth = 1.6f
                c.drawRoundRect(wr, 6f, 6f, strokePaint)

                textPaint.color = if (held) colObsidian else colGoldBright
                textPaint.textSize = whiteW * 0.24f
                val lbl = noteNames[k % 12]
                val lw = textPaint.measureText(lbl)
                c.drawText(lbl, wr.centerX() - lw / 2f, wr.bottom - 10f, textPaint)
                wIdx++
            }
        }

        // 10 Teclas Negras
        var bIdx = 0
        val blackW = whiteW * 0.64f
        val blackH = h * 0.58f
        for (k in 0 until 25) {
            if (isBlackSemitone[k % 12]) {
                val prevWhiteX = whiteXMap[k - 1]
                val bx = prevWhiteX + whiteW - blackW * 0.48f
                val br = blackKeyRects[bIdx]
                br.set(bx, y + 4f, bx + blackW, y + 4f + blackH)
                blackKeyNoteIdx[bIdx] = k

                val held = synth.heldKeys[k]
                panelPaint.color = if (held) colCyan else Color.parseColor("#0B0906")
                c.drawRoundRect(br, 5f, 5f, panelPaint)
                strokePaint.color = colCyan
                strokePaint.strokeWidth = 1.6f
                c.drawRoundRect(br, 5f, 5f, strokePaint)
                bIdx++
            }
        }
    }

    private fun findKeyAt(px: Float, py: Float): Int {
        for (b in 0 until 10) {
            if (blackKeyRects[b].contains(px, py)) return blackKeyNoteIdx[b]
        }
        for (w in 0 until 15) {
            if (whiteKeyRects[w].contains(px, py)) return whiteKeyNoteIdx[w]
        }
        return -1
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val pIdx = event.actionIndex
        val pid = event.getPointerId(pIdx)
        val px = event.getX(pIdx)
        val py = event.getY(pIdx)

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                // 1. Teclado de 25 Mini-Teclas
                val kHit = findKeyAt(px, py)
                if (kHit >= 0) {
                    pointerRole[pid] = 200
                    pointerKeyMap[pid] = kHit
                    synth.triggerKey(kHit, 0.85f)
                    invalidate()
                    return true
                }

                // 2. Tiras Táctiles Verticales
                if (pitchStripRect.contains(px, py)) {
                    pointerRole[pid] = 100
                    synth.pitchStripNorm = (1f - (py - pitchStripRect.top) / pitchStripRect.height()).coerceIn(0f, 1f)
                    invalidate()
                    return true
                }
                if (modStripRect.contains(px, py)) {
                    pointerRole[pid] = 101
                    synth.modStripNorm = (1f - (py - modStripRect.top) / modStripRect.height()).coerceIn(0f, 1f)
                    invalidate()
                    return true
                }

                // 3. Stargate Vector XY
                if (crtVectorRect.contains(px, py)) {
                    pointerRole[pid] = 102
                    synth.autoOrbit = false
                    synth.vecX = ((px - crtVectorRect.left) / crtVectorRect.width()).coerceIn(0.05f, 0.95f)
                    synth.vecY = ((py - crtVectorRect.top) / crtVectorRect.height()).coerceIn(0.05f, 0.95f)
                    invalidate()
                    return true
                }

                // 4. 16 Perillas Rotatorias (con Click en la parte inferior de K1 y K9)
                for (i in 0 until 16) {
                    if (knobRects[i].contains(px, py)) {
                        if ((i == 0 || i == 8) && py > knobRects[i].bottom - knobRects[i].height() * 0.30f) {
                            synth.triggerEncoderPush(i)
                            invalidate()
                            return true
                        }
                        pointerRole[pid] = i
                        pointerStartY[pid] = py
                        pointerStartVal[pid] = if (synth.shiftSeqEdit) (synth.seqNotes[i] / 11f) else synth.knobNorms[i]
                        return true
                    }
                }

                // 5. 8 Pads RGB
                for (p in 0..7) {
                    if (padRects[p].contains(px, py)) {
                        synth.handlePadTrigger(p, synth.padBankB)
                        invalidate()
                        return true
                    }
                }

                // 6. 16 Steps del Secuenciador
                for (s in 0 until 16) {
                    if (stepRects[s].contains(px, py)) {
                        synth.seqGates[s] = !synth.seqGates[s]
                        invalidate()
                        return true
                    }
                }

                // 7. 4 Botones de Sistema (SHIFT, PAD BANK, OCT-, OCT+)
                if (sysBtnRects[0].contains(px, py)) { synth.shiftSeqEdit = !synth.shiftSeqEdit; invalidate(); return true }
                if (sysBtnRects[1].contains(px, py)) { synth.padBankB = !synth.padBankB; invalidate(); return true }
                if (sysBtnRects[2].contains(px, py)) { synth.octaveShift = max(-2, synth.octaveShift - 1); invalidate(); return true }
                if (sysBtnRects[3].contains(px, py)) { synth.octaveShift = min(2, synth.octaveShift + 1); invalidate(); return true }

                // 8. Botones de la Barra Superior
                if (btnPresetRect.contains(px, py)) { synth.loadPreset((synth.presetIdx + 1) % 8); invalidate(); return true }
                if (btnScaleRect.contains(px, py)) { synth.scaleIdx = (synth.scaleIdx + 1) % 6; invalidate(); return true }
                if (btnHoldRect.contains(px, py)) {
                    synth.keyHoldLatch = !synth.keyHoldLatch
                    if (!synth.keyHoldLatch) synth.releaseAllKeys()
                    invalidate()
                    return true
                }
                if (btnDirRect.contains(px, py)) { synth.seqDir = (synth.seqDir + 1) % 3; invalidate(); return true }
                if (btnRndRect.contains(px, py)) { synth.randomizeSequencer(); invalidate(); return true }
                if (btnOrientRect.contains(px, py)) { onToggleOrientation(); return true }
            }

            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val id = event.getPointerId(i)
                    val x = event.getX(i)
                    val y = event.getY(i)
                    val role = pointerRole[id] ?: -1

                    when {
                        role in 0..15 -> {
                            val dy = (pointerStartY[id] ?: y) - y
                            val startV = pointerStartVal[id] ?: 0.5f
                            val nextNorm = (startV + dy / 140f).coerceIn(0f, 1f)
                            if (synth.shiftSeqEdit) {
                                synth.seqNotes[role] = (nextNorm * 11f).toInt().coerceIn(0, 11)
                            } else {
                                synth.knobNorms[role] = nextNorm
                            }
                        }
                        role == 100 -> synth.pitchStripNorm = (1f - (y - pitchStripRect.top) / pitchStripRect.height()).coerceIn(0f, 1f)
                        role == 101 -> synth.modStripNorm = (1f - (y - modStripRect.top) / modStripRect.height()).coerceIn(0f, 1f)
                        role == 102 -> {
                            synth.vecX = ((x - crtVectorRect.left) / crtVectorRect.width()).coerceIn(0.05f, 0.95f)
                            synth.vecY = ((y - crtVectorRect.top) / crtVectorRect.height()).coerceIn(0.05f, 0.95f)
                        }
                        role == 200 -> {
                            val newKey = findKeyAt(x, y)
                            val prevKey = pointerKeyMap[id] ?: -1
                            if (newKey != prevKey) {
                                if (prevKey >= 0) synth.releaseKey(prevKey)
                                if (newKey >= 0) {
                                    synth.triggerKey(newKey, 0.85f)
                                    pointerKeyMap[id] = newKey
                                } else {
                                    pointerKeyMap.remove(id)
                                }
                            }
                        }
                    }
                }
                invalidate()
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                val role = pointerRole.remove(pid) ?: -1
                if (role == 100) {
                    synth.pitchStripNorm = 0.5f // Retorno automático al centro en Pitch Strip
                } else if (role == 200) {
                    val prevKey = pointerKeyMap.remove(pid) ?: -1
                    if (prevKey >= 0) synth.releaseKey(prevKey)
                }
                invalidate()
            }
        }
        return true
    }
}
