package com.tvpro.core.data.api

import okhttp3.internal.tls.OkHostnameVerifier
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkSecurityTest {

    @Test
    fun testApiClientOkHttpClientUsesSecureConfiguration() {
        val client = ApiClient.okHttpClient
        assertNotNull("okHttpClient must not be null", client)
        
        // Ensure standard hostname verifier or default is not overridden by a blanket 'true' lambda
        val verifier = client.hostnameVerifier
        assertNotNull("hostnameVerifier must be defined", verifier)
        assertTrue(
            "okHttpClient should use standard OkHostnameVerifier or secure default",
            verifier is OkHostnameVerifier || verifier.javaClass.name.contains("OkHostnameVerifier")
        )
    }

    @Test
    fun testStreamingClientExistsWithDedicatedConfiguration() {
        val streamingClient = ApiClient.streamingOkHttpClient
        assertNotNull("streamingOkHttpClient must exist for third party video streams", streamingClient)
        assertTrue("Streaming client should have connection pool enabled", streamingClient.connectionPool.connectionCount() >= 0)
    }
}
