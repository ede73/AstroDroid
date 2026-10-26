package com.ede.astrodroid.angular

import com.ede.astrodroid.time.MINUTES_IN_HOUR
import com.ede.astrodroid.time.SECONDS_IN_HOUR
import com.ede.astrodroid.time.SECONDS_IN_MINUTE
import kotlin.math.absoluteValue
import java.util.Locale

fun degreeToRadian(degree: Double): Double {
    return degree * (Math.PI / 180.0)
}

fun radianToDegree(radian: Double): Double {
    return radian * (180.0 / Math.PI)
}

fun dmsToDegree(arcHour: Int, arcMinute: Int, arcSecond: Double): Double {
    return arcHour + arcMinute / MINUTES_IN_HOUR + arcSecond / SECONDS_IN_HOUR
}

fun degreeToDMS(
    degree: Double,
    angleOrHourMarker: String = "°",
    minuteMarker: String = "'",
    secondMarker: String = "″",
): String {
    return degreeToDMS(degree, angleOrHourMarker, minuteMarker, secondMarker) { whatever ->
        ""
    }
}

fun degreeToDMS(
    degree: Double,
    angleOrHourMarker: String = "°",
    minuteMarker: String = "'",
    secondMarker: String = "″",
    heading: (degree: Double) -> String,
): String {
    if (degree == 0.0) {
        return "0$angleOrHourMarker 0$minuteMarker 0${secondMarker}${heading.invoke(degree)}"
    }
    /**
     * Latitude: 37.386051
     * DMS Lat: 37° 23' 9.7836"N
     */
    val absoluteDegree = degree.absoluteValue
    val h = absoluteDegree.toInt()
    val decimalPart = absoluteDegree - h
    val minutes = decimalPart * MINUTES_IN_HOUR
    val m = minutes.toInt()
    val s = (minutes - m) * SECONDS_IN_MINUTE
    return String.format(
        Locale.ROOT,
        "%d$angleOrHourMarker %d$minuteMarker %f${secondMarker}%s",
        h,
        m,
        s,
        heading.invoke(degree)
    )
}
