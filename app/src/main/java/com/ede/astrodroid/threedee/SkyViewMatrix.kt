package com.ede.astrodroid.threedee

/** Converts Android's device-to-east/north/up matrix into a sky-to-camera view matrix. */
fun skyViewMatrix(deviceToWorld: FloatArray): FloatArray {
    require(deviceToWorld.size >= 9)

    // Android's matrix maps device vectors into east, north, up. Transpose it
    // to map world directions into device coordinates. The sky is on the back
    // of the phone (device -Z); in OpenGL that correctly lands in front of the
    // camera, which looks along -Z. Keep the screen's right and up axes intact.
    val view = FloatArray(16)
    view[0] = deviceToWorld[0]
    view[4] = deviceToWorld[3]
    view[8] = deviceToWorld[6]
    view[1] = deviceToWorld[1]
    view[5] = deviceToWorld[4]
    view[9] = deviceToWorld[7]
    view[2] = deviceToWorld[2]
    view[6] = deviceToWorld[5]
    view[10] = deviceToWorld[8]
    view[15] = 1f
    return view
}
