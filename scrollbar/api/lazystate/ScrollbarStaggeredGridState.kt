package lcpp.github.scrollbar.api.lazystate

import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemInfo
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
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
fun LazyStaggeredGridState.scrollbarState(// 记住由[LazyStaggeredGridState]中的变化驱动的[ScrollbarState]。
    itemsAvailable: Int,
    itemIndex: (LazyStaggeredGridItemInfo) -> Int = LazyStaggeredGridItemInfo::index,
): ScrollbarState {
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
                        visibleItemsInfo.find { it != first && it.lane == first.lane }
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
                thumbMovedPercent = thumbTravelPercent,
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
