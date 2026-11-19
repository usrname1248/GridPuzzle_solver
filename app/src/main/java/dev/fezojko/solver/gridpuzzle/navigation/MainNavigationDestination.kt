package dev.fezojko.solver.gridpuzzle.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.fezojko.solver.gridpuzzle.R
import kotlinx.serialization.Serializable

sealed interface  MainScreenItem: MainNavigationDestination {

    val label: String
        @Composable get

    val iconRes: Int

    @Serializable
    object Sudoku:  MainScreenItem {
        override val label: String
            @Composable get() = stringResource(R.string.main_menu_item_sudoku)

        override val iconRes: Int
            get() = R.drawable.ic_sudoku
    }
    @Serializable
    object Settings: MainScreenItem {
        override val label: String
            @Composable get() = stringResource(R.string.main_menu_item_settings)

        override val iconRes: Int
            get() = R.drawable.outline_settings_24
    }


    @Serializable
    object Other:  MainScreenItem {
        override val label: String
            @Composable get() = stringResource(R.string.main_menu_item_other)

        override val iconRes: Int
            get() = R.drawable.outline_menu_24
    }

    companion object {
        val mainMenuItems: List<MainScreenItem> = listOf(
            Sudoku, Settings, Other
        )
    }
}

@Serializable
object MainScreenDestination: MainNavigationDestination

interface MainNavigationDestination