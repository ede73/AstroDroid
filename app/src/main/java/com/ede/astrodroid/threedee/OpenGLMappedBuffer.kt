package com.ede.astrodroid.threedee

import android.opengl.GLES30
import android.util.Log
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

sealed class OpenGLMappedNativeBuffer(
    private val glType: Int,
    private val size: Int? = null,
    private val stride: Int? = null,
    val attribute: String? = null
) {
    lateinit var mBuffer: Buffer

    var mHandle: Int? = null

    fun initializeAttribPointer(programId: Int): Int {
        require(::mBuffer.isInitialized)
        mHandle = GLES30.glGetAttribLocation(programId, attribute)
        checkGlError("Failed getting $attribute from $programId")
        GLES30.glEnableVertexAttribArray(mHandle!!)
        checkGlError("Failed enabling $attribute from $programId")
        // https://stackoverflow.com/questions/16380005/opengl-3-4-glvertexattribpointer-stride-and-offset-miscalculation
        GLES30.glVertexAttribPointer(
            mHandle!!, size!!,
            glType, false,
            stride!!, mBuffer
        )
        checkGlError("Failed putting vertex buffer $programId $attribute, $size, $stride")
        return mHandle!!
    }

    fun releaseHandle() {
        require(mHandle != null)
        GLES30.glDisableVertexAttribArray(mHandle!!)
        checkGlError("disable handle $mHandle")
        mHandle = null
    }

    private fun checkGlError(glOperation: String) {
        var error: Int
        while (GLES30.glGetError().also { error = it } != GLES30.GL_NO_ERROR) {
            Log.e("OpenGLMappedBuffer", "$glOperation: glError $error")
            throw RuntimeException("$glOperation: glError $error")
        }
    }
}

class OpenGLMappedShortBuffer(
    size: Int? = null,
    stride: Int? = null,
    attribute: String? = null,
) :
    OpenGLMappedNativeBuffer(
        GLES30.GL_SHORT, size, stride, attribute,
    ) {

    fun prepare(array: ShortArray) {
        require(array.isNotEmpty())
        mBuffer = ByteBuffer.allocateDirect(array.size * Short.SIZE_BYTES)
            .order(ByteOrder.nativeOrder()).asShortBuffer().put(array)
            .position(0)
    }
}

class OpenGLMappedFloatBuffer(
    size: Int? = null,
    stride: Int? = null,
    attribute: String? = null
) :
    OpenGLMappedNativeBuffer(
        GLES30.GL_FLOAT, size, stride, attribute,
    ) {

    fun prepare(array: FloatArray) {
        require(array.isNotEmpty())
        mBuffer = ByteBuffer.allocateDirect(array.size * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().put(array)
            .position(0)

    }
}
