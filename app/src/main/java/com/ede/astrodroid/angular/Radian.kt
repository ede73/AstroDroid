package com.ede.astrodroid.angular

class Radian(val value: Double) {

    constructor(degree: Degree) : this(degreeToRadian(degree.value))

    fun toDegrees(): Degree {
        return Degree(this)
    }

    override fun toString(): String {
        return "$value"
    }
}