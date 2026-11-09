package org.mobile.growfy.core.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import org.mobile.growfy.core.ui.ErrorKind

/** Spot illustrations for [org.mobile.growfy.core.ui.components.ErrorState], drawn in the TreeVook palette. */
object Illustrations {
    fun forKind(kind: ErrorKind): ImageVector = when (kind) {
        ErrorKind.Offline -> Offline
        ErrorKind.NotAvailable -> ComingSoon
        ErrorKind.SessionEnded -> Locked
        ErrorKind.Generic -> Broken
    }

    /** Cloud with a cut connection. */
    val Offline by lazy {
        illustration("Offline") {
            backdrop()
            fill(blob(30f, 92f, 6f), Mint)
            fill(blob(96f, 30f, 4f), Mint)
            path("M38 82h46a14 14 0 0 0 1.6-27.9A21 21 0 0 0 45.5 50 16 16 0 0 0 38 82z", fill = Color.White, stroke = Green)
            path("M50 66h8M64 66h8", stroke = SoftLine)
            path("M30 30l60 60", stroke = Red, width = 4f)
        }
    }

    /** A page under construction with a clock badge: the endpoint isn't deployed yet. */
    val ComingSoon by lazy {
        illustration("ComingSoon") {
            backdrop()
            fill(blob(28f, 34f, 5f), Mint)
            fill(blob(98f, 88f, 4f), Mint)
            path(rect(34f, 24f, 46f, 62f, 8f), fill = Color.White, stroke = Green)
            path("M44 40h26M44 50h18M44 60h22", stroke = SoftLine)
            path(circle(80f, 82f, 15f), fill = Amber, stroke = AmberDark)
            path("M80 74v8.5l5.5 4", stroke = AmberDark)
        }
    }

    /** Padlock: sign in again. */
    val Locked by lazy {
        illustration("Locked") {
            backdrop()
            fill(blob(30f, 30f, 5f), Mint)
            fill(blob(94f, 92f, 4f), Mint)
            path("M46 54V44a14 14 0 0 1 28 0v10", stroke = Green, width = 4f)
            path(rect(38f, 54f, 44f, 34f, 8f), fill = Color.White, stroke = Green)
            path(circle(60f, 68f, 4f), fill = Green)
            path("M60 72v6", stroke = Green, width = 4f)
        }
    }

    /** A torn card with an alert badge. */
    val Broken by lazy {
        illustration("Broken") {
            backdrop()
            fill(blob(26f, 88f, 5f), Mint)
            fill(blob(96f, 32f, 4f), Mint)
            path("M34 30a6 6 0 0 1 6-6h34a6 6 0 0 1 6 6v20l-8 6 6 6-6 6 8 6v6a6 6 0 0 1-6 6H40a6 6 0 0 1-6-6z", fill = Color.White, stroke = Green)
            path("M44 40h22M44 50h14", stroke = SoftLine)
            path(circle(82f, 82f, 15f), fill = Pink, stroke = Red)
            path("M82 74v9", stroke = Red, width = 4f)
            path(circle(82f, 89f, 1.8f), fill = Red)
        }
    }

    private val Backdrop = Color(0xFFEAF0EC)
    private val Mint = Color(0xFFBDEFD6)
    private val Green = Color(0xFF0F7A55)
    private val SoftLine = Color(0xFFB8D6C7)
    private val Red = Color(0xFFB3261E)
    private val Pink = Color(0xFFFFDAD6)
    private val Amber = Color(0xFFFFDDB3)
    private val AmberDark = Color(0xFFB26B00)

    private fun ImageVector.Builder.backdrop() = fill(circle(60f, 60f, 54f), Backdrop)
}

private fun illustration(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(name, 160.dp, 160.dp, 120f, 120f).apply(block).build()

private fun ImageVector.Builder.fill(path: String, color: Color) {
    addPath(addPathNodes(path), fill = SolidColor(color))
}

private fun ImageVector.Builder.path(d: String, fill: Color? = null, stroke: Color? = null, width: Float = 3f) {
    addPath(
        addPathNodes(d),
        fill = fill?.let(::SolidColor),
        stroke = stroke?.let(::SolidColor),
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    )
}

private fun blob(cx: Float, cy: Float, r: Float) = circle(cx, cy, r)

private fun circle(cx: Float, cy: Float, r: Float) =
    "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z"

private fun rect(x: Float, y: Float, w: Float, h: Float, r: Float): String {
    val iw = w - 2 * r
    val ih = h - 2 * r
    return "M${x + r} ${y}h${iw}a$r $r 0 0 1 $r ${r}v${ih}a$r $r 0 0 1 ${-r} ${r}" +
        "h${-iw}a$r $r 0 0 1 ${-r} ${-r}v${-ih}a$r $r 0 0 1 $r ${-r}z"
}
