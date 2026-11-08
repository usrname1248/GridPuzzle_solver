package com.jozeftvrdy.solver.gridpuzzle.settings

sealed class SettingsUiError {
    sealed class OneTimeError: SettingsUiError() {
        data object UpdateDataFailedError: OneTimeError()
    }
}