package com.ede.astrodroid.threedee.renderer

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import com.ede.astrodroid.threedee.PointerObject
import com.ede.astrodroid.threedee.skyViewMatrix
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class PointerRenderer(
    private val getDeviceToWorld: () -> FloatArray,
) : GLSurfaceView.Renderer {
    private val mProjectionMatrix = FloatArray(16)
    private lateinit var mPointer: PointerObject

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        Log.e("Pointer", "onSurfaceCreated")
        GLES30.glClearColor(0.0f, 0.0f, 0.2f, 1.0f)
        mPointer = PointerObject()
    }

    override fun onDrawFrame(gl: GL10) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        val viewMatrix = skyViewMatrix(getDeviceToWorld())

        val resultMVPMatrix1 = FloatArray(16)
        Matrix.multiplyMM(resultMVPMatrix1, 0, mProjectionMatrix, 0, viewMatrix, 0)
        mPointer.draw(resultMVPMatrix1)
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height.toFloat()
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
    }
}
