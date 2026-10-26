package com.ede.astrodroid.threedee

import android.R
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.opengl.GLES30
import android.opengl.GLUtils
import javax.microedition.khronos.opengles.GL10

/**
 * There's no proper support for text (proper=easy) for OpenGL. Again massive boiler plate
 * Collecting utilities here (for now)
 *
 * Not to mention we don't necessarily want to render the text in 3D, 2D is just enough
 * (say if overlaying is easier), how ever that requires projection 3D->2D as we DO intend
 * to have the text next to the object in question
 *
 * And that probably creates some hard to handle collisions for close by galaxies, if we
 * draw all the names, no-one's gonna be able to read the names(on top of the names :) )
 *
 * See:
 * - https://arm-software.github.io/opengl-es-sdk-for-android/high_quality_text.html
 * - https://stackoverflow.com/questions/1339136/draw-text-in-opengl-es
 * - https://stackoverflow.com/questions/8847899/how-to-draw-text-using-only-opengl-methods
 * - https://learnopengl.com/In-Practice/Text-Rendering
 *
 * Project uses "text atlas" approach, quick to draw for sure but needs recreate if text changes..
 * - https://github.com/sky-map-team/stardroid/blob/be8f23cc7af6966cb9eb29a91e8c669ba874a528/app/test/com/google/android/stardroid/renderer/debug/NumericSprite.java
 */
abstract class TextSupport {
    fun renderText(
        bitmap: Bitmap,
        textureDataHandle: IntArray,
    ) {
        GLES30.glGenTextures(1, textureDataHandle, 0)
        GLES30.glBindTexture(GL10.GL_TEXTURE_2D, textureDataHandle[0])

        GLES30.glTexParameterf(
            GL10.GL_TEXTURE_2D,
            GL10.GL_TEXTURE_MIN_FILTER,
            GL10.GL_NEAREST.toFloat()
        )
        GLES30.glTexParameterf(
            GL10.GL_TEXTURE_2D,
            GL10.GL_TEXTURE_MAG_FILTER,
            GL10.GL_LINEAR.toFloat()
        )

        GLES30.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT.toFloat())
        GLES30.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT.toFloat())

        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0)
    }

    /**
     * Return a bitmap with given text drawn on it...
     *
     * Remember to bitmap.recycle() once done..
     *
     * Usaage:
     *         val bitmap = makeBitmapAndDrawText(context, text)
     */
    private fun makeBitmapAndDrawText(
        context: Context,
        text: String
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_4444)
        val canvas = Canvas(bitmap)
        bitmap.eraseColor(0)

        val background: Drawable =
            context.resources.getDrawable(R.drawable.screen_background_dark)
        background.setBounds(0, 0, 256, 256)
        background.draw(canvas)

        val textPaint = Paint()
        textPaint.textSize = 32f
        textPaint.isAntiAlias = true
        textPaint.setARGB(0xff, 0x00, 0x00, 0x00)
        // draw the text centered
        canvas.drawText(text, 16f, 112f, textPaint)
        return bitmap
    }
}