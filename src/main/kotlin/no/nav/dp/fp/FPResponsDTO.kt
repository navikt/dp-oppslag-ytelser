package no.nav.dp.fp

import no.nav.dp.fp.abakusclient.models.Kildesystem
import no.nav.dp.fp.abakusclient.models.Periode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class FPResponsDTO(
    val ytelser: List<YtelseV1DTO>? = null,
    val feil: FeilmeldingDTO? = null,
) {

    enum class FeilmeldingDTO(val melding: String) {
        UkjentFeil("Ukjent feil"),
    }

    data class YtelseV1DTO(
        val version: String,
        val aktør: String,
        val vedtattTidspunkt: LocalDateTime,
        val ytelse: YtelserOutput,
        val saksnummer: String?,
        val vedtakReferanse: String,
        val ytelseStatus: dtoStatus,
        val kildesystem: dtoKildesystem,
        val periode: dtoPeriode,
        val tilleggsopplysninger: String?,
        val anvist: List<AnvisningDTO>,
    )

    data class AnvisningDTO(
        val periode: dtoPeriode,
        val beløp: BigDecimal?,
        val dagsats: BigDecimal?,
        val utbetalingsgrad: BigDecimal?,
    )

    enum class YtelserInput {
        /** Folketrygdloven K9 ytelser.  */
        PSB, // PLEIEPENGER_SYKT_BARN,
        PPN, // PLEIEPENGER_NÆRSTÅENDE,
        OMP, // OMSORGSPENGER,
        OLP, // OPPLÆRINGSPENGER,

        /** Folketrygdloven K14 ytelser.  */
        ES, // ENGANGSTØNAD,
        FP, // FORELDREPENGER,
        SVP, // SVANGERSKAPSPENGER,

        /** Midlertidig ytelse for Selvstendig næringsdrivende og Frilansere (Anmodning 10).  */
        // FRISINN
    }

    enum class YtelserOutput {
        /** Folketrygdloven K9 ytelser.  */
        PLEIEPENGER_SYKT_BARN,
        PLEIEPENGER_NÆRSTÅENDE,
        OMSORGSPENGER,
        OPPLÆRINGSPENGER,

        /** Folketrygdloven K14 ytelser.  */
        ENGANGSTØNAD,
        FORELDREPENGER,
        SVANGERSKAPSPENGER,

        /** Midlertidig ytelse for Selvstendig næringsdrivende og Frilansere (Anmodning 10).  */
        FRISINN,
    }

    enum class dtoStatus {
        UNDER_BEHANDLING, LØPENDE, AVSLUTTET, UKJENT
    }

    enum class dtoKildesystem {
        FPSAK,
        K9SAK,
    }

    data class dtoPeriode(
        val fom: LocalDate,
        val tom: LocalDate,
    )
}
