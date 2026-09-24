package com.jozeftvrdy.solver.sudoku.presentation.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.jozeftvrdy.solver.sudoku.model.SudokuInputTileType
import com.jozeftvrdy.solver.sudoku.model.SudokuPosition
import com.jozeftvrdy.solver.sudoku.presentation.theme.LocalSudokuColors
import com.jozeftvrdy.solver.sudoku.presentation.theme.SudokuTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun SudokuPositionComponent(
    position: SudokuPosition,
    size: Dp,
    provideBorderSides: (position: SudokuPosition) -> List<BorderSide>,
    modifier: Modifier = Modifier,
    provideBackgroundColor: @Composable (position: SudokuPosition) -> Color = {
        SudokuTheme.colors.tileBackground
    },
    provideItemContent: @Composable SudokuItemContentScope.(position: SudokuPosition) -> Unit,
) {
    Box(
        modifier = modifier.size(size),
    ) {
        val borderSides = provideBorderSides(position)
        val savedBorderSides = remember(borderSides) {
            borderSides.toImmutableList()
        }

        SudokuPositionComponentBackground(
            size = size,
            borderSides = savedBorderSides,
            provideBackgroundColor = remember(position) {
                {
                    provideBackgroundColor(position)
                }
            }
        ) {
            provideItemContent(position)
        }
    }
}

private fun ImmutableList<BorderSide>.shouldDrawBoldLine(onSide: BorderSide) : Boolean =
    this.contains(onSide)

interface SudokuItemContentScope {
    val size: Dp
}

class SudokuItemContentScopeImpl (
    override val size: Dp
): SudokuItemContentScope

@Composable
internal fun SudokuPositionComponentBackground(
    size: Dp,
    borderSides: ImmutableList<BorderSide>,
    modifier: Modifier = Modifier,
    provideBackgroundColor: @Composable () -> Color = {
        SudokuTheme.colors.tileBackground
    },
    content: @Composable SudokuItemContentScope.() -> Unit = {}
) {
    val normalLineWidth = size.getNormalLineWidth()
    val boldLineWidth = size.getBoldLineWidth()

    val borderColor = LocalSudokuColors.current.sudokuBorder
    val backgroundColor = animateColorAsState(provideBackgroundColor())

    val prePostBorderValue = (boldLineWidth - normalLineWidth).div(2)
    val preBorderPadding = PaddingValues(
        start = if (borderSides.shouldDrawBoldLine(BorderSide.Left)) 0.dp else prePostBorderValue,
        top = if (borderSides.shouldDrawBoldLine(BorderSide.Top)) 0.dp else prePostBorderValue,
        end = if (borderSides.shouldDrawBoldLine(BorderSide.Right)) 0.dp else prePostBorderValue,
        bottom = if (borderSides.shouldDrawBoldLine(BorderSide.Bottom)) 0.dp else prePostBorderValue,
    )
    val postBorderPadding = PaddingValues(
        start = if (borderSides.shouldDrawBoldLine(BorderSide.Left)) boldLineWidth else normalLineWidth,
        top = if (borderSides.shouldDrawBoldLine(BorderSide.Top)) boldLineWidth else normalLineWidth,
        end = if (borderSides.shouldDrawBoldLine(BorderSide.Right)) boldLineWidth else normalLineWidth,
        bottom = if (borderSides.shouldDrawBoldLine(BorderSide.Bottom)) boldLineWidth else normalLineWidth,
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(preBorderPadding)
            .drawBehind {
                val leftWidth = postBorderPadding.calculateLeftPadding(LayoutDirection.Ltr).toPx()
                val topHeight = postBorderPadding.calculateTopPadding().toPx()
                val rightWidth = postBorderPadding.calculateRightPadding(LayoutDirection.Ltr).toPx()
                val bottomHeight = postBorderPadding.calculateBottomPadding().toPx()

                // Top border (full width)
                drawRect(
                    color = borderColor,
                    topLeft = Offset(0f, 0f),
                    size = Size(this.size.width, topHeight)
                )
                // Bottom border (full width)
                drawRect(
                    color = borderColor,
                    topLeft = Offset(0f, this.size.height - bottomHeight),
                    size = Size(this.size.width, bottomHeight)
                )
                // Left border (between top and bottom)
                drawRect(
                    color = borderColor,
                    topLeft = Offset(0f, topHeight),
                    size = Size(leftWidth, this.size.height - topHeight - bottomHeight)
                )
                // Right border (between top and bottom)
                drawRect(
                    color = borderColor,
                    topLeft = Offset(this.size.width - rightWidth, topHeight),
                    size = Size(rightWidth, this.size.height - topHeight - bottomHeight)
                )
            }
            .padding(postBorderPadding)
            .drawBehind{
                drawRect(
                    color = backgroundColor.value
                )
            },
        contentAlignment = Alignment.Center
    ) {
        content(
            SudokuItemContentScopeImpl(size)
        )
    }
}

