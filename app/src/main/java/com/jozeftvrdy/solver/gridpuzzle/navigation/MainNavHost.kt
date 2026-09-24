package com.jozeftvrdy.solver.gridpuzzle.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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

        composable<MainScreenItem.Other> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Other")
            }
        }
    }
}