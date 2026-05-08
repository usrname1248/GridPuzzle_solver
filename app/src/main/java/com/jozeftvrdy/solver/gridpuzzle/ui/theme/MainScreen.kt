package com.jozeftvrdy.solver.gridpuzzle.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jozeftvrdy.solver.gridpuzzle.navigation.MainScreenItem
import com.jozeftvrdy.solver.gridpuzzle.ui.extension.Spacer

@Composable
fun MainScreen(
    onNavigateToMenuItem: (MainScreenItem) -> Unit,
    modifier: Modifier = Modifier,
    scaffoldPadding: PaddingValues = PaddingValues()
) {
    Column(
        modifier = modifier.padding(
            scaffoldPadding
        ).systemBarsPadding()
    ) {
        repeat(MainScreenItem.mainMenuItems.size) { index ->
            val menuItem = MainScreenItem.mainMenuItems[index]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                onClick = { onNavigateToMenuItem(menuItem) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        ,
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        modifier = Modifier.size(80.dp),
                        painter = painterResource(menuItem.iconRes),
                        contentDescription = menuItem.label
                    )
                    Spacer(12)
                    Text(
                        text = menuItem.label,
                        modifier = Modifier.weight(1f, fill = false),
                        fontWeight = FontWeight.Medium,
                        fontSize = 22.sp
                    )
                }
            }
        }
    }
}