package app.tendwell.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.tendwell.apps.AppEntry
import app.tendwell.data.Prefs
import app.tendwell.ui.theme.Tendwell
import app.tendwell.ui.theme.TendwellType

/**
 * Long-press menu for an app, shared by Home and search.
 * Move up/down only appear when opened from Home.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionsSheet(
    app: AppEntry,
    onHome: Boolean,
    homeIsFull: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onAppInfo: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Tendwell.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = c.surface,
        contentColor = c.ink,
        scrimColor = c.scrim,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .background(c.line, RoundedCornerShape(999.dp)),
            )
        },
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp)) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text(app.label, style = TendwellType.cardTitle, color = c.ink)
                if (app.isWorkProfile) Text("Work profile", style = TendwellType.small, color = c.ink3)
            }
            Column(Modifier.padding(horizontal = 8.dp)) {
                when {
                    onHome -> {
                        if (canMoveUp) TwRow("Move up", onClick = { onMoveUp(); onDismiss() }, icon = TwIcons.ChevronUp)
                        if (canMoveDown) TwRow("Move down", onClick = { onMoveDown(); onDismiss() }, icon = TwIcons.ChevronDown)
                        TwRow("Remove from Home", onClick = { onRemove(); onDismiss() }, icon = TwIcons.Minus)
                    }
                    homeIsFull -> TwRow(
                        "Add to Home",
                        onClick = null,
                        icon = TwIcons.Plus,
                        supporting = "Home holds ${Prefs.MAX_ESSENTIALS} apps. Remove one first.",
                    )
                    else -> TwRow("Add to Home", onClick = { onAdd(); onDismiss() }, icon = TwIcons.Plus)
                }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = c.line)
                TwRow("App info", onClick = { onAppInfo(); onDismiss() }, icon = TwIcons.Info)
            }
        }
    }
}
