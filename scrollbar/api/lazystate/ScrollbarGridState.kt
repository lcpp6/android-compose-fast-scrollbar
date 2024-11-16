package lcpp.github.scrollbar.api.lazystate

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import lcpp.github.scrollbar.api.ScrollbarState
import lcpp.github.scrollbar.api.lazyutils.interpolateFirstItemIndex
import lcpp.github.scrollbar.api.lazyutils.itemVisibilityPercentage
import lcpp.github.scrollbar.api.scrollbarStateValue
import lcpp.github.scrollbar.api.valueOf
import kotlin.math.min


@Composable
fun LazyGridState.scrollbarState(// 计算由[LazyGridState]中的变化驱动的[ScrollbarState]
    itemsAvailable: Int,// 网格中可滚动的项目总数。
    itemIndex: (LazyGridItemInfo) -> Int = LazyGridItemInfo::index,// 一个查找函数，用于查找网格中某个项目相对于[itemsAvailable]的索引。
): ScrollbarState {// 返回值
    val state = remember { ScrollbarState() }
    LaunchedEffect(this, itemsAvailable) {
        snapshotFlow {
            if (itemsAvailable == 0) return@snapshotFlow null

            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) return@snapshotFlow null

            val firstIndex = min(
                a = interpolateFirstItemIndex(
                    visibleItems = visibleItemsInfo,
                    itemSize = { layoutInfo.orientation.valueOf(it.size) },
                    offset = { layoutInfo.orientation.valueOf(it.offset) },
                    nextItemOnMainAxis = { first ->
                        when (layoutInfo.orientation) {
                            Orientation.Vertical -> visibleItemsInfo.find {
                                it != first && it.row != first.row
                            }

                            Orientation.Horizontal -> visibleItemsInfo.find {
                                it != first && it.column != first.column
                            }
                        }
                    },
                    itemIndex = itemIndex,
                ),
                b = itemsAvailable.toFloat(),
            )
            if (firstIndex.isNaN()) return@snapshotFlow null

            val itemsVisible = visibleItemsInfo.floatSumOf { itemInfo ->
                itemVisibilityPercentage(
                    itemSize = layoutInfo.orientation.valueOf(itemInfo.size),
                    itemStartOffset = layoutInfo.orientation.valueOf(itemInfo.offset),
                    viewportStartOffset = layoutInfo.viewportStartOffset,
                    viewportEndOffset = layoutInfo.viewportEndOffset,
                )
            }

            val thumbTravelPercent = min(
                a = firstIndex / itemsAvailable,
                b = 1f,
            )
            val thumbSizePercent = min(
                a = itemsVisible / itemsAvailable,
                b = 1f,
            )
            scrollbarStateValue(
                thumbSizePercent = thumbSizePercent,
                thumbMovedPercent = when {
                    layoutInfo.reverseLayout -> 1f - thumbTravelPercent
                    else -> thumbTravelPercent
                },
            )
        }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { state.onScroll(it) }
    }
    return state
}

private inline fun <T> List<T>.floatSumOf(selector: (T) -> Float): Float =
    fold(initial = 0f) { accumulator, listItem -> accumulator + selector(listItem) }
