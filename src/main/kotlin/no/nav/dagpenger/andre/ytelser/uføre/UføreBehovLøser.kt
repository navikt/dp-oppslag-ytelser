package no.nav.dagpenger.andre.ytelser.uføre

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

class UføreBehovLøser(
    rapidsConnection: RapidsConnection,
    private val client: UføreClient,
) : River.PacketListener {
    val logger = KotlinLogging.logger { }

    private object Behov {
        const val UFØRE = "Uføre"
    }

    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    it.requireAllOrAny("@behov", listOf(Behov.UFØRE))
                }
                validate {
                    it.forbid("@løsning")
                    it.requireKey("ident")
                    it.requireKey(Behov.UFØRE)
                    it.interestedIn("@behovId", "behandlingId")
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
            val prøvingsdato =
                packet[Behov.UFØRE]["Prøvingsdato"].asLocalDate()

            val uføre: Uføre? =
                runBlocking(MDCContext()) {
                    client.hentUføre(ident, prøvingsdato)
                }

            val løsning = uføre?.let { it.uforegrad != null } == true
            packet["@løsning"] = mapOf(Behov.UFØRE to løsning)

            // Ta med ufiltret respons for å sikre bedre sporing
            packet["@kilde"] =
                mapOf(
                    Behov.UFØRE to
                        mapOf(
                            "navn" to "pensjon-pen",
                            "data" to uføre,
                        ),
                )

            logger.info { "løser behov '${Behov.UFØRE}' - $uføre" }
            context.publish(packet.toJson())
        }
    }
}
