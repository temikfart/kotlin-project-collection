import java.awt.Color
import java.awt.Transparency
import java.awt.image.BufferedImage
import java.io.File
import java.lang.IllegalArgumentException
import javax.imageio.ImageIO
import kotlin.system.exitProcess

enum class ImageType {
    Image,
    Watermark
}
enum class InputMethod {
    Single,
    Grid
}

class Point(var x: Int, var y: Int)

object ImageChecker {
    fun checkNumComponents(image: BufferedImage, type: ImageType) {
        if (image.colorModel.numComponents != 3) {
            println("The number of $type color components isn't 3.")
            exitProcess(1)
        }
    }
    fun checkPixelSize(image: BufferedImage, type: ImageType) {
        val pixelSize = image.colorModel.pixelSize
        if (pixelSize != 24 && pixelSize != 32) {
            println("The $type isn't 24 or 32-bit.")
            exitProcess(1)
        }
    }
    fun checkDimensions(image: Image, watermark: Image) {
        if (image.width() < watermark.width() || image.height() < watermark.height()) {
            println("The watermark's dimensions are larger.")
            exitProcess(1)
        }
    }
}

class Image(private val type: ImageType) {
    val bufferedImage = getImage()
    var useAlpha = false
    var useTransparencyColor = false
    var transparencyColor = Color(255, 255, 255)
    var pos = Point(0, 0)

    init {
        if (type == ImageType.Watermark
            && bufferedImage.colorModel.hasAlpha()
            && bufferedImage.transparency == Transparency.TRANSLUCENT) {
            println("Do you want to use the watermark's Alpha channel?")
            val response = readln().lowercase()
            if (response == "yes")
                useAlpha = true
        } else {
            ImageChecker.checkNumComponents(bufferedImage, type)
            ImageChecker.checkPixelSize(bufferedImage, type)
        }
    }

    fun width(): Int = bufferedImage.width
    fun height(): Int = bufferedImage.height
    fun getPositionRelativeTo(image: Image) {
        val diffX = image.width() - bufferedImage.width
        val diffY = image.height() - bufferedImage.height
        println("Input the watermark position ([x 0-$diffX] [y 0-$diffY]):")
        try {
            val input = readln().split(" ")
            if (input.size != 2)
                throw IllegalArgumentException("")
            pos.x = input[0].toInt()
            pos.y = input[1].toInt()
        } catch (e: Exception) {
            println("The position input is invalid.")
            exitProcess(1)
        }
        if (pos.x !in 0..diffX || pos.y !in 0..diffY) {
            println("The position input is out of range.")
            exitProcess(1)
        }
    }

    private fun getImageFile(imageFileName: String): File {
        val imageFile = File(imageFileName)
        if (!imageFile.exists()) {
            println("The file $imageFileName doesn't exist.")
            exitProcess(1)
        }
        return imageFile
    }
    private fun getImage(): BufferedImage {
        val name = when (type) {
            ImageType.Image -> "image"
            ImageType.Watermark -> "watermark image"
        }
        println("Input the $name filename:")
        val imageFileName = readln()
        return ImageIO.read(getImageFile(imageFileName))
    }
}

object Blender {
    fun blend(
        image: Image,
        watermark: Image,
        weight: Int,
        inputMethod: InputMethod
    ): BufferedImage {
        val outImage = BufferedImage(image.width(), image.height(), BufferedImage.TYPE_INT_RGB)

        val isSingle = inputMethod == InputMethod.Single

        val allowableXRange = watermark.pos.x until watermark.pos.x + watermark.width()
        val allowableYRange = watermark.pos.y until watermark.pos.y + watermark.height()
        var inRange = false

        for (y in 0 until image.height()) {
            for (x in 0 until image.width()) {
                inRange = x in allowableXRange && y in allowableYRange

                val w = if (isSingle && inRange) {
                    Color(watermark.bufferedImage.getRGB(
                            x - watermark.pos.x,
                            y - watermark.pos.y),
                        watermark.useAlpha)
                }  else {
                    Color(watermark.bufferedImage.getRGB(
                        x % watermark.width(),
                        y % watermark.height()),
                        watermark.useAlpha)
                }
                val i = Color(image.bufferedImage.getRGB(x, y), watermark.useAlpha)

                if (isSingle && !inRange
                    || watermark.useAlpha && w.alpha == 0
                    || watermark.useTransparencyColor && w.rgb == watermark.transparencyColor.rgb) {
                    outImage.setRGB(x, y, i.rgb)
                } else {
                    val color = Color(
                        (weight * w.red + (100 - weight) * i.red) / 100,
                        (weight * w.green + (100 - weight) * i.green) / 100,
                        (weight * w.blue + (100 - weight) * i.blue) / 100
                    )
                    outImage.setRGB(x, y, color.rgb)
                }
            }
        }
        return outImage
    }
}

fun getTransparency(): Int {
    println("Input the watermark transparency percentage (Integer 0-100):")
    try {
        val transparency = readln().toInt()
        if (transparency < 0 || 100 < transparency) {
            println("The transparency percentage is out of range.")
            exitProcess(1)
        }
        return transparency
    } catch (e: NumberFormatException) {
        println("The transparency percentage isn't an integer number.")
        exitProcess(1)
    }
}
fun getInputMethod(): InputMethod {
    println("Choose the position method (single, grid):")
    val response = readln().lowercase()
    if (response != "grid" && response != "single") {
        println("The position method input is invalid.")
        exitProcess(1)
    }
    return InputMethod.valueOf(response.replaceFirstChar { it.uppercase() })
}
fun getOutputFile(): File {
    println("Input the output image filename (jpg or png extension):")
    val outImageFileName = readln()
    val outputFile = File(outImageFileName)
    val extension = outputFile.extension
    if (extension != "jpg" && extension != "png") {
        println("The output file extension isn't \"jpg\" or \"png\".")
        exitProcess(1)
    }
    return File(outImageFileName)
}
fun getTransparencyColor(): Color {
    println("Input a transparency color ([Red] [Green] [Blue]):")
    try {
        val input = readln().split(" ")
        if (input.size != 3)
            throw IllegalArgumentException("")
        return Color(
            input[0].toInt(),
            input[1].toInt(),
            input[2].toInt()
        )
    } catch (e: Exception) {
        println("The transparency color input is invalid.")
        exitProcess(1)
    }
}

fun main() {
    val image = Image(ImageType.Image)
    val watermark = Image(ImageType.Watermark)
    ImageChecker.checkDimensions(image, watermark)

    if (!watermark.bufferedImage.colorModel.hasAlpha()) {
        println("Do you want to set a transparency color?")
        val response = readln().lowercase()
        if (response == "yes") {
            watermark.transparencyColor = getTransparencyColor()
            watermark.useTransparencyColor = true
        }
    }

    val weight = getTransparency()
    val inputMethod = getInputMethod()
    if (inputMethod == InputMethod.Single)
        watermark.getPositionRelativeTo(image)
    val outImageFile = getOutputFile()
    val extension = outImageFile.extension
    val outImage = Blender.blend(image, watermark, weight, inputMethod)

    ImageIO.write(outImage, extension, outImageFile)
    println("The watermarked image ${outImageFile.path} has been created.")
}
