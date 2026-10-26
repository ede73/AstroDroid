package com.ede.astrodroid.threedee

import android.opengl.GLES30
import android.util.Log

private const val COORDINATES_PER_VERTEX = 3
private const val VERTEX_STRIDE = COORDINATES_PER_VERTEX * 4

abstract class ThreeDObject {
    protected val mProgramID: Int
    protected val mDrawListBuffer = OpenGLMappedShortBuffer()
    private val mVertexBuffer = OpenGLMappedFloatBuffer(
        COORDINATES_PER_VERTEX,
        VERTEX_STRIDE,
        "a_Position"
    )
    private val mColorBuffer = OpenGLMappedFloatBuffer(4, 0, "a_Color")

    init {
        Log.e(TAG, "load vertex shader")
        val vertexShaderCode = getVertexShader()
        require(vertexShaderCode.contains("u_MVPMatrix"))
        require(vertexShaderCode.contains(mVertexBuffer.attribute!!))
        require(vertexShaderCode.contains(mColorBuffer.attribute!!))

        val vertexShader: Int = loadShader(
            GLES30.GL_VERTEX_SHADER,
            // TODO: Smelly, pass from child
            vertexShaderCode
        )
        val fragmentShaderCode = getFragmentShader()
        val fragmentShader: Int = loadShader(
            GLES30.GL_FRAGMENT_SHADER,
            // TODO: Smelly, pass from child
            fragmentShaderCode
        )
        mProgramID = GLES30.glCreateProgram()
        checkGlError("Create program")
        GLES30.glAttachShader(mProgramID, vertexShader)
        checkGlError("  Attach vertex shader")
        GLES30.glAttachShader(mProgramID, fragmentShader)
        checkGlError("  Attach fragment shader")
        GLES30.glLinkProgram(mProgramID)
        checkGlError("  Link program")
    }

    // Child implements this, but basically just calls back for boilerplate
    abstract fun draw(mvpMatrix: FloatArray?)

    protected open fun getVertexShader(): String {
        return """
            uniform mat4 u_MVPMatrix; // constant

            attribute vec4 a_Position; // IN: per vertex position
            attribute vec4 a_Color;

            varying vec4 v_Color;
            
            void main() {
              gl_Position = u_MVPMatrix * a_Position;
              v_Color = a_Color;
            }
            """.trimIndent()
    }

    protected open fun getFragmentShader(): String {
        return """
            precision mediump float;

            varying vec4 v_Color;
             
            void main() {
              gl_FragColor = v_Color;
            }
            """.trimIndent()
    }

    /**
     * Must be called before drawing, will prepare the native buffers
     */
    fun prepareArrays(
        mPointerCoordinates: FloatArray,
        mDrawOrder: ShortArray,
        mColors: FloatArray
    ) {
        mVertexBuffer.prepare(mPointerCoordinates)
        mColorBuffer.prepare(mColors)
        mDrawListBuffer.prepare(mDrawOrder)
    }

    protected fun beginDraw(mvpMatrix: FloatArray?, doDraw: () -> Unit) {
        if (mvpMatrix == null)
            return
        checkGlError("BEFORE program, ID=$mProgramID")
        GLES30.glUseProgram(mProgramID)
        checkGlError("Use program, ID=$mProgramID")

        mVertexBuffer.initializeAttribPointer(mProgramID)
        mColorBuffer.initializeAttribPointer(mProgramID)

        val lMVPMatrixHandle = GLES30.glGetUniformLocation(mProgramID, "u_MVPMatrix")
        checkGlError("Failed getting u_MVPMatrix")
        GLES30.glUniformMatrix4fv(lMVPMatrixHandle, 1, false, mvpMatrix, 0)
        checkGlError("Failed setting u_MVPMatrix")

        doDraw.invoke()

        mVertexBuffer.releaseHandle()
        mColorBuffer.releaseHandle()
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES30.glCreateShader(type)
        checkGlError("  Created shader ID=$shader")
        GLES30.glShaderSource(shader, shaderCode)
        checkGlError("  Shader source")
        GLES30.glCompileShader(shader)
        checkGlError("  Compile shader")
        val compiled = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            GLES30.glDeleteShader(shader)
            throw RuntimeException(
                "Could not compile program: "
                        + GLES30.glGetShaderInfoLog(shader) + " | " + shaderCode
            )
        }
        Log.e(TAG, "  loaded code ID  = $shader")
        Log.e(TAG, "   SourceCode  = $shaderCode")
        return shader
    }

    protected fun checkGlError(glOperation: String) {
        var error: Int
        while (GLES30.glGetError().also { error = it } != GLES30.GL_NO_ERROR) {
            Log.e(TAG, "$glOperation: glError $error")
            throw RuntimeException("$glOperation: glError $error")
        }
    }

    companion object {
        private const val TAG = "ThreeDObject"
    }
}