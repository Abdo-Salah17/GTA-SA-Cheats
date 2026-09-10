package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
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
            e.printStackTrace()
        }

        // 2. إرسال الحروف عبر خيط خلفي ومحاكاة المفاتيح
        Thread {
            try {
                val instrumentation = android.app.Instrumentation()
                cleanCode.forEachIndexed { index, char ->
                    val keyCode = getKeyCodeForChar(char)
                    if (keyCode != -1) {
                        instrumentation.sendKeyDownUpSync(keyCode)
                        onCharTyped?.invoke(index + 1, cleanCode.length, char)
                        Thread.sleep(120) // فاصل زمني لتتعرف اللعبة على الحروف
                    }
                }
                onFinished?.invoke(true)
            } catch (e: Exception) {
                // طريقة بديلة لإرسال الأحداث لو النظام منع Instrumentation
                cleanCode.forEachIndexed { index, char ->
                    val keyCode = getKeyCodeForChar(char)
                    if (keyCode != -1) {
                        sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
                        sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
                        onCharTyped?.invoke(index + 1, cleanCode.length, char)
                        Thread.sleep(120)
                    }
                }
                onFinished?.invoke(true)
            }
        }.start()
    }

    private fun getKeyCodeForChar(char: Char): Int {
        return when (char) {
            in 'A'..'Z' -> KeyEvent.KEYCODE_A + (char - 'A')
            in '0'..'9' -> KeyEvent.KEYCODE_0 + (char - '0')
            else -> -1
        }
    }

    companion object {
        var instance: CheatAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }
}
