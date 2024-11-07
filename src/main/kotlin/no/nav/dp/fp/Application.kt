package no.nav.dp.fp

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import mu.KotlinLogging
import no.nav.dp.fp.abakusclient.AbakusClient
import no.nav.dp.fp.auth.AzureTokenProvider
import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    System.setProperty("logback.configurationFile", "egenLogback.xml")
    val log = KotlinLogging.logger {}
    val securelog = KotlinLogging.logger("tjenestekall")

    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error { "Uncaught exception logget i securelog" }
        securelog.error(e) { e.message }
    }

    val tokenProviderClient = AzureTokenProvider()
    val abakusClient =
        AbakusClient(
            getToken = tokenProviderClient::getToken,
        )

    RapidApplication.create(Configuration.rapidsAndRivers).apply {
        ForeldrepengerService(
            rapidsConnection = this,
            client = abakusClient,
        )

        register(
            object : RapidsConnection.StatusListener {
                override fun onStartup(rapidsConnection: RapidsConnection) {
                    log.info { "Starter dp-oppslag-ytelser" }
                }

                override fun onShutdown(rapidsConnection: RapidsConnection) {
                    log.info { "Stopper dp-oppslag-ytelser" }
                    super.onShutdown(rapidsConnection)
                }
            },
        )
    }.start()
}
