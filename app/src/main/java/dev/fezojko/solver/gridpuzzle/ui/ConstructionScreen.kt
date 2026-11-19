package dev.fezojko.solver.gridpuzzle.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import dev.fezojko.solver.core.ui.components.BackButton
import dev.fezojko.solver.core.ui.extensions.Spacer
import dev.fezojko.solver.gridpuzzle.R

@Composable
fun ConstructionScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                navigationIcon = {
                    BackButton(onBackClick)
                },
                title = {
                    Text(
                        text = stringResource(R.string.main_menu_item_other)
                    )
                }
            )
        }
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    scaffoldPadding
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_construction_80),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )

                Spacer(24)

                Text(
                    stringResource(R.string.construction_screen_text),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}