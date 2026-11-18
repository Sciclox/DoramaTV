package com.tvpro.core.data.extractor

/**
 * Interfaz común para todos los extractores de servidores de streaming.
 * Permite añadir nuevos servidores de forma aislada sin modificar pantallas ni repositorios.
 */
interface VideoExtractor {
    /**
     * Nombre descriptivo del servidor (ej: "VOE", "Ok.ru", "Doodstream", "Vidhide").
     */
    val name: String

    /**
     * Prioridad de resolución en cascada (menor número = mayor prioridad de intento).
     */
    val priority: Int get() = 10

    /**
     * Determina si este extractor puede procesar la URL o dominio recibido.
     */
    fun canHandle(url: String): Boolean

    /**
     * Extrae uno o más streams directos reproducibles (HLS/MP4) a partir de la URL de incrustación (embed).
     */
    suspend fun extract(url: String, headers: Map<String, String> = emptyMap()): Result<List<ExtractedStream>>
}
