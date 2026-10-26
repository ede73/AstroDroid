package com.ede.astrodroid.objects

data class SkyDisplayOptions(
    val showStars: Boolean = true,
    val showPlanets: Boolean = true,
    val showGalaxies: Boolean = true,
    val showNames: Boolean = true,
    /** Show objects with visual magnitude at or below this limit. */
    val magnitudeLimit: Float = 10f,
)
