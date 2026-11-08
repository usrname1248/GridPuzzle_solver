package com.jozeftvrdy.solver.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.jozeftvrdy.solver.core.ui.R

private const val backButtonRawSize = 40

@Composable
fun BackButton(
    onClick: () -> Unit
) {
    Box (
        modifier = Modifier
            .size(backButtonRawSize.dp)
            .clickable(onClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier,
            painter = painterResource(R.drawable.outline_arrow_back_24),
            contentDescription = stringResource(R.string.content_description_back_button),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}
