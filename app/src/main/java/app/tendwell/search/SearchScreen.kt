package app.tendwell.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.tendwell.apps.AppEntry
import app.tendwell.ui.components.TwIconButton
import app.tendwell.ui.components.TwIcons
import app.tendwell.ui.components.TwSecondaryButton
import app.tendwell.ui.theme.Fraunces
import app.tendwell.ui.theme.Tendwell
import app.tendwell.ui.theme.TendwellType

/**
 * Search all apps. With no query it is the full A to Z list under letter
 * headers. Typing narrows it to a ranked list; Enter opens the top result.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchScreen(
    apps: List<AppEntry>,
    query: String,
    onQueryChange: (String) -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
    onClose: () -> Unit,
) {
    val c = Tendwell.colors
    val hits = remember(apps, query) { AppSearch.search(apps, query) { it.label } }
    val querying = query.isNotBlank()
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }
    LaunchedEffect(query) { listState.scrollToItem(0) }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .safeDrawingPadding(),
    ) {
        Row(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(c.surface)
                    .border(1.5.dp, if (focused || query.isNotEmpty()) c.primary else c.line, RoundedCornerShape(999.dp)),
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(
                    TwIcons.Search,
                    contentDescription = null,
                    tint = if (focused || query.isNotEmpty()) c.ink2 else c.ink3,
                    modifier = Modifier.padding(start = 20.dp).size(20.dp),
                )
                if (query.isEmpty()) {
                    Text("Search apps", style = TendwellType.body, color = c.ink3, modifier = Modifier.padding(start = 52.dp))
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TendwellType.body.copy(color = c.ink, fontWeight = FontWeight.Medium),
                    cursorBrush = SolidColor(c.primary),
                    interactionSource = interaction,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Go,
                    ),
                    keyboardActions = KeyboardActions(onGo = { if (querying) hits.firstOrNull()?.let { onLaunch(it.item) } }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 52.dp, end = if (query.isNotEmpty()) 76.dp else 20.dp)
                        .focusRequester(focusRequester)
                        .semantics { contentDescription = "Search apps" },
                )
                if (query.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp)
                            .height(44.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .combinedClickable(role = Role.Button, onClickLabel = "Clear search", onClick = { onQueryChange("") })
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Clear", style = TendwellType.small.copy(fontWeight = FontWeight.SemiBold), color = c.ink2)
                    }
                }
            }
            TwIconButton(TwIcons.Close, contentDescription = "Back to Home", onClick = onClose, modifier = Modifier.offset(x = 10.dp))
        }

        if (hits.isEmpty() && querying) {
            NoMatches(query = query.trim(), onClear = { onQueryChange("") })
            return@Column
        }

        val count = hits.size
        Text(
            text = if (count == 1) "1 app" else "$count apps",
            style = TendwellType.caption,
            color = c.ink3,
            modifier = Modifier
                .padding(start = 24.dp, end = 24.dp, top = 14.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).imePadding(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
        ) {
            if (querying) {
                items(hits, key = { it.item.key }) { hit ->
                    AppRow(hit, querying = true, onLaunch = onLaunch, onLongPress = onLongPress)
                }
            } else {
                AppSearch.groupByLetter(hits).forEachIndexed { index, group ->
                    item(key = "header-${group.letter}") {
                        Text(
                            group.letter,
                            style = TendwellType.eyebrow,
                            color = c.ink3,
                            modifier = Modifier
                                .padding(top = if (index == 0) 22.dp else 20.dp)
                                .semantics { heading() },
                        )
                    }
                    items(group.hits, key = { it.item.key }) { hit ->
                        AppRow(hit, querying = false, onLaunch = onLaunch, onLongPress = onLongPress)
                    }
                }
                item(key = "hint") {
                    Text(
                        "Long-press an app to add it to Home.",
                        style = TendwellType.caption,
                        color = c.ink3,
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppRow(
    hit: SearchHit<AppEntry>,
    querying: Boolean,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
) {
    val c = Tendwell.colors
    val app = hit.item
    val text = remember(hit, c) {
        buildAnnotatedString {
            if (hit.hasMatch) {
                append(hit.label.substring(0, hit.matchStart))
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = c.ink)) {
                    append(hit.label.substring(hit.matchStart, hit.matchEnd))
                }
                append(hit.label.substring(hit.matchEnd))
            } else {
                append(hit.label)
            }
        }
    }
    val description = if (app.isWorkProfile) "${app.label}, work profile" else app.label
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .combinedClickable(
                role = Role.Button,
                onClickLabel = "Open",
                onLongClickLabel = "Options",
                onLongClick = { onLongPress(app) },
                onClick = { onLaunch(app) },
            )
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = text,
            style = TendwellType.listApp,
            color = if (querying) c.ink2 else c.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (app.isWorkProfile) {
            Text(
                "Work",
                style = TendwellType.caption,
                color = c.onTimeSoft,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(c.timeSoft)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            )
        }
    }
}

@Composable
private fun NoMatches(query: String, onClear: () -> Unit) {
    val c = Tendwell.colors
    Column(
        modifier = Modifier
            .padding(start = 24.dp, end = 24.dp, top = 28.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("No app called “$query”", style = TendwellType.cardTitle.copy(fontFamily = Fraunces), color = c.ink)
            Text("Check the spelling, or it may not be installed.", style = TendwellType.body, color = c.ink2)
        }
        TwSecondaryButton("Clear search", onClick = onClear)
    }
}
