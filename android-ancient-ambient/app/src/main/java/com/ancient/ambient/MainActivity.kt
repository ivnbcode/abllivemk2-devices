package com.ancient.ambient

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import com.ancient.ambient.audio.AncientAmbientSynthEngine
import com.ancient.ambient.ui.AncientAmbientView

class MainActivity : Activity() {

    private lateinit var synthEngine: AncientAmbientSynthEngine
    private lateinit var ambientView: AncientAmbientView
    private var midiManager: MidiManager? = null
    private val openPorts = mutableListOf<MidiOutputPort>()

    // Mapeo de los 16 CCs del Arturia MiniLab MkII (Memory 1 y Memory 8)
    private val primaryCcMap = intArrayOf(
        112, 74, 71, 76, 77, 93, 73, 75,
        114, 18, 19, 16, 17, 91, 79, 72
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        synthEngine = AncientAmbientSynthEngine(applicationContext)
        ambientView = AncientAmbientView(
            context = this,
            synth = synthEngine,
            onToggleOrientation = { toggleScreenOrientation() }
        )

        setContentView(ambientView)
        enableImmersiveFullscreen()
        setupUsbOtgMidi()
    }

    private fun setupUsbOtgMidi() {
        val mm = getSystemService(Context.MIDI_SERVICE) as? MidiManager ?: return
        midiManager = mm
        val handler = Handler(Looper.getMainLooper())

        mm.devices.forEach { info -> connectMidiDevice(info, handler) }

        mm.registerDeviceCallback(object : MidiManager.DeviceCallback() {
            override fun onDeviceAdded(device: MidiDeviceInfo) {
                connectMidiDevice(device, handler)
            }
        }, handler)
    }

    private fun connectMidiDevice(info: MidiDeviceInfo, handler: Handler) {
        midiManager?.openDevice(info, { device: MidiDevice? ->
            device ?: return@openDevice
            for (i in 0 until info.outputPortCount) {
                val port = device.openOutputPort(i) ?: continue
                port.connect(object : MidiReceiver() {
                    override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) {
                        var idx = offset
                        val end = offset + count
                        while (idx + 2 < end) {
                            val status = msg[idx].toInt() and 0xFF
                            val d1 = msg[idx + 1].toInt() and 0x7F
                            val d2 = msg[idx + 2].toInt() and 0x7F
                            handleMidiPacket(status, d1, d2)
                            idx += 3
                        }
                        ambientView.postInvalidateOnAnimation()
                    }
                })
                openPorts.add(port)
            }
        }, handler)
    }

    private fun handleMidiPacket(status: Int, d1: Int, d2: Int) {
        val cmd = status and 0xF0
        val ch = (status and 0x0F) + 1

        when (cmd) {
            0xB0 -> { // Control Change (16 Knobs, K1/K9 Clicks, Mod Strip CC1)
                if (d1 == 113 && d2 > 63) { synthEngine.triggerEncoderPush(0); return }
                if (d1 == 115 && d2 > 63) { synthEngine.triggerEncoderPush(8); return }
                if (d1 == 1) { synthEngine.modStripNorm = d2 / 127f; return }
                if (d1 == 64) {
                    synthEngine.keyHoldLatch = d2 >= 64
                    if (!synthEngine.keyHoldLatch) synthEngine.releaseAllKeys()
                    return
                }

                val kIdx = primaryCcMap.indexOf(d1)
                if (kIdx >= 0) {
                    val nextVal = if (d2 in 61..67 && d2 != 64) {
                        (synthEngine.knobNorms[kIdx] + (d2 - 64) * 0.015f).coerceIn(0f, 1f)
                    } else {
                        (d2 / 127f).coerceIn(0f, 1f)
                    }
                    if (synthEngine.shiftSeqEdit) {
                        synthEngine.seqNotes[kIdx] = (nextVal * 11f).toInt().coerceIn(0, 11)
                    } else {
                        synthEngine.knobNorms[kIdx] = nextVal
                    }
                }
            }
            0xE0 -> { // Pitch Bend Strip
                val pb14 = (d2 shl 7) or d1
                synthEngine.pitchStripNorm = (pb14 / 16383f).coerceIn(0f, 1f)
            }
            0x90 -> { // Note On
                if (d2 > 0) {
                    if (ch == 10 && d1 in 36..43) { synthEngine.handlePadTrigger(d1 - 36, false); return }
                    if (ch == 10 && d1 in 44..51) { synthEngine.handlePadTrigger(d1 - 44, true); return }
                    val keyIdx = ((d1 - 48) % 25 + 25) % 25
                    synthEngine.triggerKey(keyIdx, d2 / 127f)
                } else {
                    val keyIdx = ((d1 - 48) % 25 + 25) % 25
                    synthEngine.releaseKey(keyIdx)
                }
            }
            0x80 -> { // Note Off
                if (ch == 10 && d1 in 36..51) return
                val keyIdx = ((d1 - 48) % 25 + 25) % 25
                synthEngine.releaseKey(keyIdx)
            }
        }
    }

    private fun toggleScreenOrientation() {
        val currentOrientation = resources.configuration.orientation
        requestedOrientation = if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    private fun enableImmersiveFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableImmersiveFullscreen()
    }

    override fun onResume() {
        super.onResume()
        enableImmersiveFullscreen()
        synthEngine.start()
    }

    override fun onPause() {
        synthEngine.stop()
        super.onPause()
    }

    override fun onDestroy() {
        openPorts.forEach { runCatching { it.close() } }
        synthEngine.stop()
        super.onDestroy()
    }
}
