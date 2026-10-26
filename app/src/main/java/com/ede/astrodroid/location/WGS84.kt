package com.ede.astrodroid.location

import com.ede.astrodroid.coordinates.HMSParser

abstract class WGS84(private val position: String) :
    HMSParser(position, "°o", "′'", "″\"")