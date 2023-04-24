package com.nextome.test.data

import kotlinx.serialization.Serializable

@Serializable
data class NextomeSdkCredentials(
    val clientId: String,
    val clientSecret: String,
)