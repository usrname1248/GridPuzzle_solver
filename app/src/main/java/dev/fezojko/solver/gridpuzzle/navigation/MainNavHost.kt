package dev.fezojko.solver.gridpuzzle.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.fezojko.solver.core.ui.theme.GridPuzzleSolverTheme
import dev.fezojko.solver.gridpuzzle.settings.SettingsScreen
import dev.fezojko.solver.gridpuzzle.ui.ConstructionScreen
import dev.fezojko.solver.gridpuzzle.ui.theme.MainScreen
import dev.fezojko.solver.sudoku.presentation.screen.SudokuMainScreen

@Composable
fun MainNavHost(
    navController: NavHostController = rememberNavController()
) {
    val startDestination = MainScreenDestination

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<MainScreenDestination> {
            MainScreen(
                onNavigateToMenuItem = { menuItem ->
                    navController.navigate(menuItem)
                }
            )
        }

        composable<MainScreenItem.Sudoku> {
            SudokuMainScreen {
                navController.popBackStack()
            }
        }

        composable<MainScreenItem.Settings> {
            SettingsScreen {
                navController.popBackStack()
            }
        }

        composable<MainScreenItem.Other> {
            GridPuzzleSolverTheme {
                ConstructionScreen {
                    navController.popBackStack()
                }
            }
        }
    }
}