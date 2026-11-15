package com.lokit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lokit.ui.FileBlobStore
import com.lokit.ui.LokitRoot
import com.lokit.ui.SettingsStore
import com.lokit.ui.SharedPrefsStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val store = SettingsStore(SharedPrefsStore(applicationContext))
        val blobs = FileBlobStore(applicationContext)
        setContent { LokitRoot(store, blobs) }
    }
}
