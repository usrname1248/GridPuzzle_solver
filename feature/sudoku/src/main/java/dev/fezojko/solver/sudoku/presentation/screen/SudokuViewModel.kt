package dev.fezojko.solver.sudoku.presentation.screen

import android.os.Bundle
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.core.os.BundleCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fezojko.solver.sudoku.data.SudokuRepository
import dev.fezojko.solver.sudoku.model.FinalSudokuResult
import dev.fezojko.solver.sudoku.model.PartiallySolvedSudokuResult
import dev.fezojko.solver.sudoku.model.SudokuFieldInputModel
import dev.fezojko.solver.sudoku.model.SudokuPosition
import dev.fezojko.solver.sudoku.model.SudokuSolvedTurnReason
import dev.fezojko.solver.sudoku.model.SudokuTileValueInputModel
import dev.fezojko.solver.sudoku.model.createStandardAreas
import dev.fezojko.solver.sudoku.presentation.component.BorderSide
import dev.fezojko.solver.sudoku.presentation.screen.uiModels.SolvedSudokuIterationButtonType
import dev.fezojko.solver.sudoku.presentation.screen.uiModels.SudokuDialogState
import dev.fezojko.solver.sudoku.presentation.screen.uiModels.SudokuScreenErrorState
import dev.fezojko.solver.sudoku.presentation.screen.uiModels.SudokuScreenState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class SudokuViewModel(
    savedStateHandle: SavedStateHandle,
    private val sudokuRepository: SudokuRepository,
    initialState: SudokuScreenState
): ViewModel() {

    private val sudokuSavedStateProviderKey = "sudokuScreenBundle"
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
    var screenState: SudokuScreenState by mutableStateOf(initialState)

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

    val visibleSolvedResultsMap by derivedStateOf {
        visibleSolvedResults.associateBy { it.position }
    }

    val visibleSolvedResult: PartiallySolvedSudokuResult?
        get() = when (solvedVisibleIndex) {
            Int.MAX_VALUE -> allSolvedResults.lastOrNull()
            else -> allSolvedResults.getOrNull(solvedVisibleIndex)
        }

    private val _dialogState: MutableState<SudokuDialogState> = mutableStateOf(SudokuDialogState.Hidden)
    val dialogState: State<SudokuDialogState>
        get() = _dialogState

    private var solvingJob: Job? = null
    private var solvedVisibleIndex: Int by mutableIntStateOf(0)

    var errorState: SudokuScreenErrorState by mutableStateOf(SudokuScreenErrorState.NoError)

    init {
        // 2. RESTORE: Load data if we are recovering from process death
        val restoredBundle: Bundle? = savedStateHandle[sudokuSavedStateProviderKey]

        restoredBundle?.run {
            BundleCompat.getParcelableArray( this, userAddedPositionStateHandleKey, SudokuTileValueInputModel::class.java)
                ?.filterIsInstance<SudokuTileValueInputModel>()?.associateBy { it.position }?.let(_userAddedPositions::putAll)

            solvedVisibleIndex = getInt(solvedVisibleIndexHandleKey, 0)
            screenState =
                BundleCompat.getSerializable(this, isEditableModeHandleKey, SudokuScreenState::class.java)?:SudokuScreenState.SolvingScreenState
        }

        // Only serialize when the OS actually asks for it
        savedStateHandle.setSavedStateProvider(sudokuSavedStateProviderKey) {
            Bundle().apply {
                putParcelableArray(userAddedPositionStateHandleKey, _userAddedPositions.values.toTypedArray())
                putInt(solvedVisibleIndexHandleKey, solvedVisibleIndex)
                putSerializable(isEditableModeHandleKey, screenState)
            }
        }

        viewModelScope.launch {
            // anytime when _userAddedPositions are changed, clear solved results
            snapshotFlow { _userAddedPositions.toMap() }
                .collectLatest { _ ->
                    allSolvedResults.clear()
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
        screenState = SudokuScreenState.SolvingScreenState
        startSolving()
    }

    fun onSolveAllStepsClick() {
        solvedVisibleIndex = Int.MAX_VALUE
        screenState = SudokuScreenState.SolvingScreenState
        startSolving()
    }

    private fun startSolving() {
        if (_userAddedPositions.isEmpty() || allSolvedResults.isNotEmpty() || solvingJob != null) {
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
                        _dialogState.value = SudokuDialogState.ErrorState.DuplicateInputValue
                        errorState = SudokuScreenErrorState.DuplicateInput(result)
                        screenState = SudokuScreenState.EditingScreenState
                    }
                    FinalSudokuResult.Failure.NoSolutionFound -> {
                        _dialogState.value = SudokuDialogState.ErrorState.NoSolution
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
        clearErrorIfPositionMatchesError(inputModel.position)
        _userAddedPositions[inputModel.position] = inputModel
    }

    fun onValueCleared(position: SudokuPosition) {
        clearErrorIfPositionMatchesError(position)
        _userAddedPositions.remove(position)
    }

    fun clearErrorIfPositionMatchesError(position: SudokuPosition) {
        if ((errorState as? SudokuScreenErrorState.DuplicateInput)?.contains(position) == true) {
            clearError()
        }
    }

    fun clearError() {
        errorState = SudokuScreenErrorState.NoError
    }

    fun onDialogDismissRequest() {
        _dialogState.value = SudokuDialogState.Hidden
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
                    } else if (solvedVisibleIndex > 0) {
                        solvedVisibleIndex--
                    }
                }
            }
            SolvedSudokuIterationButtonType.Reason -> {
                showDetailedReason = showDetailedReason.not()

                if (showDetailedReason && visibleSolvedResult?.reason is SudokuSolvedTurnReason.GuessedValue) {
                    _dialogState.value = SudokuDialogState.ErrorState.GuessedValue
                }
            }
            SolvedSudokuIterationButtonType.Next -> {
                if (solvedVisibleIndex < allSolvedResults.lastIndex)
                    solvedVisibleIndex++

            }
            SolvedSudokuIterationButtonType.Last -> solvedVisibleIndex = Int.MAX_VALUE
        }
    }

    fun onSudokuImported(data: List<SudokuTileValueInputModel>) {
        val groupedData = data.associateBy { it.position }
        Snapshot.withMutableSnapshot {
            _userAddedPositions.clear()
            _userAddedPositions.putAll(groupedData)
        }
        _dialogState.value = SudokuDialogState.WarningState.SudokuImportedNeedUserCheck
        errorState = SudokuScreenErrorState.NoError
        screenState = SudokuScreenState.EditingScreenState
    }
}