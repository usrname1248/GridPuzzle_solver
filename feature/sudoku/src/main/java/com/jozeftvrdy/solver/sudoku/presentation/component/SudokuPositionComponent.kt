package com.jozeftvrdy.solver.sudoku.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
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
    provideContentData: (position: SudokuPosition) -> SudokuPositionValuePresentationModel?,
    provideBorderSides: (position: SudokuPosition) -> ImmutableList<BorderSide>,
    modifier: Modifier = Modifier,
    provideBackgroundColor: @Composable (position: SudokuPosition) -> Color = {
        SudokuTheme.colors.tileBackground
    },
) {
    Box(
        modifier = modifier.size(size),
    ) {
        SudokuPositionComponentBackground(
            size = size,
            borderSides = provideBorderSides(position),
            provideBackgroundColor = remember(position) {
                {
                    provideBackgroundColor(position)
                }
            }
        ) {
            provideContentData(position)?.let { valueData ->
                SudokuPositionComponentContent(
                    valueData = valueData,
                    size = size,
                )
            }
        }

    }
}

private fun ImmutableList<BorderSide>.shouldDrawBoldLine(onSide: BorderSide) : Boolean =
    this.contains(onSide)

@Composable
internal fun SudokuPositionComponentBackground(
    size: Dp,
    borderSides: ImmutableList<BorderSide>,
    modifier: Modifier = Modifier,
    provideBackgroundColor: @Composable () -> Color = {
        SudokuTheme.colors.tileBackground
    },
    content: @Composable BoxScope.() -> Unit = {}
) {
    val normalLineWidth = size.getNormalLineWidth()
    val boldLineWidth = size.getBoldLineWidth()

    val borderColor = LocalSudokuColors.current.sudokuBorder
    val backgroundColor = provideBackgroundColor()

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
            .background(color = borderColor)
            .padding(postBorderPadding)
            .background(
                color = backgroundColor
            ),
        content = content
    )
}

@Composable
internal fun BoxScope.SudokuPositionComponentContent(
    valueData: SudokuPositionValuePresentationModel,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    Text(
        valueData.value.toString(10),
        modifier = modifier.align(alignment = Alignment.Center),
        fontSize = with(density) {
            size.toSp() * 0.5f
        },
        fontWeight = when (valueData.inputTileType) {
            SudokuInputTileType.FixedValue -> FontWeight.Bold
            SudokuInputTileType.SolvedValue -> FontWeight.Normal
        },
        textAlign = TextAlign.Center,
        color = if (valueData.isErrorValue) SudokuTheme.colors.textValueErrorColor else SudokuTheme.colors.textValueColor,
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
                provideContentData = { null },
                provideBorderSides = { persistentListOf() },
            )

            SudokuPositionComponent(
                position = SudokuPosition(1, 2),
                provideContentData = { null },
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
                provideContentData = {
                    SudokuPositionValuePresentationModel(
                        value = 4,
                        inputTileType = SudokuInputTileType.FixedValue,
                    )
                },
                size = 40.dp,
            )

            SudokuPositionComponent(
                position = SudokuPosition(1, 2),
                provideContentData = {
                    SudokuPositionValuePresentationModel(
                        value = 4,
                        inputTileType = SudokuInputTileType.FixedValue,
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
                provideContentData = {
                    SudokuPositionValuePresentationModel(
                        value = 4,
                        inputTileType = SudokuInputTileType.SolvedValue,
                    )
                },
                provideBorderSides = { persistentListOf() },
                size = 40.dp,
            )

            SudokuPositionComponent(
                position = SudokuPosition(1, 2),
                provideContentData = {
                    SudokuPositionValuePresentationModel(
                        value = 4,
                        inputTileType = SudokuInputTileType.FixedValue,
                    )
                },
                provideBorderSides = { BorderSide.entries.toImmutableList() },
                size = 40.dp,
            )
        }
    }
}