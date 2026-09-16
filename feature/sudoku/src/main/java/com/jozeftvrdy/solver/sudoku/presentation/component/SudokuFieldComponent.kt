package com.jozeftvrdy.solver.sudoku.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.jozeftvrdy.solver.sudoku.model.SudokuInputTileType
import com.jozeftvrdy.solver.sudoku.model.SudokuPosition
import com.jozeftvrdy.solver.sudoku.presentation.theme.SudokuTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlin.math.roundToInt

@Composable
fun SudokuField(
    positions: ImmutableList<SudokuPosition>,
    provideBorderSide: (position: SudokuPosition) -> List<BorderSide>,
    provideBackgroundColor: @Composable (position: SudokuPosition) -> Color,
    provideItemContent: @Composable SudokuItemContentScope.(position: SudokuPosition) -> Unit,
) {
    val maxItemsCount = positions.maxOf { it.x }

    fun calculateStartPoint(singleItemPxSize: Int, itemPosition: Int): Int {
        if (itemPosition == 1) {
            return 0
        }

        val borderPxSize = singleItemPxSize .times(boldLineWidthConstant)
        return ((singleItemPxSize - borderPxSize) * (itemPosition - 1)).roundToInt()
    }
    fun calculateItemFullSize(size: Dp, itemCount: Int): Dp = size.div (( 1 - boldLineWidthConstant) * itemCount + boldLineWidthConstant)


    BoxWithConstraints(
        modifier = Modifier
            .aspectRatio(1f)
//            .widthIn(
//                min = calculateTotalFieldSize(minimalItemSpace, maxItemsInRow),
//                max = calculateTotalFieldSize(maximalItemSpace, maxItemsInRow),
//            )
//            .heightIn(
//                min = calculateTotalFieldSize(minimalItemSpace, maxItemsInColumn),
//                max = calculateTotalFieldSize(maximalItemSpace, maxItemsInColumn),
//            )
    ) {
//        if  < min  -> do it scrollable
//        else < max -> do it non-scrollable with calculated size
//        else do it nonscrollable with max size
        val maxSize = min(this.maxHeight, this.maxWidth)
        val singleItemSize = calculateItemFullSize(maxSize, maxItemsCount)

        repeat(positions.size) { index ->
            val position = positions[index]
            key(position) {
                SudokuPositionComponent(
                    position = position,
                    size = singleItemSize,
                    provideBorderSides = provideBorderSide,
                    provideBackgroundColor = provideBackgroundColor,
                    modifier = Modifier.layout { measurable, constraints ->
                        val singleItemPxSize = singleItemSize.roundToPx()
                        val modifiedConstraints = constraints.copy(
                            minWidth = singleItemPxSize,
                            maxWidth = singleItemPxSize,
                            minHeight = singleItemPxSize,
                            maxHeight = singleItemPxSize,
                        )
                        val placeable = measurable.measure(
                            modifiedConstraints
                        )
                        layout(
                            width = singleItemPxSize,
                            height = singleItemPxSize,
                        ) {
                            placeable.place(
                                x = calculateStartPoint(singleItemPxSize, position.x),
                                y = calculateStartPoint(singleItemPxSize, position.y),
                            )
                        }

                    },
                    provideItemContent = remember(position) {
                        {
                            provideItemContent(position)
                        }
                    },
                )
            }
        }
    }
}


@Preview
@Composable
private fun EmptySudokuFieldPreview() {
    val positions = buildList {
        val minValue = 1
        val maxValue = 9
        for (x in minValue..maxValue) {
            for (y in minValue..maxValue) {
                add(
                    SudokuPosition(
                        x, y
                    )
                )
            }
        }
    }.toImmutableList()

    SudokuTheme {
        Box(
            modifier = Modifier
                .size(400.dp)
                .background(color = Color.Cyan)
                .padding(2.dp)
        ) {
            SudokuField(
                positions = positions,
                provideBorderSide = { position ->
                    buildList {
                        if ((position.x - 1) % 3 == 0) {
                            add(BorderSide.Left)
                        }

                        if (position.x % 3 == 0) {
                            add(BorderSide.Right)
                        }

                        if ((position.y - 1) % 3 == 0) {
                            add(BorderSide.Top)
                        }

                        if (position.y % 3 == 0) {
                            add(BorderSide.Bottom)
                        }
                    }.toImmutableList()
                },
                provideBackgroundColor = {
                    SudokuTheme.colors.tileBackground
                },
                provideItemContent = { _ ->
                    SudokuPositionComponentContent(
                    SudokuPositionValuePresentationModel(4, inputTileType = SudokuInputTileType.SolvedValue),
                    )
                }
            )
        }
    }

}