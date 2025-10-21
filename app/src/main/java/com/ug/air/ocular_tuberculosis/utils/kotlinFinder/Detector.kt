package com.ug.air.ocular_tuberculosis.utils.kotlinFinder

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ug.air.ocular_tuberculosis.ui.HomeActivity
import com.ug.air.ocular_tuberculosis.ui.slideAnalysis.CameraActivity
//import com.ug.air.ocular_tuberculosis.ui.slideAnalysis.ImageActivity
import com.ug.air.ocular_tuberculosis.models.Image
import com.ug.air.ocular_tuberculosis.utils.model.ModelLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.core.graphics.scale
import androidx.core.graphics.get
import com.ug.air.ocular_tuberculosis.ui.HomeActivity.IMAGES
import kotlin.math.max
import kotlin.math.min
import kotlin.system.measureTimeMillis

class Detector (private val context: Context) {

    private val inputSize = 960
    private val confidenceThreshold = 0.25f
    private val nmsThreshold = 0.25f
    private val classesList = mapOf(
        0 to "AFB",
    )
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    suspend fun processImage(originalBitmap: Bitmap, originalPath: String): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val modelFile = "270A42F7C3A3.tflite"
                val interpreter = ModelLoader.loadModel(context, modelFile)
                    ?: throw IllegalStateException("Failed to load TFLite model")

