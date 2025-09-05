package no.nav.dagpenger.andre.ytelser.uføre

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate

@Disabled()
class UføreBehovLøserTest {
    private val testRapid = TestRapid()

    private val ident = "11109233444"
    private val prøvingsdato = LocalDate.of(2022, 1, 1)

    private val uføreClient = mockk<UføreClient>()

    init {
        UføreBehovLøser(
            rapidsConnection = testRapid,
            client = uføreClient,
        )
    }

    @Test
    fun `returner Ja hvis det finnes uføre på prøvingsdato`() {
        coEvery { uføreClient.hentUføre(ident, prøvingsdato) } returns Uføre(uforegrad = 100)

        testRapid.sendTestMessage(behov)

        with(testRapid.inspektør) {
            size shouldBe 1
            field(0, "ident").asText() shouldBe ident
            field(0, "@løsning")["Uføre"].asBoolean() shouldBe true
            field(0, "@kilde")["Uføre"].shouldNotBeNull()
        }
    }

    @Test
    fun `returner Nei hvis personen finnes men det er ikke uføre på prøvingsdato`() {
        coEvery { uføreClient.hentUføre(ident, prøvingsdato) } returns Uføre(uforegrad = null)

        testRapid.sendTestMessage(behov)

        with(testRapid.inspektør) {
            size shouldBe 1
            field(0, "ident").asText() shouldBe ident
            field(0, "@løsning")["Uføre"].asBoolean() shouldBe false
            field(0, "@kilde")["Uføre"].shouldNotBeNull()
        }
    }

    @Test
    fun `returner Nei hvis personen ikke finnes i uføre`() {
        coEvery { uføreClient.hentUføre(ident, prøvingsdato) } returns null

        testRapid.sendTestMessage(behov)

        with(testRapid.inspektør) {
            size shouldBe 1
            field(0, "ident").asText() shouldBe ident
            field(0, "@løsning")["Uføre"].asBoolean() shouldBe false
            field(0, "@kilde")["Uføre"].shouldNotBeNull()
        }
    }

    // language=JSON
    private val behov =
        """
        {
          "@event_name": "behov",
          "@behovId": "83894fc2-6e45-4534-abd1-97a441c57b2f",
          "@behov": [
            "Uføre"
          ],
          "ident": "$ident",
          "behandlingId": "018e9e8d-35f3-7835-9569-5c59ec0737da",
          "fagsakId": "123",
          "søknadId": "4afce924-6cb4-4ab4-a92b-fe91e24f31bf",
          "søknad_uuid": "4afce924-6cb4-4ab4-a92b-fe91e24f31bf",
          "Uføre": {
            "Prøvingsdato": "$prøvingsdato"
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
