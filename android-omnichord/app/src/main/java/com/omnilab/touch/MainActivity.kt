package com.omnilab.touch

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import com.omnilab.touch.audio.OmnichordSynthEngine
import com.omnilab.touch.ui.OmnichordInstrumentView

class MainActivity : Activity() {

    private lateinit var synthEngine: OmnichordSynthEngine
    private lateinit var instrumentView: OmnichordInstrumentView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on while playing instrument
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        synthEngine = OmnichordSynthEngine(applicationContext)
        instrumentView = OmnichordInstrumentView(
            context = this,
            synth = synthEngine,
            onToggleOrientation = { toggleScreenOrientation() }
        )

        setContentView(instrumentView)
        enableImmersiveFullscreen()
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
        if (hasFocus) {
            enableImmersiveFullscreen()
        }
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
        synthEngine.stop()
        super.onDestroy()
    }
}
