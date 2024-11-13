package no.nav.dagpenger.andre.ytelser

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import mu.withLoggingContext
import net.logstash.logback.argument.StructuredArguments
import no.nav.dagpenger.andre.ytelser.FPResponsDTO.AnvisningDTO
import no.nav.dagpenger.andre.ytelser.FPResponsDTO.KildesystemDTO
import no.nav.dagpenger.andre.ytelser.FPResponsDTO.PeriodeDTO
import no.nav.dagpenger.andre.ytelser.FPResponsDTO.StatusDTO
import no.nav.dagpenger.andre.ytelser.FPResponsDTO.YtelseV1DTO
import no.nav.dagpenger.andre.ytelser.FPResponsDTO.YtelserOutput
import no.nav.dagpenger.andre.ytelser.abakus.AbakusClient
import no.nav.dagpenger.andre.ytelser.abakus.models.Anvisning
import no.nav.dagpenger.andre.ytelser.abakus.models.Kildesystem
import no.nav.dagpenger.andre.ytelser.abakus.models.Periode
import no.nav.dagpenger.andre.ytelser.abakus.models.Status
import no.nav.dagpenger.andre.ytelser.abakus.models.YtelseV1
import no.nav.dagpenger.andre.ytelser.abakus.models.Ytelser
import java.time.LocalDate

private val LOG = KotlinLogging.logger {}
private val SECURELOG = KotlinLogging.logger("tjenestekall")

