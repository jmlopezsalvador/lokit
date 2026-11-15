package com.lokit

import androidx.compose.ui.window.ComposeUIViewController
import com.lokit.ui.DocumentsBlobStore
import com.lokit.ui.LokitRoot
import com.lokit.ui.SettingsStore
import com.lokit.ui.UserDefaultsStore
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    LokitRoot(SettingsStore(UserDefaultsStore()), DocumentsBlobStore())
}
