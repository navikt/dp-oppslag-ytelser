package no.nav.dagpenger.andre.ytelser.abakusclient.models

enum class Ytelser {
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
