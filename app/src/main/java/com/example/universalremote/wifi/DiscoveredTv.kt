package com.example.universalremote.wifi

import com.example.universalremote.model.TvBrand

data class DiscoveredTv(
    val name: String,
    val ip: String,
    val brand: TvBrand,
    val modelInfo: String = "",
    val port: Int = 8060,
    val isVerified: Boolean = true
)
