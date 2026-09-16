package com.jozeftvrdy.solver.sudoku.presentation.screen.uiModels

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.jozeftvrdy.solver.sudoku.R

enum class SolvedSudokuIterationButtonType(
    private val drawableResId: Int,
    private val descriptionResId: Int,
) {


    First(R.drawable.ic_skip_next_24,R.string.iteration_button_first_description),
    Previous(R.drawable.ic_play_arrow_24,R.string.iteration_button_previous_description),

    Reason(R.drawable.ic_question_mark_24, R.string.iteration_button_reason_description),

    Next(R.drawable.ic_play_arrow_24,R.string.iteration_button_next_description),
    Last(R.drawable.ic_skip_next_24,R.string.iteration_button_last_description);

    @Composable
    fun getDescription(): String = stringResource(descriptionResId)

    @Composable
    fun getDrawable(): Painter = painterResource(drawableResId)

    val modifier: Modifier
        get() = when (this) {
        First,
        Previous -> Modifier.graphicsLayer(
            rotationY = 180f
        )
        Next,
        Last,
        Reason,
             -> Modifier
    }
}