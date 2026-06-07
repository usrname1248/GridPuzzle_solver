package com.jozeftvrdy.solver.sudoku.data

import com.jozeftvrdy.solver.sudoku.model.FinalSudokuResult
import com.jozeftvrdy.solver.sudoku.model.PartiallySolvedSudokuResult
import com.jozeftvrdy.solver.sudoku.model.SudokuFieldInputModel
import com.jozeftvrdy.solver.sudoku.model.SudokuInputTileType
import com.jozeftvrdy.solver.sudoku.model.SudokuPosition
import com.jozeftvrdy.solver.sudoku.model.SudokuSolveType
import com.jozeftvrdy.solver.sudoku.model.SudokuTileValueFullSolvedModel
import com.jozeftvrdy.solver.sudoku.model.SudokuTileValueInputModel
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

// manually written
class SudokuRepositoryImplTest {
    val repo = SudokuRepositoryImpl()

    val completedSudoku = listOf(
        8, 2, 7, 1, 5, 4, 3, 9, 6,
        9, 6, 5, 3, 2, 7, 1, 4, 8,
        3, 4, 1, 6, 8, 9, 7, 5, 2,
        5, 9, 3, 4, 6, 8, 2, 7, 1,
        4, 7, 2, 5, 1, 3, 6, 8, 9,
        6, 1, 8, 9, 7, 2, 4, 3, 5,
        7, 8, 6, 2, 3, 5, 9, 1, 4,
        1, 5, 4, 7, 9, 6, 8, 2, 3,
        2, 3, 9, 8, 4, 1, 5, 6, 7
    )

    val sudokuWithoutNumberEight = listOf(
        0, 2, 7, 1, 5, 4, 3, 9, 6,
        9, 6, 5, 3, 2, 7, 1, 4, 0,
        3, 4, 1, 6, 0, 9, 7, 5, 2,
        5, 9, 3, 4, 6, 0, 2, 7, 1,
        4, 7, 2, 5, 1, 3, 6, 0, 9,
        6, 1, 0, 9, 7, 2, 4, 3, 5,
        7, 0, 6, 2, 3, 5, 9, 1, 4,
        1, 5, 4, 7, 9, 6, 0, 2, 3,
        2, 3, 9, 0, 4, 1, 5, 6, 7
    )

    val veryEasySudoku = listOf (
        0, 4, 1,    3, 8, 0,    2, 9, 6,
        0, 2, 0,    7, 6, 1,    4, 3, 8,
        3, 8, 6,    0, 2, 9,    0, 0, 1,

        0, 0, 2,    5, 9, 3,    1, 0, 4,
        4, 0, 0,    8, 1, 6,    5, 2, 9,
        1, 9, 5,    0, 0, 7,    8, 0, 3,

        0, 1, 0,    6, 7, 2,    3, 8, 0,
        2, 5, 8,    9, 0, 0,    0, 1, 7,
        6, 7, 0,    1, 5, 8,    9, 4, 0,
    )
    val masterDifficultySudoku = listOf(
        0, 9, 2,    0, 0, 0,    6, 0, 7,
        0, 0, 0,    0, 7, 0,    0, 2, 8,
        7, 0, 6,    0, 2, 4,    0, 0, 0,

        0, 4, 7,    0, 0, 8,    0, 0, 5,
        0, 0, 9,    0, 5, 0,    0, 0, 0,
        3, 5, 0,    0, 1, 0,    2, 7, 0,

        8, 1, 5,    0, 0, 0,    7, 6, 4,
        0, 0, 0,    1, 0, 5,    8, 0, 0,
        0, 0, 0,    0, 0, 0,    5, 0, 0,
    )

    val sudoku6Size = listOf(
        6, 3, 0,    1, 4, 0,
        0, 0, 0,    0, 2, 0,

        0, 2, 0,    3, 6, 4,
        3, 4, 6,    5, 0, 0,

        0, 0, 0,    0, 0, 1,
        4, 5, 1,    2, 3, 0,
    )

