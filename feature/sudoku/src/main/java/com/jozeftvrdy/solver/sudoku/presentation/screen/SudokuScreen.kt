package com.jozeftvrdy.solver.sudoku.presentation.screen

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.jozeftvrdy.solver.sudoku.R
import com.jozeftvrdy.solver.sudoku.domain.ProcessSudokuImageUseCase
import com.jozeftvrdy.solver.sudoku.model.SudokuInputTileType
import com.jozeftvrdy.solver.sudoku.model.SudokuPosition
import com.jozeftvrdy.solver.sudoku.model.SudokuSolvedTurnReason
import com.jozeftvrdy.solver.sudoku.model.SudokuTileValueInputModel
import com.jozeftvrdy.solver.sudoku.presentation.component.BorderSide
import com.jozeftvrdy.solver.sudoku.presentation.component.SudokuField
import com.jozeftvrdy.solver.sudoku.presentation.component.SudokuPositionComponent
import com.jozeftvrdy.solver.sudoku.presentation.component.SudokuPositionComponentContent
import com.jozeftvrdy.solver.sudoku.presentation.component.SudokuPositionValuePresentationModel
import com.jozeftvrdy.solver.sudoku.presentation.screen.uiModels.SolvedSudokuIterationButtonType
import com.jozeftvrdy.solver.sudoku.presentation.screen.uiModels.SudokuErrorDialogState
import com.jozeftvrdy.solver.sudoku.presentation.screen.uiModels.SudokuScreenState
import com.jozeftvrdy.solver.sudoku.presentation.theme.SudokuTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import kotlin.coroutines.resume

private const val pickerItemRawSize = 40

