package com.tvpro.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class GetHomeCatalogUseCaseTest {

    @Test
    fun testGetOfflineFallbackReturnsLocalCatalog() {
        val useCase = GetHomeCatalogUseCase()
        val fallback = useCase.getOfflineFallback()

        assertNotNull(fallback)
        assertFalse(fallback.allMovies.isEmpty())
        assertFalse(fallback.featured.isEmpty())
        assertEquals(fallback.featured.take(6).size, fallback.featured.size)
    }
}
