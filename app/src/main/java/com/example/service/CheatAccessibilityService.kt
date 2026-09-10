package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
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

        // 2. إرسال الشفرة حرفاً بحرف إلى العنصر النشط على الشاشة
        val handler = Handler(Looper.getMainLooper())
        var currentIdx = 0

        fun sendNextChar() {
            if (currentIdx >= cleanCode.length) {
                onFinished?.invoke(true)
                return
            }

            val char = cleanCode[currentIdx]
            val targetNode = findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: rootInActiveWindow

            if (targetNode != null) {
                val arguments = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, char.toString())
                }
                targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            }

            onCharTyped?.invoke(currentIdx + 1, cleanCode.length, char)
            currentIdx++

            handler.postDelayed({ sendNextChar() }, 100)
        }

        sendNextChar()
    }

    companion object {
        private const val TAG = "CheatAccessibility"

        @Volatile
        var instance: CheatAccessibilityService? = null
            private set

        @JvmStatic
        val isServiceRunning: Boolean
            get() = instance != null

        @JvmStatic
        fun isServiceRunning(context: Context? = null): Boolean {
            return instance != null
        }
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