private data class EditIconData(
    @param:DrawableRes val drawableRes: Int,
    @param:StringRes val contentDescriptionStringRes: Int,
    val editableStateTarget: SudokuScreenState
) {

    @Composable
    fun getPainter() = painterResource(drawableRes)

    @Composable
    fun getContentDescription() = stringResource(contentDescriptionStringRes)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SudokuScreen(
    onBackClick: () -> Unit
) {
    val vm = koinViewModel<SudokuViewModel>()

    val provideEditContentData: (position: SudokuPosition) -> SudokuPositionValuePresentationModel? =
        remember(vm) {
            { position ->
                vm.userAddedPositions[position]?.let {
                    SudokuPositionValuePresentationModel(
                        value = it.value,
                        inputTileType = it.tileType,
                    )
                }
            }
        }

    val provideSolveContentData: (position: SudokuPosition) -> SudokuPositionValuePresentationModel? =
        remember(vm, provideEditContentData) {
            { position ->
                provideEditContentData(position) ?: vm.visibleSolvedResultsMap[position]?.let {
                    SudokuPositionValuePresentationModel(
                        value = it.value,
                        inputTileType = SudokuInputTileType.SolvedValue,
                    )
                }
            }
        }

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text("Sudoku")
                },
                navigationIcon = {
                    Icon(
                        modifier = Modifier.clickable(onClick = onBackClick),
                        painter = painterResource(R.drawable.outline_arrow_back_24),
                        contentDescription = stringResource(R.string.content_description_back_button),
                        tint = SudokuTheme.colors.textValueColor
                    )
                },
                actions = {
                    when (vm.screenState) {
                        SudokuScreenState.EditingScreenState -> {
                            EditIconData(
                                drawableRes = R.drawable.outline_edit_off_24,
                                contentDescriptionStringRes = R.string.content_description_exit_edit,
                                editableStateTarget = SudokuScreenState.SolvingScreenState
                            )
                        }
                        SudokuScreenState.PhotoingScreenState -> {
                            null
                        }
                        SudokuScreenState.SolvingScreenState -> {
                            EditIconData(
                                drawableRes = R.drawable.outline_edit_24,
                                contentDescriptionStringRes = R.string.content_description_enter_edit,
                                editableStateTarget = SudokuScreenState.EditingScreenState
                            )
                        }
                    }?.let { iconState ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clickable {
                                    vm.screenState = iconState.editableStateTarget
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = iconState.getPainter(),
                                contentDescription = iconState.getContentDescription(),
                                tint = SudokuTheme.colors.textValueColor,
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (vm.screenState != SudokuScreenState.PhotoingScreenState) {
                FloatingActionButton(
                    onClick = {
                        vm.screenState = SudokuScreenState.PhotoingScreenState
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_photo_camera_24),
                        contentDescription = stringResource(R.string.sudoku_camera_fab_description),
                        tint = SudokuTheme.colors.textValueColor,
                    )
                }
            }
        }
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
        ) {
            SudokuAnyContent(
                screenState = vm.screenState,
                allPossibleItemValues = vm.allPossibleItemValues,
                positions = vm.allPositions,
                provideEditContentData = provideEditContentData,
                provideSolveContentData = provideSolveContentData,
                provideBorderSide = vm::calculateBorderSide,
                provideBackgroundColor = provideBackgroundColor@ { position ->
                    val defaultValue = SudokuTheme.colors.tileBackground
                    val currentVisibleSolvedResult = vm.visibleSolvedResult?: return@provideBackgroundColor defaultValue
                    return@provideBackgroundColor when {
                        (currentVisibleSolvedResult.position == position) -> {
                            SudokuTheme.colors.markedAsAddedTileBackground
                        }
                        (vm.showDetailedReason) -> {
                            when (val localReason = currentVisibleSolvedResult.reason) {
                                SudokuSolvedTurnReason.GuessedValue -> defaultValue
                                is SudokuSolvedTurnReason.OnlyFreeSpaceInArea -> {
                                    if (localReason.area.positions.contains(position)) {
                                        SudokuTheme.colors.markedAsAreaTileBackground
                                    } else defaultValue
                                }
                                is SudokuSolvedTurnReason.OnlyOptionInArea -> {
                                    if (localReason.externalReasons.contains(position)) {
                                        SudokuTheme.colors.markedAsReasonTileBackground
                                    } else if (localReason.area.positions.contains(position)) {
                                        SudokuTheme.colors.markedAsAreaTileBackground
                                    } else defaultValue
                                }
                                is SudokuSolvedTurnReason.OnlyValueOptionForThisTile -> {
                                    if (localReason.otherValuesPositions.contains(position)) {
                                        SudokuTheme.colors.markedAsReasonTileBackground
                                    } else defaultValue
                                }
                            }
                        }
                        else -> {
                            defaultValue
                        }
                    }
                },
                onValueSet = vm::onValueSet,
                onValueCleared = vm::onValueCleared,
                onSolveNextStepClick = vm::onSolveNextStepClick,
                onSolveAllStepsClick = vm::onSolveAllStepsClick,
                onIterationButtonClicked = vm::onIterationButtonClicked,
                onSudokuImported = vm::onSudokuImported,
                onCameraPermissionReject = {
                    vm.screenState = SudokuScreenState.EditingScreenState
                },
                provideHasSolvedContent = {
                    vm.visibleSolvedResults.isNotEmpty()
                },
            )
        }
    }

    ErrorDialog(
        vm.dialogState,
        vm::onErrorDialogDismissRequest,
    )
}

