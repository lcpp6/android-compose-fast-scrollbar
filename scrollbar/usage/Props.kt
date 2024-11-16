package lcpp.github.scrollbar.usage

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

//# 项目说明
//该项目修改自 now in android (https://github.com/android/nowinandroid) 里面的滚动条，并改成了指定大小的滚动条，以及修复了指定大小滚动条的显示问题。

/// 定义一些属性常量
// 滚动条的颜色配置
@Composable
internal fun draggingColor():Color = Color.Magenta//MaterialTheme.colorScheme.secondary// 手动拖动滚动条的颜色

// 拇指常规悬停的颜色
internal val thumbScrollingColor = Color.Gray.copy(0.9f)

// 拇指的形状
internal val thumbShape = RoundedCornerShape(1.dp, 0.dp, 0.dp, 1.dp)

