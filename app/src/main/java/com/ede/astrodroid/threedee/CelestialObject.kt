package com.ede.astrodroid.threedee

import android.graphics.Bitmap
import android.opengl.GLES30

/**
 * Objects in our solar system
 */
class CelestialObject(
    private val mXCoordinate: Float,
    private val mYCoordinate: Float,
    private val mZCoordinate: Float,
    private val mDistance: Float,
    private val mBitmap: Bitmap?,
) : ThreeDObject() {
    constructor(fa: FloatArray, distance: Float, bitmap: Bitmap?) : this(
        fa[0],
        fa[1],
        fa[2],
        distance,
        bitmap
    )

    private val mColors = floatArrayOf(
        1f, 1f, 1f, 1f,
        1f, 1f, 1f, 1f,
        1f, 1f, 1f, 1f,
        1f, 1f, 1f, 1f,
        1f, 1f, 1f, 1f,
        1f, 1f, 1f, 1f,
    )

    private val mCelestialCoordinates = floatArrayOf(
        -0.1f + mXCoordinate, 0.1f + mYCoordinate, mDistance + mZCoordinate, // top left
        -0.1f + mXCoordinate, -0.1f + mYCoordinate, mDistance + mZCoordinate, // bottom left
        0.1f + mXCoordinate, -0.1f + mYCoordinate, mDistance + mZCoordinate, // bottom right
        0.1f + mXCoordinate, 0.1f + mYCoordinate, mDistance + mZCoordinate // top right
    )
    private val mDrawOrder = shortArrayOf(0, 1, 2, 0, 2, 3) // order to draw vertices

    init {
        prepareArrays(mCelestialCoordinates, mDrawOrder, mColors)
    }

    override fun draw(mvpMatrix: FloatArray?) {
        beginDraw(mvpMatrix) {
            GLES30.glDrawElements(
                GLES30.GL_TRIANGLES, mDrawOrder.size,
                GLES30.GL_UNSIGNED_SHORT, mDrawListBuffer.mBuffer
            )
        }
    }
}