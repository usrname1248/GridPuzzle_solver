package com.jozeftvrdy.solver.gridpuzzle.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.jozeftvrdy.solver.core.datastore.model.HandwrittenRecognizerModel
import com.jozeftvrdy.solver.core.datastore.model.SudokuImageProcessingType
import com.jozeftvrdy.solver.core.ui.components.BackButton
import com.jozeftvrdy.solver.core.ui.extensions.Spacer
import com.jozeftvrdy.solver.gridpuzzle.R
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit
) {
    val vm = koinViewModel<SettingsViewModel>()
    val lifecycleOwner = LocalLifecycleOwner.current

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val updateDataFailedError = stringResource(R.string.settings_onetime_error_update_data)
    val handwrittenRecognizerModelState = vm.handwrittenRecognizerModelFlow.collectAsStateWithLifecycle()
    val sudokuImageProcessingTypeFlowState = vm.sudokuImageProcessingTypeFlow.collectAsStateWithLifecycle()

    LaunchedEffect(vm.oneTimeErrorFlow, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.oneTimeErrorFlow.collect { error ->
                when (error) {
                    SettingsUiError.OneTimeError.UpdateDataFailedError -> {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = updateDataFailedError
                            )
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.main_menu_item_settings)
                    )
                },
                navigationIcon = {
                    BackButton(
                        onBackClick
                    )
                },
            )
        },
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(scaffoldPadding)
        ) {
            SudokuSection(
                getHandwrittenRecognizerModelState = { handwrittenRecognizerModelState.value },
                onHandwrittenRecognizerModelChange = vm::onHandwrittenRecognizerModelChange,
                getSudokuImageProcessingTypeFlowState = { sudokuImageProcessingTypeFlowState.value },
                onSudokuImageProcessingTypeChange = vm::onImageProcessingTypeChange
            )

            SectionDivider()
        }
    }
}

@Composable
fun SudokuSection(
    getSudokuImageProcessingTypeFlowState: () -> SudokuImageProcessingType?,
    onSudokuImageProcessingTypeChange: (SudokuImageProcessingType) -> Unit,
    getHandwrittenRecognizerModelState: () -> HandwrittenRecognizerModel?,
    onHandwrittenRecognizerModelChange: (HandwrittenRecognizerModel) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        SudokuImageProcessingTypeSection(
            getSudokuImageProcessingTypeFlowState = getSudokuImageProcessingTypeFlowState,
            onSudokuImageProcessingTypeChange = onSudokuImageProcessingTypeChange,
        )

        ValueDivider()

        SudokuHandwrittenRecognizerModelSection(
            getHandwrittenRecognizerModelState = getHandwrittenRecognizerModelState,
            onHandwrittenRecognizerModelChange = onHandwrittenRecognizerModelChange,
        )
    }
}

@Composable
fun SudokuImageProcessingTypeSection(
    getSudokuImageProcessingTypeFlowState: () -> SudokuImageProcessingType?,
    onSudokuImageProcessingTypeChange: (SudokuImageProcessingType) -> Unit,
) {
    val entries = SudokuImageProcessingType.entries

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.image_processing_type_description)
        )

        Spacer(16)

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .zIndex(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                repeat(entries.size) { index ->
                    val entry = entries[index]
                    SettingsRadioButton(
                        entry,
                        isSelected = {
                            it == getSudokuImageProcessingTypeFlowState()
                        },
                        isEnabled = {
                            getSudokuImageProcessingTypeFlowState() != null
                        },
                        onSelected = onSudokuImageProcessingTypeChange,
                        text = {
                            stringResource(it.toUserTextId())
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = if (getSudokuImageProcessingTypeFlowState() == null) 1f else 0f
                    }
                    .matchParentSize()
                    .background(color = MaterialTheme.colorScheme.background.copy(alpha = 0.33f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

fun SudokuImageProcessingType.toUserTextId(): Int = when (this) {
    SudokuImageProcessingType.FastestLeastReliable -> R.string.image_processing_type_fastest
    SudokuImageProcessingType.Average -> R.string.image_processing_type_average
    SudokuImageProcessingType.SlowestMostReliable -> R.string.image_processing_type_most_precise
}

@Composable
fun SudokuHandwrittenRecognizerModelSection(
    getHandwrittenRecognizerModelState: () -> HandwrittenRecognizerModel?,
    onHandwrittenRecognizerModelChange: (HandwrittenRecognizerModel) -> Unit,
) {
    val entries = HandwrittenRecognizerModel.entries

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.handwritten_recognizer_model_description)
        )

        Spacer(16)

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .zIndex(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                repeat(entries.size) { index ->
                    val entry = entries[index]
                    SettingsRadioButton(
                        entry,
                        isSelected = {
                            it == getHandwrittenRecognizerModelState()
                        },
                        isEnabled = {
                            getHandwrittenRecognizerModelState() != null
                        },
                        onSelected = onHandwrittenRecognizerModelChange,
                        text = {
                            stringResource(it.toUserTextId())
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = if (getHandwrittenRecognizerModelState() == null) 1f else 0f
                    }
                    .matchParentSize()
                    .background(color = MaterialTheme.colorScheme.background.copy(alpha = 0.33f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

fun HandwrittenRecognizerModel.toUserTextId(): Int = when (this) {
    HandwrittenRecognizerModel.Model1 -> R.string.handwritten_recognizer_model_1
    HandwrittenRecognizerModel.Model2 -> R.string.handwritten_recognizer_model_2
    HandwrittenRecognizerModel.Model3 -> R.string.handwritten_recognizer_model_3
    HandwrittenRecognizerModel.Model4 -> R.string.handwritten_recognizer_model_4
}

@Composable
private fun <T> SettingsRadioButton(
    value: T,
    isSelected: (T) -> Boolean,
    isEnabled: (T) -> Boolean,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    text: @Composable (T) -> String? = { null }
) {

    val isSelectedLocal = isSelected(value)
    val isEnabledLocal = isEnabled(value)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(1f)
            .clickable(
                enabled = isEnabledLocal,
                role = Role.RadioButton,
                onClick = {
                    onSelected(value)
                }
            )
            .padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RadioButton(
            selected = isSelectedLocal,
            onClick = null,
            enabled = isEnabledLocal,
        )

        text(value)?.let { text ->
            Spacer(8)

            Text(
                text,
                modifier = Modifier.alpha(if (isEnabledLocal) 1f else 0.66f),
                fontWeight = FontWeight.SemiBold,
                color = if (isSelectedLocal) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Unspecified
                },
                textAlign = TextAlign.Center,

                )
        }

    }
}

@Composable
fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 32.dp),
        thickness = 2.dp,
    )
}

@Composable
fun ValueDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 24.dp),
        thickness = 1.dp,
    )
}
