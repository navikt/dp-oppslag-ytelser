package no.nav.dagpenger.andre.ytelser

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import mu.KotlinLogging
import no.nav.dagpenger.andre.ytelser.abakus.AbakusClient
import no.nav.dagpenger.andre.ytelser.sykepenger.SykepengerClient
import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    val log = KotlinLogging.logger {}
    val sikkerlogg = KotlinLogging.logger("tjenestekall")

    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error { "Uncaught exception logget i securelog" }
        sikkerlogg.error(e) { e.message }
    }

    val abakusClient by lazy {
        AbakusClient(
            baseUrl = Configuration.abakusUrl(),
            tokenProvider = Configuration.abakusTokenProvider(),
        )
    }

    val sykepenger =
        SykepengerClient(
            baseUrl = Configuration.sykepengerUrl(),
            tokenProvider = Configuration.sykepengerTokenProvider(),
        )

    RapidApplication
        .create(Configuration.config)
        .apply {
//            ForeldrepengerService(
//                rapidsConnection = this,
//                client = abakusClient,
//            )

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
