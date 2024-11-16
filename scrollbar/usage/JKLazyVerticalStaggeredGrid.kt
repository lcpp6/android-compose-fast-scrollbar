package lcpp.github.scrollbar.usage

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import lcpp.github.scrollbar.api.DraggableScrollbar
import lcpp.github.scrollbar.api.lazystate.rememberDraggableScroller
import lcpp.github.scrollbar.api.lazystate.scrollbarState

// 带滚动的 LazyColumn
@Composable
fun JKLazyVerticalStaggeredGrid(
    modifier: Modifier = Modifier,
    itemSize: Int,// 项目总数
    columns: StaggeredGridCells,
    state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalItemSpacing: Dp = 0.dp,
    horizontalArrangement: Arrangement. Horizontal = Arrangement. spacedBy(0.dp),
    flingBehavior: FlingBehavior = ScrollableDefaults. flingBehavior(),
    userScrollEnabled: Boolean = true,
    content: LazyStaggeredGridScope.() -> Unit
) {

    // 将 LazyList/LazyGrid 和滚动组件 DecorativeScrollbar 放在同一个 Box 内部
    Box(
        modifier = modifier,
    ) {
        // 1 滚动网格
        LazyVerticalStaggeredGrid(
            modifier = modifier,
            columns = columns,
            state = state,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            verticalItemSpacing = verticalItemSpacing,
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