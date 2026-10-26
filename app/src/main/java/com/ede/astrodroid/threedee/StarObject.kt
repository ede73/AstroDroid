package com.ede.astrodroid.threedee

import android.opengl.GLES30

/**
 * Distant stars outside our solar system (probably in our galaxy though)
 * (point cloud)
 */
class StarObject(
    private val mStarCoordinates: FloatArray,
    private val mStarColors: FloatArray,
    private val pointSize: Float = 12f,
) : ThreeDObject() {
    private val mDrawOrder =
        (0 until (mStarCoordinates.size / 3)).map { it.toShort() }.toShortArray()

    override fun getVertexShader(): String {
        return """
            uniform mat4 u_MVPMatrix; // constant

            attribute vec4 a_Position; // IN: per vertex position
            attribute vec4 a_Color; // IN: per vertex position

            varying vec4 v_Color;

            void main() {
              // Stars are directions at infinity (w=0), never nearby points.
              vec4 clipPosition = u_MVPMatrix * vec4(a_Position.xyz, 0.0);
              // A finite perspective matrix maps w=0 directions just beyond
              // its far clip plane. Depth is irrelevant for the star sphere.
              clipPosition.z = 0.0;
              gl_Position = clipPosition;
              gl_PointSize = $pointSize;
              v_Color = a_Color;
            }
            """.trimIndent()
    }

    init {
        prepareArrays(mStarCoordinates, mDrawOrder, mStarColors)
    }

    override fun draw(mvpMatrix: FloatArray?) {
        beginDraw(mvpMatrix) {
            GLES30.glDrawElements(
                GLES30.GL_POINTS, mDrawOrder.size,
                GLES30.GL_UNSIGNED_SHORT, mDrawListBuffer.mBuffer
            )
        }
    }
}
