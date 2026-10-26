package com.ede.astrodroid.coordinates

import com.ede.astrodroid.angular.Degree
import com.ede.astrodroid.angular.Radian
import com.ede.astrodroid.angular.degreeToDMS
import com.ede.astrodroid.angular.dmsToDegree

private const val ONE_LONGITUDINAL_HOUR_IN_DEGREES = 360.0 / 24.0

class RightAscension(private val rightAscension: String) :
    HMSParser(rightAscension, "h", "m", "s") {
    val hour = hourPart
    val minute = minutePart
    val second = secondsPart
    val degree: Degree = Degree(
        dmsToDegree(hour, minute, this.second) * ONE_LONGITUDINAL_HOUR_IN_DEGREES
    )
    val radian: Radian = Radian(degree)

    init {
        require(this.degree.value >= 0) { "Max ascension is between 0 and <360 degrees, was ${this.degree}" }
        require(this.degree.value < 360) { "Max right ascension is between 0 and <360 degrees, was ${this.degree}" }
    }

    override fun toString(): String {
        return degreeToDMS(degree.value, "h", "m", "s")
    }
}