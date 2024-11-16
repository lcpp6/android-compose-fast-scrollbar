package lcpp.github.scrollbar.usage

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import lcpp.github.scrollbar.api.DraggableScrollbar
import lcpp.github.scrollbar.api.lazystate.rememberDraggableScroller
import lcpp.github.scrollbar.api.lazystate.scrollbarState

// 带滚动的 LazyColumn
@Composable
fun JKLazyGrid(
    modifier: Modifier = Modifier,
    itemSize: Int,// 项目总数
    columns: GridCells,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement. Vertical = if (!reverseLayout) Arrangement. Top else Arrangement. Bottom,
    horizontalArrangement: Arrangement. Horizontal = Arrangement. Start,
    flingBehavior: FlingBehavior = ScrollableDefaults. flingBehavior(),
    userScrollEnabled: Boolean = true,
    content: LazyGridScope.() -> Unit
) {

    // 将 LazyList/LazyGrid 和滚动组件 DecorativeScrollbar 放在同一个 Box 内部
    Box(
        modifier = modifier,
    ) {
        // 1 滚动网格
        LazyVerticalGrid(
            modifier = modifier,
            columns = columns,
            state = state,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            verticalArrangement = verticalArrangement,
            horizontalArrangement = horizontalArrangement,
            flingBehavior = flingBehavior,
            userScrollEnabled = userScrollEnabled,
            content = content,
        )

        // 2 滚动条——————默认项目数量大于20才出现
        //if (itemSize > 20)
            state.DraggableScrollbar(
                modifier = Modifier.fillMaxHeight()
                    .align(Alignment.CenterEnd),//
                state = state.scrollbarState(
                    itemsAvailable = itemSize,// 传入大小
                ),
                orientation = Orientation.Horizontal,
                isSupperSmall = itemSize < 100,// 默认数量小于50就启用超小模式———— 大小减少一半、不可点击、拖动
                // 拇指移动，带动屏幕同步滚动...
                onThumbMoved = state.rememberDraggableScroller(
                    itemsAvailable = itemSize,// 传入项目数量
                ),
            )
    }
}