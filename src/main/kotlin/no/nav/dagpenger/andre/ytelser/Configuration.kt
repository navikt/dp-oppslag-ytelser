package no.nav.dagpenger.andre.ytelser

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.getValue
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import no.nav.dp.fp.abakusclient.AbakusClient
import no.nav.dp.fp.auth.AzureTokenProvider

object Configuration {
    private val defaultProperties =
        ConfigurationMap(
            mapOf(
                "RAPID_APP_NAME" to "dp-oppslag-ytelser",
                "KAFKA_RAPID_TOPIC" to "teamdagpenger.rapid.v1",
                "KAFKA_RESET_POLICY" to "LATEST",
                "KAFKA_CONSUMER_GROUP_ID" to "dp-oppslag-ytelser-v1",
                "SERVICEUSER_TPTS_USERNAME" to System.getenv("SERVICEUSER_TPTS_USERNAME"),
                "SERVICEUSER_TPTS_PASSWORD" to System.getenv("SERVICEUSER_TPTS_PASSWORD"),
            ),
        )

    private val devProperties =
        ConfigurationMap(
            mapOf(
                "application.profile" to Profile.DEV.toString(),
                "abakusScope" to "api://dev-fss.teamforeldrepenger.fpabakus/.default",
                "abakusBaseUrl" to "http://fpabakus.teamforeldrepenger",
            ),
        )
    private val prodProperties =
        ConfigurationMap(
            mapOf(
                "application.profile" to Profile.PROD.toString(),
                "abakusScope" to "api://prod-fss.teamforeldrepenger.fpabakus/.default",
                "abakusBaseUrl" to "http://fpabakus.teamforeldrepenger",
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

    fun oauthConfig(
        scope: String = config()[Key("abakusScope", stringType)],
        clientId: String = config()[Key("AZURE_APP_CLIENT_ID", stringType)],
        clientSecret: String = config()[Key("AZURE_APP_CLIENT_SECRET", stringType)],
        wellknownUrl: String = config()[Key("AZURE_APP_WELL_KNOWN_URL", stringType)],
    ) = AzureTokenProvider.OauthConfig(
        scope = scope,
        clientId = clientId,
        clientSecret = clientSecret,
        wellknownUrl = wellknownUrl,
    )

    fun abakusClientConfig(baseUrl: String = config()[Key("abakusBaseUrl", stringType)]) =
        AbakusClient.AbakusClientConfig(baseUrl = baseUrl)
}

enum class Profile {
    LOCAL,
    DEV,
    PROD,
}
