package com.jozeftvrdy.solver.sudoku.model

sealed class SudokuTileValueDataModel {
    abstract fun valueOrNull(): Int?

    data class FixedTileValue(val value: Int) : SudokuTileValueDataModel() {
        override fun valueOrNull() = value
    }
    sealed class FlexibleTileValue() : SudokuTileValueDataModel() {
        data class SolvedTileValue(val value: Int) : FlexibleTileValue() {
            override fun valueOrNull() = value
        }
        class UnsolvedTileValue() : FlexibleTileValue() {
            override fun valueOrNull() = null
            private val _possibleValues: MutableSet<Int> = mutableSetOf(1,2,3,4,5,6,7,8,9)
            val possibleValues: Set<Int>
                get() = _possibleValues
            private val impossibleValuesMap: MutableMap<Int, SudokuPosition> = mutableMapOf()
            fun removePossibilityIfMissing(value: Int, becauseOfPosition: SudokuPosition, force: Boolean = false) {
                val shouldRemove = force || _possibleValues.contains(value)

                if (shouldRemove) {
                    _possibleValues.remove(value)
                    impossibleValuesMap[value] = becauseOfPosition
                }
            }

            fun findReasonPositionForValue(value: Int): SudokuPosition? = impossibleValuesMap[value]
            fun getAllReasons(): List<SudokuPosition> = impossibleValuesMap.values.toList()
        }
    }
}