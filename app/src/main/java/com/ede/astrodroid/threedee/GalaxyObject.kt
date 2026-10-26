package com.ede.astrodroid.threedee

import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLUtils
import javax.microedition.khronos.opengles.GL10


/**
 * Distant galaxies
 * TODO: make point flat towards viewer
 */
class GalaxyObject(
    private val mXCoordinate: Float, // TODO: Remove, use view
    private val mYACoordinate: Float, // TODO: Remove, use view
    private val mZCoordinate: Float, // TODO: Remove, use view
    private val mDistance: Float, // TODO: Remove, use view
    private val mBitmap: Bitmap,
) : ThreeDObject() {
    constructor(fa: FloatArray, distance: Float, bitmap: Bitmap) : this(
        fa[0],
        fa[1],
        fa[2],
        distance,
        bitmap
    )

    private val galaxySize = 0.7f
    private val mGalaxyCoordinates = floatArrayOf(
        -galaxySize + mXCoordinate,
        galaxySize + mYACoordinate,
        mDistance + mZCoordinate,  // top left

        -galaxySize + mXCoordinate,
        -galaxySize + mYACoordinate,
        mDistance + mZCoordinate,  // bottom left

        galaxySize + mXCoordinate,
        -galaxySize + mYACoordinate,
        mDistance + mZCoordinate,  // bottom right

        galaxySize + mXCoordinate,
        galaxySize + mYACoordinate,
        mDistance + mZCoordinate  // top right
    )
    private val mDrawOrder = shortArrayOf(0, 1, 2, 0, 2, 3) // order to draw vertices

    private val mUseTexture = true

    /** This is a handle to our texture data.  */
    private val mTextureDataHandle = IntArray(1)

    private val mTextureBuffer = OpenGLMappedFloatBuffer(2, 0, "a_TexCoordinate")
    private val mTextureCoordinates = floatArrayOf(
        0.0f, 0.0f,
        0.0f, 1.0f,
        1.0f, 1.0f,
        1.0f, 0.0f,
    )

    private val mColors = floatArrayOf(
        // Front face
        1f, 1f, 1f, 1f,
        1f, 1f, 1f, 1f,
        1f, 1f, 1f, 1f,
        1f, 1f, 1f, 1f,
        1f, 1f, 1f, 1f,
        1f, 1f, 1f, 1f,
    )

    /** Size of the cube normals data in elements.  */
//    val mNormalsBuffer = OpenGLMappedFloatBuffer(3, 0, "a_Normals")
//    private val mCubeNormals = floatArrayOf(
//        // Front face
//        0.0f, 0.0f, 1.0f,
//        0.0f, 0.0f, 1.0f,
//        0.0f, 0.0f, 1.0f,
//        0.0f, 0.0f, 1.0f,
//        0.0f, 0.0f, 1.0f,
//        0.0f, 0.0f, 1.0f,
//    )

    // TODO: Apply texture
    override fun getVertexShader(): String {
        return """
                // Vertex Shader
                uniform mat4 u_MVPMatrix; // constant
                
                attribute vec4 a_Position;      // IN: position we pass in
                attribute vec2 a_TexCoordinate; // IN: texture coordinate we pass in
                attribute vec3 a_Normal;        // IN
                attribute vec4 a_Color;        // IN
                
                varying vec3 v_Position;
                varying vec2 v_TexCoordinate;   // OUT: to fragment shader
                varying vec3 v_Normal;
                varying vec4 v_Color;
                
                void main() {
                  v_TexCoordinate = a_TexCoordinate;
                  v_Normal = vec3(u_MVPMatrix * vec4(a_Normal, 0.0));
                  v_Position = vec3(u_MVPMatrix * a_Position);
                
                  gl_Position = u_MVPMatrix * a_Position;
                  v_Color = a_Color;
                }
                """.trimIndent()
    }

    // TODO: Apply texture
    override fun getFragmentShader(): String {
        return """
                // Fragment Shader
                precision mediump float;
                
                uniform vec4 u_Color;
                uniform sampler2D u_Texture; // constant..texture
                
                varying vec3 v_Position;
                varying vec3 v_Normal;
                varying vec2 v_TexCoordinate; // Interpolated texture coordinate per fragment
                varying vec4 v_Color;
                
                void main() {
                  float diffuse = 1.0;
                  gl_FragColor =  (v_Color * diffuse * texture2D(u_Texture, v_TexCoordinate));
                }
                """.trimIndent()
    }

    private fun loadGLTexture() {
        // generate one texture pointer
        GLES30.glGenTextures(1, mTextureDataHandle, 0)
        checkGlError("gen textures")
        // ...and bind it to our array
        GLES30.glBindTexture(GL10.GL_TEXTURE_2D, mTextureDataHandle[0])
        checkGlError("bind texture")
        if (mTextureDataHandle[0] == 0) {
            throw  RuntimeException("Error loading texture.")
        }

        // create nearest filtered texture
        GLES30.glTexParameterf(
            GL10.GL_TEXTURE_2D,
            GL10.GL_TEXTURE_MIN_FILTER,
            GL10.GL_NEAREST.toFloat()
        )
        checkGlError("bind glTexParameterf min")

        GLES30.glTexParameterf(
            GL10.GL_TEXTURE_2D,
            GL10.GL_TEXTURE_MAG_FILTER,
            GL10.GL_LINEAR.toFloat()
        )
        checkGlError("bind glTexParameterf max")

        // texture wrap
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_WRAP_S,
            GLES30.GL_CLAMP_TO_EDGE
        )
        checkGlError("texture wrap1")

        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_WRAP_T,
            GLES30.GL_CLAMP_TO_EDGE
        )
        checkGlError("texture wrap2")

        // Use Android GLUtils to specify a two-dimensional texture image from our bitmap
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, mBitmap, 0)
        checkGlError("bind texImage2D")
    }

    init {
        loadGLTexture()
        mTextureBuffer.prepare(mTextureCoordinates)
        //mNormalsBuffer.prepare(mCubeNormals)
        prepareArrays(mGalaxyCoordinates, mDrawOrder, mColors)
    }

    override fun draw(mvpMatrix: FloatArray?) {
        beginDraw(mvpMatrix) {
            if (mUseTexture) {
                // Set the active texture unit to texture unit 0.
                GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
                checkGlError("GL activate texture")

                // Bind the texture to this unit.
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextureDataHandle[0])
                checkGlError("bind textyure")

                // get handle to texture..
                val textureUniformHandle = GLES30.glGetUniformLocation(mProgramID, "u_Texture")
                checkGlError("get handle to u_Texture")

                // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
                GLES30.glUniform1i(textureUniformHandle, 0)
                checkGlError("uniform0")

                mTextureBuffer.initializeAttribPointer(mProgramID)
//            val mNormalsHandle = mNormalsBuffer.initializeAttribPointer(mProgramID)
            }

            GLES30.glDrawElements(
                GLES30.GL_TRIANGLES, mDrawOrder.size,
                GLES30.GL_UNSIGNED_SHORT, mDrawListBuffer.mBuffer
            )
            checkGlError("glDrawElements")

            if (mUseTexture) {
                mTextureBuffer.releaseHandle()
                //mNormalsHandle.releaseHandle()
            }
        }
    }

    companion object {
        const val TAG = "GalaxyObject"
    }
}