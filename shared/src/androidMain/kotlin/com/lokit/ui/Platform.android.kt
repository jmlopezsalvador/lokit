package com.lokit.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.File

/** Android: modo inmersivo (oculta barra de estado y de navegación; reaparecen al deslizar desde el borde). */
@Composable
actual fun SystemBarsEffect() {
    val view = LocalView.current
    SideEffect {
        var ctx: Context = view.context
        while (ctx is ContextWrapper && ctx !is Activity) ctx = ctx.baseContext
        val window = (ctx as? Activity)?.window ?: return@SideEffect
        val c = WindowCompat.getInsetsController(window, view)
        c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        c.hide(WindowInsetsCompat.Type.systemBars())
    }
}

actual fun decodeImage(bytes: ByteArray): ImageBitmap? =
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()

@Composable
actual fun rememberImagePicker(onPicked: (ByteArray) -> Unit): () -> Unit {
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) ctx.contentResolver.openInputStream(uri)?.use { onPicked(it.readBytes()) }
    }
    return { launcher.launch("image/*") }
}

class SharedPrefsStore(context: Context) : KeyValueStore {
    private val p = context.getSharedPreferences("lokit", Context.MODE_PRIVATE)
    override fun getString(key: String): String? = p.getString(key, null)
    override fun getBoolean(key: String, default: Boolean) = p.getBoolean(key, default)
    override fun getLong(key: String, default: Long) = p.getLong(key, default)
    override fun putString(key: String, value: String) = p.edit().putString(key, value).apply()
    override fun putBoolean(key: String, value: Boolean) = p.edit().putBoolean(key, value).apply()
    override fun putLong(key: String, value: Long) = p.edit().putLong(key, value).apply()
}

class FileBlobStore(context: Context) : BlobStore {
    private val dir = File(context.filesDir, "capturas").apply { mkdirs() }
    override fun read(name: String): ByteArray? = File(dir, name).takeIf { it.exists() }?.readBytes()
    override fun write(name: String, bytes: ByteArray) { File(dir, name).writeBytes(bytes) }
    override fun delete(name: String) { File(dir, name).delete() }
}
