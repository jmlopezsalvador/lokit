package com.lokit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private class Section(val title: String, val paragraphs: List<String>)

/** Texto completo de las instrucciones: objetivo, funcionamiento y descripción de todos los ajustes. */
private val sections = listOf(
    Section(
        "Qué es Lokit",
        listOf(
            "Lokit es una herramienta de magia. Imita el móvil del mago —pantalla de bloqueo, teclado de PIN o pantalla de patrón, y escritorio— usando capturas de pantalla de su propio teléfono, " +
                "con zonas táctiles invisibles («cajas») colocadas encima.",
            "El espectador intenta desbloquear el móvil con un número (o un patrón). Lokit guarda en secreto lo que teclea o dibuja, " +
                "y más tarde el mago puede ver ese número o patrón en el escritorio, como si lo hubiera adivinado.",
            "Todo se guarda solo en el teléfono: no hay cuentas ni conexión a internet.",
        ),
    ),
    Section(
        "Cómo se usa, paso a paso",
        listOf(
            "1. Prepara: en Ajustes carga una captura de cada pantalla y, si quieres tapar la hora, la fecha o la rejilla original, un «Fondo limpio». Coloca las cajas y elige PIN o patrón.",
            "2. Empieza: al abrir la app aparece el bloqueo (y, si está activado, antes elige «Código» o «Patrón»). Toca la pantalla para abrir el teclado o la rejilla.",
            "3. El espectador teclea su número y pulsa OK, o dibuja su patrón. El primer intento se guarda en secreto. Los intentos anteriores al último fallan con una vibración; el último desbloquea y lleva al escritorio.",
            "4. Descubre: en el escritorio, mantén pulsada la zona secreta (por defecto, el centro). Mientras la mantienes aparece el número o el patrón guardado; al soltar desaparece.",
            "5. Reinicia: una pulsación larga en la esquina superior izquierda (o el botón «Bloquear» del escritorio, si lo activas) borra lo guardado y vuelve a la pantalla de bloqueo.",
            "Acceso a Ajustes: pulsación larga en la esquina superior derecha de cualquier pantalla. Los dos botones ocultos dan una doble vibración al activarse.",
        ),
    ),
    Section(
        "Ajustes · Ayuda",
        listOf(
            "Mostrar la guía rápida al abrir la app: si está activado, la guía de pasos con ilustraciones aparece cada vez que abres Lokit. También puedes desactivarlo en el último paso de la guía.",
            "Guía rápida: la abre en cualquier momento. Instrucciones: esta página.",
        ),
    ),
    Section(
        "Ajustes · Efecto y tipo de desbloqueo",
        listOf(
            "Estado del efecto: indica si se espera el primer intento, cuántos fallos van y cuál es el PIN o patrón guardado. Para reiniciar usa el botón oculto de reset.",
            "Preguntar al abrir la app: si está activado, al abrir Lokit aparece un aviso para elegir entre «Código (PIN)» y «Patrón». Es lo mismo que cambiar el tipo en Ajustes. Cambiar de tipo reinicia el efecto, porque lo guardado no vale para el otro tipo.",
            "Tipo de desbloqueo: PIN numérico o Patrón.",
            "Con PIN — Mínimo de dígitos para OK (1 a 20, por defecto 4): hasta que se teclean al menos esos dígitos, la tecla OK no hace nada. Máximo de dígitos (por defecto 12, nunca menor que el mínimo): a partir de ahí las teclas numéricas se ignoran.",
            "Con patrón — Rejilla (3×3, 4×4 o 5×5) y Mínimo de puntos (por defecto 4): el patrón se valida al soltar el dedo y, si tiene menos puntos del mínimo, el trazo se borra sin gastar intento. Si el trazo pasa por encima de un punto intermedio sin visitar, ese punto cuenta, como en Android.",
            "Intentos del espectador (1 a 10, por defecto 2): el primer intento se guarda; los anteriores al último fallan y el último desbloquea. Con 1, el primer intento ya desbloquea y se guarda.",
        ),
    ),
    Section(
        "Ajustes · Botones ocultos",
        listOf(
            "Pulsación para Ajustes y Pulsación para reset (0,2 a 5 segundos, por defecto 0,8): cuánto hay que mantener pulsada cada esquina. Al activarse dan una doble vibración.",
            "Posición y tamaño de las cajas: abre el editor de los dos botones ocultos. «Acceso a Ajustes» está arriba a la derecha y no se puede desactivar. «Reiniciar efecto» está arriba a la izquierda y se puede desactivar.",
        ),
    ),
    Section(
        "Ajustes · Modo atenuado",
        listOf(
            "Si lo activas, la pantalla de bloqueo arranca desenfocada y oscura, como un móvil con la pantalla apagada. Un toque la despierta; si pasan unos segundos sin tocar vuelve a apagarse, y con la pantalla despierta otro toque abre el teclado.",
            "Se apaga tras (1 a 120 segundos), Oscuridad y Desenfoque (0 a 40). El desenfoque solo se ve en Android 12 o posterior y en iOS.",
        ),
    ),
    Section(
        "Ajustes · Capturas y fondos limpios",
        listOf(
            "Hay cuatro pantallas, cada una con su propia captura y su propio fondo limpio: Pantalla de bloqueo, Teclado de PIN, Pantalla de patrón y Escritorio.",
            "Captura: la imagen de esa pantalla, que se muestra estirada a pantalla completa (haz los pantallazos con el mismo móvil que vas a usar). Quitar: la elimina.",
            "Fondo limpio: la misma captura sin la hora, la fecha, los iconos o la rejilla originales. Las máscaras y las rejillas de Lokit se apoyan en esa imagen para tapar lo que trae la captura. Sin ella, las máscaras con fondo no tapan nada. Debe tener la misma resolución y proporción que la captura.",
            "Cajas: abre el editor de las cajas de esa pantalla.",
        ),
    ),
    Section(
        "Editor de cajas",
        listOf(
            "Las cajas son zonas invisibles. Toca una para seleccionarla; arrástrala para moverla y arrastra el círculo de su esquina inferior derecha para cambiarle el tamaño. «Restablecer caja» la devuelve a su valor por defecto. «Panel arriba/abajo» y «Ocultar panel» te dejan ver mejor la captura. Las cajas opcionales se activan y desactivan con su interruptor.",
            "Pantalla de bloqueo — Abrir teclado: al tocarla, aparece el teclado de PIN o la pantalla de patrón. Por defecto ocupa toda la pantalla.",
            "Teclado de PIN — una caja por tecla (1 a 9, 0 y OK), más Borrar (opcional), Puntos del PIN y Atrás/Cancelar (opcional, vuelve al bloqueo sin reiniciar el efecto). «Mismo tamaño para todas» copia el tamaño de la tecla elegida al resto; «Rejilla por defecto» restablece todas las teclas.",
            "Pantalla de patrón — Rejilla del patrón y Atrás/Cancelar.",
            "Escritorio — Revelar PIN (la zona secreta de pulsación larga), Posición del número, Patrón revelado y Bloquear y reiniciar (opcional: vuelve al bloqueo y borra lo guardado).",
        ),
    ),
    Section(
        "Teclado de PIN: resalte y puntos",
        listOf(
            "Resalte al pulsar una tecla: Ninguno, Círculo (estilo iPhone), Píldora, Cuadrado redondeado (estilo Pixel) u Onda (estilo Android clásico / Samsung). Un interruptor elige resalte claro (para teclados oscuros) u oscuro (para teclados claros); además hay un tinte y la Intensidad. «Probar resalte» te deja tocar las teclas para verlo.",
            "Puntos del PIN: aparece un punto por cada dígito tecleado, centrados y sin huecos vacíos, y se encogen si no caben. Tamaño de los círculos y Separación. La máscara tapa los puntos o el recuadro de la captura: «Sin máscara» o «Imagen de fondo limpia».",
        ),
    ),
    Section(
        "Pantalla de patrón: rejilla y colores",
        listOf(
            "La caja «Rejilla del patrón» solo marca el centro y la zona táctil: el tamaño y la forma de la rejilla no dependen de ella. Se ajustan con sus propios controles, en % de la altura de la pantalla: Tamaño del punto, Separación horizontal, Separación vertical y Grosor de la línea. El Color de los puntos y el Color de la línea son independientes.",
            "Su máscara (por defecto, «Imagen de fondo limpia») tapa la rejilla que trae la captura, y Lokit dibuja la suya encima.",
        ),
    ),
    Section(
        "Escritorio: número y patrón revelados",
        listOf(
            "Posición del número: dónde se dibuja el PIN revelado; el texto se ajusta al tamaño de la caja. Opacidad del fondo (lo oscuro que se vuelve la pantalla mientras se ve) y Color del número.",
            "Patrón revelado: el patrón guardado se dibuja pequeño, como un cuadrado dentro de la caja, con los puntos numerados por orden de recorrido, líneas entre ellos y una flecha en cada tramo que indica la dirección. Tiene también Opacidad del fondo y Color del patrón.",
        ),
    ),
    Section(
        "Máscaras de hora y fecha",
        listOf(
            "En el editor de cualquier pantalla, «＋ Añadir máscara» coloca una máscara libre que tapa la hora o la fecha de la captura y dibuja la real. Puedes añadir y eliminar todas las que quieras.",
            "Valor (Hora o Fecha) y Formato (24 horas, 12 horas, con AM/PM, vertical en dos líneas, fecha corta, larga…). Posición del texto en la caja (izquierda, centro, derecha). Tipo de letra y grosor. Tamaño del texto: absoluto, no depende de la caja. Separación entre caracteres. Color del texto. Fondo de la máscara: transparente o imagen de fondo limpia.",
        ),
    ),
    Section(
        "Vibraciones",
        listOf(
            "Doble vibración: al abrir Ajustes y al reiniciar con los botones ocultos. Vibración simple: cuando un intento falla, tanto con PIN como con patrón.",
        ),
    ),
    Section(
        "Consejos y limitaciones",
        listOf(
            "Haz todas las capturas con el mismo móvil y resolución. Si el fondo limpio tiene otra proporción que la captura, el trozo no cuadra.",
            "Prueba el efecto completo antes de usarlo delante de público: intentos, mínimos, revelado y reinicio.",
            "Los datos (ajustes, capturas y el PIN o patrón guardado) se guardan solo en el teléfono.",
        ),
    ),
)

/** Página de instrucciones, abierta desde Ajustes. */
@Composable
fun InstructionsScreen(onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Instrucciones de Lokit", fontSize = 22.sp, color = MaterialTheme.colorScheme.onBackground)
        for (s in sections) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(s.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    for (p in s.paragraphs) Text(p, fontSize = 13.sp, color = Color(0xFFD0D5DD))
                }
            }
        }
        Button(onClick = onBack, Modifier.fillMaxWidth()) { Text("Volver a Ajustes") }
    }
}