@Composable
private fun SudokuAnyContent(
    screenState: SudokuScreenState,
    allPossibleItemValues: IntRange,
    positions: ImmutableList<SudokuPosition>,
    provideEditContentData: (position: SudokuPosition) -> SudokuPositionValuePresentationModel?,
    provideSolveContentData: (position: SudokuPosition) -> SudokuPositionValuePresentationModel?,
    provideBorderSide: (position: SudokuPosition) -> List<BorderSide>,
    provideBackgroundColor: @Composable (position: SudokuPosition) -> Color,
    onValueSet: (SudokuTileValueInputModel) -> Unit,
    onValueCleared: (SudokuPosition) -> Unit,
    onSolveNextStepClick: () -> Unit,
    onSolveAllStepsClick: () -> Unit,
    onIterationButtonClicked: (SolvedSudokuIterationButtonType) -> Unit,
    onSudokuImported: (Map<SudokuPosition, Int>) -> Unit,
    onCameraPermissionReject: () -> Unit,
    provideHasSolvedContent: () -> Boolean,
) {
    when (screenState) {
        SudokuScreenState.EditingScreenState -> {
            SudokuEditableContent(
                allPossibleItemValues = allPossibleItemValues,
                positions = positions,
                provideContentData = provideEditContentData,
                provideBorderSide = provideBorderSide,
                onValueSet = onValueSet,
                onValueCleared = onValueCleared,
                onSolveNextStepClick = onSolveNextStepClick,
                onSolveAllStepsClick = onSolveAllStepsClick,
                )
        }
        SudokuScreenState.SolvingScreenState -> {
            SudokuSolvingContent(
                positions = positions,
                provideContentData = provideSolveContentData,
                provideBorderSide = provideBorderSide,
                provideBackgroundColor = provideBackgroundColor,
                onIterationButtonClicked = onIterationButtonClicked,
                provideHasSolvedContent = provideHasSolvedContent
            )
        }
        SudokuScreenState.PhotoingScreenState -> {
            SudokuPhotoingPermissionContent(
                positions = positions,
                provideBorderSide = provideBorderSide,
                onSudokuImported = onSudokuImported,
                onCameraPermissionReject = onCameraPermissionReject,
            )
        }
    }
}

private data class SudokuValuePicker(
    val value: Int?,
    val tileType: SudokuInputTileType,
)

private sealed class SudokuEditableSelectedState {
    data class SudokuEditableSelectedSudokuTile(
        val sudokuPosition: SudokuPosition
    ) : SudokuEditableSelectedState()

    data class SudokuEditableSelectedValuePicker(
        val picker: SudokuValuePicker
    ) : SudokuEditableSelectedState()
}

@Composable
private fun SudokuEditableContent(
    allPossibleItemValues: IntRange,
    positions: ImmutableList<SudokuPosition>,
    provideContentData: (position: SudokuPosition) -> SudokuPositionValuePresentationModel?,
    provideBorderSide: (position: SudokuPosition) -> List<BorderSide>,
    onValueSet: (SudokuTileValueInputModel) -> Unit,
    onValueCleared: (SudokuPosition) -> Unit,
    onSolveNextStepClick: () -> Unit,
    onSolveAllStepsClick: () -> Unit,
) {
    val selectedValueState: MutableState<SudokuEditableSelectedState?> = remember {
        mutableStateOf(null)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        SudokuEditableSudokuField(
            selectedValueState = selectedValueState,
            positions = positions,
            provideContentData = provideContentData,
            provideBorderSide = provideBorderSide,
            onValueSet = onValueSet,
            onValueCleared = onValueCleared
        )

        Spacer(modifier = Modifier.size(16.dp))

        SudokuEditableBelowFieldContent(
            selectedValueState = selectedValueState,
            allPossibleItemValues = allPossibleItemValues,
            onValueSet = onValueSet,
            onValueCleared = onValueCleared,
            onSolveNextStepClick = onSolveNextStepClick,
            onSolveAllStepsClick = onSolveAllStepsClick,
        )
    }
}