                interpreter.use { interpreter ->
                    val scaledBitmap = originalBitmap.scale(inputSize, inputSize)
                    val input = Array(1) { Array(inputSize) { Array(inputSize) { FloatArray(3) } } }

                    for (y in 0 until inputSize) {
                        for (x in 0 until inputSize) {
                            val pixel = scaledBitmap[x, y]
                            input[0][y][x][0] = Color.red(pixel) / 255.0f
                            input[0][y][x][1] = Color.green(pixel) / 255.0f
                            input[0][y][x][2] = Color.blue(pixel) / 255.0f
                        }
                    }

                    // Prepare output
                    val outputBuffer = Array(1) { Array(300) { FloatArray(6) } }

                    val inferenceTime = measureTimeMillis {
                        interpreter.runForMultipleInputsOutputs(arrayOf(input), mapOf(0 to outputBuffer))
                    }

                    Log.d("ObjectDetector", "Model inference time: ${inferenceTime}ms")

                    // Process detections
                    val detections = ArrayList<Detection>()

                    val originalWidth = originalBitmap.width.toFloat()
                    val originalHeight = originalBitmap.height.toFloat()

                    var validDetectionsCount = 0
                    var highConfidenceCount = 0

                    for (i in 0 until 300) {
                        val xCenter = outputBuffer[0][i][0]
                        val yCenter = outputBuffer[0][i][1]
                        val width = outputBuffer[0][i][2]
                        val height = outputBuffer[0][i][3]
                        val classId = outputBuffer[0][i][4].toInt()
                        val confidence = outputBuffer[0][i][5]

                        if (i < 5) {
                            Log.d("ObjectDetector", "Detection $i: x=$xCenter, y=$yCenter, w=$width, h=$height, cls=$classId, conf=$confidence")
                        }

                        if (confidence > confidenceThreshold) {
                            validDetectionsCount++
                            val isNormalized = xCenter <= 1.0f && yCenter <= 1.0f

                            val left: Float
                            val top: Float
                            val right: Float
                            val bottom: Float

                            if (isNormalized) {
                                // Coordinates are normalized (0-1), scale to image dimensions
                                left = max(0f, min(originalWidth, (xCenter - width / 2) * originalWidth))
                                top = max(0f, min(originalHeight, (yCenter - height / 2) * originalHeight))
                                right = max(0f, min(originalWidth, (xCenter + width / 2) * originalWidth))
                                bottom = max(0f, min(originalHeight, (yCenter + height / 2) * originalHeight))
                            } else {
                                // Coordinates are already in pixel space (less common)
                                left = max(0f, min(originalWidth, xCenter - width / 2))
                                top = max(0f, min(originalHeight, yCenter - height / 2))
                                right = max(0f, min(originalWidth, xCenter + width / 2))
                                bottom = max(0f, min(originalHeight, yCenter + height / 2))
                            }


                            if (right > left && bottom > top) {
                                val boxWidth = right - left
                                val boxHeight = bottom - top
                                val boxArea = boxWidth * boxHeight
                                val imageArea = originalWidth * originalHeight

                                if (boxArea > imageArea * 0.0001f && boxArea < imageArea * 0.9f) {
                                    detections.add(Detection(classId, confidence, left, top, right, bottom))
                                    if (confidence > 0.5f) highConfidenceCount++
                                }
                            }
                        }
                    }

                    Log.d("ObjectDetector", "Raw detections above threshold: $validDetectionsCount")
                    Log.d("ObjectDetector", "High confidence detections (>0.5): $highConfidenceCount")
                    Log.d("ObjectDetector", "Valid boxes before NMS: ${detections.size}")

                    val finalDetections = applyNMS(detections, nmsThreshold)

                    Log.d("ObjectDetector", "Final detections after NMS: ${finalDetections.size}")

                    if (finalDetections.isEmpty()) {
                        Log.d("ObjectDetector", "No detections found")
                        return@withContext null
                    }

                    val classCounts = HashMap<String, Int>()
                    for (classId in classesList.keys) {
                        val className = classesList[classId] ?: continue
                        val count = finalDetections.count { it.classId == classId }
                        classCounts[className] = count
                    }

                    // Draw results on original image
                    val resultBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
                    val canvas = Canvas(resultBitmap)

                    val boxPaint = Paint().apply {
                        style = Paint.Style.STROKE
                        strokeWidth = max(5f, originalWidth / 200f) // Scale stroke width with image size
                        color = Color.YELLOW
                    }

                    val textPaint = Paint().apply {
                        color = Color.YELLOW
                        textSize = max(24f, originalWidth / 40f)
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }

                    finalDetections.forEach { detection ->
                        // Draw bounding box
                        canvas.drawRect(
                            detection.left,
                            detection.top,
                            detection.right,
                            detection.bottom,
                            boxPaint
                        )

                        // Draw confidence label
                        val label = String.format("%.2f", detection.confidence)
                        canvas.drawText(
                            label,
                            detection.left,
                            max(detection.top - 5f, textPaint.textSize),
                            textPaint
                        )
                    }

                    Log.d("ObjectDetector", "Detections: $classCounts")

                    saveImage(resultBitmap, originalPath, classCounts, inferenceTime)
                    return@withContext resultBitmap
                }

            } catch (e: Exception) {
                Log.e("ObjectDetector", "Error processing image", e)
                e.printStackTrace()
                return@withContext null
            }
        }

    private fun applyNMS(boxes: List<Detection>, nmsThreshold: Float): List<Detection> {
        val sortedBoxes = boxes.sortedByDescending { it.confidence }
        val selected = ArrayList<Detection>()
        val suppressed = BooleanArray(sortedBoxes.size)

        for (i in sortedBoxes.indices) {
            if (suppressed[i]) continue

            selected.add(sortedBoxes[i])

            for (j in i + 1 until sortedBoxes.size) {
                if (suppressed[j]) continue

                val iou = calculateIoU(sortedBoxes[i], sortedBoxes[j])
                if (iou > nmsThreshold) {
                    suppressed[j] = true
                }
            }
        }

        return selected
    }

    private fun calculateIoU(box1: Detection, box2: Detection): Float {
        val intersectionX1 = maxOf(box1.left, box2.left)
        val intersectionY1 = maxOf(box1.top, box2.top)
        val intersectionX2 = minOf(box1.right, box2.right)
        val intersectionY2 = minOf(box1.bottom, box2.bottom)

        if (intersectionX2 < intersectionX1 || intersectionY2 < intersectionY1) {
            return 0.0f
        }

        val intersection = (intersectionX2 - intersectionX1) * (intersectionY2 - intersectionY1)
        val box1Area = (box1.right - box1.left) * (box1.bottom - box1.top)
        val box2Area = (box2.right - box2.left) * (box2.bottom - box2.top)
        val union = box1Area + box2Area - intersection

        return intersection / union
    }

    data class Detection(
        val classId: Int,
        val confidence: Float,
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    )

    private fun saveImage(bitmap: Bitmap, originalPath: String, classCounts: Map<String, Int>, inferenceTimeMs: Long): String? {

        sharedPreferences = context.getSharedPreferences(
            CameraActivity.GALLERY,
            AppCompatActivity.MODE_PRIVATE
        )
        editor = sharedPreferences.edit()

        val slideName = sharedPreferences.getString(HomeActivity.SLIDE_NAME, "") ?: ""

        return try {
            val filename = File(originalPath).name
            val baseName = filename.substringBeforeLast(".")
            val resolver = context.contentResolver

            // First, check if file already exists
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
            val selectionArgs = arrayOf(
                "${baseName}.jpg",
                "Pictures/Ocular/Tuberculosis/$slideName/analysed%"
            )

            // Query for existing file
            resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media._ID),
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    // File exists, delete it
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                    val deleteUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                    resolver.delete(deleteUri, null, null)
                    Log.d("ObjectDetector", "Deleted existing file: ${baseName}.jpg")
                }
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "${baseName}.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Ocular/Tuberculosis/$slideName/analysed")
            }

            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            imageUri?.let { uri ->
                resolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                }
                // Return the path for reference
                val filePath = getPathFromUri(context, uri)
                if (filePath != null) {
                    updateList(filePath, classCounts, slideName, originalPath, inferenceTimeMs)
                }
                return filePath
            }
            null
        }catch (e: Exception) {
            Log.e("ObjectDetector", "Error saving image: ${e.message}")
            null
        }
    }

    private fun getPathFromUri(context: Context, uri: Uri): String? {
        var filePath: String? = null
        val projection = arrayOf(MediaStore.Images.Media.DATA)

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                filePath = cursor.getString(columnIndex)
            }
        }

        return filePath
    }

    private fun updateList(filePath: String, classCounts: Map<String, Int>, slideName: String, original: String, inferenceTimeMs: Long) {

        val afbCount = classCounts.getOrDefault("AFB", 0)
//        val wbcCount = classCounts.getOrDefault("wbc", 0)

        val sharedPreferences: SharedPreferences = context.getSharedPreferences(
            CameraActivity.GALLERY,
            Context.MODE_PRIVATE
        )
        val editor = sharedPreferences.edit()

        val gson = Gson()
        val jsonList = sharedPreferences.getString(IMAGES, null)

        val type = object : TypeToken<ArrayList<Image>>() {}.type
        val imagesArrayList: ArrayList<Image> = gson.fromJson(jsonList, type) ?: ArrayList()

        for (image in imagesArrayList) {
            if (image.slideName == slideName) {
                for (url in image.images) {
                    if (url.original == original) {
                        url.analysed = filePath
                        url.afb = afbCount
                        url.inferenceTime = inferenceTimeMs
                        break
                    }
                }
                image.status = "analysed"
                break
            }
        }

        val json2 = gson.toJson(imagesArrayList)
        editor.putString(IMAGES, json2)
        editor.apply()
    }

}