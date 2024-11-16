package lcpp.github.scrollbar.api

import android.annotation.SuppressLint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.tween
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import lcpp.github.scrollbar.api.ThumbStateType.Dormant
import lcpp.github.scrollbar.api.ThumbStateType.Dragging
import lcpp.github.scrollbar.api.ThumbStateType.Scrolling
import lcpp.github.scrollbar.usage.draggingColor
import lcpp.github.scrollbar.usage.thumbScrollingColor
import lcpp.github.scrollbar.usage.thumbShape
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

// 可拖动的滚动条
@Composable
fun ScrollableState.DraggableScrollbar(
    modifier: Modifier = Modifier,
    state: ScrollbarState,
    orientation: Orientation = Orientation.Vertical,
    currentIsFlingEnd: Boolean = true,
    isShowTrack: Boolean = true,
    isSupperSmall: Boolean = false,// 是否超小模式，通常用于内容较少的时候，表示装饰型不可拖动的滚动条
    thumbSize: Dp = if (isSupperSmall) 3.dp else 8.dp,// 拇指宽度

    onThumbMoved: (Float, Float) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    // 轨道的宽度——————拇指宽度的倍数（一般需要大于拇指，以使得用户可拖动范围更广...）
    val trackSize = thumbSize * 1.5f

    // 滚动条组件——————轨道 + 拇指
    Scrollbar(
        // 1——————轨道
        modifier = modifier.run {
            when (orientation) {
                Vertical -> width(trackSize).fillMaxHeight()
                Horizontal -> height(trackSize).fillMaxWidth()
            }
        },
        orientation = orientation,
        isSupperSmall = isSupperSmall,
        interactionSource = interactionSource,
        isShowTrack = isShowTrack,
        //itemSize = itemSize,
        state = state,
        // 2——————拇指
        thumb = {
            Box(
                modifier = Modifier
                    // 因为轨道比拇指大一些，所以这个拇指需要右移相应的位置，使其显示在轨道的最右侧
                    .offset(x = trackSize - thumbSize)
                    .run {
                        when (orientation) {
                            Vertical -> width(thumbSize).fillMaxHeight()
                            Horizontal -> height(thumbSize).fillMaxWidth()
                        }
                    }
                    // 定义了滚动拇指的交互状态变化、颜色、动画等
                    .scrollThumb(
                        this,
                        interactionSource,
                        currentIsFlingEnd = currentIsFlingEnd,
                        isSupperSmall = isSupperSmall,
                        thumbSize = thumbSize,
                    ),
            )
        },
        onThumbMoved = onThumbMoved,
    )
}

/*======================================内部使用=============================================*/
@Composable// 绘制滚动中的拇指
private fun Modifier.scrollThumb(
    scrollableState: ScrollableState,
    interactionSource: InteractionSource,
    isSupperSmall: Boolean = false,
    currentIsFlingEnd: Boolean = true,
    thumbSize: Dp = 0.dp,
): Modifier {
    // 获取颜色...
    val colorState_offsetXState = scrollbarThumbColor_OffsetX(
        scrollableState,
        interactionSource,
        isSupperSmall = isSupperSmall,
        currentIsFlingEnd = currentIsFlingEnd,
        thumbSize = thumbSize,
    )
    val colorState = colorState_offsetXState.first
    // 进行向右移动的偏移动画、背景透明动画
    val that = this then Modifier
        .offset(x = colorState_offsetXState.second.value)
        .graphicsLayer {
            alpha = 1f - (colorState_offsetXState.second.value / thumbSize)
        }
    // 进行拇指的颜色变化
    return that then ScrollThumbElement { colorState.value }
}

/**
 * The color of the scrollbar thumb as a function of its interaction state.
 * @param interactionSource source of interactions in the scrolling container
 */
enum class ThumbStateType {
    Scrolling,// 用户按下页面滚动
    Dragging,// 用户按下拇指滚动
    Dormant,// 休眠--彻底消失
}

