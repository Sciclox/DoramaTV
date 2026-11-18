package com.tvpro.core.data.extractor

import com.tvpro.core.data.model.StreamItem

/**
 * Registro y orquestador central de extractores de video.
 * Implementa el patrón Chain of Responsibility para resolver streams directos con fallback automático.
 */
object ExtractorRegistry {

    private val registeredExtractors: List<VideoExtractor> = listOf(
        VoeVideoExtractor,
        OkRuVideoExtractor,
        FilemoonVideoExtractor,
        StreamwishVideoExtractor,
        StreamtapeVideoExtractor,
        DoodVideoExtractor,
        VidhideVideoExtractor
    ).sortedBy { it.priority }

    /**
     * Retorna el primer extractor capaz de manejar la URL.
     */
    fun findExtractor(url: String): VideoExtractor? =
        registeredExtractors.firstOrNull { it.canHandle(url) }

    /**
     * Intenta extraer streams de una URL específica usando el extractor registrado correspondiente.
     */
    suspend fun extract(url: String, headers: Map<String, String> = emptyMap()): Result<List<ExtractedStream>> {
        val extractor = findExtractor(url)
            ?: return Result.failure(IllegalArgumentException("No registered extractor can handle URL: $url"))
        return extractor.extract(url, headers)
    }

    /**
     * Recibe una lista de URLs candidatas (servidores de un episodio) y las procesa en orden de prioridad.
     * Retorna los streams del primer extractor que resuelva exitosamente (Fallback Automático).
     */
    suspend fun extractFirstAvailable(candidateUrls: List<String>): Result<List<ExtractedStream>> {
        if (candidateUrls.isEmpty()) {
            return Result.failure(IllegalArgumentException("Candidate URLs list is empty"))
        }

        // Ordenar candidatos según la prioridad del extractor asociado
        val prioritizedCandidates = candidateUrls.mapNotNull { url ->
            val extractor = findExtractor(url)
            if (extractor != null) Pair(url, extractor) else null
        }.sortedBy { it.second.priority }

        val errors = mutableListOf<String>()

        for ((url, extractor) in prioritizedCandidates) {
            val result = extractor.extract(url)
            if (result.isSuccess && !result.getOrNull().isNullOrEmpty()) {
                return result
            } else {
                errors.add("${extractor.name}: ${result.exceptionOrNull()?.message ?: "empty"}")
            }
        }

        return Result.failure(
            IllegalStateException("All extractors failed for candidates. Details: ${errors.joinToString(" | ")}")
        )
    }

    /**
     * Mapea un [ExtractedStream] al modelo de dominio existente [StreamItem] para compatibilidad total con la UI.
     */
    fun ExtractedStream.toStreamItem(sourceLabel: String? = null): StreamItem {
        return StreamItem(
            type = if (format == StreamFormat.HLS) "HLS" else "MP4",
            url = url,
            source = sourceLabel ?: serverName,
            foundVia = "ExtractorRegistry (${serverName})",
            subtitleTracks = subtitles
        )
    }
}