    @Test
    fun `Test that when solve input has duplicates, specific failure result is returned`() = runTest {

        suspend fun testDuplicatesOnPosition(
            position1: SudokuPosition,
            position2: SudokuPosition
        ) {
            repo.solve(
                sudokuWithoutNumberEight.toSudokuTileValueInputModel().map {
                    if (it.position == position1 || it.position == position2) {
                        it.copy(value = 8)
                    } else {
                        it
                    }
                },
                SudokuFieldInputModel.classicSudoku9,
            ).last().also {
                assertIs<FinalSudokuResult.Failure.InputWithDuplicate>(it)
                assert(it.value1 == position1 || it.value2 == position1)
                assert(it.value1 == position2 || it.value2 == position2)
                assert(it.value1 != it.value2)
            }
        }

        // duplicates in same row, // y is constant
        testDuplicatesOnPosition(SudokuPosition(6, 5), SudokuPosition(2, 5))

        // duplicates in same column, // x is constant
        testDuplicatesOnPosition(SudokuPosition(4, 1), SudokuPosition(4, 7))

        // duplicates in same square,
        testDuplicatesOnPosition(SudokuPosition(7, 4), SudokuPosition(9, 6))
    }

    @Test
    fun `Test that solves function solves valid sudoku`() = runTest {
        val modifiedSudokuInput = completedSudoku.map {
            if (it == 8) {
                0
            } else it
        }.toSudokuTileValueInputModel()

        val result = repo.solve(modifiedSudokuInput, SudokuFieldInputModel.classicSudoku9).last()
        assertIs<FinalSudokuResult.Success>(result)
        assertEquals(completedSudoku, result.successValues.map {
            it.value
        })
    }

    @Test
    fun `Test that solves function solves valid expert sudoku`() = runTest {
        val modifiedSudokuInput = masterDifficultySudoku.toSudokuTileValueInputModel()

        val result = repo.solve(modifiedSudokuInput, SudokuFieldInputModel.classicSudoku9).last()
        assertIs<FinalSudokuResult.Success>(result)

        // checkUniqueValues in row
        val rows = result.successValues.map {
            it.value
        }.chunked(9)
        assert(rows.all {
            it.toSet().size == 9
        })

        // checkUniqueValues in column
        val columns = result.successValues
            .mapIndexed { index, successValue -> (index % 9) to successValue.value  }
            .groupBy { (sudokuColumnIndex, _) -> sudokuColumnIndex }
            .values.map { value ->
                value.map {
                    it.second
                }
            }

        assert(
            columns.all {
                it.toSet().size == 9
            }
        )

        // checkUniqueValues in squares
        val squareIndexes = listOf(
            1, 1, 1,     2, 2, 2,    3, 3, 3,
            1, 1, 1,     2, 2, 2,    3, 3, 3,
            1, 1, 1,     2, 2, 2,    3, 3, 3,

            4, 4, 4,     5, 5, 5,    6, 6, 6,
            4, 4, 4,     5, 5, 5,    6, 6, 6,
            4, 4, 4,     5, 5, 5,    6, 6, 6,

            7, 7, 7,     8, 8, 8,    9, 9, 9,
            7, 7, 7,     8, 8, 8,    9, 9, 9,
            7, 7, 7,     8, 8, 8,    9, 9, 9,
        )
        val squares = result.successValues
            .mapIndexed { index, successValue -> squareIndexes[index] to successValue.value  }
            .groupBy { (sudokuSquareIndex, _) -> sudokuSquareIndex }
            .values.map { value ->
                value.map {
                    it.second
                }
            }

        assert(
            squares.all {
                it.toSet().size == 9
            }
        )


        // when we have all conditions matched, we have valid sudoku
    }

