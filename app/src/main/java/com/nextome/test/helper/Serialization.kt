package com.nextome.test.helper

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object NmSerialization {
    val nmSerializer = Json { ignoreUnknownKeys = true }

    inline fun<reified T> T.asJson(): String = nmSerializer.encodeToString(this)
    inline fun<reified T> String.fromJson(): T = nmSerializer.decodeFromString(this)
}