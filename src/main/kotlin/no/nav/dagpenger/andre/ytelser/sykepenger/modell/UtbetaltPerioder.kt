package no.nav.dagpenger.andre.ytelser.sykepenger.modell

import java.time.LocalDate

data class Perioder(
    val utbetaltePerioder: List<Periode>,
)

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate,
    val grad: Int,
)