@Composable
fun SudokuItemContentScope.SudokuPositionComponentContent(
    valueData: SudokuPositionValuePresentationModel,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    Text(
        valueData.value.toString(10),
        modifier = modifier,
        fontSize = with(density) {
            this@SudokuPositionComponentContent.size.toSp() * 0.5f
        },
        fontWeight = when (valueData.inputTileType) {
            SudokuInputTileType.FixedValue -> FontWeight.Bold
            SudokuInputTileType.SolvedValue -> FontWeight.Normal
        },
        textAlign = TextAlign.Center,
        color = SudokuTheme.colors.textValueColor,
    )
}

@Preview
@Composable
fun SudokuPositionEmptyComponentPreview() {
    SudokuTheme {
        Row(modifier = Modifier
            .background(color = Color.Cyan)
            .padding(5.dp)
        ) {
            SudokuPositionComponent(
                size = 40.dp,
                position = SudokuPosition(1, 1),
                provideItemContent = { },
                provideBorderSides = { persistentListOf() },
            )

            SudokuPositionComponent(
                position = SudokuPosition(1, 2),
                provideItemContent = { },
                provideBorderSides = { BorderSide.entries.toImmutableList() },
                size = 40.dp,
            )
        }
    }
}



@Preview
@Composable
fun SudokuPositionComponentPreviewWithFixedValue() {
    SudokuTheme {
        Row(modifier = Modifier
            .background(color = Color.Cyan)
            .padding(5.dp)
        ) {
            SudokuPositionComponent(
                position = SudokuPosition(1, 1),
                provideBorderSides = { persistentListOf() },
                provideItemContent = {
                    SudokuPositionComponentContent(
                        SudokuPositionValuePresentationModel(
                            value = 4,
                            inputTileType = SudokuInputTileType.FixedValue,
                        )
                    )
                },
                size = 40.dp,
            )

            SudokuPositionComponent(
                position = SudokuPosition(1, 2),
                provideItemContent = {
                    SudokuPositionComponentContent(
                        SudokuPositionValuePresentationModel(
                            value = 4,
                            inputTileType = SudokuInputTileType.FixedValue,
                        )
                    )
                },
                provideBorderSides = { BorderSide.entries.toImmutableList() },
                size = 40.dp,
            )
        }
    }
}

@Preview
@Composable
fun SudokuPositionComponentPreviewWithSolvedValue() {
    SudokuTheme {
        Row(modifier = Modifier
            .background(color = Color.Cyan)
            .padding(5.dp)
        ) {
            SudokuPositionComponent(
                position = SudokuPosition(1, 1),
                provideItemContent = {
                    SudokuPositionComponentContent(
                        SudokuPositionValuePresentationModel(
                            value = 4,
                            inputTileType = SudokuInputTileType.SolvedValue,
                        )
                    )
                },
                provideBorderSides = { persistentListOf() },
                size = 40.dp,
            )

            SudokuPositionComponent(
                position = SudokuPosition(1, 2),
                provideItemContent = {
                    SudokuPositionComponentContent(
                        SudokuPositionValuePresentationModel(
                            value = 4,
                            inputTileType = SudokuInputTileType.FixedValue,
                        )
                    )
                },
                provideBorderSides = { BorderSide.entries.toImmutableList() },
                size = 40.dp,
            )
        }
    }
}