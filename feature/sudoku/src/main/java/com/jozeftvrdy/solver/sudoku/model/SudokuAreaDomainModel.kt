package com.jozeftvrdy.solver.sudoku.model

sealed class SudokuAreaPriority(private val internalPriorityValue: Int): Comparable<SudokuAreaPriority> {

    /**
     * Squares, Internal squares, other areas that must be prioritized
     */
    data object Highest: SudokuAreaPriority(8)

    /**
     * Columns, Rows
     */
    data object High: SudokuAreaPriority(6)

    /**
     * Diagonal areas,
     */
    data object Medium: SudokuAreaPriority(4)

    /**
     * other areas, custom areas etc.
     */
    data object Low: SudokuAreaPriority(2)

    override fun compareTo(other: SudokuAreaPriority): Int = this.internalPriorityValue.compareTo(other.internalPriorityValue)
}

data class SudokuAreaDomainModel(
    val positions: List<SudokuPosition>,
    val priority: SudokuAreaPriority,
    val tag: Any? = null,
)

data class SudokuAreasInputModel(
    val areas: List<SudokuAreaDomainModel>
) {
    operator fun plus(other: SudokuAreasInputModel): SudokuAreasInputModel = SudokuAreasInputModel(
            this.areas + other.areas
        )

}

fun createStandardAreas(
    sudokuFieldWidth: Int,
    sudokuFieldHeight: Int,
    areaWidth: Int,
    areaHeight: Int,
): SudokuAreasInputModel {

    require(sudokuFieldWidth % areaWidth == 0)
    require(sudokuFieldHeight % areaHeight == 0)

    val horizontalSquareCount = sudokuFieldWidth / areaWidth
    val verticalSquareCount = sudokuFieldHeight / areaHeight

    val squareList = List(horizontalSquareCount * verticalSquareCount) { index ->
        val squareX = index % horizontalSquareCount
        val squareY = index / horizontalSquareCount
        val startX = squareX * areaWidth
        val startY = squareY * areaHeight

        SudokuAreaDomainModel(
            buildList {
                for (yOffset in 1 .. areaHeight) {
                    for (xOffset in 1 .. areaWidth) {
                        this.add(SudokuPosition(startX + xOffset, startY + yOffset))
                    }
                }
            },
            priority = SudokuAreaPriority.Highest,
        )
    }

    val columnList = List(
        sudokuFieldWidth
    ) { columnIndex ->
        SudokuAreaDomainModel(
            List(sudokuFieldHeight) { rowIndex ->
                SudokuPosition(columnIndex + 1, rowIndex + 1)
            },
            priority = SudokuAreaPriority.High,
        )
    }

    val rowList = List(
        sudokuFieldHeight
    ) { rowIndex ->
        SudokuAreaDomainModel(
            List(sudokuFieldWidth) { columnIndex ->
                SudokuPosition(columnIndex + 1, rowIndex + 1)
            },
            priority = SudokuAreaPriority.High,
        )
    }

    return SudokuAreasInputModel(
        squareList + columnList + rowList
    )
}

fun createDiagonalAreas(
    sudokuSize: Int,
): SudokuAreasInputModel {
    val topLeftToRightBottom = SudokuAreaDomainModel(
        List(sudokuSize) { index ->
            SudokuPosition(index + 1, index + 1)
        },
        priority = SudokuAreaPriority.Medium,
    )

    val topRightToBottomLeft = SudokuAreaDomainModel(
        List(sudokuSize) { index ->
            SudokuPosition(sudokuSize - index,index + 1)
        },
        priority = SudokuAreaPriority.Medium,
    )

    return listOf(
        topLeftToRightBottom,
        topRightToBottomLeft,
    ).let {
        SudokuAreasInputModel(it)
    }
}
