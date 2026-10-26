package com.ede.astrodroid.angular

class Degree(value: Double) {
    val value: Double = value % 360.0

    constructor(radian: Radian) : this(radianToDegree(radian.value))

    fun toRadians(): Radian {
        return Radian(this)
    }

    override fun toString(): String {
        return "$value"
    }
}