    @Test
    fun `Test that all final result contains all input data`() = runTest {
        val modifiedSudokuInput = masterDifficultySudoku.toSudokuTileValueInputModel()

        val result = repo.solve(modifiedSudokuInput, SudokuFieldInputModel.classicSudoku9).last()
        assertIs<FinalSudokuResult.Success>(result)

        // check that result does not changed fixed data
        masterDifficultySudoku.forEachIndexed { index, startSudokuValue ->
            if (startSudokuValue != 0) {
                assertEquals(startSudokuValue, result.successValues[index].value)
            }
        }
    }

    @Test
    fun `Test that all final result contains all partial results`() = runTest {
        val partialResults = masterDifficultySudoku.chunked(9).map {
            it.toMutableList()
        }.toMutableList()

        repo.solve(
            masterDifficultySudoku.toSudokuTileValueInputModel(),
            SudokuFieldInputModel.classicSudoku9,
        ).collect { result ->
            when (result) {
                is FinalSudokuResult.Failure.InputWithDuplicate -> assert(false)
                FinalSudokuResult.Failure.NoSolutionFound -> assert(false)
                is FinalSudokuResult.Success -> {
                    result.successValues.forEach { model ->
                        assertEquals(partialResults[model.position.y - 1][model.position.x - 1], model.value)
                    }
                }
                is PartiallySolvedSudokuResult -> {
                    partialResults[result.position.y - 1][result.position.x - 1] = result.value
                }
            }
        }
    }

    @Test
    fun `Test that item solve reason in partial result, comes with correct reason`() = runTest {
        val partialResults = mutableListOf<PartiallySolvedSudokuResult>()

        repo.solve(
            veryEasySudoku.toSudokuTileValueInputModel(),
            SudokuFieldInputModel.classicSudoku9,
        ).collect { result ->
            when (result) {
                is FinalSudokuResult.Failure.InputWithDuplicate -> assert(false)
                FinalSudokuResult.Failure.NoSolutionFound -> assert(false)
                is FinalSudokuResult.Success -> {

                }
                is PartiallySolvedSudokuResult -> {
                    partialResults.add(result)
                }
            }
        }

        // x = 1, y = 1
        partialResults.firstOrNull {
            it.position.x == 1 && it.position.y == 1
        }?.run {
            assert(value == 7)
            assert(reason.solveType == SudokuSolveType.TheOnlyOptionInSquare)
            assert(reason.causes.size == 1)
            assert(reason.causes.first() == SudokuPosition(4,2))
        }?:assert(false)

        // x = 6, y = 1
        partialResults.firstOrNull {
            it.position.x == 6 && it.position.y == 1
        }?.run {
            assert(value == 5)
            assert(reason.solveType == SudokuSolveType.TheOnlyOptionInSquare)
            assert(reason.causes.size == 1)
            assert(reason.causes.first() == SudokuPosition(4,4))
        }?:assert(false)

        // x = 1, y = 2
        partialResults.firstOrNull {
            it.position.x == 1 && it.position.y == 2
        }?.run {
            assert(value == 5)
            assert(reason.solveType == SudokuSolveType.TheOnlyOptionInRow)
            assert(reason.causes.size == 1)
            assert(reason.causes.first() == SudokuPosition(3,6))
        }?:assert(false)


        // x = 3, y = 2
        partialResults.firstOrNull {
            it.position.x == 3 && it.position.y == 2
        }?.run {
            assert(value == 9)
            assert(reason.solveType == SudokuSolveType.TheOnlyOptionInPlace)
            assert(reason.causes.size == 8)
        }?:assert(false)

        // x = 8, y = 3
        partialResults.firstOrNull {
            it.position.x == 8 && it.position.y == 3
        }?.run {
            assert(value == 5)
            assert(reason.solveType == SudokuSolveType.TheOnlyOptionInSquare)
            assert(reason.causes.size == 1)
            assert(reason.causes.first() == SudokuPosition(7,5))
        }?:assert(false)

        // x = 8, y = 4
        partialResults.firstOrNull {
            it.position.x == 8 && it.position.y == 4
        }?.run {
            assert(value == 7)
            assert(reason.solveType == SudokuSolveType.TheOnlyOptionInSquare)
            assert(reason.causes.size == 1)
            assert(reason.causes.first() == SudokuPosition(6,6))
        }?:assert(false)

        // x = 8, y = 6
        partialResults.firstOrNull {
            it.position.x == 8 && it.position.y == 6
        }?.run {
            assert(value == 6)
            assert(reason.solveType == SudokuSolveType.TheOnlyOptionInRow)
            assert(reason.causes.size == 1)
            assert(reason.causes.first() == SudokuPosition(6,5))
        }?:assert(false)

        // x = 4, y = 6
        partialResults.firstOrNull {
            it.position.x == 4 && it.position.y == 6
        }?.run {
            assert(value == 2)
            assert(reason.solveType == SudokuSolveType.TheOnlyOptionInSquare)
            assert(reason.causes.size == 1)
            assert(reason.causes.first() == SudokuPosition(5,3))
        }?:assert(false)

        // x = 5, y = 8
        partialResults.firstOrNull {
            it.position.x == 5 && it.position.y == 8
        }?.run {
            assert(value == 3)
            assert(reason.solveType == SudokuSolveType.TheOnlyOptionInSquare)
            assert(reason.causes.size == 1)
            assert(reason.causes.first() == SudokuPosition(6,4))
        }?:assert(false)
    }

