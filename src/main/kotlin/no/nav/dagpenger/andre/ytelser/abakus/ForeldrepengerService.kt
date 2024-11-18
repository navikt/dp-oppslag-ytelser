package no.nav.dagpenger.andre.ytelser.abakus

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.andre.ytelser.abakus.modell.Periode
import no.nav.dagpenger.andre.ytelser.abakus.modell.YtelseV1
import no.nav.dagpenger.andre.ytelser.abakus.modell.Ytelser

class ForeldrepengerService(
    rapidsConnection: RapidsConnection,
    private val client: AbakusClient,
) : River.PacketListener {
    companion object {
        internal object BEHOV {
            const val FORELDREPENGER_BEHOV = "Foreldrepenger"
        }

        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall")
    }

    init {
        River(rapidsConnection)
            .apply {
                validate {
                    it.demandAllOrAny("@behov", listOf(BEHOV.FORELDREPENGER_BEHOV))
                    it.forbid("@løsning")
                    it.requireKey("ident")
                    it.requireKey(BEHOV.FORELDREPENGER_BEHOV)
                    it.interestedIn("søknadId", "@behovId", "behandlingId")
                }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        withLoggingContext(
            "behandlingId" to packet["behandlingId"].asText(),
            "behovId" to packet["@behovId"].asText(),
        ) {
            val ident = packet["ident"].asText()
            val ønsketDato = packet[BEHOV.FORELDREPENGER_BEHOV]["Virkningsdato"].asLocalDate()
            val periode = Periode(fom = ønsketDato.minusWeeks(8), tom = ønsketDato)

            val ytelser: List<YtelseV1> =
                runBlocking(MDCContext()) {
                    client.hentYtelser(ident, periode, listOf(Ytelser.FORELDREPENGER))
                }

            val løsning = ytelser.harYtelse(Ytelser.FORELDREPENGER)
            packet["@løsning"] = mapOf(BEHOV.FORELDREPENGER_BEHOV to løsning)

            // Ta med ufiltret respons fra arbeidssøkerregisteret for å sikre bedre sporing
            packet["@kilde"] =
                mapOf(
                    "navn" to "abakus-api",
                    "data" to ytelser,
                )

            logger.info { "løser behov '$BEHOV'" }
            context.publish(packet.toJson())
        }
    }

    private fun List<YtelseV1>.harYtelse(ytelse: Ytelser): Boolean = this.any { it.ytelse == ytelse }
}
