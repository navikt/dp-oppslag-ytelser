package no.nav.dagpenger.andre.ytelser.abakus.behovløsere

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.dagpenger.andre.ytelser.abakus.AbakusClient
import no.nav.dagpenger.andre.ytelser.abakus.modell.Aktør
import no.nav.dagpenger.andre.ytelser.abakus.modell.Anvisning
import no.nav.dagpenger.andre.ytelser.abakus.modell.Desimaltall
import no.nav.dagpenger.andre.ytelser.abakus.modell.Kildesystem
import no.nav.dagpenger.andre.ytelser.abakus.modell.Periode
import no.nav.dagpenger.andre.ytelser.abakus.modell.Status
import no.nav.dagpenger.andre.ytelser.abakus.modell.YtelseV1
import no.nav.dagpenger.andre.ytelser.abakus.modell.Ytelser
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class PleiengerBehovløserTest {
    private val testRapid = TestRapid()

    private val ident = "11109233444"
    private val prøvingsdato = LocalDate.of(2022, 1, 1)
    private val periode = Periode(prøvingsdato, prøvingsdato)

    private val abakusClient = mockk<AbakusClient>()

    init {
        PleiengerBehovløser(
            rapidsConnection = testRapid,
            client = abakusClient,
        )
    }

    @Test
    fun `Sjekk happy case`() {
        coEvery { abakusClient.hentYtelser(ident, periode, listOf(Ytelser.PLEIEPENGER_NÆRSTÅENDE, Ytelser.PLEIEPENGER_SYKT_BARN)) } returns
            listOf(
                mockYtelse,
            )

        testRapid.sendTestMessage(json)

        with(testRapid.inspektør) {
            size shouldBe 1
            field(0, "ident").asText() shouldBe ident
            field(0, "@løsning")["Pleienger"].asBoolean() shouldBe true
        }
    }

    private val beløp = 100
    private val sats = 50
    private val grad = 10

    private val mockYtelse =
        YtelseV1(
            version = "v1",
            aktør = Aktør(verdi = "aktørId"),
            vedtattTidspunkt = LocalDateTime.of(2022, 1, 1, 12, 0, 0, 0),
            ytelse = Ytelser.PLEIEPENGER_NÆRSTÅENDE,
            saksnummer = "sakNr",
            vedtakReferanse = "Ref",
            ytelseStatus = Status.LØPENDE,
            kildesystem = Kildesystem.FPSAK,
            periode =
                Periode(
                    fom = LocalDate.of(2022, 1, 1),
                    tom = LocalDate.of(2022, 1, 31),
                ),
            tilleggsopplysninger = "Tillegg",
            anvist =
                listOf(
                    Anvisning(
                        periode =
                            Periode(
                                fom = LocalDate.of(2022, 1, 1),
                                tom = LocalDate.of(2022, 1, 31),
                            ),
                        beløp = Desimaltall(beløp.toBigDecimal()),
                        dagsats = Desimaltall(sats.toBigDecimal()),
                        utbetalingsgrad = Desimaltall(grad.toBigDecimal()),
                    ),
                ),
        )

    // language=json
    private val json =
        """
        {
          "@event_name": "behov",
          "@behovId": "83894fc2-6e45-4534-abd1-97a441c57b2f",
          "@behov": [
            "Pleienger"
          ],
          "ident": "11109233444",
          "behandlingId": "018e9e8d-35f3-7835-9569-5c59ec0737da",
          "fagsakId": "123",
          "søknadId": "4afce924-6cb4-4ab4-a92b-fe91e24f31bf",
          "søknad_uuid": "4afce924-6cb4-4ab4-a92b-fe91e24f31bf",
          "Pleienger": {
            "Virkningsdato": "$prøvingsdato",
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
