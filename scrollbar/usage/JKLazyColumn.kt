package lcpp.github.scrollbar.usage

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import lcpp.github.scrollbar.api.DraggableScrollbar
import lcpp.github.scrollbar.api.lazystate.rememberDraggableScroller
import lcpp.github.scrollbar.api.lazystate.scrollbarState
import lcpp.nt.application.components.animator.nestedScrollFling

// 带滚动的 LazyColumn
@Composable
fun JKLazyColumn(
    modifier: Modifier = Modifier,
    itemSize: Int,// 项目总数
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    content: LazyListScope.() -> Unit,
) {


    val scope = rememberCoroutineScope()
    // 定义用于控制弹簧动画的Animatable值
    var animatableValue by remember { mutableFloatStateOf(0f) }
    var currentIsFlingEnd by remember { mutableStateOf(true) }
    // 将 LazyList/LazyGrid 和滚动组件 DecorativeScrollbar 放在同一个 Box 内部

    val isSmallScrollbar = itemSize < 100
    Box(
        modifier = modifier// 添加快速滚动到边缘的动画...
            .nestedScrollFling(
                state,
                scope,
                isFlingEnd = {
                    currentIsFlingEnd = it
                },
                onValueChange = {delta->
                    animatableValue = if (!isSmallScrollbar)delta else delta/2
                }
            ).graphicsLayer {
                translationY = (animatableValue.dp).toPx() // 根据动画值改变 y 位置
            },
    ) {
        // 1 滚动列表
        LazyColumn(
            modifier = modifier,
            state = state,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            flingBehavior = flingBehavior,
            userScrollEnabled = userScrollEnabled,
            content = content,
        )
        // 2 滚动条——————默认项目数量大于20才出现
        //if (itemSize > 20)
            state.DraggableScrollbar(
                modifier = Modifier.fillMaxHeight()
                    //.windowInsetsPadding(WindowInsets.systemBars)
                    .align(Alignment.CenterEnd),//
                state = state.scrollbarState(
                    itemsAvailable = itemSize,// 传入大小
                ),
                currentIsFlingEnd = currentIsFlingEnd,
                orientation = Orientation.Vertical,
                isSupperSmall = isSmallScrollbar,// 默认数量小于100就启用超小模式———— 大小减少一半、不可点击、拖动?
                // 拇指移动，带动屏幕同步滚动...
                onThumbMoved = state.rememberDraggableScroller(
                    itemsAvailable = itemSize,// 传入项目数量
                ),
            )
    }
}