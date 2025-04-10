package no.nav.dagpenger.andre.ytelser.sykepenger

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDate

data class Perioder(
    val utbetaltePerioder: List<Periode>,
)

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate,
    val grad: Int,
    @JsonIgnore private val range: ClosedRange<LocalDate> = fom..tom,
) {
    operator fun contains(date: LocalDate): Boolean = date in range

    override fun toString(): String = "Periode(fom=$fom, tom=$tom), grad=$grad"
}
