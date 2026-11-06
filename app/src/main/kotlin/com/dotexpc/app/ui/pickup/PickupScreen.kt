package com.dotexpc.app.ui.pickup

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dotexpc.app.data.PickupCode
import com.dotexpc.app.data.PickupStore
import com.dotexpc.app.data.Platform
import kotlinx.coroutines.delay
import com.dotexpc.app.ui.component.ClickAnchoredPositionProvider
import com.dotexpc.app.ui.component.trackPressX
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.DropdownArrowEndAction
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Undo
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 取件码页：手动导入、本机保存，不依赖任何平台接口。 */
@Composable
fun PickupScreen(
    pickups: PickupStore,
    addMenu: PickupAddMenuState,
    onOpenImport: (Platform) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollBehavior = MiuixScrollBehavior()

    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<PickupCode?>(null) }
    var draftCode by remember { mutableStateOf("") }
    var draftPlatform by remember { mutableStateOf(Platform.TAOBAO) }
    var draftNote by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showPlatformMenu by remember { mutableStateOf(false) }
    val platformTapX = remember { mutableFloatStateOf(0f) }
    val platformAnchor = remember { ClickAnchoredPositionProvider(platformTapX) }

    // 删除确认：勾过「本次打开不再提醒」后只在本进程内直接删除，不写盘。
    var pendingDelete by remember { mutableStateOf<PickupCode?>(null) }
    var rememberSkipDelete by remember { mutableStateOf(false) }
    var skipDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    // 已取件的剩余时间每分钟走一次，顺便把到期的件删掉。
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            now = System.currentTimeMillis()
            pickups.pruneExpired(now)
        }
    }

    fun openEditor(target: PickupCode?, platform: Platform) {
        editing = target
        draftCode = target?.code ?: ""
        draftPlatform = target?.platform ?: platform
        draftNote = target?.note ?: ""
        error = null
        showEditor = true
    }

    fun closeEditor() {
        showEditor = false
        editing = null
        error = null
        showPlatformMenu = false
        focusManager.clearFocus()
    }

    fun save() {
        val target = editing
        error = if (target == null) {
            pickups.add(draftCode, draftPlatform, draftNote)
        } else {
            pickups.update(target, draftCode, draftPlatform, draftNote)
        }
        if (error == null) closeEditor()
    }

    fun copy(code: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("取件码", code))
        Toast.makeText(context, "已复制取件码", Toast.LENGTH_SHORT).show()
    }

    fun requestDelete(item: PickupCode) {
        if (skipDeleteConfirm) {
            pickups.remove(item)
        } else {
            rememberSkipDelete = false
            pendingDelete = item
        }
    }

    fun confirmDelete() {
        val target = pendingDelete ?: return
        pickups.remove(target)
        if (rememberSkipDelete) skipDeleteConfirm = true
        pendingDelete = null
    }

    // 手动添加走取件码页自己的编辑器，入口借给主页面层的悬浮按钮。
    SideEffect { addMenu.openManualAdd = { openEditor(null, Platform.TAOBAO) } }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = "取件码",
                scrollBehavior = scrollBehavior,
            )

            if (pickups.codes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 100.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "还没有取件码\n点右下角的加号添加",
                        style = MiuixTheme.textStyles.footnote1,
                        color = colors.onSurfaceVariantSummary,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Platform.entries.forEach { platform ->
                        val group = pickups.codes.filter { it.platform == platform }
                        if (group.isEmpty()) return@forEach
                        item(key = "header-${platform.id}") {
                            PlatformHeader(platform = platform, count = group.size)
                        }
                        items(group, key = { it.code }) { item ->
                            PickupRow(
                                item = item,
                                now = now,
                                onCopy = { copy(item.code) },
                                onEdit = { openEditor(item, item.platform) },
                                onTogglePicked = { pickups.togglePicked(item) },
                                onDelete = { requestDelete(item) },
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = addMenu.expanded,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { addMenu.expanded = false },
            )
        }
    }

    OverlayBottomSheet(
        show = showEditor,
        title = if (editing != null) "编辑取件码" else "添加取件码",
        onDismissRequest = { closeEditor() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextField(
                value = draftCode,
                onValueChange = { value ->
                    if (value.length <= CODE_LIMIT) {
                        draftCode = value
                        error = null
                    }
                },
                label = "取件码，如 8-3-2048",
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )

            // 下拉锚点必须和弹层在同一个布局节点里，否则菜单会从别处弹出。
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .trackPressX(platformTapX)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.secondaryContainer)
                        .clickable { showPlatformMenu = true }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = draftPlatform.title,
                        style = MiuixTheme.textStyles.main,
                    )
                    Spacer(Modifier.weight(1f))
                    DropdownArrowEndAction(actionColor = colors.onSurfaceVariantSummary)
                }
                OverlayListPopup(
                    show = showPlatformMenu,
                    popupPositionProvider = platformAnchor,
                    onDismissRequest = { showPlatformMenu = false },
                ) {
                    ListPopupColumn {
                        Platform.entries.forEachIndexed { index, platform ->
                            DropdownImpl(
                                text = platform.title,
                                optionSize = Platform.entries.size,
                                isSelected = platform == draftPlatform,
                                index = index,
                                onSelectedIndexChange = { selected ->
                                    draftPlatform = Platform.entries[selected]
                                    showPlatformMenu = false
                                },
                            )
                        }
                    }
                }
            }

            TextField(
                value = draftNote,
                onValueChange = { value ->
                    if (value.length <= NOTE_LIMIT) {
                        draftNote = value
                        error = null
                    }
                },
                label = "备注（选填，$NOTE_LIMIT 字以内）",
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { save() }),
            )

            if (error != null) {
                Text(
                    text = error!!,
                    style = MiuixTheme.textStyles.footnote2,
                    color = colors.error,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(
                    text = "取消",
                    onClick = { closeEditor() },
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = { save() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                ) {
                    Text(text = if (editing != null) "保存" else "添加")
                }
            }
        }
    }

    OverlayDialog(
        show = pendingDelete != null,
        title = "删除取件码",
        summary = pendingDelete?.code,
        onDismissRequest = { pendingDelete = null },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { rememberSkipDelete = !rememberSkipDelete }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    state = if (rememberSkipDelete) ToggleableState.On else ToggleableState.Off,
                    onClick = { rememberSkipDelete = !rememberSkipDelete },
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "本次打开不再提醒",
                    style = MiuixTheme.textStyles.body1,
                    color = colors.onSurface,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TextButton(
                    text = "取消",
                    onClick = { pendingDelete = null },
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = { confirmDelete() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        color = colors.error,
                        contentColor = colors.onError,
                    ),
                ) {
                    Text(text = "删除")
                }
            }
        }
    }
}

