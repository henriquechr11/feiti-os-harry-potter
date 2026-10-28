package com.example.harry_potter.userinterface

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.harry_potter.data.GesturePoint
import com.example.harry_potter.data.RecognitionResult
import com.example.harry_potter.viewmodel.SpellViewModel

@Composable
fun DrawScreen(viewModel: SpellViewModel, onOpenDetail: (String) -> Unit) {
    var points by remember { mutableStateOf(emptyList<GesturePoint>()) }
    var result by remember { mutableStateOf<RecognitionResult?>(null) }
    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Desenhe um gesto", style = MaterialTheme.typography.headlineSmall)
        Text("Use apenas um dedo e faça um único traço. O reconhecimento acontece quando você tira o dedo da tela.")
        GestureCanvas(
            points = points,
            onPointsChange = { points = it },
            onStrokeComplete = { result = viewModel.recognize(it) }
        )
        result?.let { recognition ->
            Text(recognition.message, color = if (recognition.spellName == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            if (recognition.spellName != null) {
                val spell = viewModel.spellForName(recognition.spellName)
                Text("${recognition.spellName} (${(recognition.score * 100).toInt()}% de semelhança)")
                if (spell != null) {
                    Button(onClick = { onOpenDetail(spell.id) }) { Text("Ver feitiço") }
                } else {
                    Text("Esse gesto pertence a um feitiço que não veio na seleção atual da API.")
                }
            }
        }
        if (points.isNotEmpty()) Button(onClick = { points = emptyList(); result = null }) { Text("Limpar desenho") }
    }
}

@Composable
fun GestureCanvas(
    points: List<GesturePoint>,
    onPointsChange: (List<GesturePoint>) -> Unit,
    onStrokeComplete: (List<GesturePoint>) -> Unit,
    modifier: Modifier = Modifier
) {
    val latestPoints by rememberUpdatedState(points)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(310.dp)
            .clipToBounds()
            .border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> onPointsChange(listOf(offset.toGesturePoint())) },
                    onDrag = { change, _ ->
                        change.consume()
                        onPointsChange(latestPoints + change.position.toGesturePoint())
                    },
                    onDragEnd = { onStrokeComplete(latestPoints) },
                    onDragCancel = { }
                )
            }
    ) {
        drawGesture(points, Color(0xFF3A2577), 8f)
    }
}

@Composable
fun GesturePreview(points: List<GesturePoint>) {
    if (points.isEmpty()) {
        Text("Ainda não há um modelo de gesto para este feitiço.")
        return
    }
    Canvas(
        modifier = Modifier
            .size(180.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
            .padding(12.dp)
    ) {
        val maxX = points.maxOf { it.x }.coerceAtLeast(1f)
        val maxY = points.maxOf { it.y }.coerceAtLeast(1f)
        val previewPoints = points.map { GesturePoint(it.x / maxX * size.width, it.y / maxY * size.height) }
        drawGesture(previewPoints, Color(0xFF3A2577), 6f)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGesture(points: List<GesturePoint>, color: Color, width: Float) {
    if (points.size < 2) return
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }
    drawPath(path, color = color, style = Stroke(width = width, cap = StrokeCap.Round))
}

private fun Offset.toGesturePoint() = GesturePoint(x, y)
