package com.jozeftvrdy.solver.gridpuzzle.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jozeftvrdy.solver.gridpuzzle.R
import kotlinx.serialization.Serializable

sealed interface  MainScreenItem: MainNavigationDestination {

    val label: String
        @Composable get

    val iconRes: Int

    @Serializable
    object Sudoku:  MainScreenItem {
        override val label: String
            @Composable get() = stringResource(R.string.grid_puzzle_sudoku)

        override val iconRes: Int
            get() = R.drawable.ic_sudoku
    }
    @Serializable
    object Other:  MainScreenItem {
        override val label: String
            @Composable get() = stringResource(R.string.bottom_nav_item_other)

        override val iconRes: Int
            get() = R.drawable.outline_menu_24
    }

    companion object {
        val mainMenuItems: List<MainScreenItem> = listOf(
            Sudoku, Other
        )
    }
}

@Serializable
object MainScreenDestination: MainNavigationDestination

interface MainNavigationDestination {}