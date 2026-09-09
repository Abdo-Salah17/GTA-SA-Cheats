package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.CheatCategory
import com.example.data.CheatCode
import com.example.data.CheatDatabase
import kotlin.math.abs
import kotlin.math.min

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingBubbleView: View? = null
    private var overlayMenuView: View? = null

    private var bubbleLayoutParams: WindowManager.LayoutParams? = null
    private var menuLayoutParams: WindowManager.LayoutParams? = null

    private var selectedCategory: CheatCategory? = null
    private var currentSearchQuery: String = ""

    companion object {
        private const val TAG = "OverlayService"
        const val NOTIFICATION_CHANNEL_ID = "gta_cheat_overlay_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.action.START_OVERLAY"
        const val ACTION_STOP = "com.example.action.STOP_OVERLAY"

        @Volatile
        var isOverlayRunning = false
            private set
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForegroundNotification()
        setupFloatingBubble()
        setupOverlayMenu()
        showBubble()
        isOverlayRunning = true
        Log.i(TAG, "OverlayService initialized successfully.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "GTA SA Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active floating cheat menu for GTA San Andreas"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, OverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("GTA SA Cheats Active")
            .setContentText("Tap floating icon to open in-game cheat codes")
            .setSmallIcon(R.drawable.ic_star_gta)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_power_red, "Stop Overlay", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun getOverlayWindowType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    private fun setupFloatingBubble() {
        val inflater = LayoutInflater.from(this)
        floatingBubbleView = inflater.inflate(R.layout.layout_floating_bubble, null)

        val layoutType = getOverlayWindowType()
        bubbleLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 350
        }

        // Dragging and click detection logic
        floatingBubbleView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val params = bubbleLayoutParams ?: return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (abs(dx) > 10 || abs(dy) > 10) {
                            isDragging = true
                        }
                        if (isDragging) {
                            params.x = initialX + dx
                            params.y = initialY + dy
                            try {
                                windowManager.updateViewLayout(floatingBubbleView, params)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error updating bubble layout", e)
                            }
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            // User tapped the bubble -> open expanded cheat menu
                            openMenu()
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    @SuppressLint("InflateParams")
    private fun setupOverlayMenu() {
        val inflater = LayoutInflater.from(this)
        overlayMenuView = inflater.inflate(R.layout.layout_overlay_menu, null)

        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(metrics)
        val screenWidth = metrics.widthPixels
        val menuWidth = min((screenWidth * 0.92f).toInt(), 1050)

        val layoutType = getOverlayWindowType()
        menuLayoutParams = WindowManager.LayoutParams(
            menuWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val menu = overlayMenuView ?: return

        val btnClose = menu.findViewById<ImageView>(R.id.btnCloseMenu)
        val btnStop = menu.findViewById<ImageView>(R.id.btnStopService)
        val etSearch = menu.findViewById<EditText>(R.id.etSearchCheat)
        val btnClearSearch = menu.findViewById<ImageView>(R.id.btnClearSearch)
        val tvA11y = menu.findViewById<TextView>(R.id.tvA11yStatus)

        // Minimize menu back to floating bubble
        btnClose.setOnClickListener {
            closeMenu()
        }

        // Stop the entire overlay service
        btnStop.setOnClickListener {
            Toast.makeText(this, "GTA SA Overlay Stopped", Toast.LENGTH_SHORT).show()
            stopSelf()
        }

        // Search text watcher
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                currentSearchQuery = s?.toString()?.trim() ?: ""
                btnClearSearch.visibility = if (currentSearchQuery.isNotEmpty()) View.VISIBLE else View.GONE
                refreshCheatList()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnClearSearch.setOnClickListener {
            etSearch.text?.clear()
        }

        // Update accessibility status indicator
        updateA11yStatusView(tvA11y)

        // Populate Category Tabs
        setupCategoryTabs(menu)

        // Populate initial cheat list
        refreshCheatList()
    }

    private fun updateA11yStatusView(tvA11y: TextView) {
        if (CheatAccessibilityService.isServiceRunning) {
            tvA11y.text = "A11y: ACTIVE"
            tvA11y.setTextColor(0xFF00E676.toInt())
            tvA11y.setBackgroundResource(R.drawable.bg_tab_chip_active)
        } else {
            tvA11y.text = "A11y: OFF"
            tvA11y.setTextColor(0xFFFF8A80.toInt())
            tvA11y.setBackgroundResource(R.drawable.bg_tab_chip_inactive)
            tvA11y.setOnClickListener {
                Toast.makeText(this, "Opening Accessibility Settings...", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            }
        }
    }

    private fun setupCategoryTabs(menuView: View) {
        val layoutCategoryTabs = menuView.findViewById<LinearLayout>(R.id.layoutCategoryTabs)
        layoutCategoryTabs.removeAllViews()

        // "ALL" tab
        val allTab = createCategoryTabChip("ALL", selectedCategory == null) {
            selectedCategory = null
            setupCategoryTabs(menuView)
            refreshCheatList()
        }
        layoutCategoryTabs.addView(allTab)

        // Specific category tabs
        for (cat in CheatCategory.values()) {
            val isSelected = selectedCategory == cat
            val tab = createCategoryTabChip(cat.title, isSelected) {
                selectedCategory = cat
                setupCategoryTabs(menuView)
                refreshCheatList()
            }
            layoutCategoryTabs.addView(tab)
        }
    }

    private fun createCategoryTabChip(text: String, isSelected: Boolean, onClick: () -> Unit): View {
        val textView = TextView(this).apply {
            this.text = text
            textSize = 12f
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            if (isSelected) {
                setBackgroundResource(R.drawable.bg_tab_chip_active)
                setTextColor(0xFF121217.toInt())
                paint.isFakeBoldText = true
            } else {
                setBackgroundResource(R.drawable.bg_tab_chip_inactive)
                setTextColor(0xFFDDDDDD.toInt())
                paint.isFakeBoldText = false
            }
            setPadding(32, 16, 32, 16)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 16
            }
            layoutParams = params
            setOnClickListener { onClick() }
        }
        return textView
    }

    private fun refreshCheatList() {
        val menu = overlayMenuView ?: return
        val layoutCheatList = menu.findViewById<LinearLayout>(R.id.layoutCheatList)
        val tvEmptyState = menu.findViewById<TextView>(R.id.tvEmptyState)
        val scrollContainer = menu.findViewById<ScrollView>(R.id.scrollCheatList)
        layoutCheatList.removeAllViews()

        val filteredCheats = CheatDatabase.allCheats.filter { cheat ->
            val matchCategory = selectedCategory == null || cheat.category == selectedCategory
            val matchSearch = if (currentSearchQuery.isEmpty()) {
                true
            } else {
                cheat.name.contains(currentSearchQuery, ignoreCase = true) ||
                        cheat.code.contains(currentSearchQuery, ignoreCase = true) ||
                        cheat.description.contains(currentSearchQuery, ignoreCase = true)
            }
            matchCategory && matchSearch
        }

        if (filteredCheats.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            scrollContainer.visibility = View.GONE
        } else {
            tvEmptyState.visibility = View.GONE
            scrollContainer.visibility = View.VISIBLE

            val inflater = LayoutInflater.from(this)
            for (cheat in filteredCheats) {
                val itemView = inflater.inflate(R.layout.item_overlay_cheat, layoutCheatList, false)
                bindCheatItemView(itemView, cheat)
                layoutCheatList.addView(itemView)
            }
        }
    }

    private fun bindCheatItemView(itemView: View, cheat: CheatCode) {
        val tvName = itemView.findViewById<TextView>(R.id.tvCheatName)
        val tvCategory = itemView.findViewById<TextView>(R.id.tvCheatCategory)
        val tvCode = itemView.findViewById<TextView>(R.id.tvCheatCodeBadge)
        val tvDesc = itemView.findViewById<TextView>(R.id.tvCheatDesc)
        val btnCopy = itemView.findViewById<Button>(R.id.btnCopyCheat)
        val btnInject = itemView.findViewById<Button>(R.id.btnInjectCheat)

        tvName.text = cheat.name
        tvCategory.text = cheat.category.title
        tvCode.text = cheat.code

        if (cheat.description.isNotEmpty()) {
            tvDesc.text = cheat.description
            tvDesc.visibility = View.VISIBLE
        } else {
            tvDesc.visibility = View.GONE
        }

        // Copy button
        btnCopy.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("GTA SA Cheat", cheat.code))
            Toast.makeText(this, "Copied: ${cheat.code}", Toast.LENGTH_SHORT).show()
        }

        // Inject button
        btnInject.setOnClickListener {
            executeCheatInjection(cheat)
        }
    }

    private fun executeCheatInjection(cheat: CheatCode) {
        val menu = overlayMenuView ?: return
        val layoutHud = menu.findViewById<LinearLayout>(R.id.layoutInjectionHud)
        val tvHud = menu.findViewById<TextView>(R.id.tvInjectionStatus)

        val a11yService = CheatAccessibilityService.instance
        if (a11yService == null) {
            Toast.makeText(
                this,
                "Accessibility Service not enabled! Code copied to clipboard: ${cheat.code}",
                Toast.LENGTH_LONG
            ).show()
            // Copy as fallback
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("GTA SA Cheat", cheat.code))

            // Guide user to enable accessibility
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            return
        }

        // Show real-time injection HUD
        layoutHud.visibility = View.VISIBLE
        tvHud.text = "Injecting [ ${cheat.code} ] ..."

        a11yService.injectCheatCode(
            code = cheat.code,
            cheatName = cheat.name,
            onCharTyped = { index, total, char ->
                tvHud.text = "Injecting $char ($index/$total) -> [ ${cheat.code} ]"
            },
            onFinished = { success ->
                tvHud.text = if (success) "✓ Activated: ${cheat.name}" else "Injection failed"
                layoutHud.postDelayed({
                    layoutHud.visibility = View.GONE
                    // Optionally minimize to bubble so user can resume GTA SA gameplay immediately!
                    closeMenu()
                }, 1200)
            }
        )
    }

    private fun showBubble() {
        try {
            if (floatingBubbleView?.windowToken == null) {
                windowManager.addView(floatingBubbleView, bubbleLayoutParams)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add floating bubble", e)
        }
    }

    private fun hideBubble() {
        try {
            if (floatingBubbleView?.windowToken != null) {
                windowManager.removeView(floatingBubbleView)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove floating bubble", e)
        }
    }

    private fun openMenu() {
        hideBubble()
        try {
            if (overlayMenuView?.windowToken == null) {
                // Refresh accessibility status view in case user enabled it in background
                overlayMenuView?.findViewById<TextView>(R.id.tvA11yStatus)?.let {
                    updateA11yStatusView(it)
                }
                windowManager.addView(overlayMenuView, menuLayoutParams)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay menu", e)
            showBubble()
        }
    }

    private fun closeMenu() {
        try {
            if (overlayMenuView?.windowToken != null) {
                windowManager.removeView(overlayMenuView)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove overlay menu", e)
        }
        showBubble()
    }

    override fun onDestroy() {
        super.onDestroy()
        isOverlayRunning = false
        try {
            if (floatingBubbleView?.windowToken != null) {
                windowManager.removeView(floatingBubbleView)
            }
            if (overlayMenuView?.windowToken != null) {
                windowManager.removeView(overlayMenuView)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up overlay views", e)
        }
        Log.i(TAG, "OverlayService destroyed.")
    }
}
