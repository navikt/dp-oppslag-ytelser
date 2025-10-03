package no.nav.dagpenger.andre.ytelser.abakus.behovløsere

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import no.nav.dagpenger.andre.ytelser.abakus.AbakusClient
import no.nav.dagpenger.andre.ytelser.abakus.modell.Periode
import no.nav.dagpenger.andre.ytelser.abakus.modell.YtelseV1
import no.nav.dagpenger.andre.ytelser.abakus.modell.Ytelser

abstract class AbakusBehovløser(
    rapidsConnection: RapidsConnection,
    private val client: AbakusClient,
) : River.PacketListener {
    abstract val behov: String
    abstract val filtrertePåYtelse: List<Ytelser>

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    it.requireAllOrAny("@behov", listOf(behov))
                }
                validate {
                    it.forbid("@løsning")
                    it.requireKey("ident")
                    it.requireKey(behov)
                    it.interestedIn("søknadId", "@behovId", "behandlingId")
                }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        withLoggingContext(
            "behandlingId" to packet["behandlingId"].asText(),
            "behovId" to packet["@behovId"].asText(),
        ) {
            val ident = packet["ident"].asText()
            val prøvingsdato = packet[behov]["Prøvingsdato"].asLocalDate()
            val periode = Periode(fom = prøvingsdato, tom = prøvingsdato)

            val ytelser: List<YtelseV1> =
                runBlocking(MDCContext()) {
                    client.hentYtelser(ident, periode, filtrertePåYtelse)
                }

            val løsning = ytelser.any { it.ytelse in filtrertePåYtelse }
            packet["@løsning"] = mapOf(behov to løsning)

            // Ta med ufiltret respons for å sikre bedre sporing
            packet["@kilde"] =
                mapOf(
                    behov to
                        mapOf(
                            "navn" to "abakus-api",
                            "data" to ytelser,
                        ),
                )
            context.publish(packet.toJson())
        }
    }
}
