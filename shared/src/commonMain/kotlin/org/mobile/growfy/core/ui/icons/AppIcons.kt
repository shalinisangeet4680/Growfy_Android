package org.mobile.growfy.core.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Icons traced from the TreeVook design's SVGs (24-unit viewport unless noted). */
object AppIcons {
    val Search by lazy { icon("Search") { stroke(circle(11f, 11f, 6.5f) + "M16 16l4 4") } }
    val Home by lazy { icon("Home") { stroke(HOME) } }
    val HomeFilled by lazy { icon("HomeFilled") { fill(HOME) } }
    val Reviews by lazy { icon("Reviews") { stroke(REVIEWS) } }
    val ReviewsFilled by lazy { icon("ReviewsFilled") { fill(REVIEWS) } }
    val Posts by lazy { icon("Posts") { stroke(rect(4f, 4f, 16f, 16f, 2f) + "M8 9h8M8 13h8M8 17h5") } }
    val PostsFilled by lazy {
        icon("PostsFilled") { fill("M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2zm3 5v2h8V8zm0 4v2h8v-2zm0 4v2h5v-2z") }
    }
    val More by lazy { icon("More") { stroke(GRID) } }
    val MoreFilled by lazy { icon("MoreFilled") { fill(GRID) } }
    val ChevronRight by lazy { icon("ChevronRight") { fill("M10 6 8.6 7.4 13.2 12l-4.6 4.6L10 18l6-6z") } }
    val DropDown by lazy { icon("DropDown") { fill("M7 10l5 5 5-5z") } }
    val Back by lazy { icon("Back") { fill("M20 11H7.8l5.6-5.6L12 4l-8 8 8 8 1.4-1.4L7.8 13H20z") } }
    val Send by lazy { icon("Send") { fill("M3 20V4l19 8z") } }
    val Close by lazy { icon("Close") { stroke("M6 6l12 12M18 6 6 18") } }
    val Check by lazy { icon("Check") { stroke("M5 12l5 5 9-10", width = 2.4f) } }
    val Calendar by lazy { icon("Calendar") { stroke(rect(4f, 5f, 16f, 15f, 2f) + "M4 10h16M9 3v4M15 3v4") } }
    val Edit by lazy { icon("Edit") { stroke("M14 4l6 6L9 21H3v-6z") } }
    val ButtonPill by lazy { icon("ButtonPill") { stroke(rect(3f, 8f, 18f, 8f, 4f)) } }
    val Eye by lazy { icon("Eye") { stroke("M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z" + circle(12f, 12f, 3f)) } }
    val Star by lazy { icon("Star") { stroke("M12 3l2.6 5.3 5.9.9-4.3 4.1 1 5.8L12 16.4 6.8 19.1l1-5.8L3.5 9.2l5.9-.9z") } }
    val Bell by lazy { icon("Bell") { stroke("M18 8a6 6 0 1 0-12 0c0 7-3 8-3 8h18s-3-1-3-8M10 20a2 2 0 0 0 4 0") } }

    /** TreeVook mark, 16-unit viewport. */
    val Logo by lazy { icon("Logo", viewport = 16f) { stroke("M8 15V8M8 8 3.5 2.5M8 8l4.5-5.5") } }

    /** Multi-colour Google "G". Render with `tint = Color.Unspecified`. */
    val Google by lazy {
        icon("Google", viewport = 48f) {
            fill("M43.6 20H24v8h11.3C33.7 32.7 29.3 36 24 36c-6.6 0-12-5.4-12-12s5.4-12 12-12c3 0 5.8 1.1 7.9 3l5.7-5.7C34 6.1 29.3 4 24 4 12.9 4 4 12.9 4 24s8.9 20 20 20 20-8.9 20-20c0-1.3-.1-2.7-.4-4z", Color(0xFFFFC107))
            fill("m6.3 14.7 6.6 4.8C14.7 15.1 19 12 24 12c3 0 5.8 1.1 7.9 3l5.7-5.7C34 6.1 29.3 4 24 4 16.3 4 9.7 8.3 6.3 14.7z", Color(0xFFFF3D00))
            fill("M24 44c5.2 0 9.9-2 13.4-5.2l-6.2-5.2C29.2 35.1 26.7 36 24 36c-5.3 0-9.7-3.3-11.3-8l-6.5 5C9.5 39.6 16.2 44 24 44z", Color(0xFF4CAF50))
            fill("M43.6 20H24v8h11.3c-.8 2.2-2.2 4.2-4.1 5.6l6.2 5.2C37 39.2 44 34 44 24c0-1.3-.1-2.7-.4-4z", Color(0xFF1976D2))
        }
    }

    private const val HOME = "M4 10.5 12 4l8 6.5V20h-5.5v-5.5h-5V20H4z"
    private const val REVIEWS = "M4 4h16v12H7l-3 3z"
    private val GRID = listOf(4f to 4f, 13.5f to 4f, 4f to 13.5f, 13.5f to 13.5f)
        .joinToString("") { (x, y) -> rect(x, y, 6.5f, 6.5f, 1.5f) }
}

private fun icon(name: String, viewport: Float = 24f, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, viewport, viewport).apply(block).build()

private fun ImageVector.Builder.fill(path: String, color: Color = Color.Black) {
    addPath(addPathNodes(path), fill = SolidColor(color))
}

private fun ImageVector.Builder.stroke(path: String, width: Float = 2f) {
    addPath(
        addPathNodes(path),
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    )
}

private fun circle(cx: Float, cy: Float, r: Float) =
    "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z"

private fun rect(x: Float, y: Float, w: Float, h: Float, r: Float): String {
    val iw = w - 2 * r
    val ih = h - 2 * r
    return "M${x + r} ${y}h${iw}a$r $r 0 0 1 $r ${r}v${ih}a$r $r 0 0 1 ${-r} ${r}" +
        "h${-iw}a$r $r 0 0 1 ${-r} ${-r}v${-ih}a$r $r 0 0 1 $r ${-r}z"
}
