package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Path
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class CheatAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

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

        // 1. نسخ الشفرة للحافظة كنسخة احتياطية
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("GTA SA Cheat", cleanCode))
        } catch (e: Exception) {
            Log.e(TAG, "Clipboard error", e)
        }

        // 2. محاكاة ضغط المفاتيح عبر KeyEvents
        val handler = Handler(Looper.getMainLooper())
        var currentIdx = 0

        fun sendNextChar() {
            if (currentIdx >= cleanCode.length) {
                onFinished?.invoke(true)
                return
            }

            val char = cleanCode[currentIdx]
            val keyCode = getKeyCodeForChar(char)

            if (keyCode != -1) {
                try {
                    // إرسال الضغطة للنظام
                    sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
                    sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending key code $keyCode", e)
                }
            }

            onCharTyped?.invoke(currentIdx + 1, cleanCode.length, char)
            currentIdx++

            // فاصل زمني 100 ملي ثانية بين كل حرف ليتعرف عليه المحرك
            handler.postDelayed({ sendNextChar() }, 100)
        }

        sendNextChar()
    }

    private fun getKeyCodeForChar(char: Char): Int {
        return when (char) {
            in 'A'..'Z' -> KeyEvent.KEYCODE_A + (char - 'A')
            in '0'..'9' -> KeyEvent.KEYCODE_0 + (char - '0')
            else -> -1
        }
    }

    companion object {
        private const val TAG = "CheatAccessibility"
        
        @Volatile
        var instance: CheatAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "Service connected successfully.")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }
}
