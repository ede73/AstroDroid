package com.ede.astrodroid.threedee

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES30
import android.opengl.GLUtils
import javax.microedition.khronos.opengles.GL10

data class SolarSystemSpritePoint(
    val name: String,
    val position: FloatArray,
    val visual: com.ede.astrodroid.objects.SkyVisual,
    val magnitude: Float,
)

/** Renders the optional, small solar-system atlas sprites as sky directions. */
class SolarSystemSpriteObject(
    context: Context,
    private val points: List<SolarSystemSpritePoint>,
) : ThreeDObject() {
    private val cellBuffer = OpenGLMappedFloatBuffer(1, 0, "a_AtlasCell")
    private val pointSizeBuffer = OpenGLMappedFloatBuffer(1, 0, "a_PointSize")
    private val maxZoomPointSizeBuffer = OpenGLMappedFloatBuffer(1, 0, "a_MaxZoomPointSize")
    private val textureHandle = IntArray(1)
    private val atlasResource = points.first().visual.atlasResource
    private var pointCount = 0
    private var zoomFraction = 0f

    init {
        require(points.all { it.visual.atlasResource == atlasResource }) {
            "All sprites in one batch must use the same texture atlas"
        }
        loadTexture(context, atlasResource)
        update(points)
    }

    override fun getVertexShader(): String = """
        uniform mat4 u_MVPMatrix;
        attribute vec4 a_Position;
        attribute vec4 a_Color;
        attribute float a_AtlasCell;
        attribute float a_PointSize;
        attribute float a_MaxZoomPointSize;
        uniform float u_ZoomFraction;
        varying vec4 v_Color;
        varying float v_AtlasCell;

        void main() {
          vec4 clipPosition = u_MVPMatrix * vec4(a_Position.xyz, 0.0);
          clipPosition.z = 0.0;
          gl_Position = clipPosition;
          gl_PointSize = mix(a_PointSize, a_MaxZoomPointSize, u_ZoomFraction);
          v_Color = a_Color;
          v_AtlasCell = a_AtlasCell;
        }
    """.trimIndent()

    override fun getFragmentShader(): String = """
        precision mediump float;
        uniform sampler2D u_Texture;
        varying vec4 v_Color;
        varying float v_AtlasCell;

        void main() {
          float column = mod(v_AtlasCell, 4.0);
          float row = floor(v_AtlasCell / 4.0);
          vec2 local = clamp(gl_PointCoord, vec2(0.04), vec2(0.96));
          // GLUtils uploads Android's top-first bitmap rows at texture v=0.
          vec2 uv = vec2((column + local.x) / 4.0, (row + local.y) / 4.0);
          vec4 sprite = texture2D(u_Texture, uv) * v_Color;
          if (sprite.a < 0.02) discard;
          gl_FragColor = sprite;
        }
    """.trimIndent()

    fun update(newPoints: List<SolarSystemSpritePoint>) {
        require(newPoints.all { it.visual.atlasResource == atlasResource })
        pointCount = newPoints.size
        if (newPoints.isEmpty()) return
        val coordinates = newPoints.flatMap { it.position.asIterable() }.toFloatArray()
        val drawOrder = ShortArray(pointCount) { it.toShort() }
        val colors = FloatArray(pointCount * 4) { 1f }
        prepareArrays(coordinates, drawOrder, colors)
        cellBuffer.prepare(newPoints.map { it.visual.atlasCell.toFloat() }.toFloatArray())
        pointSizeBuffer.prepare(newPoints.map { it.visual.pointSizePx }.toFloatArray())
        maxZoomPointSizeBuffer.prepare(
            newPoints.map { it.visual.pointSizeAtMaxZoomPx }.toFloatArray()
        )
    }

    fun setZoomFraction(fraction: Float) {
        zoomFraction = fraction.coerceIn(0f, 1f)
    }

    override fun draw(mvpMatrix: FloatArray?) {
        if (pointCount == 0) return
        beginDraw(mvpMatrix) {
            GLES30.glEnable(GLES30.GL_BLEND)
            GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
            cellBuffer.initializeAttribPointer(mProgramID)
            pointSizeBuffer.initializeAttribPointer(mProgramID)
            maxZoomPointSizeBuffer.initializeAttribPointer(mProgramID)
            val zoomHandle = GLES30.glGetUniformLocation(mProgramID, "u_ZoomFraction")
            GLES30.glUniform1f(zoomHandle, zoomFraction)
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GL10.GL_TEXTURE_2D, textureHandle[0])
            val sampler = GLES30.glGetUniformLocation(mProgramID, "u_Texture")
            GLES30.glUniform1i(sampler, 0)
            GLES30.glDrawArrays(GLES30.GL_POINTS, 0, pointCount)
            maxZoomPointSizeBuffer.releaseHandle()
            pointSizeBuffer.releaseHandle()
            cellBuffer.releaseHandle()
            GLES30.glDisable(GLES30.GL_BLEND)
        }
    }

    private fun loadTexture(context: Context, resourceId: Int) {
        val options = BitmapFactory.Options().apply { inScaled = false; inSampleSize = 2 }
        val bitmap = BitmapFactory.decodeResource(context.resources, resourceId, options)
            ?: error("Could not decode celestial sprite atlas")
        GLES30.glGenTextures(1, textureHandle, 0)
        checkGlError("generate celestial atlas texture")
        GLES30.glBindTexture(GL10.GL_TEXTURE_2D, textureHandle[0])
        GLES30.glTexParameteri(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR)
        GLES30.glTexParameteri(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR)
        GLES30.glTexParameteri(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0)
        bitmap.recycle()
        checkGlError("upload celestial atlas texture")
    }
}
