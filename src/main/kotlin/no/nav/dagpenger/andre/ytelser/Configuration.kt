package no.nav.dagpenger.andre.ytelser

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.oauth2.CachedOauth2Client
import no.nav.dagpenger.oauth2.OAuth2Config

object Configuration {
    private val defaultProperties =
        ConfigurationMap(
            mapOf(
                "RAPID_APP_NAME" to "dp-oppslag-ytelser",
                "KAFKA_RAPID_TOPIC" to "teamdagpenger.rapid.v1",
                "KAFKA_RESET_POLICY" to "LATEST",
                "KAFKA_CONSUMER_GROUP_ID" to "dp-oppslag-ytelser-v1",
            ),
        )

    private val devProperties =
        ConfigurationMap(
            mapOf(
                "application.profile" to Profile.DEV.toString(),
                "abakus.scope" to "api://dev-fss.teamforeldrepenger.fpabakus/.default",
                "abakus.url" to "https://fpabakus.dev-fss-pub.nais.io/fpabakus/ekstern/api/ytelse/v1",
                "sykepenger.scope" to "api://dev-gcp.tbd.spokelse/.default",
                "sykepenger.url" to "http://spokelse.tbd",
                "uføre.url" to "https://pensjon-pen-q2.dev-fss-pub.nais.io/api/uforetrygd/uforegrad",
                "uføre.scope" to "api://dev-fss.pensjon-q2.pensjon-pen-q2/.default",
            ),
        )
    private val prodProperties =
        ConfigurationMap(
            mapOf(
                "application.profile" to Profile.PROD.toString(),
                "abakus.scope" to "api://prod-fss.teamforeldrepenger.fpabakus/.default",
                "abakus.url" to "https://fpabakus.prod-fss-pub.nais.io/fpabakus/ekstern/api/ytelse/v1",
                "sykepenger.scope" to "api://prod-gcp.tbd.spokelse/.default",
                "sykepenger.url" to "http://spokelse.tbd",
                "uføre.url" to "https://pensjon-pen.prod-fss-pub.nais.io/api/uforetrygd/uforegrad",
                "uføre.scope" to "api://prod-fss.pensjondeployer.pensjon-pen/.default",
            ),
        )

    private fun config() =
        when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
            "dev-gcp" ->
                systemProperties() overriding EnvironmentVariables overriding devProperties overriding defaultProperties

            "prod-gcp" ->
                systemProperties() overriding EnvironmentVariables overriding prodProperties overriding defaultProperties

            else -> {
                systemProperties() overriding EnvironmentVariables overriding devProperties overriding defaultProperties
            }
        }

    val config: Map<String, String> =
        config().list().reversed().fold(emptyMap()) { map, pair ->
            map + pair.second
        }

    fun abakusTokenProvider(): () -> String = azureAdTokenSupplier(config()[Key("abakus.scope", stringType)])

    fun sykepengerTokenProvider(): () -> String = azureAdTokenSupplier(config()[Key("sykepenger.scope", stringType)])

    fun uføreTokenProvider(): () -> String = azureAdTokenSupplier(config()[Key("uføre.scope", stringType)])

    fun abakusUrl(): String = config()[Key("abakus.url", stringType)]

    fun sykepengerUrl(): String = config()[Key("sykepenger.url", stringType)]

    fun uføreUrl(): String = config()[Key("uføre.url", stringType)]

    private val azureAdClient: CachedOauth2Client by lazy {
        val azureAdConfig = OAuth2Config.AzureAd(config())
        CachedOauth2Client(
            tokenEndpointUrl = azureAdConfig.tokenEndpointUrl,
            authType = azureAdConfig.clientSecret(),
        )
    }

    private fun azureAdTokenSupplier(scope: String): () -> String =
        {
            runBlocking { azureAdClient.clientCredentials(scope).access_token }
                ?: throw RuntimeException("Kunne ikke hente 'access_token' fra Azure AD for scope $scope")
        }
}

enum class Profile {
    LOCAL,
    DEV,
    PROD,
}
