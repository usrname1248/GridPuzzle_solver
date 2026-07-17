package com.jozeftvrdy.solver.sudoku.data

import com.jozeftvrdy.solver.sudoku.model.SudokuAreaPriority
import com.jozeftvrdy.solver.sudoku.model.SudokuPosition
import com.jozeftvrdy.solver.sudoku.model.SudokuTileValueDataModel.FlexibleTileValue.UnsolvedTileValue
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SudokuAreaTest {

    @Test
    fun `findAllItemsWithSinglePossibility identifies hidden singles and wraps them in solveType`() {
        val pos1 = SudokuPosition(1, 1)
        val pos2 = SudokuPosition(1, 2)
        val pos3 = SudokuPosition(1, 3)

        val tile1 = mockTile(pos1, setOf(5, 6))
        val tile2 = mockTile(pos2, setOf(6, 7))
        val tile3 = mockTile(pos3, setOf(8, 9))

        val area = SudokuArea(
            tiles = listOf(tile1, tile2, tile3),
            priority = SudokuAreaPriority.High
        )

        val results = area.findAllItemsWithSinglePossibility()

        // 5, 7, 8, 9 are all unique in this set of tiles
        assertEquals(4, results.size)

        val solutionFor5 = results.find { it.value == 5 }
        assertEquals(pos1, solutionFor5?.position)

        // Verify the SolveType points back to the correct area instance
        val solveType = solutionFor5?.solveType as? SudokuSolveType.TheOnlyOptionInArea
        assertEquals(area, solveType?.area)
    }

    @Test
    fun `findSingleUnsolvedTileSolution returns solution when exactly one tile is unsolved`() {
        val pos = SudokuPosition(5, 5)
        val unsolvedTile = mockTile(pos, setOf(9))
        val solvedTile = mockk<SudokuTile>()
        every { solvedTile.tileValue } returns mockk<com.jozeftvrdy.solver.sudoku.model.SudokuTileValueDataModel.FixedTileValue>()

        val area = SudokuArea(
            tiles = listOf(unsolvedTile, solvedTile),
            priority = SudokuAreaPriority.High
        )

        val result = area.findSingleUnsolvedTileSolution()

        assertEquals(9, result?.value)
        assertEquals(pos, result?.position)
        assertTrue(result?.solveType is SudokuSolveType.TheOnlyUnsolvedTileInArea)
    }

    @Test
    fun `findDuplicates identifies value collisions`() {
        val tile1 = mockTileWithValue(5)
        val tile2 = mockTileWithValue(9)
        val tile3 = mockTileWithValue(5)

        val area = SudokuArea(
            tiles = listOf(tile1, tile2, tile3),
            priority = SudokuAreaPriority.High
        )

        val result = area.findDuplicates()

        assertEquals(tile1, result?.first)
        assertEquals(tile3, result?.second)
    }

    @Test
    fun `findDuplicates returns null when multiple tiles are unsolved`() {
        val tile1 = mockTileWithValue(null)
        val tile2 = mockTileWithValue(null)
        val tile3 = mockTileWithValue(7)

        val area = SudokuArea(
            tiles = listOf(tile1, tile2, tile3),
            priority = SudokuAreaPriority.High
        )

        assertNull(area.findDuplicates())
    }

    private fun mockTile(position: SudokuPosition, possibilities: Set<Int>): SudokuTile {
        val tile = mockk<SudokuTile>()
        val unsolvedValue = mockk<UnsolvedTileValue>()

        every { tile.position } returns position
        every { tile.tileValue } returns unsolvedValue
        every { unsolvedValue.possibleValues } returns possibilities

        return tile
    }

    private fun mockTileWithValue(value: Int?): SudokuTile {
        val tile = mockk<SudokuTile>()
        // Mocks the it.tileValue.valueOrNull() call
        every { tile.tileValue.valueOrNull() } returns value
        return tile
    }
}