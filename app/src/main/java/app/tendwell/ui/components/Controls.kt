package app.tendwell.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.tendwell.ui.theme.Tendwell
import app.tendwell.ui.theme.TendwellType

/** 44dp round icon button (spec: back/close/settings buttons). */
@Composable
fun TwIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Tendwell.colors.ink,
    iconSize: Dp = 24.dp,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick, role = Role.Button, onClickLabel = contentDescription),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** Secondary pill button: surface2 background, ink text. */
@Composable
fun TwSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Tendwell.colors
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(c.surface2)
            .clickable(onClick = onClick, role = Role.Button)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = TendwellType.button, color = c.ink)
    }
}

/** A tappable row in a sheet or grouped list: optional icon, label, optional supporting text. */
@Composable
fun TwRow(
    label: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    supporting: String? = null,
    trailing: ImageVector? = null,
) {
    val c = Tendwell.colors
    val enabled = onClick != null
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick, role = Role.Button) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (enabled) c.ink else c.ink3, modifier = Modifier.size(22.dp))
        }
        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
            Text(label, style = TendwellType.rowLabel, color = if (enabled) c.ink else c.ink3)
            if (supporting != null) {
                Text(supporting, style = TendwellType.small, color = c.ink3)
            }
        }
        if (trailing != null) {
            Icon(trailing, contentDescription = null, tint = c.ink3, modifier = Modifier.size(20.dp))
        }
    }
}
