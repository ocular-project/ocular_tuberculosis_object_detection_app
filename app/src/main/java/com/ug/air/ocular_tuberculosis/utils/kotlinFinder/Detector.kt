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
import com.ug.air.ocular_tuberculosis.ui.HomeActivity.SLIDE_NAME
import com.ug.air.ocular_tuberculosis.ui.slideAnalysis.CameraActivity.GALLERY
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

                    // Normalize input to [0, 1]
                    for (y in 0 until inputSize) {
                        for (x in 0 until inputSize) {
                            val pixel = scaledBitmap[x, y]
                            input[0][y][x][0] = Color.red(pixel) / 255.0f
                            input[0][y][x][1] = Color.green(pixel) / 255.0f
                            input[0][y][x][2] = Color.blue(pixel) / 255.0f
                        }
                    }

                    // Prepare output - [1, 300, 6]
                    val outputBuffer = Array(1) { Array(300) { FloatArray(6) } }

                    val inferenceTime = measureTimeMillis {
                        interpreter.runForMultipleInputsOutputs(arrayOf(input), mapOf(0 to outputBuffer))
                    }

                    Log.d("ObjectDetector", "Model inference time: ${inferenceTime}ms")

                    // Process detections
                    val detections = ArrayList<Detection>()

                    val originalWidth = originalBitmap.width.toFloat()
                    val originalHeight = originalBitmap.height.toFloat()

                    Log.d("ObjectDetector", "Original dimensions: ${originalWidth}x${originalHeight}")

                    for (i in 0 until 300) {
                        // Read output correctly: [x1, y1, x2, y2, confidence, class]
                        val x1_raw = outputBuffer[0][i][0]
                        val y1_raw = outputBuffer[0][i][1]
                        val x2_raw = outputBuffer[0][i][2]
                        val y2_raw = outputBuffer[0][i][3]
                        val confidence = outputBuffer[0][i][4]
                        val classId = outputBuffer[0][i][5].toInt()

                        // Log first few detections for debugging
                        if (i < 5 && confidence > 0.1f) {
                            Log.d("ObjectDetector", "Detection $i: x1=$x1_raw, y1=$y1_raw, x2=$x2_raw, y2=$y2_raw, conf=$confidence, cls=$classId")
                        }

                        if (confidence > confidenceThreshold && classId != -1) {
                            // Check if coordinates are normalized (0-1)
                            val isNormalized = x2_raw <= 1.0f && y2_raw <= 1.0f

                            val x1: Float
                            val y1: Float
                            val x2: Float
                            val y2: Float

                            if (isNormalized) {
                                // Scale normalized coordinates to original image size
                                x1 = x1_raw * originalWidth
                                y1 = y1_raw * originalHeight
                                x2 = x2_raw * originalWidth
                                y2 = y2_raw * originalHeight
                            } else {
                                // Coordinates are in pixels relative to input size (960)
                                // Scale to original image size
                                val scaleX = originalWidth / inputSize
                                val scaleY = originalHeight / inputSize
                                x1 = x1_raw * scaleX
                                y1 = y1_raw * scaleY
                                x2 = x2_raw * scaleX
                                y2 = y2_raw * scaleY
                            }

                            // Clip to image bounds
                            val left = max(0f, min(originalWidth, x1))
                            val top = max(0f, min(originalHeight, y1))
                            val right = max(0f, min(originalWidth, x2))
                            val bottom = max(0f, min(originalHeight, y2))

                            // Validate bounding box
                            if (right > left && bottom > top) {
                                val boxWidth = right - left
                                val boxHeight = bottom - top
                                val boxArea = boxWidth * boxHeight
                                val imageArea = originalWidth * originalHeight

                                // Filter out very small and very large boxes
                                if (boxArea > imageArea * 0.0001f && boxArea < imageArea * 0.9f) {
                                    detections.add(Detection(classId, confidence, left, top, right, bottom))

                                    if (detections.size <= 5) {
                                        Log.d("ObjectDetector", "Valid box $i: left=$left, top=$top, right=$right, bottom=$bottom, size=${boxWidth}x${boxHeight}")
                                    }
                                }
                            }
                        }
                    }

                    Log.d("ObjectDetector", "Valid boxes before NMS: ${detections.size}")

                    val finalDetections = applyNMS(detections, nmsThreshold)

                    Log.d("ObjectDetector", "Final detections after NMS: ${finalDetections.size}")

                    if (finalDetections.isEmpty()) {
                        Log.d("ObjectDetector", "No detections found")
                        return@withContext null
                    }

                    // Count detections by class
                    val classCounts = HashMap<String, Int>()
                    for (classId in classesList.keys) {
                        val className = classesList[classId] ?: continue
                        val count = finalDetections.count { it.classId == classId }
                        classCounts[className] = count
                    }

                    Log.d("ObjectDetector", "Detections: $classCounts")

                    // Draw results on original image
                    val resultBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
                    val canvas = Canvas(resultBitmap)

                    val boxPaint = Paint().apply {
                        style = Paint.Style.STROKE
                        strokeWidth = max(3f, originalWidth / 200f)
                        color = Color.YELLOW
                    }

                    val textPaint = Paint().apply {
                        color = Color.YELLOW
                        textSize = max(20f, originalWidth / 50f)
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
            GALLERY,
            Context.MODE_PRIVATE
        )
        editor = sharedPreferences.edit()

        val slideName = sharedPreferences.getString(SLIDE_NAME, "") ?: ""

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
            GALLERY,
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