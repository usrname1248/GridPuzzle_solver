package com.jozeftvrdy.solver.core.ui.extensions

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Spacer(value: Int) {
    Spacer(modifier = Modifier.size(value.dp))
}