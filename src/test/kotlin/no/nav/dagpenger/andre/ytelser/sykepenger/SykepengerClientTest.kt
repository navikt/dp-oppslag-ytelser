package no.nav.dagpenger.andre.ytelser.sykepenger

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SykepengerClientTest {
    @Test
    fun hentSykepenger(): Unit =
        runBlocking {
            val mockEngine =
                MockEngine { request ->
                    request.url.toString() shouldBe
                        "https://spokelse.tbd/utbetalte-perioder-dagpenger"
                    respond(
                        // language=json
                        content =
                            """
                            { "utbetaltePerioder" : [
                                  { "fom": "2018-01-01", "tom": "2018-02-10", "grad": 100 },
                                  { "fom": "2018-03-01", "tom": "2018-03-31", "grad": 50 } 
                                ] }
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val sykpenger =
                SykepengerClient(
                    httpClientEngine = mockEngine,
                    tokenProvider = { "token" },
                )

            val perioder = sykpenger.hentSykepenger("12345678901", LocalDate.parse("2018-01-01"), LocalDate.parse("2018-03-31"))
            with(perioder.utbetaltePerioder) {
                shouldHaveSize(2)
                this[0].fom shouldBe LocalDate.parse("2018-01-01")
                this[0].tom shouldBe LocalDate.parse("2018-02-10")
                this[0].grad shouldBe 100
                this[1].fom shouldBe LocalDate.parse("2018-03-01")
                this[1].tom shouldBe LocalDate.parse("2018-03-31")
                this[1].grad shouldBe 50
            }
        }
}
