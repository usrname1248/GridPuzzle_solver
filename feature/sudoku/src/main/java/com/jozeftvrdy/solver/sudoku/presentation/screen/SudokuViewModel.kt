package com.jozeftvrdy.solver.sudoku.presentation.screen

import android.os.Bundle
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jozeftvrdy.solver.sudoku.data.SudokuRepository
import com.jozeftvrdy.solver.sudoku.model.FinalSudokuResult
import com.jozeftvrdy.solver.sudoku.model.PartiallySolvedSudokuResult
import com.jozeftvrdy.solver.sudoku.model.SudokuFieldInputModel
import com.jozeftvrdy.solver.sudoku.model.SudokuPosition
import com.jozeftvrdy.solver.sudoku.model.SudokuSolvedTurnReason
import com.jozeftvrdy.solver.sudoku.model.SudokuTileValueInputModel
import com.jozeftvrdy.solver.sudoku.model.createStandardAreas
import com.jozeftvrdy.solver.sudoku.presentation.component.BorderSide
import com.jozeftvrdy.solver.sudoku.presentation.screen.uiModels.SolvedSudokuIterationButtonType
import com.jozeftvrdy.solver.sudoku.presentation.screen.uiModels.SudokuErrorDialogState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class SudokuViewModel(
    savedStateHandle: SavedStateHandle,
    private val sudokuRepository: SudokuRepository,
): ViewModel() {
    private val userAddedPositionStateHandleKey = "userAddedPositionStateHandleKey"
    private val solvedVisibleIndexHandleKey = "solvedVisibleIndex"
    private val isEditableModeHandleKey = "isEditableMode"

    // TODO: flexible
    val allPossibleItemValues = 1..9

    // TODO: flexible
    val allPositions: ImmutableList<SudokuPosition> = buildList {
        for (y in allPossibleItemValues) {
            for (x in allPossibleItemValues) {
                add(SudokuPosition(x, y))
            }
        }
    }.toImmutableList()

    // todo: not saved in bundle
    var isEditableMode by mutableStateOf(false)

    // todo: not saved in bundle
    var showDetailedReason by mutableStateOf(false)

    private val _userAddedPositions: SnapshotStateMap<SudokuPosition, SudokuTileValueInputModel> =
        mutableStateMapOf()

    val userAddedPositions: Map<SudokuPosition, SudokuTileValueInputModel>
        get() = _userAddedPositions

    // todo: not saved in bundle
    private val allSolvedResults: SnapshotStateList<PartiallySolvedSudokuResult> =
        mutableStateListOf()

    val visibleSolvedResults: List<PartiallySolvedSudokuResult>
        get() = when (solvedVisibleIndex) {
            Int.MAX_VALUE -> allSolvedResults
            else -> allSolvedResults.take(solvedVisibleIndex + 1)
        }

    val visibleSolvedResult: PartiallySolvedSudokuResult?
        get() = when (solvedVisibleIndex) {
            Int.MAX_VALUE -> allSolvedResults.lastOrNull()
            else -> allSolvedResults.getOrNull(solvedVisibleIndex)
        }

    private val _dialogState: MutableState<SudokuErrorDialogState> = mutableStateOf(SudokuErrorDialogState.Hidden)
    val dialogState: State<SudokuErrorDialogState>
        get() = _dialogState

    private var solvingJob: Job? = null
    private var solvedVisibleIndex: Int by mutableIntStateOf(0)

    init {
        // 2. RESTORE: Load data if we are recovering from process death
        val restoredUserAddedPositions: Array<SudokuTileValueInputModel>? = savedStateHandle[userAddedPositionStateHandleKey]
        if (restoredUserAddedPositions != null) {
            _userAddedPositions.putAll(restoredUserAddedPositions.associateBy { it.position })
        }
        solvedVisibleIndex = savedStateHandle[solvedVisibleIndexHandleKey]?:0
        isEditableMode = savedStateHandle[isEditableModeHandleKey]?:false

        // Only serialize when the OS actually asks for it
        savedStateHandle.setSavedStateProvider("screenState") {
            Bundle().apply {
                putParcelableArray(userAddedPositionStateHandleKey, _userAddedPositions.values.toTypedArray())
                putInt(solvedVisibleIndexHandleKey, solvedVisibleIndex)
                putBoolean(isEditableModeHandleKey, isEditableMode)
            }
        }
    }

    fun calculateBorderSide(position: SudokuPosition) : List<BorderSide> = buildList {
        if (position.y % 3 == 0) {
            add(BorderSide.Bottom)
        }

        if ((position.y - 1) % 3 == 0) {
            add(BorderSide.Top)
        }

        if (position.x % 3 == 0) {
            add(BorderSide.Right)
        }

        if ((position.x - 1) % 3 == 0) {
            add(BorderSide.Left)
        }
    }

    fun onSolveNextStepClick() {
        solvedVisibleIndex = 0
        isEditableMode = false
        startSolving()
    }

    fun onSolveAllStepsClick() {
        solvedVisibleIndex = Int.MAX_VALUE
        isEditableMode = false
        startSolving()
    }

    private fun startSolving() {
        if (_userAddedPositions.isEmpty() or allSolvedResults.isEmpty().not() or (solvingJob != null)) {
            return
        }

        solvingJob = viewModelScope.launch {
            sudokuRepository.solve(
                fieldParams = SudokuFieldInputModel(
                    createStandardAreas(
                        sudokuFieldWidth = 9,
                        sudokuFieldHeight = 9,
                        areaWidth = 3,
                        areaHeight = 3,
                    )
                ),
                values = _userAddedPositions.values.toList()
            ).onEach {
                delay(50.milliseconds)
            }.collect { result ->
                when (result) {
                    is FinalSudokuResult.Failure.InputWithDuplicate -> {
                        _dialogState.value = SudokuErrorDialogState.Visible.DuplicateInputValue
                    }
                    FinalSudokuResult.Failure.NoSolutionFound -> {
                        _dialogState.value = SudokuErrorDialogState.Visible.NoSolution
                    }
                    is FinalSudokuResult.Success -> {
                    }
                    is PartiallySolvedSudokuResult -> {
                        allSolvedResults.add(result)
                    }
                }
            }
        }.also {
            it.invokeOnCompletion {
                solvingJob = null
            }
        }
    }

    fun onValueSet(inputModel: SudokuTileValueInputModel) {
        _userAddedPositions[inputModel.position] = inputModel
        allSolvedResults.clear()
    }

    fun onValueCleared(position: SudokuPosition) {
        _userAddedPositions.remove(position)
        allSolvedResults.clear()
    }

    fun onErrorDialogDismissRequest() {
        _dialogState.value = SudokuErrorDialogState.Hidden
    }

    fun onIterationButtonClicked(type: SolvedSudokuIterationButtonType) {
        if (type != SolvedSudokuIterationButtonType.Reason) {
            showDetailedReason = false
        }

        when (type) {
            SolvedSudokuIterationButtonType.First -> solvedVisibleIndex = 0
            SolvedSudokuIterationButtonType.Previous -> {
                if (solvedVisibleIndex>0) {
                    if (solvedVisibleIndex == Int.MAX_VALUE) {
                        solvedVisibleIndex = allSolvedResults.lastIndex - 1
                    } else {
                        solvedVisibleIndex--
                    }
                }
            }
            SolvedSudokuIterationButtonType.Reason -> {
                showDetailedReason = showDetailedReason.not()

                if (showDetailedReason && visibleSolvedResult?.reason is SudokuSolvedTurnReason.GuessedValue) {
                    _dialogState.value = SudokuErrorDialogState.Visible.GuessedValue
                }
            }
            SolvedSudokuIterationButtonType.Next -> {
                if (solvedVisibleIndex < allSolvedResults.lastIndex)
                    solvedVisibleIndex++

            }
            SolvedSudokuIterationButtonType.Last -> solvedVisibleIndex = Int.MAX_VALUE
        }
    }
}