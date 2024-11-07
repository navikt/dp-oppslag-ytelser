package no.nav.dp.fp

import no.nav.dp.fp.abakusclient.models.Periode
import java.time.LocalDate

class ForeldrepengerTolker {
    companion object {
        fun tolkeData(
            vedtak: List<FPResponsDTO.YtelseV1DTO>,
            periode: FPResponsDTO.dtoPeriode,
        ): List<Saksopplysning> {
            return vedtak.filter { it.rettTilTiltakspenger }.flatMap { type ->
                tolkeForEttVilkår(
                    vedtak = vedtak,
                    periode = periode,
                    type = type,
                    vilkår = when (type) {
                        no.nav.tiltakspenger.innsending.domene.ForeldrepengerVedtak.Ytelser.PLEIEPENGER_SYKT_BARN -> no.nav.tiltakspenger.saksbehandling.domene.vilkår.Vilkår.PLEIEPENGER_SYKT_BARN
                        no.nav.tiltakspenger.innsending.domene.ForeldrepengerVedtak.Ytelser.PLEIEPENGER_NÆRSTÅENDE -> no.nav.tiltakspenger.saksbehandling.domene.vilkår.Vilkår.PLEIEPENGER_NÆRSTÅENDE
                        no.nav.tiltakspenger.innsending.domene.ForeldrepengerVedtak.Ytelser.OMSORGSPENGER -> no.nav.tiltakspenger.saksbehandling.domene.vilkår.Vilkår.OMSORGSPENGER
                        no.nav.tiltakspenger.innsending.domene.ForeldrepengerVedtak.Ytelser.OPPLÆRINGSPENGER -> no.nav.tiltakspenger.saksbehandling.domene.vilkår.Vilkår.OPPLÆRINGSPENGER
                        no.nav.tiltakspenger.innsending.domene.ForeldrepengerVedtak.Ytelser.FORELDREPENGER -> no.nav.tiltakspenger.saksbehandling.domene.vilkår.Vilkår.FORELDREPENGER
                        no.nav.tiltakspenger.innsending.domene.ForeldrepengerVedtak.Ytelser.SVANGERSKAPSPENGER -> no.nav.tiltakspenger.saksbehandling.domene.vilkår.Vilkår.SVANGERSKAPSPENGER
                        else -> throw IllegalStateException("Ukjent ytelsestype ${type.name}")
                    },
                )
            }
        }
    }
}

enum class TypeSaksopplysning {
    IKKE_INNHENTET_ENDA,
    HAR_YTELSE,
    HAR_IKKE_YTELSE,
}

data class Saksopplysning(
    val fom: LocalDate,
    val tom: LocalDate,
    val kilde: String,
    val detaljer: String,
    val typeSaksopplysning: TypeSaksopplysning,
    val saksbehandler: String? = null,
)

private fun tolkeForEttVilkår(
    vedtak: List<FPResponsDTO.YtelseV1DTO>,
    periode: Periode,
    type: ForeldrepengerVedtak.Ytelser,
    vilkår: Vilkår,
): List<Saksopplysning> {
    return vedtak
        .filter {
            Periode(
                it.periode.fra,
                it.periode.til,
            ).overlapperMed(periode)
        }
        .filter { it.ytelse == type }
        .map {
            Saksopplysning(
                fom = maxOf(periode.fra, it.periode.fra),
                tom = minOf(periode.til, it.periode.til),
                vilkår = vilkår,
                kilde = when (it.kildesystem) {
                    ForeldrepengerVedtak.Kildesystem.FPSAK -> Kilde.FPSAK
                    ForeldrepengerVedtak.Kildesystem.K9SAK -> Kilde.K9SAK
                },
                detaljer = "",
                typeSaksopplysning = TypeSaksopplysning.HAR_YTELSE,
            )
        }
        .ifEmpty {
            listOf(
                Saksopplysning(
                    fom = periode.fra,
                    tom = periode.til,
                    vilkår = vilkår,
                    kilde = type.kilde,
                    detaljer = "",
                    typeSaksopplysning = TypeSaksopplysning.HAR_IKKE_YTELSE,
                ),
            )
        }
}