@Composable
private fun SudokuEditableSudokuField(
    selectedValueState: MutableState<SudokuEditableSelectedState?>,
    positions: ImmutableList<SudokuPosition>,
    provideContentData: (position: SudokuPosition) -> SudokuPositionValuePresentationModel?,
    provideBorderSide: (position: SudokuPosition) -> List<BorderSide>,
    onValueSet: (SudokuTileValueInputModel) -> Unit,
    onValueCleared: (SudokuPosition) -> Unit,
) {

    SudokuField(
        positions = positions,
        provideBorderSide = provideBorderSide,
        provideBackgroundColor = { position ->
            if ((selectedValueState.value as? SudokuEditableSelectedState.SudokuEditableSelectedSudokuTile)?.sudokuPosition == position) {
                SudokuTheme.colors.markedAsAddedTileBackground
            } else {
                SudokuTheme.colors.tileBackground
            }
        },
    ) { position ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    when (val localSelectedValueState = selectedValueState.value) {
                        is SudokuEditableSelectedState.SudokuEditableSelectedSudokuTile -> {
                            selectedValueState.value =
                                if (localSelectedValueState.sudokuPosition == position) {
                                    // if we clicked on this tile, and is selected, unselect it
                                    null
                                } else {
                                    // else select new selected tile
                                    SudokuEditableSelectedState.SudokuEditableSelectedSudokuTile(
                                        position
                                    )
                                }
                        }

                        null -> {
                            // if nothing was selected, just select this tile
                            selectedValueState.value =
                                SudokuEditableSelectedState.SudokuEditableSelectedSudokuTile(
                                    position
                                )
                        }

                        is SudokuEditableSelectedState.SudokuEditableSelectedValuePicker -> {
                            // if picker was selected, set / clear value
                            localSelectedValueState.picker.value?.let {
                                onValueSet(
                                    SudokuTileValueInputModel(
                                        value = localSelectedValueState.picker.value,
                                        position = position,
                                        tileType = localSelectedValueState.picker.tileType
                                    )
                                )
                            } ?: onValueCleared(
                                position
                            )
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            provideContentData(position)?.let { contentData ->
                SudokuPositionComponentContent(
                    valueData = contentData,
                )
            }
        }
    }
}

@Composable
private fun SudokuEditableBelowFieldContent(
    selectedValueState: MutableState<SudokuEditableSelectedState?>,
    allPossibleItemValues: IntRange,
    onValueSet: (SudokuTileValueInputModel) -> Unit,
    onValueCleared: (SudokuPosition) -> Unit,
    onSolveNextStepClick: () -> Unit,
    onSolveAllStepsClick: () -> Unit,
) {
    Column {
        val provideSelectedValuePicker: () -> SudokuValuePicker? = remember(selectedValueState) {
            {
                (selectedValueState.value as? SudokuEditableSelectedState.SudokuEditableSelectedValuePicker)?.picker
            }
        }
        val onValuePickerSelected: (SudokuValuePicker) -> Unit = remember(selectedValueState) {
            { selectedValuePicker ->
                when (val localSelectedValueState = selectedValueState.value) {
                    is SudokuEditableSelectedState.SudokuEditableSelectedSudokuTile -> {
                        // if tile was selected, set / clear value
                        selectedValuePicker.value?.let {
                            onValueSet(
                                SudokuTileValueInputModel(
                                    value = selectedValuePicker.value,
                                    position = localSelectedValueState.sudokuPosition,
                                    tileType = selectedValuePicker.tileType
                                )
                            )
                        } ?: onValueCleared(
                            localSelectedValueState.sudokuPosition
                        )
                    }

                    is SudokuEditableSelectedState.SudokuEditableSelectedValuePicker -> {
                        selectedValueState.value =
                            if (localSelectedValueState.picker == selectedValuePicker) {
                                // if we clicked on this picker, and is selected, unselect it
                                null
                            } else {
                                // else select new picker
                                SudokuEditableSelectedState.SudokuEditableSelectedValuePicker(
                                    selectedValuePicker
                                )
                            }
                    }

                    null -> {
                        // if nothing was selected, just select this picker
                        selectedValueState.value =
                            SudokuEditableSelectedState.SudokuEditableSelectedValuePicker(
                                selectedValuePicker
                            )
                    }
                }
            }
        }

        SudokuEditableValuePickerScrollableRow(
            description = stringResource(R.string.fixed_sudoku_values_subtitle),
            allPossibleItemValues = allPossibleItemValues,
            inputTileType = SudokuInputTileType.FixedValue,
            provideSelectedValuePicker = provideSelectedValuePicker,
            onValuePickerSelected = onValuePickerSelected,
        )
        Spacer(modifier = Modifier.size(8.dp))

        SudokuEditableValuePickerScrollableRow(
            description = stringResource(R.string.already_solved_sudoku_values_subtitle),
            allPossibleItemValues = allPossibleItemValues,
            inputTileType = SudokuInputTileType.SolvedValue,
            provideSelectedValuePicker = provideSelectedValuePicker,
            onValuePickerSelected = onValuePickerSelected,
        )

        Spacer(modifier = Modifier.size(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            SolveButton(
                stringResource(R.string.sudoku_solve_one_step_btn),
                onClick =  onSolveNextStepClick
            )
            SolveButton(
                stringResource(R.string.sudoku_solve_all_steps_btn),
                onClick =  onSolveAllStepsClick
            )
        }
    }
}

@Composable
private fun SolveButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick
    ) {
        Text(
            text,
            fontSize = 18.sp,
        )
    }
}

