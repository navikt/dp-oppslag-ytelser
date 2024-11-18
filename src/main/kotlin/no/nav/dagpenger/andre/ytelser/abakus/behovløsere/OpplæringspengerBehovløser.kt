package no.nav.dagpenger.andre.ytelser.abakus.behovløsere

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.andre.ytelser.abakus.AbakusBehovløser
import no.nav.dagpenger.andre.ytelser.abakus.AbakusClient
import no.nav.dagpenger.andre.ytelser.abakus.modell.Ytelser

class OpplæringspengerBehovløser(
    rapidsConnection: RapidsConnection,
    client: AbakusClient,
) : AbakusBehovløser(rapidsConnection, client) {
    override val behov = "Opplæringspenger"
    override val filtrertePåYtelse = listOf(Ytelser.OPPLÆRINGSPENGER)
}
