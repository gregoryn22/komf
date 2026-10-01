package snd.komf.util

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GrayscaleDetectionTest {

    private fun image(
        width: Int = 300,
        height: Int = 430,
        pixel: (x: Int, y: Int) -> Color
    ): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for (y in 0..<height) for (x in 0..<width) image.setRGB(x, y, pixel(x, y).rgb)
        return ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
    }

    private fun gray(v: Int) = Color(v, v, v)

    // white page with black "panels" and grey tones
    private fun pagePixel(x: Int, y: Int): Color = when {
        y % 140 < 6 -> gray(255)
        (x / 20 + y / 20) % 3 == 0 -> gray(20)
        (x / 20 + y / 20) % 3 == 1 -> gray(128)
        else -> gray(240)
    }

    @Test
    fun `black and white page is grayscale`() {
        assertTrue(isGrayscaleImage(image(pixel = ::pagePixel)))
    }

    @Test
    fun `sepia tinted scan is grayscale`() {
        val sepia = image { x, y ->
            val v = pagePixel(x, y).red
            Color((v * 1.0).toInt(), (v * 0.92).toInt(), (v * 0.78).toInt())
        }
        assertTrue(isGrayscaleImage(sepia))
    }

    @Test
    fun `page with small colored logo is grayscale`() {
        val withLogo = image { x, y -> if (x < 20 && y < 20) Color.RED else pagePixel(x, y) }
        assertTrue(isGrayscaleImage(withLogo))
    }

    @Test
    fun `colored cover is not grayscale`() {
        val cover = image { x, y ->
            when {
                y < 120 -> Color(30, 90, 200)
                x < 150 -> Color(230, 180, 150)
                else -> gray(240)
            }
        }
        assertFalse(isGrayscaleImage(cover))
    }

    @Test
    fun `undecodable data is not grayscale`() {
        assertFalse(isGrayscaleImage(byteArrayOf(1, 2, 3)))
    }
}