@Composable
private fun SudokuEditableValuePickerScrollableRow(
    description: String,
    allPossibleItemValues: IntRange,
    inputTileType: SudokuInputTileType,
    provideSelectedValuePicker: () -> SudokuValuePicker?,
    onValuePickerSelected: (SudokuValuePicker) -> Unit,
) {
    val density = LocalDensity.current
    val size: Dp = pickerItemRawSize.dp
    val provideBorderSides: (fieldValue: Int?) -> List<BorderSide> =
        remember(provideSelectedValuePicker) {
            { fieldValue ->
                provideSelectedValuePicker().let { selectedValuePicker ->
                    if (selectedValuePicker?.value == fieldValue && selectedValuePicker?.tileType == inputTileType) {
                        BorderSide.entries
                    } else {
                        emptyList()
                    }
                }
            }
        }

    Column {
        Text(
            description,
            fontSize = with(density) {
                size.toSp() * 0.4f
            },
            fontWeight = FontWeight.Medium,
            color = SudokuTheme.colors.sudokuBorder,
        )

        Spacer(modifier = Modifier.size(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.size(16.dp))

            SudokuPositionComponent(
                modifier = Modifier.clickable {
                    onValuePickerSelected(
                        SudokuValuePicker(
                            value = null,
                            tileType = inputTileType
                        )
                    )
                },
                position = SudokuPosition(1, 1),
                size = size,
                provideBorderSides = remember(provideBorderSides) {
                    {
                        provideBorderSides(null)
                    }
                },
            ) {
            }

            allPossibleItemValues.forEach { possibleItemValue ->
                Spacer(modifier = Modifier.size(8.dp))

                SudokuPositionComponent(
                    modifier = Modifier.clickable {
                        onValuePickerSelected(
                            SudokuValuePicker(
                                value = possibleItemValue,
                                tileType = inputTileType
                            )
                        )
                    },
                    position = SudokuPosition(1, 1),
                    size = size,
                    provideBorderSides = remember(provideBorderSides, possibleItemValue) {
                        {
                            provideBorderSides(possibleItemValue)
                        }
                    },
                ) {
                    SudokuPositionComponentContent(
                        valueData = SudokuPositionValuePresentationModel(
                            value = possibleItemValue,
                            inputTileType = inputTileType
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.size(16.dp))
        }
    }

}

@Composable
private fun SudokuSolvingContent(
    positions: ImmutableList<SudokuPosition>,
    provideContentData: (position: SudokuPosition) -> SudokuPositionValuePresentationModel?,
    provideBorderSide: (position: SudokuPosition) -> List<BorderSide>,
    provideBackgroundColor: @Composable (position: SudokuPosition) -> Color,
    onIterationButtonClicked: (SolvedSudokuIterationButtonType) -> Unit,
    provideHasSolvedContent: () -> Boolean,
) {
    Column {
        SudokuField(
            positions = positions,
            provideBorderSide = provideBorderSide,
            provideBackgroundColor = provideBackgroundColor,
        ) { position ->
            provideContentData(position)?.let { contentData ->
                SudokuPositionComponentContent(
                    valueData = contentData,
                )
            }
        }

        AnimatedVisibility(
            provideHasSolvedContent(),
            enter = slideInVertically { - it } + fadeIn(),
            exit = slideOutVertically { - it } + fadeOut()
        ) {
            Column(
               modifier = Modifier
                   .fillMaxWidth()
            ) {
                Spacer(Modifier.size(16.dp))
                SudokuSolvingBelowSudokuContent(
                    onIterationButtonClicked = onIterationButtonClicked,
                )
            }

        }

    }

}

@Composable
private fun SudokuSolvingBelowSudokuContent(
    onIterationButtonClicked: (SolvedSudokuIterationButtonType) -> Unit,
) {

    val buttonByType: @Composable (SolvedSudokuIterationButtonType) -> Unit = remember {
        { type ->
            SudokuIterationButton(
                type = type,
                onClick = remember(type) {
                    {
                        onIterationButtonClicked(type)
                    }
                }
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
        ,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SolvedSudokuIterationButtonType.entries.forEach {
                buttonByType(it)
            }
        }
    }
}

@Composable
private fun SudokuIterationButton(
    type: SolvedSudokuIterationButtonType,
    onClick: () -> Unit
) {

    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(
                color = SudokuTheme.colors.textValueColor,
                shape = CircleShape
            )
            .padding(2.dp)
            .background(
                color = SudokuTheme.colors.tileBackground,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            type.getDrawable(),
            contentDescription = type.getDescription(),
            modifier = Modifier.then(type.modifier)
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun SudokuPhotoingPermissionContent(
    positions: ImmutableList<SudokuPosition>,
    provideBorderSide: (position: SudokuPosition) -> List<BorderSide>,
    onCameraPermissionReject: () -> Unit,
    onSudokuImported: (Map<SudokuPosition, Int>) -> Unit,
) {

    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    if (cameraPermissionState.status.isGranted) {
        SudokuPhotoingContent(
            positions = positions,
            provideBorderSide = provideBorderSide,
            onSudokuImported = onSudokuImported
        )
    } else {
        SudokuPhotoingPermissionNotGrantedContent(
            permissionState = cameraPermissionState,
            onCameraPermissionReject = onCameraPermissionReject,
        )
    }
}

@Composable
private fun SudokuPhotoingContent(
    positions: ImmutableList<SudokuPosition>,
    provideBorderSide: (position: SudokuPosition) -> List<BorderSide>,
    processSudokuImageUseCase: ProcessSudokuImageUseCase = koinInject(),
    onSudokuImported: (Map<SudokuPosition, Int>) -> Unit,
) {
    val context = LocalContext.current

    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(
                CameraController.IMAGE_CAPTURE or
                        CameraController.VIDEO_CAPTURE
            )
        }
    }

    val isLoadingState = remember {
        mutableStateOf(false)
    }

    val scope = rememberCoroutineScope()

    val capturedImageState: MutableState<ImageBitmap?> = remember {
        mutableStateOf(null)
    }

    val onClick: () -> Unit = onClick@ {
        isLoadingState.value = true
        scope.launch(Dispatchers.Main) {
            val originalPicture = takePhoto(
                controller = controller,
                context = context,
            ).getOrNull()?:return@launch
            capturedImageState.value = originalPicture.asImageBitmap()
            processSudokuImageUseCase.invoke(originalPicture).also {
                isLoadingState.value = false
                onSudokuImported(it)
            }
        }
    }

    Box (
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        when  {
            capturedImageState.value != null -> {
                Image(
                    bitmap = capturedImageState.value!!,
                    contentDescription = "",
                )
            }
            else -> {
                CameraPreview(
                    controller = controller,
                    modifier = Modifier
                        .fillMaxSize()
                )
            }
        }


        SudokuField(
            modifier = Modifier.fillMaxWidth(0.9f),
            positions = positions,
            provideBorderSide = provideBorderSide,
            provideBackgroundColor = { _ ->
                Color.Transparent
            },
        ) { _ ->

        }

        Button(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .align(Alignment.BottomCenter),
            onClick = onClick
        ) {
            if (isLoadingState.value) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = SudokuTheme.colors.tileBackground
                )
            } else {
                Text(
                    "Take picture",
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp,
                    color = SudokuTheme.colors.tileBackground
                )
            }
        }
    }
}

@Composable
fun CameraPreview(
    controller: LifecycleCameraController,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        factory = {
            PreviewView(it).apply {
                this.controller = controller
                controller.bindToLifecycle(lifecycleOwner)
            }
        },
        modifier = modifier
    )
}

private suspend fun takePhoto(
    controller: LifecycleCameraController,
    context: Context,
): Result<Bitmap> = suspendCancellableCoroutine { continuation ->
    controller.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                super.onCaptureSuccess(image)

                val cropRect = image.cropRect
                val matrix = Matrix().apply {
                    postRotate(image.imageInfo.rotationDegrees.toFloat())
                }
                val rotatedBitmap = Bitmap.createBitmap(
                    image.toBitmap(),
                    cropRect.left,
                    cropRect.top,
                    cropRect.width(),
                    cropRect.height(),
                    matrix,
                    true
                )

                continuation.resume(Result.success(rotatedBitmap))
            }

            override fun onError(exception: ImageCaptureException) {
                super.onError(exception)
                continuation.resume(Result.failure(exception))
            }
        }
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SudokuPhotoingPermissionNotGrantedContent(
    permissionState: PermissionState,
    onCameraPermissionReject: () -> Unit,
) {
    val context = LocalContext.current

    val wasDialogRequested = remember {
        mutableStateOf(false)
    }

    fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    val text = stringResource(R.string.sudoku_camera_permission_text)
    val buttonText = stringResource(R.string.sudoku_camera_permission_button)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.size(24.dp))


        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (wasDialogRequested.value) {
                    openAppSettings()
                } else {
                    wasDialogRequested.value = true
                    permissionState.launchPermissionRequest()
                }
            }
        ) {
            SudokuPhotoingPermissionNotGrantedButtonText(buttonText)
        }
        Spacer(modifier = Modifier.size(8.dp))

        SudokuPhotoingPermissionNotGrantedButtonText(stringResource(R.string.option_alternative))

        Spacer(modifier = Modifier.size(8.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onCameraPermissionReject
        ) {
            SudokuPhotoingPermissionNotGrantedButtonText(stringResource(R.string.sudoku_camera_permission_do_not_provide_button))
        }
    }
}

@Composable
fun SudokuPhotoingPermissionNotGrantedButtonText(
    text: String
) {
    Text(
        text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ErrorDialog(
    dialogState: State<SudokuErrorDialogState>,
    onDismissRequest: () -> Unit,
) {

    val visibleDialogStateValue = dialogState.value as? SudokuErrorDialogState.Visible ?: return

    BasicAlertDialog(
        onDismissRequest = onDismissRequest
    ) {
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = visibleDialogStateValue.getDialogTitle(),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.size(16.dp))

                Text(
                    text = visibleDialogStateValue.getDialogText(),
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.size(16.dp))
                TextButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    onClick = onDismissRequest,
                ) {
                    Text(
                        stringResource(R.string.sudoku_dialog_confirm_button),
                        fontWeight = FontWeight.Medium,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
