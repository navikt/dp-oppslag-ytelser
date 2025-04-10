package no.nav.dagpenger.andre.ytelser.sykepenger

import java.time.LocalDate

data class SykpengerRequest(
    val personidentifikatorer: List<String>,
    val fom: LocalDate,
    val tom: LocalDate,
)
