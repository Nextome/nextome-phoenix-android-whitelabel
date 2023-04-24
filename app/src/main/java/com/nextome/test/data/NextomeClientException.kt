package com.nextome.test.data

sealed class NextomeClientException: RuntimeException()
data class ClientGenericException(override val message: String) : NextomeClientException()
data class ClientUnauthorizedException(override val message: String): NextomeClientException()