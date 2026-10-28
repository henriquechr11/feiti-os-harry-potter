package com.example.harry_potter.data

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** A small local implementation of the $1 single-stroke recognizer. */
class DollarOneRecognizer(templates: List<GestureTemplate>) {
    private val normalizedTemplates = templates.map { it.copy(points = normalize(it.points)) }

    fun recognize(points: List<GesturePoint>): RecognitionResult {
        if (points.size < 8 || pathLength(points) < MIN_PATH_LENGTH) {
            return RecognitionResult(null, 0f, "Faça um traço um pouco maior.")
        }
        val candidate = normalize(points)
        val ranked = normalizedTemplates.map { template ->
            template to pathDistance(candidate, template.points)
        }.sortedBy { it.second }
        val best = ranked.firstOrNull() ?: return RecognitionResult(null, 0f, "Nenhum gesto disponível.")
        val score = (1f - best.second / HALF_DIAGONAL).coerceIn(0f, 1f)
        val margin = ranked.getOrNull(1)?.let { it.second - best.second } ?: Float.MAX_VALUE
        return if (score >= ACCEPT_SCORE && margin >= MIN_MARGIN) {
            RecognitionResult(best.first.spellName, score, "Feitiço reconhecido!")
        } else {
            RecognitionResult(null, score, "Não reconheci o gesto. Tente novamente.")
        }
    }

    private fun normalize(raw: List<GesturePoint>): List<GesturePoint> {
        val sampled = resample(raw, POINT_COUNT)
        val centroid = centroid(sampled)
        val angle = atan2(sampled.first().y - centroid.y, sampled.first().x - centroid.x)
        val rotated = rotate(sampled, -angle, centroid)
        val minX = rotated.minOf { it.x }
        val maxX = rotated.maxOf { it.x }
        val minY = rotated.minOf { it.y }
        val maxY = rotated.maxOf { it.y }
        val width = max(maxX - minX, 1f)
        val height = max(maxY - minY, 1f)
        val scaled = rotated.map { GesturePoint(it.x * SQUARE_SIZE / width, it.y * SQUARE_SIZE / height) }
        val centered = centroid(scaled)
        return scaled.map { GesturePoint(it.x - centered.x, it.y - centered.y) }
    }

    private fun resample(source: List<GesturePoint>, count: Int): List<GesturePoint> {
        val points = source.toMutableList()
        val interval = pathLength(points) / (count - 1)
        val result = mutableListOf(points.first())
        var distance = 0f
        var index = 1
        while (index < points.size && result.size < count) {
            val previous = points[index - 1]
            val current = points[index]
            val segment = distanceBetween(previous, current)
            if (distance + segment >= interval && segment > 0f) {
                val ratio = (interval - distance) / segment
                val inserted = GesturePoint(
                    previous.x + ratio * (current.x - previous.x),
                    previous.y + ratio * (current.y - previous.y)
                )
                result += inserted
                points.add(index, inserted)
                distance = 0f
                index++
            } else {
                distance += segment
                index++
            }
        }
        while (result.size < count) result += points.last()
        return result
    }

    private fun rotate(points: List<GesturePoint>, angle: Float, center: GesturePoint): List<GesturePoint> = points.map {
        val x = (it.x - center.x) * cos(angle) - (it.y - center.y) * sin(angle) + center.x
        val y = (it.x - center.x) * sin(angle) + (it.y - center.y) * cos(angle) + center.y
        GesturePoint(x, y)
    }

    private fun pathDistance(first: List<GesturePoint>, second: List<GesturePoint>): Float =
        first.zip(second).sumOf { distanceBetween(it.first, it.second).toDouble() }.toFloat() / first.size

    private fun centroid(points: List<GesturePoint>) = GesturePoint(
        points.sumOf { it.x.toDouble() }.toFloat() / points.size,
        points.sumOf { it.y.toDouble() }.toFloat() / points.size
    )

    private fun pathLength(points: List<GesturePoint>): Float =
        points.zipWithNext().sumOf { distanceBetween(it.first, it.second).toDouble() }.toFloat()

    private fun distanceBetween(first: GesturePoint, second: GesturePoint): Float = hypot(first.x - second.x, first.y - second.y)

    private companion object {
        const val POINT_COUNT = 64
        const val SQUARE_SIZE = 250f
        const val HALF_DIAGONAL = 176.78f
        const val MIN_PATH_LENGTH = 48f
        const val ACCEPT_SCORE = .70f
        const val MIN_MARGIN = 4f
    }
}
