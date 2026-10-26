package com.ede.astrodroid.threedee

import android.opengl.GLES30

class PointerObject : ThreeDObject() {

    private val mColors = floatArrayOf(
        1f, 0f, 0f, 1f,
        1f, 0f, 0f, 1f,
        0f, 1f, 0f, 1f,
        0f, 1f, 0f, 1f,
        0f, 0f, 1f, 1f,
        0f, 0f, 1f, 1f,
    )
    private val mPointerCoordinates = floatArrayOf(
        0f, 0f, 0f,
        0f, 1f, 0f, // y
        0f, 0f, 0f,
        1f, 0f, 0f, // x
        0f, 0f, 0f,
        0f, 0f, 1f // z
    )
    private val mDrawOrder = shortArrayOf(0, 1, 2, 3, 4, 5)

    init {
        prepareArrays(mPointerCoordinates, mDrawOrder, mColors)
    }

    override fun draw(mvpMatrix: FloatArray?) {
        beginDraw(mvpMatrix) {
            GLES30.glDrawElements(
                GLES30.GL_LINES, mDrawOrder.size,
                GLES30.GL_UNSIGNED_SHORT, mDrawListBuffer.mBuffer
            )
            checkGlError("Failed drawing elements")
        }
    }
}