/// 根据用户与页面、拇指的交互状态来控制拇指的背景颜色、偏移动画
@SuppressLint("CoroutineCreationDuringComposition", "UnrememberedMutableState")
@Composable
fun scrollbarThumbColor_OffsetX(
    scrollableState: ScrollableState,
    interactionSource: InteractionSource,
    isSupperSmall: Boolean = false,
    currentIsFlingEnd: Boolean = true,
    thumbSize: Dp = 0.dp,
): Pair<State<Color>, State<Dp>> {// 返回一个颜色、偏移量
    var state by remember { mutableStateOf(Dormant) }
    // 互动：按下
    val pressed by interactionSource.collectIsPressedAsState()// 测试：总是 false
    // 互动：悬停
    val hovered by interactionSource.collectIsHoveredAsState()
    // 互动：拖动右侧的小滚动条
    val dragged by interactionSource.collectIsDraggedAsState()

    // 手指按下拖动滚动条
    val dragging = (dragged || pressed || hovered)
    // 用户正在滚动左侧页面
    val isScrolling = /*!dragging ||*/
        (scrollableState.canScrollForward || scrollableState.canScrollBackward) &&
            // 这里有个坑，只有页面彻底停止滚动之后，才会变成false
            scrollableState.isScrollInProgress// 是否正在滚动，开始滚动：true，停止滚动：false

    // 测试结果：手指在屏幕上为true，手指离开屏幕立即为false——————不适合用来判断是否显示————嵌套滚动的快速滚动fling更适合...
    //println("测试scrollableState.isScrollInProgress = ${scrollableState.isScrollInProgress}")
//    println("测试scrollableState.lastScrolledBackward = ${scrollableState.}")
    println("测试currentIsFlingEnd = ${currentIsFlingEnd}")
    // 控制是否开始启动位置偏移动画
    var startOffsetAnimation by remember { mutableStateOf(true) }
    when {
        dragging -> {
            state = Dragging
            startOffsetAnimation = false
        }
        isScrolling -> {
            state = Scrolling
            startOffsetAnimation = false
        }
        currentIsFlingEnd -> {
            state = Dormant
        }
    }
    // 颜色动画状态
    val color = animateColorAsState(
        targetValue = when (state) {
            // 休眠——————手指按下拖动的颜色
            Dragging -> draggingColor()
            else -> thumbScrollingColor
        },
        animationSpec = SpringSpec(stiffness = Spring.StiffnessLow),
        label = "",
    )


    LaunchedEffect(state == Dormant) {
        println("滚动条状态监测: state = $state")
        if (state == Dormant) {
            delay(1500)
            startOffsetAnimation = true
        }
    }
    // 偏移量动画状态
    val offsetX = animateDpAsState(
        targetValue = when {
            // 休眠——————向右偏移这么多dp
            startOffsetAnimation -> thumbSize
            else -> 0.dp
        },
        animationSpec = tween(durationMillis = 600),
        label = "",
    )
    return Pair(color, offsetX)
}

// 把颜色传入，返回一个 Modifier 节点
private data class ScrollThumbElement(val colorProducer: ColorProducer) :
    ModifierNodeElement<ScrollThumbNode>() {
    override fun create(): ScrollThumbNode = ScrollThumbNode(colorProducer)//
    override fun update(node: ScrollThumbNode) {//
        node.colorProducer = colorProducer//
        node.invalidateDraw()
    }
}

//
private class ScrollThumbNode(
    var colorProducer: ColorProducer,//
) : DrawModifierNode, Modifier.Node() {
    // naive cache outline calculation if size is the same
    private var lastSize: Size? = null
    private var lastLayoutDirection: LayoutDirection? = null
    private var lastOutline: Outline? = null

    override fun ContentDrawScope.draw() {
        val color = colorProducer()
        val outline =
            if (size == lastSize && layoutDirection == lastLayoutDirection) {
                // 如果有缓存，就直接使用缓存
                lastOutline!!
            } else {
                // 否则，就绘制圆角形状
                thumbShape.createOutline(size, layoutDirection, this)
            }
        // 根据颜色来绘制
        if (color != Color.Unspecified) drawOutline(outline, color = color)

        lastOutline = outline
        lastSize = size
        lastLayoutDirection = layoutDirection
    }
}