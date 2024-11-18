package no.nav.dagpenger.andre.ytelser.abakus.modell

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Anvisning(
    val periode: Periode,
    val beløp: Desimaltall?,
    val dagsats: Desimaltall?,
    val utbetalingsgrad: Desimaltall?,
)
