package com.jozeftvrdy.solver.sudoku.model

data class SudokuFieldInputModel(
    val sudokuFieldWidth: Int,
    val sudokuFieldHeight: Int,
    val areaWidth: Int,
    val areaHeight: Int,
) {
    constructor(
        sudokuFieldSize: Int,
        areaWidth: Int,
        areaHeight: Int,
    ) : this(
        sudokuFieldWidth  = sudokuFieldSize,
        sudokuFieldHeight = sudokuFieldSize,
        areaWidth         = areaWidth,
        areaHeight        = areaHeight,

    )

    constructor(
        sudokuFieldSize: Int,
        areaSize: Int,
    ) : this(
        sudokuFieldWidth  = sudokuFieldSize,
        sudokuFieldHeight = sudokuFieldSize,
        areaWidth         = areaSize,
        areaHeight        = areaSize,

    )

    companion object {
        val classicSudoku9 = SudokuFieldInputModel(
            sudokuFieldSize = 9,
            areaSize = 3,
        )
    }

    init {
        require(areaHeight < sudokuFieldHeight)
        require(areaWidth < sudokuFieldWidth)
    }
}
