package com.ede.astrodroid.coordinates

/**
 * https://ascom-standards.org/Help/Developer/html/72A95B28-BBE2-4C7D-BC03-2D6AB324B6F7.htm
 * J2000(geocentric) -> Topocentric -> Observed
 */
class J2000(
    val rightAscension: RightAscension,
    val declination: Declination
) {
    constructor(rightAscension: String, declination: String) : this(
        RightAscension(rightAscension.replace("−", "-")),
        Declination(declination.replace("−", "-"))
    )
}