package no.nav.dagpenger.andre.ytelser.abakus

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpHeaders.XCorrelationId
import io.ktor.http.contentType
import io.ktor.serialization.jackson.JacksonConverter
import no.nav.dagpenger.andre.ytelser.Configuration
import no.nav.dagpenger.andre.ytelser.JsonMapper.defaultObjectMapper
import no.nav.dagpenger.andre.ytelser.abakus.modell.Ident
import no.nav.dagpenger.andre.ytelser.abakus.modell.Periode
import no.nav.dagpenger.andre.ytelser.abakus.modell.Request
import no.nav.dagpenger.andre.ytelser.abakus.modell.YtelseV1
import no.nav.dagpenger.andre.ytelser.abakus.modell.Ytelser
import java.time.Duration
import java.time.LocalDate

class AbakusClient(
    private val baseUrl: String = Configuration.abakusUrl(),
    httpClientEngine: HttpClientEngine = CIO.create { requestTimeout = Long.MAX_VALUE },
    private val tokenProvider: () -> String,
) {
    companion object {
        const val NAV_CALL_ID_HEADER = "Nav-Call-Id"
    }

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

    suspend fun hentYtelser(
        ident: String,
        fom: LocalDate,
        tom: LocalDate,
        behovId: String,
    ): List<YtelseV1> =
        httpKlient
            .post("$baseUrl/hent-ytelse-vedtak") {
                header(NAV_CALL_ID_HEADER, behovId)
                header(XCorrelationId, behovId)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(
                    Request(
                        ident = Ident(verdi = ident),
                        periode = Periode(fom = fom, tom = tom),
                        ytelser = Ytelser.entries,
                    ),
                )
            }.body()
}
