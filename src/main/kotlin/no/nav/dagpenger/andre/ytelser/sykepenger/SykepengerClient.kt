package no.nav.dagpenger.andre.ytelser.sykepenger

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.jackson.JacksonConverter
import no.nav.dagpenger.andre.ytelser.Configuration
import no.nav.dagpenger.andre.ytelser.JsonMapper.defaultObjectMapper
import no.nav.dagpenger.andre.ytelser.sykepenger.modell.Perioder
import no.nav.dagpenger.andre.ytelser.sykepenger.modell.SykpengerRequest
import java.time.Duration
import java.time.LocalDate

class SykepengerClient(
    private val baseUrl: String = Configuration.sykepengerUrl(),
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

    suspend fun hentSykepenger(
        ident: String,
        fom: LocalDate,
        tom: LocalDate,
    ): Perioder =
        httpKlient
            .post(
                "$baseUrl/utbetalte-perioder-dagpenger",
            ) {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                header(HttpHeaders.Accept, ContentType.Application.Json)
                setBody(
                    SykpengerRequest(
                        personidentifikatorer = listOf(ident),
                        fom = fom,
                        tom = tom,
                    ),
                )
            }.body<Perioder>()
}