@Composable
private fun PlatformHeader(platform: Platform, count: Int) {
    val colors = MiuixTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = platform.title,
            style = MiuixTheme.textStyles.body2,
            fontWeight = FontWeight.Medium,
            color = colors.onSurfaceVariantSummary,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = count.toString(),
            style = MiuixTheme.textStyles.footnote2,
            color = colors.onSurfaceVariantSummary,
        )
    }
}

@Composable
private fun PickupRow(
    item: PickupCode,
    now: Long,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onTogglePicked: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val actionTint = colors.onSurface.copy(alpha = 0.65f)

    Card(
        onClick = onCopy,
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        insideMargin = PaddingValues(start = 18.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = item.code,
                    style = MiuixTheme.textStyles.title3,
                    color = if (item.picked) colors.onSurfaceVariantSummary else colors.onSurface,
                    textDecoration = if (item.picked) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (item.note.isEmpty()) item.platform.title else "${item.platform.title} | ${item.note}",
                    style = MiuixTheme.textStyles.footnote2,
                    color = colors.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.autoDeleteAt > 0L) {
                    Text(
                        text = remainingLabel(item.autoDeleteAt - now),
                        style = MiuixTheme.textStyles.footnote2,
                        color = colors.onSurfaceVariantSummary,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = onEdit, minWidth = 36.dp, minHeight = 36.dp) {
                    Icon(
                        imageVector = MiuixIcons.Edit,
                        contentDescription = "编辑",
                        tint = actionTint,
                    )
                }
                IconButton(onClick = onTogglePicked, minWidth = 36.dp, minHeight = 36.dp) {
                    Icon(
                        imageVector = if (item.picked) MiuixIcons.Undo else MiuixIcons.Ok,
                        contentDescription = if (item.picked) "标记为未取" else "标记为已取",
                        tint = actionTint,
                    )
                }
                IconButton(onClick = onDelete, minWidth = 36.dp, minHeight = 36.dp) {
                    Icon(
                        imageVector = MiuixIcons.Delete,
                        contentDescription = "删除",
                        tint = actionTint,
                    )
                }
            }
        }
    }
}

/** 剩余时间展示：天/小时/分钟，最多保留两个单位。 */
private fun remainingLabel(remaining: Long): String {
    val minute = 60_000L
    val hour = 60 * minute
    val day = 24 * hour
    return when {
        remaining >= day -> "还剩 ${remaining / day} 天 ${(remaining % day) / hour} 小时后自动删除"
        remaining >= hour -> "还剩 ${remaining / hour} 小时后自动删除"
        else -> "还剩 ${(remaining / minute).coerceAtLeast(1)} 分钟后自动删除"
    }
}

private const val CODE_LIMIT = 24
private const val NOTE_LIMIT = 10