class ForeldrepengerService(
    rapidsConnection: RapidsConnection,
    private val client: AbakusClient,
) : River.PacketListener {
    companion object {
        internal object BEHOV {
            const val FP_YTELSER = "fpytelser"
        }
    }

    init {
        River(rapidsConnection)
            .apply {
                validate {
                    it.demandAllOrAny("@behov", listOf(BEHOV.FP_YTELSER))
                    it.forbid("@løsning")
                    it.requireKey("@id", "@behovId")
                    it.requireKey("ident")
                    it.requireKey("fom")
                    it.requireKey("tom")
                }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        runCatching {
            loggVedInngang(packet)
            withLoggingContext(
                "id" to packet["@id"].asText(),
                "behovId" to packet["@behovId"].asText(),
            ) {
                val ident = packet["ident"].asText()
                val behovId = packet["@behovId"].asText()
                SECURELOG.debug { "mottok ident $ident" }
                val fom: String = packet["fom"].asText("1970-01-01")
                val tom: String = packet["tom"].asText("9999-12-31")

                val fomFixed =
                    try {
                        val tempFom: LocalDate = LocalDate.parse(fom)
                        if (tempFom == LocalDate.MIN) {
                            LocalDate.EPOCH
                        } else {
                            tempFom
                        }
                    } catch (e: Exception) {
                        LOG.warn("Klarte ikke å parse fom $fom", e)
                        LocalDate.EPOCH
                    }

                val tomFixed =
                    try {
                        val tempTom: LocalDate = LocalDate.parse(tom)
                        if (tempTom == LocalDate.MAX) {
                            LocalDate.of(9999, 12, 31)
                        } else {
                            tempTom
                        }
                    } catch (e: Exception) {
                        LOG.warn("Klarte ikke å parse tom $tom", e)
                        LocalDate.of(9999, 12, 31)
                    }

                val ytelser: List<YtelseV1> =
                    runBlocking(MDCContext()) {
                        client.hentYtelser(ident, fomFixed, tomFixed, behovId)
                    }

                val res =
                    ytelser
                        .filter { it.ytelse == Ytelser.PLEIEPENGER_SYKT_BARN }
                        .map { map(it) }

                packet["@løsning"] =
                    mapOf(
                        BEHOV.FP_YTELSER to res,
                    )
                loggVedUtgang(packet)
                context.publish(ident, packet.toJson())
            }
        }.onFailure {
            loggVedFeil(it, packet)
        }.getOrThrow()
    }

    data class Løsning(
        val ytelse: String,
        val periode: Periode,
        val harYtelse: Boolean,
    )

    private fun map(ytelseV1: YtelseV1) =
        Løsning(
            ytelse = ytelseV1.ytelse.name,
            periode = ytelseV1.periode,
            harYtelse = true,
        )

    private fun mapYtelseV1(ytelseV1: YtelseV1): YtelseV1DTO =
        YtelseV1DTO(
            version = ytelseV1.version,
            aktør = ytelseV1.aktør.verdi,
            vedtattTidspunkt = ytelseV1.vedtattTidspunkt,
            ytelse =
                when (ytelseV1.ytelse) {
                    Ytelser.PLEIEPENGER_SYKT_BARN -> YtelserOutput.PLEIEPENGER_SYKT_BARN
                    Ytelser.PLEIEPENGER_NÆRSTÅENDE -> YtelserOutput.PLEIEPENGER_NÆRSTÅENDE
                    Ytelser.OMSORGSPENGER -> YtelserOutput.OMSORGSPENGER
                    Ytelser.OPPLÆRINGSPENGER -> YtelserOutput.OPPLÆRINGSPENGER
                    Ytelser.ENGANGSTØNAD -> YtelserOutput.ENGANGSTØNAD
                    Ytelser.FORELDREPENGER -> YtelserOutput.FORELDREPENGER
                    Ytelser.SVANGERSKAPSPENGER -> YtelserOutput.SVANGERSKAPSPENGER
                    Ytelser.FRISINN -> YtelserOutput.FRISINN
                },
            saksnummer = ytelseV1.saksnummer,
            vedtakReferanse = ytelseV1.vedtakReferanse ?: "",
            ytelseStatus =
                when (ytelseV1.ytelseStatus) {
                    Status.UNDER_BEHANDLING -> StatusDTO.UNDER_BEHANDLING
                    Status.LØPENDE -> StatusDTO.LØPENDE
                    Status.AVSLUTTET -> StatusDTO.AVSLUTTET
                    Status.UKJENT -> StatusDTO.UKJENT
                },
            kildesystem =
                when (ytelseV1.kildesystem) {
                    Kildesystem.FPSAK -> KildesystemDTO.FPSAK
                    Kildesystem.K9SAK -> KildesystemDTO.K9SAK
                    else ->
                        when (ytelseV1.ytelse) {
                            Ytelser.PLEIEPENGER_SYKT_BARN -> KildesystemDTO.K9SAK
                            Ytelser.PLEIEPENGER_NÆRSTÅENDE -> KildesystemDTO.K9SAK
                            Ytelser.OMSORGSPENGER -> KildesystemDTO.K9SAK
                            Ytelser.OPPLÆRINGSPENGER -> KildesystemDTO.K9SAK
                            Ytelser.ENGANGSTØNAD -> KildesystemDTO.FPSAK
                            Ytelser.FORELDREPENGER -> KildesystemDTO.FPSAK
                            Ytelser.SVANGERSKAPSPENGER -> KildesystemDTO.FPSAK
                            Ytelser.FRISINN -> KildesystemDTO.FPSAK
                        }
                },
            periode =
                PeriodeDTO(
                    fom = ytelseV1.periode.fom,
                    tom = ytelseV1.periode.tom,
                ),
            tilleggsopplysninger = ytelseV1.tilleggsopplysninger,
            anvist = mapAnvist(ytelseV1.anvist),
        )

    private fun mapAnvist(anvisninger: List<Anvisning>): List<AnvisningDTO> =
        anvisninger.map { anvisning ->
            AnvisningDTO(
                periode =
                    PeriodeDTO(
                        fom = anvisning.periode.fom,
                        tom = anvisning.periode.tom,
                    ),
                beløp = anvisning.beløp?.verdi,
                dagsats = anvisning.dagsats?.verdi,
                utbetalingsgrad = anvisning.utbetalingsgrad?.verdi,
            )
        }

    fun loggVedInngang(packet: JsonMessage) {
        LOG.info(
            "løser fp-behov med {} og {}",
            StructuredArguments.keyValue("id", packet["@id"].asText()),
            StructuredArguments.keyValue("behovId", packet["@behovId"].asText()),
        )
        SECURELOG.info(
            "løser fp-behov med {} og {}",
            StructuredArguments.keyValue("id", packet["@id"].asText()),
            StructuredArguments.keyValue("behovId", packet["@behovId"].asText()),
        )
        SECURELOG.debug { "mottok melding: ${packet.toJson()}" }
    }

    private fun loggVedUtgang(packet: JsonMessage) {
        LOG.info(
            "har løst fp-behov med {} og {}",
            StructuredArguments.keyValue("id", packet["@id"].asText()),
            StructuredArguments.keyValue("behovId", packet["@behovId"].asText()),
        )
        SECURELOG.info(
            "har løst fp-behov med {} og {}",
            StructuredArguments.keyValue("id", packet["@id"].asText()),
            StructuredArguments.keyValue("behovId", packet["@behovId"].asText()),
        )
        SECURELOG.debug { "publiserer melding: ${packet.toJson()}" }
    }

    private fun loggVedFeil(
        ex: Throwable,
        packet: JsonMessage,
    ) {
        LOG.error(
            "feil ved behandling av fp-behov med {}, se securelogs for detaljer",
            StructuredArguments.keyValue("id", packet["@id"].asText()),
        )
        SECURELOG.error(
            "feil \"${ex.message}\" ved behandling av fp-behov med {} og {}",
            StructuredArguments.keyValue("id", packet["@id"].asText()),
            StructuredArguments.keyValue("packet", packet.toJson()),
            ex,
        )
    }
}
