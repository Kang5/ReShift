package ox.fzer0x.snakeloader.ui.screens

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ox.fzer0x.snakeloader.R
import ox.fzer0x.snakeloader.OverlayService
import ox.fzer0x.snakeloader.SettingsManager
import ox.fzer0x.snakeloader.FridaManager
import ox.fzer0x.snakeloader.service.ModelDownloadService
import ox.fzer0x.snakeloader.ui.components.ReShiftCard
import ox.fzer0x.snakeloader.ui.components.ReShiftTopAppBar
import ox.fzer0x.snakeloader.ui.components.ReShiftButtonShape
import ox.fzer0x.snakeloader.ui.components.ReShiftChipShape
import ox.fzer0x.snakeloader.ui.components.ReShiftDialogShape
import ox.fzer0x.snakeloader.ui.components.FridaBinaryManagerCard
import ox.fzer0x.snakeloader.ui.components.StatusRow
import ox.fzer0x.snakeloader.ui.components.StatusBadge
import ox.fzer0x.snakeloader.ui.components.SectionHeader
import ox.fzer0x.snakeloader.ui.theme.SuccessGreen
import ox.fzer0x.snakeloader.ui.theme.WarningOrange
import ox.fzer0x.snakeloader.ui.viewmodels.SettingsViewModel
import ox.fzer0x.snakeloader.utils.StealthUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = viewModel.fridaManager.settings
    val fridaManager = viewModel.fridaManager

    var isOverlayEnabled by remember { mutableStateOf(viewModel.fridaManager.settings.isOverlayEnabled) }
    var isLogcatOverlayEnabled by remember { mutableStateOf(settingsManager.isLogcatOverlayEnabled) }
    var isFridaOverlayEnabled by remember { mutableStateOf(settingsManager.isFridaOverlayEnabled) }
    var overlayTextSize by remember { mutableStateOf(settingsManager.overlayTextSize) }
    var overlayOpacity by remember { mutableStateOf(settingsManager.overlayOpacity) }
    var selectedOverlayColor by remember { mutableStateOf(settingsManager.overlayColor) }

    var isStealthModeEnabled by remember { mutableStateOf(settingsManager.isStealthModeEnabled) }
    var isAppMasked by remember {
        mutableStateOf(context.packageManager.getComponentEnabledSetting(
            ComponentName(context, "ox.fzer0x.snakeloader.LauncherMasked")
        ) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
    }

    var isSelinuxPermissive by remember { mutableStateOf(false) }
    var isPtraceScopeDisabled by remember { mutableStateOf(false) }
    var isYamaSupported by remember { mutableStateOf(true) }

    var showStealthWarning by remember { mutableStateOf(false) }
    var showInstallationDialog by remember { mutableStateOf(false) }

    fun refreshSystemStatus() {
        scope.launch(Dispatchers.IO) {
            val status = fridaManager.getFridaStatus()
            val yamaSupported = fridaManager.isYamaSupported()

            withContext(Dispatchers.Main) {
                isSelinuxPermissive = status["selinux_permissive"] as? Boolean ?: false
                isPtraceScopeDisabled = status["ptrace_scope_disabled"] as? Boolean ?: false
                isYamaSupported = yamaSupported
                isStealthModeEnabled = status["stealth_enabled"] as? Boolean ?: false
            }

            viewModel.refreshModuleStatus(context)
        }
    }

    LaunchedEffect(viewModel.isInstalling) {
        if (viewModel.isInstalling) {
            showInstallationDialog = true
        }
    }

    LaunchedEffect(Unit) {
        refreshSystemStatus()
    }

    fun updateOverlayService() {
        if (isOverlayEnabled || isLogcatOverlayEnabled || isFridaOverlayEnabled) {
            if (Settings.canDrawOverlays(context)) {
                context.startForegroundService(Intent(context, OverlayService::class.java))
            }
        } else {
            context.stopService(Intent(context, OverlayService::class.java))
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ReShiftTopAppBar(
                title = stringResource(R.string.action_settings),
                onBack = onBack,
                actions = {
                    IconButton(onClick = { refreshSystemStatus() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.cd_refresh))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SectionHeader(stringResource(R.string.settings_section_ui))
                ReShiftCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingSwitchRow(
                            title = stringResource(R.string.settings_script_log_overlay),
                            checked = isOverlayEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && !Settings.canDrawOverlays(context)) {
                                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                    context.startActivity(intent)
                                } else {
                                    isOverlayEnabled = enabled
                                    settingsManager.isOverlayEnabled = enabled
                                    updateOverlayService()
                                }
                            }
                        )
                        SettingSwitchRow(
                            title = stringResource(R.string.settings_logcat_overlay),
                            checked = isLogcatOverlayEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && !Settings.canDrawOverlays(context)) {
                                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                    context.startActivity(intent)
                                } else {
                                    isLogcatOverlayEnabled = enabled
                                    settingsManager.isLogcatOverlayEnabled = enabled
                                    updateOverlayService()
                                }
                            }
                        )
                        SettingSwitchRow(
                            title = stringResource(R.string.settings_frida_cli_overlay),
                            checked = isFridaOverlayEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && !Settings.canDrawOverlays(context)) {
                                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                    context.startActivity(intent)
                                } else {
                                    isFridaOverlayEnabled = enabled
                                    settingsManager.isFridaOverlayEnabled = enabled
                                    updateOverlayService()
                                }
                            }
                        )

                        if ((isOverlayEnabled || isLogcatOverlayEnabled || isFridaOverlayEnabled) && !Settings.canDrawOverlays(context)) {
                            Text(
                                stringResource(R.string.settings_overlay_permission),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )

                        Text(stringResource(R.string.settings_text_size, overlayTextSize.toInt()), style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = overlayTextSize,
                            onValueChange = {
                                overlayTextSize = it
                                settingsManager.overlayTextSize = it
                                if (Settings.canDrawOverlays(context)) updateOverlayService()
                            },
                            valueRange = 8f..24f,
                            steps = 16
                        )

                        Text(stringResource(R.string.settings_opacity, (overlayOpacity * 100).toInt()), style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = overlayOpacity,
                            onValueChange = {
                                overlayOpacity = it
                                settingsManager.overlayOpacity = it
                                if (Settings.canDrawOverlays(context)) updateOverlayService()
                            },
                            valueRange = 0.1f..1f
                        )

                        Spacer(Modifier.height(10.dp))
                        Text(stringResource(R.string.settings_render_color), style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(SettingsManager.OVERLAY_COLOR_PRESETS) { (colorLong, colorName) ->
                                val color = Color(colorLong)
                                val isSelected = selectedOverlayColor == colorLong
                                Surface(
                                    onClick = {
                                        selectedOverlayColor = colorLong
                                        settingsManager.overlayColor = colorLong
                                        if (Settings.canDrawOverlays(context)) updateOverlayService()
                                    },
                                    shape = CircleShape,
                                    color = color,
                                    border = if (isSelected) BorderStroke(2.5.dp, MaterialTheme.colorScheme.onSurface) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    if (isSelected) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = colorName,
                                                tint = if (colorLong == 0xFFFFFFFFL) Color.Black else Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(stringResource(R.string.settings_section_system))
                ReShiftCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingSwitchRow(
                            title = stringResource(R.string.settings_selinux_permissive),
                            description = stringResource(R.string.settings_selinux_permissive_desc),
                            checked = isSelinuxPermissive,
                            onCheckedChange = {
                                fridaManager.setSelinuxPermissive(it)
                                refreshSystemStatus()
                            }
                        )
                        SettingSwitchRow(
                            title = stringResource(R.string.settings_disable_ptrace),
                            description = stringResource(R.string.settings_disable_ptrace_desc),
                            checked = isPtraceScopeDisabled,
                            enabled = isYamaSupported,
                            onCheckedChange = {
                                scope.launch {
                                    if (it) fridaManager.disablePtraceScope() else fridaManager.enablePtraceScope()
                                    refreshSystemStatus()
                                }
                            }
                        )
                        if (!isYamaSupported) {
                            Text(stringResource(R.string.settings_ptrace_unsupported), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }

            item {
                SectionHeader(stringResource(R.string.settings_section_antidetect))
                ReShiftCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingSwitchRow(
                            title = stringResource(R.string.settings_global_stealth),
                            description = stringResource(R.string.settings_global_stealth_desc),
                            checked = isStealthModeEnabled,
                            onCheckedChange = { if (it) showStealthWarning = true else {
                                isStealthModeEnabled = false
                                settingsManager.isStealthModeEnabled = false
                                fridaManager.updateStealthConfig()
                            }}
                        )
                        SettingSwitchRow(
                            title = stringResource(R.string.settings_mask_icon),
                            description = stringResource(R.string.settings_mask_icon_desc),
                            checked = isAppMasked,
                            onCheckedChange = {
                                isAppMasked = it
                                StealthUtils.setAppMasked(context, it)
                            }
                        )

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = { StealthUtils.toggleAppIcon(context, true) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = ReShiftButtonShape,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                        ) {
                            Icon(Icons.Default.VisibilityOff, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_hide_from_launcher), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                SectionHeader(stringResource(R.string.settings_section_ai))
                ReShiftCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.settings_ai_intro),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = viewModel.aiProvider == "GEMINI",
                                onClick = { viewModel.updateAiProvider("GEMINI") },
                                label = { Text(stringResource(R.string.settings_provider_gemini), maxLines = 1) },
                                shape = ReShiftChipShape,
                                leadingIcon = { Icon(Icons.Default.Cloud, null, modifier = Modifier.size(16.dp)) }
                            )
                            FilterChip(
                                selected = viewModel.aiProvider == "OLLAMA",
                                onClick = { viewModel.updateAiProvider("OLLAMA") },
                                label = { Text(stringResource(R.string.settings_provider_ollama), maxLines = 1) },
                                shape = ReShiftChipShape,
                                leadingIcon = { Icon(Icons.Default.Dns, null, modifier = Modifier.size(16.dp)) }
                            )
                            FilterChip(
                                selected = viewModel.aiProvider == "ON_DEVICE_GGUF",
                                onClick = { viewModel.updateAiProvider("ON_DEVICE_GGUF") },
                                label = { Text(stringResource(R.string.settings_provider_gguf), maxLines = 1) },
                                shape = ReShiftChipShape,
                                leadingIcon = { Icon(Icons.Default.Smartphone, null, modifier = Modifier.size(16.dp)) }
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        if (viewModel.aiProvider == "GEMINI") {
                            var apiKeyInput by remember { mutableStateOf(viewModel.geminiApiKey) }
                            var apiKeyVisible by remember { mutableStateOf(false) }

                            OutlinedTextField(
                                value = apiKeyInput,
                                onValueChange = {
                                    apiKeyInput = it
                                    viewModel.updateGeminiApiKey(it)
                                },
                                label = { Text(stringResource(R.string.settings_gemini_api_key)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = ReShiftButtonShape,
                                singleLine = true,
                                visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                        Icon(if (apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                                    }
                                }
                            )

                            Spacer(Modifier.height(12.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SettingsManager.SUPPORTED_GEMINI_MODELS.forEach { (modelId, modelLabel) ->
                                    FilterChip(
                                        selected = viewModel.geminiModel == modelId,
                                        onClick = { viewModel.updateGeminiModel(modelId) },
                                        label = { Text(modelLabel, maxLines = 1) },
                                        shape = ReShiftChipShape,
                                        leadingIcon = {
                                            val icon = if (modelId.contains("lite")) Icons.Default.Bolt else Icons.Default.AutoAwesome
                                            Icon(icon, null, modifier = Modifier.size(16.dp))
                                        }
                                    )
                                }
                            }
                        } else if (viewModel.aiProvider == "ON_DEVICE_GGUF") {
                            val localGgufFiles = remember { viewModel.getDownloadedGgufModels(context) }

                            Text(
                                text = stringResource(R.string.settings_gguf_models_title),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))

                            data class PresetGgufModel(
                                val name: String,
                                val sizeText: String,
                                val ramText: String,
                                val url: String,
                                val filename: String,
                                val isUncensored: Boolean = false
                            )

                            val availablePresets = listOf(
                                PresetGgufModel(
                                    name = "Qwen 2.5 Coder 1.5B Instruct",
                                    sizeText = "~1.1 GB",
                                    ramText = stringResource(R.string.settings_ram_4gb),
                                    url = "https://huggingface.co/Qwen/Qwen2.5-Coder-1.5B-Instruct-GGUF/resolve/main/qwen2.5-coder-1.5b-instruct-q4_k_m.gguf",
                                    filename = "qwen2.5-coder-1.5b-instruct-q4_k_m.gguf"
                                ),
                                PresetGgufModel(
                                    name = "Qwen 2.5 Coder 3B Instruct (Abliterated)",
                                    sizeText = "~1.9 GB",
                                    ramText = stringResource(R.string.settings_ram_8gb),
                                    url = "https://huggingface.co/bartowski/Qwen2.5-Coder-3B-Instruct-abliterated-GGUF/resolve/main/Qwen2.5-Coder-3B-Instruct-abliterated-Q4_K_M.gguf",
                                    filename = "qwen2.5-coder-3b-instruct-abliterated-q4_k_m.gguf",
                                    isUncensored = true
                                ),
                                PresetGgufModel(
                                    name = "Qwen 2.5 Coder 7B Instruct (Abliterated)",
                                    sizeText = "~4.6 GB",
                                    ramText = stringResource(R.string.settings_ram_12gb),
                                    url = "https://huggingface.co/bartowski/Qwen2.5-Coder-7B-Instruct-abliterated-GGUF/resolve/main/Qwen2.5-Coder-7B-Instruct-abliterated-Q4_K_M.gguf",
                                    filename = "qwen2.5-coder-7b-instruct-abliterated-q4_k_m.gguf",
                                    isUncensored = true
                                )
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                availablePresets.forEach { preset ->
                                    val downloadedFile = localGgufFiles.find { it.name.equals(preset.filename, ignoreCase = true) }
                                    val isDownloaded = downloadedFile != null && downloadedFile.length() > 1000
                                    val isActive = isDownloaded && viewModel.onDeviceModelPath == downloadedFile?.absolutePath

                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = ReShiftButtonShape,
                                        color = if (isActive) {
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                        } else if (isDownloaded) {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                        },
                                        border = if (isActive) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(preset.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    Text("${preset.sizeText} | ${preset.ramText}", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                                }

                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    if (preset.isUncensored) {
                                                        Surface(
                                                            color = MaterialTheme.colorScheme.errorContainer,
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text(
                                                                stringResource(R.string.settings_badge_uncensored),
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.error,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }

                                                    if (isActive) {
                                                        Surface(
                                                            color = MaterialTheme.colorScheme.primary,
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text(
                                                                "ACTIVE",
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onPrimary,
                                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    } else if (isDownloaded) {
                                                        Surface(
                                                            color = SuccessGreen,
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text(
                                                                "READY",
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White,
                                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            Spacer(Modifier.height(8.dp))

                                            if (isDownloaded) {
                                                val sizeMb = (downloadedFile?.length() ?: 0L) / (1024 * 1024)
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = stringResource(R.string.settings_on_device_size, sizeMb, downloadedFile?.name ?: ""),
                                                        fontSize = 9.sp,
                                                        color = MaterialTheme.colorScheme.secondary
                                                    )

                                                    Button(
                                                        onClick = {
                                                            downloadedFile?.absolutePath?.let { viewModel.updateOnDeviceModelPath(it) }
                                                        },
                                                        enabled = !isActive,
                                                        shape = ReShiftChipShape,
                                                        modifier = Modifier.height(32.dp)
                                                    ) {
                                                        Icon(if (isActive) Icons.Default.Check else Icons.Default.PlayArrow, null, modifier = Modifier.size(12.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text(if (isActive) "Active" else "Set Active", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            } else {
                                                Button(
                                                    onClick = {
                                                        val intent = Intent(context, ModelDownloadService::class.java).apply {
                                                            action = ModelDownloadService.ACTION_DOWNLOAD_GGUF
                                                            putExtra(ModelDownloadService.EXTRA_DOWNLOAD_URL, preset.url)
                                                            putExtra(ModelDownloadService.EXTRA_FILENAME, preset.filename)
                                                        }
                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                            context.startForegroundService(intent)
                                                        } else {
                                                            context.startService(intent)
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxWidth().height(34.dp),
                                                    shape = ReShiftChipShape
                                                ) {
                                                    Icon(Icons.Default.Download, null, modifier = Modifier.size(14.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Download (${preset.sizeText})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Custom / manually added GGUF files check
                            val customFiles = localGgufFiles.filter { local ->
                                availablePresets.none { preset -> preset.filename.equals(local.name, ignoreCase = true) }
                            }

                            if (customFiles.isNotEmpty()) {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = stringResource(R.string.settings_custom_gguf),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                customFiles.forEach { customFile ->
                                    val isCustomActive = viewModel.onDeviceModelPath == customFile.absolutePath
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = ReShiftChipShape,
                                        color = if (isCustomActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        onClick = { viewModel.updateOnDeviceModelPath(customFile.absolutePath) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(customFile.name, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                Text(stringResource(R.string.settings_custom_gguf_line, customFile.length() / (1024 * 1024)), fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                            }
                                            if (isCustomActive) {
                                                Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            // BACKUP & RESTORE GGUF MODELS CARD
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = ReShiftButtonShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Backup, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(stringResource(R.string.settings_gguf_backup), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Text(
                                        text = stringResource(R.string.settings_gguf_backup_desc),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.backupGgufModelsToDownloads(context) { msg ->
                                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                }
                                            },
                                            modifier = Modifier.weight(1f).height(36.dp),
                                            shape = ReShiftChipShape
                                        ) {
                                            Icon(Icons.Default.Upload, null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(stringResource(R.string.settings_backup), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.restoreGgufModelsFromDownloads(context) { msg ->
                                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                }
                                            },
                                            modifier = Modifier.weight(1f).height(36.dp),
                                            shape = ReShiftChipShape
                                        ) {
                                            Icon(Icons.Default.Download, null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(stringResource(R.string.settings_restore), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        } else {
                            var urlInput by remember { mutableStateOf(viewModel.ollamaBaseUrl) }
                            var modelInput by remember { mutableStateOf(viewModel.ollamaModel) }

                            OutlinedTextField(
                                value = urlInput,
                                onValueChange = {
                                    urlInput = it
                                    viewModel.updateOllamaBaseUrl(it)
                                },
                                label = { Text(stringResource(R.string.settings_ollama_url)) },
                                placeholder = { Text(stringResource(R.string.settings_ollama_url_placeholder)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = ReShiftButtonShape,
                                singleLine = true
                            )

                            Spacer(Modifier.height(10.dp))

                            OutlinedTextField(
                                value = modelInput,
                                onValueChange = {
                                    modelInput = it
                                    viewModel.updateOllamaModel(it)
                                },
                                label = { Text(stringResource(R.string.settings_ollama_model)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = ReShiftButtonShape,
                                singleLine = true
                            )

                            Spacer(Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    val intent = Intent(context, ModelDownloadService::class.java).apply {
                                        action = ModelDownloadService.ACTION_PULL_MODEL
                                        putExtra(ModelDownloadService.EXTRA_BASE_URL, urlInput)
                                        putExtra(ModelDownloadService.EXTRA_MODEL_NAME, modelInput)
                                    }
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        context.startForegroundService(intent)
                                    } else {
                                        context.startService(intent)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = ReShiftButtonShape
                            ) {
                                Icon(Icons.Default.Dns, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.settings_pull_ollama), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        SettingSwitchRow(
                            title = stringResource(R.string.settings_auto_correction),
                            description = stringResource(R.string.settings_auto_correction_desc),
                            checked = viewModel.aiAutoCorrectionEnabled,
                            onCheckedChange = { viewModel.updateAiAutoCorrectionEnabled(it) }
                        )
                    }
                }
            }

            item {
                SectionHeader(stringResource(R.string.settings_section_frida_binary))
                FridaBinaryManagerCard(fridaManager = fridaManager)
            }

            item {
                SectionHeader(stringResource(R.string.settings_section_core_module))
                ReShiftCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        StatusRow(
                            label = stringResource(R.string.settings_status),
                            value = if (viewModel.isModuleInstalled) stringResource(R.string.settings_installed) else stringResource(R.string.settings_missing),
                            color = if (viewModel.isModuleInstalled) SuccessGreen else MaterialTheme.colorScheme.error,
                            icon = Icons.Default.Extension
                        )
                        StatusRow(
                            label = stringResource(R.string.settings_version_label),
                            value = viewModel.moduleVersion,
                            color = MaterialTheme.colorScheme.secondary,
                            icon = Icons.Default.Info
                        )
                        StatusRow(
                            label = stringResource(R.string.settings_service),
                            value = if (viewModel.isModuleServiceRunning) stringResource(R.string.settings_running) else stringResource(R.string.settings_stopped),
                            color = if (viewModel.isModuleServiceRunning) SuccessGreen else WarningOrange,
                            icon = Icons.Default.PlayCircle
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )

                        Button(
                            onClick = { viewModel.installModule(context) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = ReShiftButtonShape
                        ) {
                            Icon(Icons.Default.SystemUpdate, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (viewModel.isModuleInstalled) stringResource(R.string.settings_update_module) else stringResource(R.string.settings_install_module), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                SectionHeader("App Update")
                ReShiftCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Check for ReShift app updates on GitHub.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.updateManager.checkForUpdates(isManual = true)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = ReShiftButtonShape,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                        ) {
                            Icon(Icons.Default.Update, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Check for Updates", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showInstallationDialog) {
        AlertDialog(
            onDismissRequest = { if (!viewModel.isInstalling) showInstallationDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Terminal, null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Module Installation")
                }
            },
            text = {
                Column {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(400.dp),
                        color = Color(0xFF000000),
                        shape = ReShiftChipShape,
                        border = BorderStroke(1.dp, Color(0xFF333333))
                    ) {
                        val scrollState = rememberLazyListState()
                        val lines = viewModel.installationLog.split("\n")

                        LaunchedEffect(lines.size) {
                            if (lines.isNotEmpty()) {
                                scrollState.animateScrollToItem(lines.size - 1)
                            }
                        }

                        LazyColumn(
                            state = scrollState,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            items(lines) { line ->
                                Text(
                                    text = line,
                                    color = Color(0xFF00FF00),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }

                    if (viewModel.isInstalling) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            color = Color(0xFF00FF00),
                            trackColor = Color(0xFF003300)
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (viewModel.installSuccess && !viewModel.isInstalling) {
                        Button(
                            onClick = { viewModel.rebootDevice() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = ReShiftButtonShape,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.RestartAlt, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Reboot", fontWeight = FontWeight.Bold)
                        }
                    }

                    TextButton(
                        onClick = { showInstallationDialog = false },
                        enabled = !viewModel.isInstalling,
                        shape = ReShiftButtonShape
                    ) {
                        Text(if (viewModel.installSuccess) "Close" else "Done", fontWeight = FontWeight.Bold)
                    }
                }
            },
            shape = ReShiftDialogShape
        )
    }

    if (showStealthWarning) {
        AlertDialog(
            onDismissRequest = { showStealthWarning = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Enable Stealth Mode?") },
            text = {
                Text("Randomizes Frida paths, ports and names to avoid detection.\n\n" +
                     "Note: Internal CLI and standard PC tools (frida -U) will not work without manual setup.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        isStealthModeEnabled = true
                        settingsManager.isStealthModeEnabled = true
                        fridaManager.updateStealthConfig()
                        showStealthWarning = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = ReShiftButtonShape
                ) { Text("Enable", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showStealthWarning = false },
                    shape = ReShiftButtonShape
                ) { Text("Cancel") }
            },
            shape = ReShiftDialogShape
        )
    }
}

@Composable
fun SettingSwitchRow(
    title: String,
    description: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = Modifier.scale(0.85f)
        )
    }
}
