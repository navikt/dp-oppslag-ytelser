package no.nav.dagpenger.andre.ytelser.abakusclient.models

data class Request(
    val ident: Ident,
    val periode: Periode,
    val ytelser: List<Ytelser>,
)
