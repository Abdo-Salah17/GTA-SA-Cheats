package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CheatAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var injectionJob: Job? = null

    companion object {
        private const val TAG = "CheatAccessibility"
        @Volatile
        var instance: CheatAccessibilityService? = null
            private set

        val isServiceRunning: Boolean
            get() = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "CheatAccessibilityService connected and ready.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Passive observation of active game window
    }

    override fun onInterrupt() {
        Log.w(TAG, "CheatAccessibilityService interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        injectionJob?.cancel()
        Log.i(TAG, "CheatAccessibilityService destroyed.")
    }

    /**
     * Injects the cheat code character-by-character into GTA San Andreas.
     * Uses dispatchGesture, sendKeyEvent / InputManager reflection, AccessibilityNodeInfo fallbacks,
     * and haptic confirmation.
     */
    fun injectCheatCode(
        code: String,
        cheatName: String = "",
        onCharTyped: ((index: Int, total: Int, char: Char) -> Unit)? = null,
        onFinished: ((success: Boolean) -> Unit)? = null
    ) {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.isEmpty()) {
            onFinished?.invoke(false)
            return
        }

        // Copy to system clipboard as immediate backup accessibility bridge
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("GTA SA Cheat", cleanCode))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy to clipboard", e)
        }

        injectionJob?.cancel()
        injectionJob = serviceScope.launch {
            val vibrator = getVibratorService()

            for (i in cleanCode.indices) {
                val char = cleanCode[i]
                val keyCode = getKeyCodeForChar(char)

                // 1. Attempt key injection via system input / reflection fallback
                tryInjectKeyEvent(keyCode)

                // 2. Attempt node text action injection if an active editable field is present
                tryInjectToActiveNode(char.toString())

                // 3. Optional mini gesture pulse (touch event) on Android 7.0+ (API 24+)
                tryDispatchMicroGesture()

                // Haptic feedback tick
                vibrateTick(vibrator)

                onCharTyped?.invoke(i + 1, cleanCode.length, char)

                // Delay between keystrokes to mimic human / hardware keyboard timing (80ms)
                delay(80)
            }

            // Final completion vibration
            vibrateSuccess(vibrator)

            withContext(Dispatchers.Main) {
                val displayMsg = if (cheatName.isNotEmpty()) {
                    "Cheat Activated: $cheatName ($cleanCode)"
                } else {
                    "Cheat Code Entered: $cleanCode"
                }
                Toast.makeText(applicationContext, displayMsg, Toast.LENGTH_SHORT).show()
                onFinished?.invoke(true)
            }
        }
    }

    private fun tryInjectKeyEvent(keyCode: Int) {
        if (keyCode == KeyEvent.KEYCODE_UNKNOWN) return

        try {
            // Android internal InputManager injection (works when permissions/ROM allows)
            val inputManagerClass = Class.forName("android.hardware.input.InputManager")
            val getInstanceMethod = inputManagerClass.getMethod("getInstance")
            val inputManager = getInstanceMethod.invoke(null)
            val injectMethod = inputManagerClass.getMethod(
                "injectInputEvent",
                android.view.InputEvent::class.java,
                Int::class.javaPrimitiveType
            )

            val downTime = SystemClock.uptimeMillis()
            val eventTime = SystemClock.uptimeMillis()
            val downEvent = KeyEvent(downTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0)
            val upEvent = KeyEvent(downTime, eventTime + 15, KeyEvent.ACTION_UP, keyCode, 0)

            injectMethod.invoke(inputManager, downEvent, 0)
            injectMethod.invoke(inputManager, upEvent, 0)
        } catch (ignored: Throwable) {
            // Falls back smoothly across non-rooted standard OEM devices
        }
    }

    private fun tryInjectToActiveNode(text: String) {
        try {
            val root = rootInActiveWindow ?: return
            val focusedNode = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            if (focusedNode != null && focusedNode.isEditable) {
                val current = focusedNode.text?.toString() ?: ""
                val args = Bundle().apply {
                    putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        current + text
                    )
                }
                focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                focusedNode.recycle()
            }
            root.recycle()
        } catch (e: Exception) {
            Log.v(TAG, "Active node injection skipped: ${e.message}")
        }
    }

    private fun tryDispatchMicroGesture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                // Subtle swipe/tap stroke on the bottom left corner if gesture dispatch is enabled
                val path = Path().apply {
                    moveTo(10f, 10f)
                    lineTo(11f, 11f)
                }
                val gesture = GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(path, 0, 10))
                    .build()
                dispatchGesture(gesture, null, null)
            } catch (e: Exception) {
                // Ignore if gesture is rejected
            }
        }
    }

    private fun getKeyCodeForChar(c: Char): Int {
        return when (c.uppercaseChar()) {
            in 'A'..'Z' -> KeyEvent.KEYCODE_A + (c.uppercaseChar() - 'A')
            in '0'..'9' -> KeyEvent.KEYCODE_0 + (c - '0')
            ' ' -> KeyEvent.KEYCODE_SPACE
            else -> KeyEvent.KEYCODE_UNKNOWN
        }
    }

    private fun getVibratorService(): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun vibrateTick(vibrator: Vibrator?) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(15)
            }
        } catch (ignored: Exception) {}
    }

    private fun vibrateSuccess(vibrator: Vibrator?) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 40, 50, 40), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 40, 50, 40), -1)
            }
        } catch (ignored: Exception) {}
    }
}
