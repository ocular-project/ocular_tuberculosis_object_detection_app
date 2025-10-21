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
import kotlin.system.measureTimeMillis

class Detector (private val context: Context) {

    private val inputSize = 640
    private val confidenceThreshold = 0.20f
    private val nmsThreshold = 0.2f
    private val classesList = mapOf(
        0 to "trophozoites",
        1 to "wbc"
    )
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    suspend fun processImage(originalBitmap: Bitmap, originalPath: String): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val modelFile = "260F729BAC60.tflite"
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
                    val outputBuffer = Array(1) { Array(6) { FloatArray(8400) } }

                    val inferenceTime = measureTimeMillis {
                        // Run inference
                        val outputs = HashMap<Int, Any>()
                        outputs[0] = outputBuffer
                        interpreter.runForMultipleInputsOutputs(arrayOf(input), outputs)
                    }

                    Log.d("ObjectDetector", "Model inference time: ${inferenceTime}ms")

                    // Process detections
                    val detections = ArrayList<Detection>()

                    // Use actual bitmap dimensions instead of hardcoded values
                    val originalWidth = originalBitmap.width
                    val originalHeight = originalBitmap.height

                    for (i in 0 until 8400) {
                        // Get class scores (indices 4-5)
                        var maxScore = 0f
                        var classId = -1
                        for (j in 0 until 2) { // 2 classesList
                            val score = outputBuffer[0][4 + j][i]
                            if (score > maxScore) {
                                maxScore = score
                                classId = j
                            }
                        }

                        if (maxScore > confidenceThreshold && classId != -1) {
                            // Get bounding box coordinates
                            val x = outputBuffer[0][0][i] // center_x
                            val y = outputBuffer[0][1][i] // center_y
                            val w = outputBuffer[0][2][i] // width
                            val h = outputBuffer[0][3][i] // height

                            // Convert to pixel coordinates
                            val left = maxOf(0f, minOf(originalWidth.toFloat(), (x - w/2) * originalWidth))
                            val top = maxOf(0f, minOf(originalHeight.toFloat(), (y - h/2) * originalHeight))
                            val right = maxOf(0f, minOf(originalWidth.toFloat(), (x + w/2) * originalWidth))
                            val bottom = maxOf(0f, minOf(originalHeight.toFloat(), (y + h/2) * originalHeight))

//                            Log.d("Detection all boxex", "Confidence: %.3f, Box: [%.0f, %.0f, %.0f, %.0f]"
//                                .format(maxScore, left, top, right, bottom))

                            if (right > left && bottom > top) {
                                detections.add(Detection(classId, maxScore, left, top, right, bottom))
//                                Log.d("Malaria", "runDetection: Box added")
                            }
                        }
                    }

                    val finalDetections = applyNMS(detections, nmsThreshold)

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

                    finalDetections.forEach { detection ->
                        // Draw bounding box
                        val boxPaint = Paint().apply {
                            style = Paint.Style.STROKE
                            strokeWidth = 10f
                            color = if (detection.classId == 0) Color.YELLOW else Color.BLUE
                        }
                        canvas.drawRect(
                            detection.left,
                            detection.top,
                            detection.right,
                            detection.bottom,
                            boxPaint
                        )

                        // Draw label
//                        val label = String.format("%s", classesList[detection.classId])
//                        val textPaint = Paint().apply {
//                            color = Color.WHITE
//                            textSize = 50f
//                        }
//
//                        val bgPaint = Paint().apply {
//                            color = Color.BLACK
//                            style = Paint.Style.FILL
//                            alpha = 180
//                        }
//
//                        val textWidth = textPaint.measureText(label)
//                        canvas.drawRect(
//                            detection.left,
//                            detection.top - 50f,
//                            detection.left + textWidth + 10f,
//                            detection.top,
//                            bgPaint
//                        )
//                        canvas.drawText(label, detection.left + 5f, detection.top - 10f, textPaint)
                    }
                    Log.d("ObjectDetector", "Detections: $classCounts")

                    saveImage(resultBitmap, originalPath, classCounts, inferenceTime)
                    return@withContext resultBitmap
                }

            } catch (e: Exception) {
                Log.e("ObjectDetector", "Error processing image", e)
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
                "Pictures/Ocular/$slideName/analysed%"
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
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Ocular/Malaria/$slideName/analysed")
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

        val trophozoitesCount = classCounts.getOrDefault("trophozoites", 0)
        val wbcCount = classCounts.getOrDefault("wbc", 0)

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
                        url.wbc = wbcCount
                        url.trop = trophozoitesCount
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