package com.nextome.test.data

import kotlinx.serialization.Serializable

@Serializable
data class NextomeErrorResponse(
    val error: String,
)

