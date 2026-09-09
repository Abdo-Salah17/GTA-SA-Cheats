package com.example

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.CheatCategory
import com.example.data.CheatCode
import com.example.data.CheatDatabase
import com.example.service.CheatAccessibilityService
import com.example.service.OverlayService
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.AmberGold
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkGold
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LightGold
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<ComposeView>(R.id.composeView).setContent {
            MyApplicationTheme(darkTheme = true) {
                MainCheatScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainCheatScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Permission and service states (refreshed on resume)
    var hasOverlayPermission by remember { mutableStateOf(checkOverlayPermission(context)) }
    var isAccessibilityActive by remember { mutableStateOf(checkAccessibilityService(context)) }
    var isOverlayServiceActive by remember { mutableStateOf(OverlayService.isOverlayRunning) }

    // Search and filter states
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<CheatCategory?>(null) }
    var activeInjectingCode by remember { mutableStateOf<String?>(null) }
    var injectionStatusText by remember { mutableStateOf<String?>(null) }

    // Auto refresh permissions when user navigates back from system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = checkOverlayPermission(context)
                isAccessibilityActive = checkAccessibilityService(context)
                isOverlayServiceActive = OverlayService.isOverlayRunning
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Permission request launchers
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* handled silently */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkCanvas
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Title Card
            item {
                HeroHeaderCard()
            }

            // Permissions Control Card
            item {
                PermissionsCard(
                    hasOverlay = hasOverlayPermission,
                    hasA11y = isAccessibilityActive,
                    onGrantOverlay = {
                        openOverlaySettings(context)
                    },
                    onGrantA11y = {
                        openAccessibilitySettings(context)
                    }
                )
            }

            // Floating Overlay Action Card
            item {
                OverlayControlsCard(
                    isServiceRunning = isOverlayServiceActive,
                    hasOverlayPermission = hasOverlayPermission,
                    onStartOverlay = {
                        if (!checkOverlayPermission(context)) {
                            Toast.makeText(
                                context,
                                "Please grant Display Over Apps permission first!",
                                Toast.LENGTH_LONG
                            ).show()
                            openOverlaySettings(context)
                        } else {
                            startOverlayService(context)
                            isOverlayServiceActive = true
                            Toast.makeText(
                                context,
                                "Floating Cheat Bubble started! Look for the star on your screen.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onStopOverlay = {
                        stopOverlayService(context)
                        isOverlayServiceActive = false
                        Toast.makeText(context, "Floating Cheat Overlay stopped.", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Keystroke Simulation Status Banner (if currently injecting)
            if (activeInjectingCode != null) {
                item {
                    InjectionStatusBanner(
                        code = activeInjectingCode ?: "",
                        status = injectionStatusText ?: "Simulating Keystrokes..."
                    )
                }
            }

            // How To Use Guide Card
            item {
                HowToUseCard()
            }

            // Cheat Codes Database Browser Section Header
            item {
                CheatsBrowserHeader(
                    totalCount = CheatDatabase.allCheats.size,
                    searchQuery = searchQuery,
                    onSearchChanged = { searchQuery = it },
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )
            }

            // Filtered Cheats List
            val filteredCheats = CheatDatabase.allCheats.filter { cheat ->
                val matchCat = selectedCategory == null || cheat.category == selectedCategory
                val matchQuery = if (searchQuery.isBlank()) {
                    true
                } else {
                    cheat.name.contains(searchQuery, ignoreCase = true) ||
                            cheat.code.contains(searchQuery, ignoreCase = true) ||
                            cheat.description.contains(searchQuery, ignoreCase = true)
                }
                matchCat && matchQuery
            }

            if (filteredCheats.isEmpty()) {
                item {
                    EmptySearchCard(query = searchQuery)
                }
            } else {
                items(filteredCheats, key = { it.code + it.name }) { cheat ->
                    CheatCodeCard(
                        cheat = cheat,
                        onInject = {
                            executeCheatInjection(
                                context = context,
                                cheat = cheat,
                                onStart = {
                                    activeInjectingCode = cheat.code
                                    injectionStatusText = "Injecting [ ${cheat.code} ] ..."
                                },
                                onProgress = { cur, tot, ch ->
                                    injectionStatusText = "Typing $ch ($cur/$tot)"
                                },
                                onEnd = {
                                    injectionStatusText = "✓ Activated: ${cheat.name}"
                                    activeInjectingCode = null
                                }
                            )
                        },
                        onCopy = {
                            copyCheatToClipboard(context, cheat.code)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HeroHeaderCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(AmberGold, DarkSurfaceVariant)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            brush = Brush.radialGradient(listOf(AmberGold, AccentOrange)),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "GTA SA",
                        tint = Color.Black,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Column {
                    Text(
                        text = "GTA SA CHEATS",
                        color = LightGold,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Mobile Edition • Android 5.0 to 15/16+",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Universal Floating Overlay and real-time accessibility key injection for GTA San Andreas on Android. Tap any cheat code to simulate keystrokes directly into the game window.",
                color = TextPrimary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun PermissionsCard(
    hasOverlay: Boolean,
    hasA11y: Boolean,
    onGrantOverlay: () -> Unit,
    onGrantA11y: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Permissions",
                    tint = AmberGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SYSTEM PERMISSIONS",
                    color = LightGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            // 1. Overlay Permission
            PermissionStatusRow(
                title = "Display Over Other Apps",
                description = "Required to show floating star icon during gameplay",
                isGranted = hasOverlay,
                buttonText = if (hasOverlay) "Granted" else "Enable",
                onAction = onGrantOverlay
            )

            // 2. Accessibility Service
            PermissionStatusRow(
                title = "Accessibility Key Injection",
                description = "Required to simulate character keypresses into GTA SA",
                isGranted = hasA11y,
                buttonText = if (hasA11y) "Active" else "Enable",
                onAction = onGrantA11y
            )
        }
    }
}

@Composable
fun PermissionStatusRow(
    title: String,
    description: String,
    isGranted: Boolean,
    buttonText: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceVariant)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isGranted) SuccessGreen else AccentOrange,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 22.dp, top = 2.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (isGranted) {
            Surface(
                color = SuccessGreen.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = buttonText,
                    color = SuccessGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        } else {
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp).testTag("enable_permission_btn")
            ) {
                Text(
                    text = buttonText,
                    color = Color.Black,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun OverlayControlsCard(
    isServiceRunning: Boolean,
    hasOverlayPermission: Boolean,
    onStartOverlay: () -> Unit,
    onStopOverlay: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Overlay",
                        tint = AmberGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "FLOATING OVERLAY SERVICE",
                        color = LightGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Surface(
                    color = if (isServiceRunning) SuccessGreen.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isServiceRunning) "● RUNNING" else "○ STOPPED",
                        color = if (isServiceRunning) SuccessGreen else Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = if (isServiceRunning)
                    "The floating cheat icon is active over all apps. Switch to GTA San Andreas and tap the icon to cheat."
                else
                    "Launch the floating action button to easily access cheat codes while playing GTA San Andreas.",
                color = TextSecondary,
                fontSize = 12.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isServiceRunning) {
                    Button(
                        onClick = onStartOverlay,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("start_overlay_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "START FLOATING CHEATS",
                            color = Color.Black,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onStopOverlay,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("stop_overlay_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "STOP OVERLAY",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InjectionStatusBanner(code: String, status: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1E0F)),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(AmberGold, AccentOrange)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                color = AmberGold,
                modifier = Modifier.size(24.dp),
                strokeWidth = 3.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "KEYSTROKE INJECTION IN PROGRESS",
                    color = LightGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = status,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun HowToUseCard() {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = LightGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "HOW TO USE WITH GTA SAN ANDREAS",
                        color = LightGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = if (expanded) "HIDE" else "SHOW",
                    color = AmberGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "1. Enable 'Display Over Apps' & 'Accessibility Key Injection' permissions above.",
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "2. Tap 'START FLOATING CHEATS'. A floating star icon appears.",
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "3. Launch GTA San Andreas Mobile (API 21 to 15+ supported).",
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "4. Tap the floating star during gameplay to bring up the cheat list.",
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "5. Tap '⚡ INJECT' next to any cheat to send simulated keystrokes directly into CJ's game engine!",
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CheatsBrowserHeader(
    totalCount: Int,
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    selectedCategory: CheatCategory?,
    onCategorySelected: (CheatCategory?) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CHEAT CODES DATABASE",
                color = LightGold,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
            Surface(
                color = DarkSurfaceVariant,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "$totalCount Cheats",
                    color = AmberGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_cheat_input"),
            placeholder = { Text("Search cheat by name or code (e.g. BEFWKSBQ, Jetpack)...", fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.Gray
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChanged("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = Color.Gray
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedIndicatorColor = AmberGold,
                unfocusedIndicatorColor = DarkSurfaceVariant,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedPlaceholderColor = Color.Gray,
                unfocusedPlaceholderColor = Color.Gray
            )
        )

        // Category Scroll Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoryFilterChip(
                text = "ALL",
                isSelected = selectedCategory == null,
                onClick = { onCategorySelected(null) }
            )

            for (cat in CheatCategory.values()) {
                CategoryFilterChip(
                    text = cat.title,
                    isSelected = selectedCategory == cat,
                    onClick = { onCategorySelected(cat) }
                )
            }
        }
    }
}

@Composable
fun CategoryFilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) AmberGold else DarkSurfaceVariant,
        modifier = Modifier.testTag("category_chip_$text")
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.Black else TextPrimary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

@Composable
fun CheatCodeCard(
    cheat: CheatCode,
    onInject: () -> Unit,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Color(0x33FFFFFF), Color.Transparent)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cheat.name,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = cheat.category.title,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                // Cheat Code Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF2C2214))
                        .border(1.dp, AmberGold, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = cheat.code,
                        color = LightGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }

            if (cheat.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = cheat.description,
                    color = Color(0xFFB0BEC5),
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onCopy,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberGold),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "COPY", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onInject,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                    modifier = Modifier.height(34.dp).testTag("inject_cheat_${cheat.code}")
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "Inject",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "INJECT",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun EmptySearchCard(query: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No cheats match \"$query\"",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Try searching for weapons, cars, weather, or exact code letters.",
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// Preserved for testing compatibility
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

// Helpers
private fun checkOverlayPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else {
        true
    }
}

private fun checkAccessibilityService(context: Context): Boolean {
    if (CheatAccessibilityService.isServiceRunning) return true

    val expectedComponentName = "${context.packageName}/${CheatAccessibilityService::class.java.canonicalName}"
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: ""
    return enabledServices.contains(context.packageName) || enabledServices.contains(expectedComponentName)
}

private fun openOverlaySettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

private fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}

private fun startOverlayService(context: Context) {
    val intent = Intent(context, OverlayService::class.java).apply {
        action = OverlayService.ACTION_START
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

private fun stopOverlayService(context: Context) {
    val intent = Intent(context, OverlayService::class.java).apply {
        action = OverlayService.ACTION_STOP
    }
    context.startService(intent)
}

private fun copyCheatToClipboard(context: Context, code: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("GTA SA Cheat", code))
    Toast.makeText(context, "Copied $code to clipboard!", Toast.LENGTH_SHORT).show()
}

private fun executeCheatInjection(
    context: Context,
    cheat: CheatCode,
    onStart: () -> Unit,
    onProgress: (Int, Int, Char) -> Unit,
    onEnd: () -> Unit
) {
    val service = CheatAccessibilityService.instance
    if (service == null) {
        // Fallback copy to clipboard
        copyCheatToClipboard(context, cheat.code)
        Toast.makeText(
            context,
            "Accessibility Service not active! Copied ${cheat.code} to clipboard.",
            Toast.LENGTH_LONG
        ).show()
        return
    }

    onStart()
    service.injectCheatCode(
        code = cheat.code,
        cheatName = cheat.name,
        onCharTyped = { cur, tot, ch ->
            onProgress(cur, tot, ch)
        },
        onFinished = {
            onEnd()
        }
    )
}
