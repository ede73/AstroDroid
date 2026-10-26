package com.ede.astrodroid.threedee

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.opengl.GLES30
import android.opengl.GLUtils
import com.ede.astrodroid.objects.AstronomicalObjectKind
import javax.microedition.khronos.opengles.GL10

data class DeepSkySpritePoint(val position: FloatArray, val color: FloatArray)

/** Procedural, deliberately illustrative sprites for galaxies, clusters and nebulae. */
class DeepSkySpriteObject(
    private val kind: AstronomicalObjectKind,
    private val points: List<DeepSkySpritePoint>,
    private val baseSize: Float,
    private val maxZoomSize: Float,
) : ThreeDObject() {
    private val textureHandle = IntArray(1)
    private val drawOrder = ShortArray(points.size) { it.toShort() }
    private val pointSizeBuffer = OpenGLMappedFloatBuffer(1, 0, "a_PointSize")
    private val maxPointSizeBuffer = OpenGLMappedFloatBuffer(1, 0, "a_MaxPointSize")
    private var zoomFraction = 0f

    init {
        prepareArrays(
            points.flatMap { it.position.asIterable() }.toFloatArray(),
            drawOrder,
            points.flatMap { it.color.asIterable() }.toFloatArray()
        )
        pointSizeBuffer.prepare(FloatArray(points.size) { baseSize })
        maxPointSizeBuffer.prepare(FloatArray(points.size) { maxZoomSize })
        uploadSprite()
    }

    override fun getVertexShader() = """
        uniform mat4 u_MVPMatrix;
        uniform float u_ZoomFraction;
        attribute vec4 a_Position;
        attribute vec4 a_Color;
        attribute float a_PointSize;
        attribute float a_MaxPointSize;
        varying vec4 v_Color;
        void main() {
          vec4 clip = u_MVPMatrix * vec4(a_Position.xyz, 0.0);
          clip.z = 0.0;
          gl_Position = clip;
          gl_PointSize = mix(a_PointSize, a_MaxPointSize, u_ZoomFraction);
          v_Color = a_Color;
        }
    """.trimIndent()

    override fun getFragmentShader() = """
        precision mediump float;
        uniform sampler2D u_Texture;
        varying vec4 v_Color;
        void main() {
          vec4 sprite = texture2D(u_Texture, gl_PointCoord) * v_Color;
          if (sprite.a < 0.015) discard;
          gl_FragColor = sprite;
        }
    """.trimIndent()

    fun setZoomFraction(value: Float) {
        zoomFraction = value.coerceIn(0f, 1f)
    }

    override fun draw(mvpMatrix: FloatArray?) {
        beginDraw(mvpMatrix) {
            pointSizeBuffer.initializeAttribPointer(mProgramID)
            maxPointSizeBuffer.initializeAttribPointer(mProgramID)
            GLES30.glUniform1f(GLES30.glGetUniformLocation(mProgramID, "u_ZoomFraction"), zoomFraction)
            GLES30.glEnable(GLES30.GL_BLEND)
            GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GL10.GL_TEXTURE_2D, textureHandle[0])
            GLES30.glUniform1i(GLES30.glGetUniformLocation(mProgramID, "u_Texture"), 0)
            GLES30.glDrawElements(GLES30.GL_POINTS, drawOrder.size, GLES30.GL_UNSIGNED_SHORT, mDrawListBuffer.mBuffer)
            GLES30.glDisable(GLES30.GL_BLEND)
            maxPointSizeBuffer.releaseHandle()
            pointSizeBuffer.releaseHandle()
        }
    }

    private fun uploadSprite() {
        val bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        when (kind) {
            AstronomicalObjectKind.GALAXY -> drawGalaxy(canvas)
            AstronomicalObjectKind.STAR_CLUSTER -> drawCluster(canvas)
            AstronomicalObjectKind.NEBULA -> drawNebula(canvas)
            else -> drawGalaxy(canvas)
        }
        GLES30.glGenTextures(1, textureHandle, 0)
        GLES30.glBindTexture(GL10.GL_TEXTURE_2D, textureHandle[0])
        GLES30.glTexParameteri(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0)
        bitmap.recycle()
        checkGlError("upload deep-sky sprite")
    }

    private fun drawGalaxy(canvas: Canvas) {
        canvas.save()
        canvas.scale(1f, 0.68f, 64f, 64f)
        canvas.drawCircle(64f, 64f, 55f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(64f, 64f, 55f,
                intArrayOf(0xFFFFF3D6.toInt(), 0xBBD8E9FF.toInt(), 0x447A9BCB, Color.TRANSPARENT),
                floatArrayOf(0f, 0.22f, 0.62f, 1f), Shader.TileMode.CLAMP)
        })
        val armPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x88E5EEFF.toInt(); style = Paint.Style.STROKE; strokeWidth = 4f
        }
        for (direction in 0..1) {
            val arm = Path().apply {
                moveTo(64f, 64f)
                cubicTo(95f, 42f, if (direction == 0) 103f else 25f,
                    if (direction == 0) 90f else 83f, if (direction == 0) 26f else 102f,
                    if (direction == 0) 44f else 39f)
            }
            canvas.drawPath(arm, armPaint)
        }
        canvas.drawCircle(64f, 64f, 9f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFF5DE.toInt() })
        canvas.restore()
    }

    private fun drawCluster(canvas: Canvas) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFE6A8.toInt() }
        val stars = listOf(
            64 to 64, 43 to 48, 82 to 45, 91 to 69, 48 to 79, 68 to 34, 31 to 64,
            75 to 88, 101 to 51, 56 to 96, 36 to 38, 88 to 95, 63 to 52, 72 to 73,
            50 to 61, 96 to 82, 25 to 76, 104 to 37, 41 to 91, 79 to 27, 58 to 42,
            85 to 57, 33 to 54, 69 to 101, 46 to 70, 94 to 45, 58 to 82, 77 to 63
        )
        stars.forEachIndexed { i, (x, y) ->
            val size = if (i % 6 == 0) 3.4f else 2f
            paint.alpha = if (i % 4 == 0) 255 else 190
            canvas.drawRect(x - size, y - size, x + size, y + size, paint)
        }
    }

    private fun drawNebula(canvas: Canvas) {
        val clouds = listOf(
            Triple(51f, 64f, 44f), Triple(77f, 52f, 37f), Triple(75f, 78f, 34f)
        )
        clouds.forEachIndexed { index, (x, y, radius) ->
            val color = if (index == 1) 0xCCFFA0D5.toInt() else 0xCC70DFFF.toInt()
            canvas.drawCircle(x, y, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(x, y, radius,
                    intArrayOf(color, color and 0x00FFFFFF, Color.TRANSPARENT),
                    floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
            })
        }
        canvas.drawCircle(66f, 63f, 8f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xCCFFF1D2.toInt() })
    }
}
