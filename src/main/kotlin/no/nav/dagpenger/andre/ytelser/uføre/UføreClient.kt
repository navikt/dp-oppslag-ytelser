package no.nav.dagpenger.andre.ytelser.uføre

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.JacksonConverter
import no.nav.dagpenger.andre.ytelser.Configuration
import no.nav.dagpenger.andre.ytelser.JsonMapper.defaultObjectMapper
import java.time.Duration
import java.time.LocalDate

class UføreClient(
    private val baseUrl: String = Configuration.uføreUrl(),
    httpClientEngine: HttpClientEngine = CIO.create { },
    private val tokenProvider: () -> String,
) {
    private val httpKlient =
        HttpClient(httpClientEngine) {
            expectSuccess = true
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(defaultObjectMapper))
            }
            install(Logging) {
                level = LogLevel.INFO
            }
            install(HttpTimeout) {
                connectTimeoutMillis = Duration.ofSeconds(15).toMillis()
                requestTimeoutMillis = Duration.ofSeconds(15).toMillis()
                socketTimeoutMillis = Duration.ofSeconds(15).toMillis()
            }
            defaultRequest {
                header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
            }
        }

    suspend fun hentUføre(
        ident: String,
        dato: LocalDate,
    ): Uføre? =
        try {
            httpKlient
                .get(baseUrl) {
                    header("fnr", ident)
                    parameter("dato", dato)
                }.body()
        } catch (e: ClientRequestException) {
            when (e.response.status) {
                HttpStatusCode.NotFound -> null
                else -> throw e
            }
        }
}
