package com.prog2.frontend.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Crea la misma configuración HTTP para el engine Android y las pruebas de contrato. */
class ApiHttpClient {
    fun create(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
        expectSuccess = false
        followRedirects = false
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 10_000
            requestTimeoutMillis = 15_000
        }
    }
}
