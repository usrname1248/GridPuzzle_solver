package com.jozeftvrdy.solver.gridpuzzle.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jozeftvrdy.solver.core.ui.theme.GridPuzzleSolverTheme
import com.jozeftvrdy.solver.gridpuzzle.settings.SettingsScreen
import com.jozeftvrdy.solver.gridpuzzle.ui.ConstructionScreen
import com.jozeftvrdy.solver.gridpuzzle.ui.theme.MainScreen
import com.jozeftvrdy.solver.sudoku.presentation.screen.SudokuMainScreen

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