package app.bookflow.reader.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal const val COMPACT_MAX_WIDTH_DP = 359
internal const val WIDE_MIN_WIDTH_DP = 840
internal const val WIDE_STAGE_MAX_WIDTH_DP = 1_040

internal enum class BookFlowLayoutClass {
    COMPACT,
    REGULAR,
    WIDE,
}

internal data class BookFlowLayout(
    val widthDp: Int,
    val heightDp: Int,
    val layoutClass: BookFlowLayoutClass,
    val horizontalPadding: Dp,
    val cardPadding: Dp,
) {
    val isCompact: Boolean get() = layoutClass == BookFlowLayoutClass.COMPACT
    val isWide: Boolean get() = layoutClass == BookFlowLayoutClass.WIDE
}

internal fun classifyBookFlowLayout(widthDp: Int): BookFlowLayoutClass = when {
    widthDp <= COMPACT_MAX_WIDTH_DP -> BookFlowLayoutClass.COMPACT
    widthDp >= WIDE_MIN_WIDTH_DP -> BookFlowLayoutClass.WIDE
    else -> BookFlowLayoutClass.REGULAR
}

@Composable
internal fun rememberBookFlowLayout(): BookFlowLayout {
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp
    val heightDp = configuration.screenHeightDp

    return remember(widthDp, heightDp) {
        val layoutClass = classifyBookFlowLayout(widthDp)
        BookFlowLayout(
            widthDp = widthDp,
            heightDp = heightDp,
            layoutClass = layoutClass,
            horizontalPadding = when (layoutClass) {
                BookFlowLayoutClass.COMPACT -> 12.dp
                BookFlowLayoutClass.REGULAR -> 20.dp
                BookFlowLayoutClass.WIDE -> 32.dp
            },
            cardPadding = when (layoutClass) {
                BookFlowLayoutClass.COMPACT -> 10.dp
                BookFlowLayoutClass.REGULAR -> 14.dp
                BookFlowLayoutClass.WIDE -> 16.dp
            },
        )
    }
}