    @Test
    fun `Test that solves function works on sudoku 6x6`() = runTest {
        val squareIndexes = listOf(
            0, 0, 0,    1, 1, 1,
            0, 0, 0,    1, 1, 1,

            2, 2, 2,    3, 3, 3,
            2, 2, 2,    3, 3, 3,

           4, 4, 4,    5, 5, 5,
           4, 4, 4,    5, 5, 5,
        )

        val resultField = sudoku6Size.chunked(6).map {
            it.toMutableList()
        }.let { reversedField ->
            MutableList(6) { xIndex ->
                MutableList(6) { yIndex ->
                    reversedField[yIndex][xIndex]
                }
            }
        }

        lateinit var successValues: List<SudokuTileValueFullSolvedModel>

        repo.solve(
            values = sudoku6Size.toSudokuTileValueInputModel(maxValue = 6),
            fieldParams = SudokuFieldInputModel(
                sudokuFieldSize = 6,
                areaWidth = 3,
                areaHeight = 2,
            )
        ).collect {
            when (it) {
                is FinalSudokuResult.Failure.InputWithDuplicate -> assert(false)
                FinalSudokuResult.Failure.NoSolutionFound -> assert(false)
                is FinalSudokuResult.Success -> {
                    successValues = it.successValues
                }
                is PartiallySolvedSudokuResult -> {
                    println(
                        "at [x = ${it.position.x}, y = ${it.position.y}, is value ${it.value}, because ${it.reason.solveType}"
                    )
                    resultField[it.position.x-1][it.position.y-1] = it.value
                }
            }
        }

        assert(successValues.size >= 0)
        successValues.forEach {
            assertEquals(it.value, resultField[it.position.x - 1][it.position.y - 1])
        }

        val squares = successValues.mapIndexed(::Pair).groupBy {
            squareIndexes[it.first]
        }

        for (index in 0..5) {
            // check row
            assertEquals(6, resultField[index].toSet().size)
            // check column
            assertEquals(6, (0..5).map {
                resultField[it][index]
            }.toSet().size)
            // check square
            assertEquals(6, squares[index]!!.map { it.second.value }.toSet().size)
        }

        println(resultField)
    }

    private fun List<Int>.toSudokuTileValueInputModel(maxValue: Int = 9): List<SudokuTileValueInputModel> = this.mapIndexedNotNull { index, value ->
            if (value in 1..maxValue) {
                SudokuTileValueInputModel(
                    value = value,
                    position = SudokuPosition(
                        x = (index % maxValue ) + 1,
                        y = (index / maxValue ) + 1,
                    ),
                    tileType = SudokuInputTileType.SolvedValue
                )
            } else null
        }

}