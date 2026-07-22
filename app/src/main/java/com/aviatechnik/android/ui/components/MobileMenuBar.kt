package com.aviatechnik.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aviatechnik.android.ui.theme.AviaBlue
import com.aviatechnik.android.ui.theme.AviaMenuActive
import com.aviatechnik.android.ui.theme.AviaMenuDisabled

/**
 * Top menu bar — parity with components/mobile-menu.blade.php: blue #0d6efd
 * band, 36dp icon circles with labels below, the ACTIVE item gets a green
 * ring, disabled items are grey.
 */
data class MenuItem(
    val key: String,
    val label: String,
    val icon: ImageVector? = null,
    val letter: String? = null,
    val active: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit = {},
)

@Composable
fun MobileMenuBar(items: List<MenuItem>) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(AviaBlue)
            .padding(vertical = 6.dp),
    ) {
        items.forEach { item ->
            val tint = if (item.enabled) Color.White else AviaMenuDisabled
            Column(
                Modifier
                    .weight(1f)
                    .clickable(enabled = item.enabled && !item.active, onClick = item.onClick),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    if (item.active) {
                        Canvas(Modifier.size(34.dp)) {
                            drawCircle(color = AviaMenuActive, style = Stroke(width = 2.dp.toPx()))
                        }
                    }
                    if (item.icon != null) {
                        Icon(item.icon, contentDescription = item.label, tint = tint, modifier = Modifier.size(20.dp))
                    } else {
                        Text(item.letter ?: "", color = tint, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    item.label,
                    fontSize = 11.sp,
                    color = tint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                )
            }
        }
    }
}

/** WO context menu — parity with the web onShowPage menu: WO (back to the
 *  list) · Workorder · Tasks · Parts · Process, active item ringed. */
@Composable
fun WoMenuBar(active: String, onGo: (String) -> Unit) {
    MobileMenuBar(
        listOf(
            MenuItem("wo", "WO", icon = Icons.Filled.Brush,
                onClick = { onGo("list") }),
            MenuItem("workorder", "Workorder", letter = "W", active = active == "workorder",
                onClick = { onGo("detail") }),
            MenuItem("tasks", "Tasks", icon = Icons.Filled.Alarm,
                active = active == "tasks", onClick = { onGo("tasks") }),
            MenuItem("parts", "Parts", icon = Icons.Filled.Settings,
                active = active == "parts", onClick = { onGo("parts") }),
            MenuItem("process", "Process", icon = Icons.Filled.Timeline,
                active = active == "process", onClick = { onGo("process") }),
        ),
    )
}
