package com.jozeftvrdy.solver.sudoku.data

import com.jozeftvrdy.solver.sudoku.model.SudokuPosition
import com.jozeftvrdy.solver.sudoku.model.SudokuTileValueDataModel
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

// manually written
class SudokuFieldTest {

    @Test
    fun `Test that all items count are 81, and are ordered correctly`() = runTest {
        val sudokuField = createSudokuField()
        assert(sudokuField.allValues.size == 81)
        var x = 1
        var y = 1
        sudokuField.allValues.forEach { valueHolder ->
            assert(valueHolder.position.x == x)
            assert(valueHolder.position.y == y)

            if (x == 9) {
                x = 1
                y++
            } else {
                x++
            }
        }
    }

    @Test
    fun `Test that row items count are 81, and are ordered correctly`() = runTest {
        val sudokuField = createSudokuField()
        assert(sudokuField.rows.size == 9)
        assert(sudokuField.rows.all { it.items.size == 9 })

        sudokuField.rows.forEachIndexed { indexY, row ->
            row.items.forEachIndexed { indexX, item ->
                assert(item.position.x == indexX + 1)
                assert(item.position.y == indexY + 1)
            }
        }
    }

    @Test
    fun `Test that columns items count are 81, and are ordered correctly`() = runTest {
        val sudokuField = createSudokuField()
        assert(sudokuField.columns.size == 9)
        assert(sudokuField.columns.all { it.items.size == 9 })

        sudokuField.columns.forEachIndexed { indexX, row ->
            row.items.forEachIndexed { indexY, item ->
                assert(item.position.x == indexX + 1)
                assert(item.position.y == indexY + 1)
            }
        }
    }

    @Test
    fun `Test that square items count are 81, and are ordered correctly`() = runTest {
        val sudokuField = createSudokuField()
        assert(sudokuField.squares.size == 9)
        assert(sudokuField.squares.all { it.items.size == 9 })

        var squareIndex = 0
        for (square3Y in 0..2) {
            for (square3X in 0..2) {
                var itemIndex = 0
                for (squareY in 1..3) {
                    for (squareX in 1..3) {
                        val position = sudokuField.squares[squareIndex][itemIndex].position
                        assert(position.x == square3X * 3 + squareX)
                        assert(position.y == square3Y * 3 + squareY)

                        itemIndex++
                    }
                }
                squareIndex++
            }
        }
    }

    @Test
    fun `Test that all items count are 192, and are ordered correctly`() = runTest {
        val sudokuField = SudokuField(
            sudokuFieldWidth = 16,
            sudokuFieldHeight = 12,
            areaWidth = 4,
            areaHeight = 3,
            emptyList()
        )
        assertEquals(16*12, sudokuField.allValues.size)
        assertEquals(12, sudokuField.rows.size)
        assertEquals(16, sudokuField.columns.size)
        assertEquals(16, sudokuField.squares.size)
        assert(sudokuField.rows.all {
            it.items.size == 16
        })
        assert(sudokuField.columns.all {
            it.items.size == 12
        })
        assert(sudokuField.squares.all {
            it.items.size == 12
        })
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
    fun `Test that columnAt return correct collections`() = runTest {
        val sudokuField = createSudokuField()

        SudokuPosition(1,1).let { position ->
            sudokuField.columnAt(position) == sudokuField.columns.findCollectionWithPosition(position)
        }

        SudokuPosition(5,7).let { position ->
            sudokuField.columnAt(position) == sudokuField.columns.findCollectionWithPosition(position)
        }

        SudokuPosition(8,3).let { position ->
            sudokuField.columnAt(position) == sudokuField.columns.findCollectionWithPosition(position)
        }
    }

    @Test
    fun `Test that rowAt return correct collections`() = runTest {
        val sudokuField = createSudokuField()

        SudokuPosition(1,1).let { position ->
            sudokuField.rowAt(position) == sudokuField.rows.findCollectionWithPosition(position)
        }

        SudokuPosition(5,7).let { position ->
            sudokuField.rowAt(position) == sudokuField.rows.findCollectionWithPosition(position)
        }

        SudokuPosition(8,3).let { position ->
            sudokuField.rowAt(position) == sudokuField.rows.findCollectionWithPosition(position)
        }
    }

    @Test
    fun `Test that squareAt return correct collections`() = runTest {
        val sudokuField = createSudokuField()

        SudokuPosition(1,1).let { position ->
            sudokuField.squareAt(position) == sudokuField.squares.findCollectionWithPosition(position)
        }

        SudokuPosition(5,7).let { position ->
            sudokuField.squareAt(position) == sudokuField.squares.findCollectionWithPosition(position)
        }

        SudokuPosition(8,3).let { position ->
            sudokuField.squareAt(position) == sudokuField.squares.findCollectionWithPosition(position)
        }
    }

    private fun <T: SudokuArea> List<T>.findCollectionWithPosition(position: SudokuPosition): T {
        this.forEach { area ->
            if (area.items.any {
                it.position == position
            }) {
                return area
            }
        }
        throw IllegalStateException()
    }

    @Test
    fun `Test that setNumberToPosition invalidates options in row, column and square`() = runTest {
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
            (
                sudokuField.rowAt(positon1).items +
                sudokuField.columnAt(positon1).items +
                sudokuField.squareAt(positon1).items
            )
                .forEach { valueHolder ->
                    (valueHolder.tileValue as? SudokuTileValueDataModel.FlexibleTileValue.UnsolvedTileValue)?.let {
                        assert(it.possibleValues.contains(value1).not())
                        assert(it.findReasonPositionForValue(value1) == positon1)
                    }
                }
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
            (
                sudokuField.rowAt(positon2).items +
                sudokuField.columnAt(positon2).items +
                sudokuField.squareAt(positon2).items
            )
                .forEach { valueHolder ->
                    (valueHolder.tileValue as? SudokuTileValueDataModel.FlexibleTileValue.UnsolvedTileValue)?.let {
                        assert(it.possibleValues.contains(value2).not())
                        assert(it.findReasonPositionForValue(value2) == positon2)
                    }
                }
        }
    }

    private fun createSudokuField() = SudokuField(
        sudokuFieldWidth = 9,
        sudokuFieldHeight = 9,
        areaWidth = 3,
        areaHeight = 3,
        emptyList()
    )


}