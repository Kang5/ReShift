package ox.fzer0x.snakeloader.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import ox.fzer0x.snakeloader.R

@Composable
fun LaunchParametersSection(
    useExceptorOff: Boolean,
    onExceptorOffChange: (Boolean) -> Unit,
    useRuntimeV8: Boolean,
    onRuntimeV8Change: (Boolean) -> Unit,
    useNoPause: Boolean,
    onNoPauseChange: (Boolean) -> Unit,
    useDebugLog: Boolean,
    onDebugLogChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    ReShiftCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "LAUNCH PARAMETERS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ParameterToggle(
                    label = stringResource(R.string.lp_exceptor_off),
                    description = "--exceptor off",
                    checked = useExceptorOff,
                    onCheckedChange = onExceptorOffChange
                )
                ParameterToggle(
                    label = stringResource(R.string.lp_runtime_v8),
                    description = "--runtime=v8",
                    checked = useRuntimeV8,
                    onCheckedChange = onRuntimeV8Change
                )
                ParameterToggle(
                    label = stringResource(R.string.lp_no_pause),
                    description = "--no-pause",
                    checked = useNoPause,
                    onCheckedChange = onNoPauseChange
                )
                ParameterToggle(
                    label = stringResource(R.string.lp_debug_log),
                    description = "--debug",
                    checked = useDebugLog,
                    onCheckedChange = onDebugLogChange
                )
            }
        }
    }
}

@Composable
fun ParameterToggle(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.8f)
        )
    }
}

@Composable
fun ConfigGroup(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            label.uppercase(), 
            style = MaterialTheme.typography.labelMedium, 
            color = MaterialTheme.colorScheme.primary, 
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
        content()
    }
}
