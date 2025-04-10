package no.nav.dagpenger.andre.ytelser.uføre

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UføreClientTest {
    private val ident = "12345678901"
    private val testDate = LocalDate.now()
    private val baseUrl = "https://pensjon-pen-q2.dev-fss-pub.nais.io/api/uforetrygd/uforegrad"

    @Test
    fun `hent uføre der person har uføre`() {
        val uføreClient =
            createClient {
                respond(
                    """{ "uforegrad": 50 }""",
                    HttpStatusCode.OK,
                    jsonHeaders(),
                )
            }
        runBlocking { uføreClient.hentUføre(ident, testDate) }?.uforegrad shouldBe 50
    }

    @Test
    fun `hent uføre der person finnes men ikke har uføre på dato`() {
        val uføreClient =
            createClient {
                respond(
                    """{ "uforegrad": null }""",
                    HttpStatusCode.OK,
                    jsonHeaders(),
                )
            }
        runBlocking { uføreClient.hentUføre(ident, testDate) }?.uforegrad.shouldBeNull()
    }

    @Test
    fun `hent uføre der person ikke finnes`() {
        val uføreClient =
            createClient {
                respondError(HttpStatusCode.NotFound)
            }
        runBlocking { uføreClient.hentUføre(ident, testDate) }?.uforegrad.shouldBeNull()
    }

    private fun createClient(responseHandler: MockRequestHandleScope.() -> HttpResponseData): UføreClient {
        val mockEngine =
            MockEngine { request ->
                validateRequest(request)
                responseHandler()
            }
        return UføreClient(httpClientEngine = mockEngine, tokenProvider = { "token" })
    }

    private fun validateRequest(request: HttpRequestData) {
        request.url.toString() shouldBe "$baseUrl?dato=$testDate"
        request.headers["Fnr"] shouldBe ident
        request.headers[HttpHeaders.Authorization] shouldBe "Bearer token"
    }

    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
}
