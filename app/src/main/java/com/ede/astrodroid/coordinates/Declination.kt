package com.ede.astrodroid.coordinates

import com.ede.astrodroid.angular.Degree
import com.ede.astrodroid.angular.Radian
import com.ede.astrodroid.angular.degreeToDMS
import com.ede.astrodroid.angular.dmsToDegree

class Declination(declination: String) : HMSParser(declination, "°o", "′'", "″\"") {
    val arcHour = hourPart
    val arcMinute = minutePart
    val arcSecond = secondsPart
    // Preserve a leading minus even when the degree field is -00. Kotlin's
    // integer parser turns "-0" into zero, which otherwise loses the sign.
    private val sign = if (declination.trim().startsWith("-") || arcHour < 0) -1.0 else 1.0
    val degree: Degree = Degree(
        sign * dmsToDegree(kotlin.math.abs(arcHour), arcMinute, arcSecond)
    )
    val radian: Radian = Radian(degree)

    init {
        require(degree.value <= 90) { "Max declination must be between -90 and 90 degrees, was ${this.degree} parsed via $declination as ${this.arcHour}" }
        require(degree.value >= -90) { "Max declination must be between -90 and 90 degrees, was ${this.degree} parsed via $declination" }
    }

    override fun toString(): String {
        return degreeToDMS(degree.value)
    }
}
