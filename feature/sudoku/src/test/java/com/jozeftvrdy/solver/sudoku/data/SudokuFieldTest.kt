package com.jozeftvrdy.solver.sudoku.data

import com.jozeftvrdy.solver.sudoku.model.SudokuPosition
import com.jozeftvrdy.solver.sudoku.model.SudokuTileValueDataModel
import com.jozeftvrdy.solver.sudoku.model.createStandardAreas
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

// manually written
class SudokuFieldTest {

    @Test
    fun `Test that all items count are 81, and all items have unique position`() = runTest {
        val sudokuField = createSudokuField()
        assert(sudokuField.allValues.size == 81)
        var x = 1
        var y = 1
        repeat(81) {
            assertEquals(1, sudokuField.allValues.count { valueHolder ->
                valueHolder.position.x == x && valueHolder.position.y == y
            })

            if (x == 9) {
                x = 1
                y++
            } else {
                x++
            }
        }
    }

    @Test
    fun `Test that all items count are 192, and are ordered correctly`() = runTest {
        val sudokuField = SudokuField(
            valuesInputModel = emptyList(),
            areasInputModel = createStandardAreas(
                sudokuFieldWidth = 16,
                sudokuFieldHeight = 12,
                areaWidth = 4,
                areaHeight = 3,
            )
        )
        assertEquals(16*12, sudokuField.allValues.size)

        // 16 columns,
        // 12 rows,
        // 16 squares
        assertEquals(16+12+16, sudokuField.allAreas.size)
    }

    @Test
    fun `Test that setNumberToPosition sets number to position`() = runTest {
        val sudokuField = createSudokuField()
        // run test for setNumberToPosition with not fixed position
        run {
            val positon1 = SudokuPosition(1,1)
            val value1 = 1
            sudokuField.setNumberToPosition(
                value = value1,
                position = positon1,
                isFixed = false,
            )
            val tileValue1 = sudokuField[positon1].tileValue
            assertIs<SudokuTileValueDataModel.FlexibleTileValue.SolvedTileValue>(tileValue1)
            assert(tileValue1.value == value1)
        }

        // run test for setNumberToPosition with fixed position
        run {
            val positon2 = SudokuPosition(2,2)
            val value2 = 2
            sudokuField.setNumberToPosition(
                value = value2,
                position = positon2,
                isFixed = true,
            )
            val tileValue2 = sudokuField[positon2].tileValue
            assertIs<SudokuTileValueDataModel.FixedTileValue>(tileValue2)
            assert(tileValue2.value == value2)
        }
    }

    @Test
    fun `Test that setNumberToPosition invalidates options in row, column and square`() = runTest {
        val sudokuField = createSudokuField()
        // run test for setNumberToPosition with not fixed position
        run {
            val position1 = SudokuPosition(1,1)
            val value1 = 1
            sudokuField.setNumberToPosition(
                value = value1,
                position = position1,
                isFixed = false,
            )
            val areas = sudokuField.areasByPosition[position1]
            requireNotNull(areas)
            areas.flatMap { it.tiles }
                .forEach { valueHolder ->
                    (valueHolder.tileValue as? SudokuTileValueDataModel.FlexibleTileValue.UnsolvedTileValue)?.let {
                        assert(it.possibleValues.contains(value1).not())
                        assert(it.findReasonPositionForValue(value1) == position1)
                    }
                }
        }

        // run test for setNumberToPosition with fixed position
        run {
            val position2 = SudokuPosition(2,2)
            val value2 = 2
            sudokuField.setNumberToPosition(
                value = value2,
                position = position2,
                isFixed = true,
            )
            val areas = sudokuField.areasByPosition[position2]
            requireNotNull(areas)
            areas.flatMap { it.tiles }
                .forEach { valueHolder ->
                    (valueHolder.tileValue as? SudokuTileValueDataModel.FlexibleTileValue.UnsolvedTileValue)?.let {
                        assert(it.possibleValues.contains(value2).not())
                        assert(it.findReasonPositionForValue(value2) == position2)
                    }
                }
        }
    }

    private fun createSudokuField() = SudokuField(
        valuesInputModel = emptyList(),
        areasInputModel = createStandardAreas(
            sudokuFieldWidth = 9,
            sudokuFieldHeight = 9,
            areaWidth = 3,
            areaHeight = 3,
        )
    )
}