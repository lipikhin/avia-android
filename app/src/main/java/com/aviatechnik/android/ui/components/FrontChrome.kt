package com.aviatechnik.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aviatechnik.android.BuildConfig

/** Public asset URL on the API host (img/…, no auth required). */
fun assetUrl(path: String): String =
    BuildConfig.API_BASE_URL.substringBefore("/api/") + "/" + path.trimStart('/')

/**
 * Front (title) chrome — parity with front/master.blade.php: black header
 * with the AVIATECHNIK logo + hamburger, navy gradient background
 * (#00008B → #0066CC) with the NODUS CORE hero centered.
 *
 * The hamburger toggles a right-aligned menu row below the bar (web parity:
 * tapping it reveals the Login link).
 */
@Composable
fun FrontShell(
    onHamburger: (() -> Unit)? = null,
    underBar: (@Composable () -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit = {},
) {
    Column(Modifier.fillMaxSize()) {
        // black header bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0B0B0B))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            AsyncImage(
                model = assetUrl("img/favicon.webp"),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(6.dp))
            AsyncImage(
                model = assetUrl("img/icons/AT_logo-rb.svg"),
                contentDescription = "AVIATECHNIK",
                modifier = Modifier.height(26.dp).width(170.dp),
            )
            Spacer(Modifier.weight(1f))
            // bordered hamburger button (bootstrap navbar-toggler look)
            Box(
                Modifier
                    .border(1.dp, Color(0xFF6C757D), RoundedCornerShape(6.dp))
                    .clickable(enabled = onHamburger != null) { onHamburger?.invoke() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = Color(0xFFADB5BD))
            }
        }

        // navy gradient with the NODUS hero
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF00008B), Color(0xFF0066CC)))),
        ) {
            AsyncImage(
                model = assetUrl("img/nodus.png"),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(300.dp),
            )
            // menu row revealed by the hamburger (right-aligned, below the bar)
            if (underBar != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 14.dp),
                ) { underBar() }
            }
            content()
        }
    }
}

/** The white "Login" link revealed by the hamburger (web parity). */
@Composable
fun FrontLoginLink(onClick: () -> Unit) {
    Text(
        "Login",
        color = Color.White,
        fontSize = 14.sp,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(6.dp),
    )
}
