package com.nextome.test.helper.flutter

data class FlutterPoi(
        val id: Int? = -1,
        val name: String? = "",
        val descriptions: List<Descriptions?> = listOf(),
        val x: Double? = -1.0,
        val y: Double? = -1.0,
        val map: Int? = -1
)

data class Descriptions(
        val id: Int?,
        val name: String?,
        val description: String?,
        val language: String?
)