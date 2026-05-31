package com.jozeftvrdy.solver.sudoku.model

data class SudokuSolvedTileReason(
    /** defines solve type, by which was result found */
    val solveType: SudokuSolveType,
    /** defines all tiles, that denies other results by solve type, for example:
     *
    * in column it is list of items, that deny this result in other place in column
    * in row it is list of items, that deny this result in other place in row
    * in square it is list of items, that deny this result in other place in square
    * in place it is list of items, that deny this result in other place in place
    */
    val causes: List<SudokuPosition>
)