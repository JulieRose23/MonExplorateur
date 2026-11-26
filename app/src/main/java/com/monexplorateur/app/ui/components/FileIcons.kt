package com.monexplorateur.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.monexplorateur.app.utils.FileUtils
import java.io.File
import java.util.Locale

val IconeDossierModerne: ImageVector by lazy {
    ImageVector.Builder(
        name = "FolderModern",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.NonZero,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(10f, 4f)
            horizontalLineTo(4f)
            curveTo(2.9f, 4f, 2.01f, 4.9f, 2.01f, 6f)
            lineTo(2f, 18f)
            curveTo(2f, 19.1f, 2.9f, 20f, 4f, 20f)
            horizontalLineTo(20f)
            curveTo(21.1f, 20f, 22f, 19.1f, 22f, 18f)
            verticalLineTo(8f)
            curveTo(22f, 6.9f, 21.1f, 6f, 20f, 6f)
            horizontalLineTo(12f)
            lineTo(10f, 4f)
            close()
        }
    }.build()
}

val IconeArchiveModerne: ImageVector by lazy {
    ImageVector.Builder(
        name = "ArchiveModern",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(20f, 6f)
            horizontalLineTo(12f)
            lineTo(10f, 4f)
            horizontalLineTo(4f)
            curveTo(2.9f, 4f, 2f, 4.9f, 2f, 6f)
            verticalLineTo(18f)
            curveTo(2f, 19.1f, 2.9f, 20f, 4f, 20f)
            horizontalLineTo(20f)
            curveTo(21.1f, 20f, 22f, 19.1f, 22f, 18f)
            verticalLineTo(8f)
            curveTo(22f, 6.9f, 21.1f, 6f, 20f, 6f)
            close()
            moveTo(16f, 12f)
            horizontalLineTo(14f)
            verticalLineTo(14f)
            horizontalLineTo(16f)
            verticalLineTo(16f)
            horizontalLineTo(14f)
            verticalLineTo(18f)
            horizontalLineTo(12f)
            verticalLineTo(16f)
            horizontalLineTo(14f)
            verticalLineTo(14f)
            horizontalLineTo(12f)
            verticalLineTo(12f)
            horizontalLineTo(14f)
            verticalLineTo(10f)
            horizontalLineTo(12f)
            verticalLineTo(8f)
            horizontalLineTo(14f)
            verticalLineTo(10f)
            horizontalLineTo(16f)
            verticalLineTo(12f)
            close()
        }
    }.build()
}

val IconeFichierModerne: ImageVector by lazy {
    ImageVector.Builder(
        name = "FileModern",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(14f, 2f)
            horizontalLineTo(6f)
            curveTo(4.9f, 2f, 4.01f, 2.9f, 4.01f, 4f)
            lineTo(4f, 20f)
            curveTo(4f, 21.1f, 4.89f, 22f, 5.99f, 22f)
            horizontalLineTo(18f)
            curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f)
            verticalLineTo(8f)
            lineTo(14f, 2f)
            close()
            moveTo(13f, 9f)
            verticalLineTo(3.5f)
            lineTo(18.5f, 9f)
            horizontalLineTo(13f)
            close()
        }
    }.build()
}

@Composable
fun BadgeIconeElement(
    nom: String,
    estDossier: Boolean,
    estSelectionne: Boolean
) {
    val ext = File(nom).extension.lowercase(Locale.getDefault())
    val estArchive = !estDossier && FileUtils.estArchiveSupportee(nom)

    val (fond, teinte, icone) = when {
        estSelectionne -> Triple(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary,
            Icons.Default.CheckCircle
        )
        estDossier -> Triple(
            Color(0xFFFFB300).copy(alpha = 0.18f),
            Color(0xFFFFB300),
            IconeDossierModerne
        )
        estArchive -> Triple(
            Color(0xFFAB47BC).copy(alpha = 0.18f),
            Color(0xFFCE93D8),
            IconeArchiveModerne
        )
        ext == "apk" -> Triple(
            Color(0xFF66BB6A).copy(alpha = 0.18f),
            Color(0xFF81C784),
            IconeFichierModerne
        )
        ext in setOf("jpg", "jpeg", "png", "webp", "gif", "mp4", "mkv", "mp3", "flac") -> Triple(
            Color(0xFF29B6F6).copy(alpha = 0.18f),
            Color(0xFF4FC3F7),
            IconeFichierModerne
        )
        else -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            IconeFichierModerne
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(fond)
    ) {
        Icon(
            imageVector = icone,
            contentDescription = null,
            tint = teinte,
            modifier = Modifier.size(24.dp)
        )
    }
}
