package no.nav.dagpenger.andre.ytelser.sykepenger

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SykepengerBehovløserTest {
    private val testRapid = TestRapid()

    private val ident = "11109233444"
    private val prøvingsdato = LocalDate.of(2022, 1, 1)
    private val fom = prøvingsdato.minusWeeks(8)
    private val tom = prøvingsdato

    private val sykepengerClient = mockk<SykepengerClient>()

    init {
        SykepengerBehovløser(
            rapidsConnection = testRapid,
            client = sykepengerClient,
        )
    }

    @Test
    fun `returner true hvis det finnes sykepenger på prøvingsdato`() {
        coEvery { sykepengerClient.hentSykepenger(ident, fom, tom) } returns mockPerioderTreff

        testRapid.sendTestMessage(json)

        with(testRapid.inspektør) {
            size shouldBe 1
            field(0, "ident").asText() shouldBe ident
            field(0, "@løsning")["Sykepenger"].asBoolean() shouldBe true
            field(0, "@kilde")["Sykepenger"].shouldNotBeNull()
        }
    }

    @Test
    fun `returnerer false hvis det ikke finnes sykepenger på prøvingsdato`() {
        coEvery { sykepengerClient.hentSykepenger(ident, fom, tom) } returns mockPerioderBom

        testRapid.sendTestMessage(json)

        with(testRapid.inspektør) {
            size shouldBe 1
            field(0, "ident").asText() shouldBe ident
            field(0, "@løsning")["Sykepenger"].asBoolean() shouldBe false
        }
    }

    @Test
    fun `returnerer false hvis vi får tom liste`() {
        coEvery { sykepengerClient.hentSykepenger(ident, fom, tom) } returns mockPerioderTom

        testRapid.sendTestMessage(json)

        with(testRapid.inspektør) {
            size shouldBe 1
            field(0, "ident").asText() shouldBe ident
            field(0, "@løsning")["Sykepenger"].asBoolean() shouldBe false
        }
    }

    private val mockPerioderTreff =
        Perioder(
            utbetaltePerioder =
                listOf(
                    Periode(
                        fom = prøvingsdato.minusWeeks(4),
                        tom = prøvingsdato.plusWeeks(1),
                        grad = 50,
                    ),
                ),
        )

    private val mockPerioderBom =
        Perioder(
            utbetaltePerioder =
                listOf(
                    Periode(
                        fom = prøvingsdato.minusWeeks(4),
                        tom = prøvingsdato.minusWeeks(1),
                        grad = 50,
                    ),
                ),
        )

    private val mockPerioderTom =
        Perioder(
            utbetaltePerioder = emptyList(),
        )

    // language=json
    private val json =
        """
        {
          "@event_name": "behov",
          "@behovId": "83894fc2-6e45-4534-abd1-97a441c57b2f",
          "@behov": [
            "Sykepenger"
          ],
          "ident": "11109233444",
          "behandlingId": "018e9e8d-35f3-7835-9569-5c59ec0737da",
          "fagsakId": "123",
          "søknadId": "4afce924-6cb4-4ab4-a92b-fe91e24f31bf",
          "søknad_uuid": "4afce924-6cb4-4ab4-a92b-fe91e24f31bf",
          "Sykepenger": {
            "Prøvingsdato": "$prøvingsdato",
            "InnsendtSøknadsId": {
              "urn": "urn:soknad:4afce924-6cb4-4ab4-a92b-fe91e24f31bf"
            },
            "søknad_uuid": "4afce924-6cb4-4ab4-a92b-fe91e24f31bf"
          },
          "InnsendtSøknadsId": {
            "urn": "urn:soknad:4afce924-6cb4-4ab4-a92b-fe91e24f31bf"
          },
          "@id": "0c60ca43-f54b-4a1b-9ab3-5646024a0815",
          "@opprettet": "2024-04-02T13:23:58.789361",
          "system_read_count": 0,
          "system_participating_services": [
            {
              "id": "0c60ca43-f54b-4a1b-9ab3-5646024a0815",
              "time": "2024-04-02T13:23:58.789361"
            }
          ]
        }
        """.trimIndent()
}
