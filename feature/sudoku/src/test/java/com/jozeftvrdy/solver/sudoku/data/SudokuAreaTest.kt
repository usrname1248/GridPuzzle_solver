package com.jozeftvrdy.solver.sudoku.data

import com.jozeftvrdy.solver.sudoku.model.SudokuPosition
import com.jozeftvrdy.solver.sudoku.model.SudokuSolveType
import com.jozeftvrdy.solver.sudoku.model.SudokuTileValueDataModel.FlexibleTileValue.UnsolvedTileValue
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// AI generated
class SudokuAreaTest {

    @Test
    fun `findAllItemsWithSinglePossibility identifies hidden singles correctly`() {
        // Arrange
        val pos1 = SudokuPosition(1, 1)
        val pos2 = SudokuPosition(1, 2)
        val pos3 = SudokuPosition(1, 3)

        // Tile 1 can be 5 or 6
        val tile1 = mockTile(pos1, setOf(5, 6))
        // Tile 2 can be 6 or 7 or 9
        val tile2 = mockTile(pos2, setOf(6, 7, 9))
        // Tile 3 can be 8 or 9
        val tile3 = mockTile(pos3, setOf(8, 9))

        // In this set:
        // 5 is unique (only in Tile 1)
        // 7 is unique (only in Tile 2)
        // 8 is unique (only in Tile 3)
        // 6 and 9 is NOT unique ( 6 in Tile 1 and Tile 2, 9 in Tile 2 and Tile 3)

        val row = SudokuRow(listOf(tile1, tile2, tile3))

        // Act
        val results = row.findAllItemsWithSinglePossibility()

        // Assert
        assertEquals(3, results.size)

        val solutionFor5 = results.find { it.value == 5 }
        assertEquals(pos1, solutionFor5?.position)
        assertEquals(SudokuSolveType.TheOnlyOptionInRow, solutionFor5?.solveType)

        val solutionFor7 = results.find { it.value == 7 }
        assertEquals(pos2, solutionFor7?.position)

        val solutionFor8 = results.find { it.value == 8 }
        assertEquals(pos3, solutionFor8?.position)

        // Verify '6' is not present because it appeared in two tiles
        val solutionFor6 = results.find { it.value == 6 }
        assertEquals(null, solutionFor6)

        // Verify '9' is not present because it appeared in two tiles
        val solutionFor9 = results.find { it.value == 9 }
        assertEquals(null, solutionFor9)
    }

    private fun mockTile(position: SudokuPosition, possibilities: Set<Int>): SudokuTileValueHolder {
        val holder = mockk<SudokuTileValueHolder>()
        val unsolvedValue = mockk<UnsolvedTileValue>()

        every { holder.position } returns position
        every { holder.tileValue } returns unsolvedValue
        every { unsolvedValue.possibleValues } returns possibilities

        return holder
    }

    @Test
    fun `findDuplicates returns null when all values are unique`() {
        val tiles = listOf(
            mockTileWithValue(1),
            mockTileWithValue(2),
            mockTileWithValue(3)
        )
        val area = SudokuRow(tiles)

        val result = area.findDuplicates()

        assertNull(result)
    }

    @Test
    fun `findDuplicates returns the first two matching tiles when a duplicate exists`() {
        val tile1 = mockTileWithValue(5)
        val tile2 = mockTileWithValue(9)
        val tile3 = mockTileWithValue(5)
        val area = SudokuRow(listOf(tile1, tile2, tile3))

        val result = area.findDuplicates()

        assertEquals(tile1, result?.first)
        assertEquals(tile3, result?.second)
    }

    @Test
    fun `findDuplicates ignores null values even if multiple are present`() {
        val tiles = listOf(
            mockTileWithValue(null),
            mockTileWithValue(null),
            mockTileWithValue(7)
        )
        val area = SudokuRow(tiles)

        val result = area.findDuplicates()

        assertNull(result)
    }

    @Test
    fun `findDuplicates returns null for an empty list`() {
        val area = SudokuRow(emptyList())
        val result = area.findDuplicates()
        assertNull(result)
    }

    private fun mockTileWithValue(value: Int?): SudokuTileValueHolder {
        val holder = mockk<SudokuTileValueHolder>()
        // Mocks the chain it.tileValue.valueOrNull()
        every { holder.tileValue.valueOrNull() } returns value
        return holder
    }
}