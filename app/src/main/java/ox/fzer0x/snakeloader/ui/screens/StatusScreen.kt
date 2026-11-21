package ox.fzer0x.snakeloader.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ox.fzer0x.snakeloader.R
import ox.fzer0x.snakeloader.AppInfo
import ox.fzer0x.snakeloader.FridaManager
import ox.fzer0x.snakeloader.ScriptManager
import ox.fzer0x.snakeloader.ui.components.*
import ox.fzer0x.snakeloader.ui.theme.SuccessGreen
import ox.fzer0x.snakeloader.ui.theme.WarningOrange
import ox.fzer0x.snakeloader.ui.viewmodels.DumperEngine
import ox.fzer0x.snakeloader.ui.viewmodels.StatusViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen(
    viewModel: StatusViewModel,
    scriptManager: ScriptManager,
    fridaManager: FridaManager,
    onNavigateToToolbox: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToStalker: () -> Unit,
    onNavigateToMemoryInspector: (() -> Unit)? = null,
    onNavigateToIl2CppInspector: ((DumperEngine) -> Unit)? = null,
    onNavigateToDeviceSpoofer: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refreshStatus(context)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ReShiftTopAppBar(
                title = "ReShift",
                isBrandTitle = true,
                isActiveStatus = viewModel.fridaRunning,
                actions = {
                    IconButton(onClick = { scope.launch { viewModel.refreshStatus(context, force = true) } }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.cd_refresh))
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.cd_options))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_close)) },
                            onClick = {
                                showMenu = false
                                context.findActivity()?.finishAndRemoveTask()
                            },
                            leadingIcon = { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_close_app), tint = MaterialTheme.colorScheme.error) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_frida_toolbox)) },
                            onClick = {
                                showMenu = false
                                onNavigateToToolbox()
                            },
                            leadingIcon = { Icon(Icons.Default.Terminal, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_settings)) },
                            onClick = {
                                showMenu = false
                                onNavigateToSettings()
                            },
                            leadingIcon = { Icon(Icons.Default.Settings, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_join_community)) },
                            onClick = {
                                showMenu = false
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, "https://t.me/+1FZrr4SqgMg1MDky".toUri())
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            leadingIcon = { Icon(Icons.Default.Group, null) }
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                QuickActionsGrid(
                    isFridaRunning = viewModel.fridaRunning,
                    onStartFrida = { viewModel.startFridaServer(force = viewModel.fridaRunning) },
                    onOpenToolbox = onNavigateToToolbox,
                    onOpenSettings = onNavigateToSettings,
                    onOpenIl2CppDumper = { onNavigateToIl2CppInspector?.invoke(DumperEngine.IL2CPP) },
                    onOpenUe4Dumper = { onNavigateToIl2CppInspector?.invoke(DumperEngine.UE4) },
                    onOpenHardwareSpoofer = { onNavigateToDeviceSpoofer?.invoke() },
                    onOpenMemoryInspector = { onNavigateToMemoryInspector?.invoke() },
                    isLoading = viewModel.isLoading,
                    hasRoot = viewModel.hasRoot
                )
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionHeader(stringResource(R.string.status_section_environment))
                    ReShiftCard {
                        Column(modifier = Modifier.padding(14.dp)) {
                            StatusRow(
                                label = stringResource(R.string.status_frida_service),
                                value = if (viewModel.isLoading) "..." else if (viewModel.fridaRunning) stringResource(R.string.common_active) else stringResource(R.string.common_inactive),
                                color = if (viewModel.fridaRunning) SuccessGreen else MaterialTheme.colorScheme.error,
                                icon = Icons.Outlined.Dns
                            )
                            if (viewModel.fridaRunning) {
                                Text(
                                    stringResource(R.string.status_version, viewModel.fridaVersion),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 28.dp)
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )

                            StatusRow(
                                label = stringResource(R.string.status_injection_mode),
                                value = when (if (viewModel.selectedFridaBinary != "auto") viewModel.selectedFridaBinary else viewModel.runningFridaBinary) {
                                    "cli" -> stringResource(R.string.status_mode_cli)
                                    "inject" -> stringResource(R.string.status_mode_inject)
                                    "server" -> stringResource(R.string.status_mode_server)
                                    "gadget" -> stringResource(R.string.status_mode_gadget)
                                    "auto" -> stringResource(R.string.status_mode_auto)
                                    else -> if (viewModel.fridaRunning) stringResource(R.string.common_active) else stringResource(R.string.common_none)
                                },
                                color = MaterialTheme.colorScheme.secondary,
                                icon = Icons.Outlined.Layers
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )

                            StatusRow(
                                label = stringResource(R.string.status_detection_stealth),
                                value = if (viewModel.isStealthModeEnabled) stringResource(R.string.common_active) else stringResource(R.string.common_default),
                                color = if (viewModel.isStealthModeEnabled) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                icon = Icons.Outlined.AdminPanelSettings
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Extension,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = viewModel.moduleLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    StatusBadge(
                                        text = if (!viewModel.isModuleInstalled) stringResource(R.string.status_not_installed) else "v${viewModel.moduleVersion}",
                                        color = if (!viewModel.isModuleInstalled) MaterialTheme.colorScheme.error
                                                else if (viewModel.hasModuleUpdate) WarningOrange
                                                else SuccessGreen
                                    )
                                    if (viewModel.isModuleInstalled) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        if (viewModel.isModuleLatest) {
                                            Surface(
                                                color = SuccessGreen.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.status_latest),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = SuccessGreen,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        } else if (viewModel.hasModuleUpdate) {
                                            Surface(
                                                color = WarningOrange.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.status_need_update),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = WarningOrange,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionHeader(stringResource(R.string.status_section_security))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SecurityInfoBox(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.status_root_access),
                            value = if (viewModel.hasRoot) stringResource(R.string.status_granted) else stringResource(R.string.status_restricted),
                            color = if (viewModel.hasRoot) SuccessGreen else MaterialTheme.colorScheme.error
                        )
                        SecurityInfoBox(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.status_selinux),
                            value = if (viewModel.isSelinuxPermissive) stringResource(R.string.status_permissive) else stringResource(R.string.status_enforcing),
                            color = if (viewModel.isSelinuxPermissive) WarningOrange else SuccessGreen
                        )
                    }
                }
            }

            if (viewModel.recentApps.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SectionHeader(stringResource(R.string.status_section_recent))
                    }
                }
                items(viewModel.recentApps) { app ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        RecentAppListItem(app, scriptManager, fridaManager, scope)
                    }
                }
            }

            item {
                val appVersion = remember(context) {
                    try {
                        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            context.packageManager.getPackageInfo(
                                context.packageName,
                                PackageManager.PackageInfoFlags.of(0)
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            context.packageManager.getPackageInfo(context.packageName, 0)
                        }
                        packageInfo.versionName ?: "1.1.1"
                    } catch (_: Exception) {
                        "1.1.1"
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, start = 16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        "ReShift $appVersion",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionsGrid(
    isFridaRunning: Boolean,
    onStartFrida: () -> Unit,
    onOpenToolbox: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenIl2CppDumper: () -> Unit,
    onOpenUe4Dumper: () -> Unit,
    onOpenHardwareSpoofer: () -> Unit,
    onOpenMemoryInspector: (() -> Unit)? = null,
    isLoading: Boolean,
    hasRoot: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            QuickActionChip(
                label = if (isFridaRunning) stringResource(R.string.status_restart_frida) else stringResource(R.string.status_start_frida),
                icon = Icons.Default.PowerSettingsNew,
                onClick = onStartFrida,
                enabled = !isLoading && hasRoot,
                modifier = Modifier.weight(1f)
            )
            QuickActionChip(
                label = stringResource(R.string.status_toolbox),
                icon = Icons.Default.Build,
                onClick = onOpenToolbox,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            QuickActionChip(
                label = stringResource(R.string.status_il2cpp_dumper),
                icon = Icons.Default.Inventory2,
                onClick = onOpenIl2CppDumper,
                tint = SuccessGreen,
                modifier = Modifier.weight(1f)
            )
            QuickActionChip(
                label = stringResource(R.string.status_ue4_dumper),
                icon = Icons.Default.ViewInAr,
                onClick = onOpenUe4Dumper,
                tint = WarningOrange,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            QuickActionChip(
                label = stringResource(R.string.status_memory_inspector),
                icon = Icons.Default.Memory,
                onClick = { onOpenMemoryInspector?.invoke() },
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
            QuickActionChip(
                label = stringResource(R.string.status_hardware_spoofer),
                icon = Icons.Default.PhoneAndroid,
                onClick = onOpenHardwareSpoofer,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun QuickActionChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color? = null
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = ReShiftChipShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) (tint ?: MaterialTheme.colorScheme.onSurface) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                fontSize = 11.5.sp,
                maxLines = 1,
                softWrap = false,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }
    }
}

@Composable
fun SecurityInfoBox(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    color: Color
) {
    ReShiftCard(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun RecentAppListItem(
    app: AppInfo,
    scriptManager: ScriptManager,
    fridaManager: FridaManager,
    scope: CoroutineScope
) {
    val assignedCount = remember(app.packageName) { scriptManager.getAssignmentsForApp(app.packageName).size }
    val iconBitmap = remember(app.packageName) {
        try {
            app.icon?.toBitmap()?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    ReShiftCard(
        onClick = {
            scope.launch {
                val scripts = scriptManager.getAssignmentsForApp(app.packageName)
                if (scripts.isNotEmpty()) {
                    val combinedScript = scripts.joinToString("\n\n") { s ->
                        "// --- ${s.name} ---\n${s.content}"
                    }
                    val success = fridaManager.executeScriptContent(app.packageName, combinedScript)
                    if (success) {
                        scriptManager.addToRecentApps(app.packageName)
                    }
                }
            }
        }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Android, null, modifier = Modifier.size(20.dp), tint = Color(0xFF3DDC84))
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(app.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(app.packageName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            if (assignedCount > 0) {
                Text(
                    text = stringResource(R.string.status_scripts_assigned, assignedCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            IconButton(
                onClick = {
                    fridaManager.closeTargetApp(app.packageName)
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.status_close_app),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(6.dp))

            Icon(
                Icons.Default.PlayArrow,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
