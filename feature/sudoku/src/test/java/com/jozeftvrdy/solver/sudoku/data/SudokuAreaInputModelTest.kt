package com.jozeftvrdy.solver.sudoku.data

import com.jozeftvrdy.solver.sudoku.model.SudokuAreaPriority
import com.jozeftvrdy.solver.sudoku.model.SudokuPosition
import com.jozeftvrdy.solver.sudoku.model.createDiagonalAreas
import com.jozeftvrdy.solver.sudoku.model.createStandardAreas
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SudokuAreaInputModelTest {
    @Test
    fun `Test that createStandardAreas returns correct positions`() {

        val fieldSize = 12
        val areaWidth = 4
        val areaHeight = 3
        val model = createStandardAreas(
            sudokuFieldWidth = fieldSize,
            sudokuFieldHeight = fieldSize,
            areaWidth = areaWidth,
            areaHeight = areaHeight,
        )
        val areas = model.areas

        assertEquals(fieldSize.times(3), areas.size)


        val (squareList, lineList) = areas.partition {
            it.priority == SudokuAreaPriority.Highest
        }

        squareList.forEach { area ->
            val topLeftAreaPosition = area.positions.getTopLeftPosition()
            assertEquals(topLeftAreaPosition, area.positions.first())
            // we must have unique positions,
            assertEquals(area.positions.size, area.positions.toSet().size)

            // and all positions must be inside this area, therefore
            area.positions.forEach { position ->
                // x must be at least x of topLeft item
                assert(position.x >= topLeftAreaPosition.x)
                // x must be less than x of next item
                assert(position.x < topLeftAreaPosition.x + areaWidth)

                // y must be at least y of topLeft item
                assert(position.y >= topLeftAreaPosition.y)
                // y must be less than y of next item
                assert(position.y < topLeftAreaPosition.y + areaHeight)
            }
        }

        val (columnList, rowList) = lineList.partition { model ->
            val firstPosition = model.positions.first()
            model.positions.all {
                it.x == firstPosition.x
            }
        }

        assert(
            rowList.all { row ->
                val firstPosition = row.positions.first()
                row.positions.all {
                    it.y == firstPosition.y
                }
            }
        )

        columnList.forEach { column ->
            assertEquals(column.positions.map { it.y }.sorted(), (1..fieldSize).toList())
        }


        rowList.forEach { row ->
            assertEquals(row.positions.map { it.x }.sorted(), (1..fieldSize).toList())
        }

        assertEquals(fieldSize, squareList.size)
        assertEquals(fieldSize, columnList.size)
        assertEquals(fieldSize, rowList.size)
    }

    private fun List<SudokuPosition>.getTopLeftPosition(): SudokuPosition {
        var minValue = this.first()
        for (index in 1..this.lastIndex) {
            if (this[index].x + this[index].y < minValue.x + minValue.y) {
                minValue = this[index]
            }
        }

        return minValue
    }

    @Test
    fun `Test that createDiagonalAreas returns correct positions`() {
        for (size in 2..25) {
            val diagonalAreasModel = createDiagonalAreas(size)
            val diagonalAreas = diagonalAreasModel.areas
            assertEquals(2, diagonalAreas.size)

            val topLeftToBottomRight = diagonalAreas.find {
                it.positions.contains(SudokuPosition(x = 1, y = 1))
            }
            assertNotNull(topLeftToBottomRight)
            val otherDiagonalArea = diagonalAreas.first {
                it != topLeftToBottomRight
            }
            topLeftToBottomRight.positions.run {
                forEach { position ->
                    assert(position.x == position.y)
                }
                assertEquals(
                    size,
                    map {
                        it.x
                    }
                        .toSet()
                        .size,
                )
            }

            otherDiagonalArea.positions.run {
                forEach { position ->
                    assertEquals(size + 1, position.x + position.y)
                }
            }
        }
    }
}