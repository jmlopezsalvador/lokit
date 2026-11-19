package com.lokit.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Lo que depende del sistema se declara aquí (`expect`) y se implementa en androidMain / iosMain.
 */

/** Modo inmersivo (oculta barras del sistema). */
@Composable
expect fun SystemBarsEffect()

/** Decodifica una imagen (PNG/JPEG...) a bitmap de Compose. Devuelve null si no se puede. */
expect fun decodeImage(bytes: ByteArray): ImageBitmap?

/** Devuelve una función que hace vibrar el móvil: `vibrar(1)` una vibración corta, `vibrar(2)` dos seguidas. */
@Composable
expect fun rememberVibrator(): (Int) -> Unit

/** Devuelve una función que abre la galería; al elegir una imagen llama a `onPicked` con sus bytes. */
@Composable
expect fun rememberImagePicker(onPicked: (ByteArray) -> Unit): () -> Unit
