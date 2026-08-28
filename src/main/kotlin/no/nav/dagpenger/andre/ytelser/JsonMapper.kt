package no.nav.dagpenger.andre.ytelser

import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper

object JsonMapper {
    val defaultObjectMapper: ObjectMapper = jacksonObjectMapper()
}
