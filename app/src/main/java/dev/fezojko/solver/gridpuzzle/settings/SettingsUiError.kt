package dev.fezojko.solver.gridpuzzle.settings

sealed class SettingsUiError {
    sealed class OneTimeError: SettingsUiError() {
        data object UpdateDataFailedError: OneTimeError()
    }
}