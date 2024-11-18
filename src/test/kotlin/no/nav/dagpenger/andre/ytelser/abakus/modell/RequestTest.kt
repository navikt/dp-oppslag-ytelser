package no.nav.dagpenger.andre.ytelser.abakus.modell

import io.kotest.assertions.json.shouldEqualJson
import no.nav.dagpenger.andre.ytelser.JsonMapper.defaultObjectMapper
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class RequestTest {
    @Test
    fun `skal mappes korrekt`() {
        val expectedJson =
            """
            {"ident":{"verdi":"123"},"periode":{"fom":"2022-12-01","tom":"2022-09-01"},"ytelser":["ENGANGSTØNAD","FORELDREPENGER"]}
            """.trimIndent()

        val request =
            Request(
                ident = Ident(verdi = "123"),
                periode =
                    Periode(
                        fom = LocalDate.of(2022, 12, 1),
                        tom = LocalDate.of(2022, 9, 1),
                    ),
                ytelser = listOf(Ytelser.ENGANGSTØNAD, Ytelser.FORELDREPENGER),
            )

        val result = defaultObjectMapper.writeValueAsString(request)
        result shouldEqualJson expectedJson
    }

    @Test
    fun `skal mappes LocalDate MAX korrekt`() {
        val expectedJson =
            """
            {"ident":{"verdi":"123"},"periode":{"fom":"1970-01-01","tom":"9999-12-31"},"ytelser":["ENGANGSTØNAD","FORELDREPENGER"]}
            """.trimIndent()

        val request =
            Request(
                ident = Ident(verdi = "123"),
                periode =
                    Periode(
                        fom = LocalDate.of(1970, 1, 1),
                        tom = LocalDate.of(9999, 12, 31),
                    ),
                ytelser = listOf(Ytelser.ENGANGSTØNAD, Ytelser.FORELDREPENGER),
            )

        val result = defaultObjectMapper.writeValueAsString(request)
        result shouldEqualJson expectedJson
    }
}
