package com.ug.air.ocular_tuberculosis.ui.slideAnalysis.kotlin

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ug.air.ocular_tuberculosis.R
import com.ug.air.ocular_tuberculosis.adapters.kotlinFinder.MediaAdapter
import com.ug.air.ocular_tuberculosis.databinding.ActivityGalleryBinding
import com.ug.air.ocular_tuberculosis.models.Image
import com.ug.air.ocular_tuberculosis.models.Urls
import com.ug.air.ocular_tuberculosis.ui.HomeActivity
import com.ug.air.ocular_tuberculosis.ui.HomeActivity.CATEGORY
import com.ug.air.ocular_tuberculosis.ui.HomeActivity.SLIDE_COUNT
import com.ug.air.ocular_tuberculosis.ui.HomeActivity.SLIDE_NAME
import com.ug.air.ocular_tuberculosis.ui.slideAnalysis.CameraActivity.GALLERY
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.ug.air.ocular_tuberculosis.ui.HomeActivity.IMAGES
import com.ug.air.ocular_tuberculosis.utils.Functions

class GalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var urlList: ArrayList<Urls>
    private lateinit var slideName: String
    private var slideCount: Int = 0
    private lateinit var default: String
    private var imagePath: String? = null
    private var imagesTotal: Int = 0
    private var imagesAfb: Int = 0
    private var imagesTime: String? = null
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private var imagesArrayList: ArrayList<Image> = ArrayList()
    private lateinit var adapterMedia: MediaAdapter

    companion object {
        const val CAMERA_PERM_CODE: Int = 101
        const val CAMERA_REQUEST_CODE: Int = 152
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPreferences = getSharedPreferences(GALLERY, MODE_PRIVATE)
        editor = sharedPreferences.edit()

        slideName = sharedPreferences.getString(SLIDE_NAME, "") ?: ""
        slideCount = sharedPreferences.getInt(SLIDE_COUNT, 0)
//        Toast.makeText(this@GalleryActivity, "count $slideCount", Toast.LENGTH_SHORT).show()
        default = sharedPreferences.getString(CATEGORY, "") ?: ""
        "Slide name: $slideName".also { binding.slideName.text = it }

        binding.recyclerview.setHasFixedSize(true)

        binding.analyse.setOnClickListener {
            editor.putString(CATEGORY, "original")
            editor.apply()

            val intent = Intent(this, ImageViewActivity::class.java)
            intent.putExtra("Position", 0)
            intent.putExtra("Model", "Yes")
            startActivity(intent)
        }

        binding.more.setOnClickListener {
            openDialog();
        }

        binding.report.setOnClickListener {
            openReportDialog()
        }

        onBackPressedDispatcher.addCallback(this) {
            startActivity(Intent(this@GalleryActivity, HomeActivity::class.java))
            finish()
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val file = imagePath?.let { File(it) }

                Log.d("Chodrine", "onCreate: $imagePath")

                file?.let {
                    val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                    val contentUri = Uri.fromFile(it)
                    mediaScanIntent.data = contentUri
                    sendBroadcast(mediaScanIntent)

                    saveData()
                    imagePath = null

                    Log.d("Chodrine", "onCreate: Hello")
                    val newItems = ArrayList(loadMediaItems())
                    adapterMedia.submitList(newItems)
                }

                Handler(Looper.getMainLooper()).postDelayed({
                    dispatchTakePictureIntent()
                }, 500)
            }
        }

        val screen = intent.getStringExtra("Screen")
        if (screen == null) {
            default = "original"
            setDataDefault()
//            askCameraPermission()
        } else {
//            default = screen.toString()
            default = "original"
            setDataDefault()
            askCameraPermission()
        }
    }

    private fun openReportDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.report)
        dialog.setCancelable(true)

        val window = dialog.window
        window?.let {
            it.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)

            // Apply margins
            val params = it.attributes
            params.width = (this.resources.displayMetrics.widthPixels * 0.9).toInt() // 90% of screen width
            it.attributes = params

            it.setBackgroundDrawableResource(R.drawable.dialog_background)
        }

        val images: TextView = dialog.findViewById(R.id.images)
        val afb: TextView = dialog.findViewById(R.id.afb)
        val time: TextView = dialog.findViewById(R.id.time)

        images.text = "$imagesTotal"
        afb.text = "$imagesAfb"
        time.text = imagesTime

        dialog.show()
    }

    private fun setDataDefault() {

        adapterMedia = MediaAdapter(default)

        adapterMedia.setOnItemClickListener { position ->
//            Toast.makeText(this@GalleryActivity, "Position: $position", Toast.LENGTH_SHORT).show()
            sendToImageView(position)
        }

        val layoutManager = GridLayoutManager(this, 4)
        binding.recyclerview.layoutManager = layoutManager

        if (default == "both") {
            binding.analyse.visibility = View.GONE
            binding.report.visibility = View.VISIBLE
            adapterMedia.submitList(loadMediaItemsForBoth())
        } else {
            adapterMedia.submitList(loadMediaItems())
            if (default == "analysed") {
                binding.analyse.visibility = View.GONE
                binding.report.visibility = View.VISIBLE
            } else {
                binding.analyse.visibility = View.VISIBLE
                binding.report.visibility = View.GONE
            }
        }

        binding.recyclerview.adapter = adapterMedia
        urlList = ArrayList()
//        getData()
    }

    private fun loadInRecycler() {
        adapterMedia.submitList(loadMediaItems())
        binding.recyclerview.apply {
            adapter = adapterMedia
            layoutManager = GridLayoutManager(this@GalleryActivity, 4)
        }
    }

    private fun loadMediaItems(): List<Urls> {

        val gson = Gson()
        val jsonList = sharedPreferences.getString(IMAGES, null)

        val type = object : TypeToken<ArrayList<Image>>() {}.type
        val imagesArrayList: ArrayList<Image> = gson.fromJson(jsonList, type) ?: ArrayList()

        for (image in imagesArrayList) {
            if (image.slideName == slideName) {
                val newUrlList = image.images.reversed().toMutableList() as ArrayList<Urls>
                urlList = newUrlList
                if (default != "original") {
                    getTotals()
                }

                return ArrayList(newUrlList)
            }
        }

        return emptyList()
    }

    private fun openDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.list)
        dialog.setCancelable(true)

        val window = dialog.window
        window!!.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        window.let {
            val params = it.attributes
            params.gravity = Gravity.TOP or Gravity.END // Position at the top-right
            params.y = 50 // Adjust vertical offset if needed
            params.x = 50 // Adjust horizontal offset if needed
            it.attributes = params
            it.setBackgroundDrawableResource(R.drawable.dialog_background)
        }

        val original: TextView = dialog.findViewById(R.id.original)
        val analysed: TextView = dialog.findViewById(R.id.analysed)
        val both: TextView = dialog.findViewById(R.id.both)
        val camera: TextView = dialog.findViewById(R.id.camera)

        original.setOnClickListener {
            default = "original"
            setDataDefault()
            binding.report.visibility = View.GONE
            binding.analyse.visibility = View.VISIBLE
            editor.putString(CATEGORY, default)
            editor.apply()
            dialog.dismiss()
        }

        analysed.setOnClickListener {
            default = "analysed"
            setDataDefault()
            binding.report.visibility = View.VISIBLE
            editor.putString(CATEGORY, default)
            editor.apply()
            dialog.dismiss()
        }

        both.setOnClickListener {
            default = "both"
            setDataDefault()
            binding.report.visibility = View.VISIBLE
            editor.putString(CATEGORY, default)
            editor.apply()
            dialog.dismiss()
        }

        camera.setOnClickListener {
            dialog.dismiss()
            askCameraPermission()
        }

        dialog.show()

    }

    private fun getData() {
//        val gson = Gson()
//        val jsonList = sharedPreferences.getString(IMAGES, null)
//
//        val type = object : TypeToken<ArrayList<Image>>() {}.type
//        val imagesArrayList: ArrayList<Image> = gson.fromJson(jsonList, type) ?: ArrayList()
//
//        for (image in imagesArrayList) {
//            if (image.slideName == slideName) {
//
////                urlList = image.images.reversed() as ArrayList<Urls>
//                urlList = image.images.reversed().toMutableList() as ArrayList<Urls>
//
//                if (default == "both") {
//                    imagesList.clear()
//                    binding.analyse.visibility = View.GONE
//                }
//                else {
//                    if (default == "original"){
//                        for (url in urlList){
//                            if (url.analysed.isNotEmpty()){
//                                analysedImagesList.add(url.analysed)
//                            }
//                            imagesList.add(url.original)
//                        }
//                    }else {
//                        binding.analyse.visibility = View.GONE
//                        imagesList.clear()
//                    }
//                }
//                imageAdapter = ImageAdapter(urlList, this, default, object : ImageAdapter.OnItemClickListener {
//                    override fun onItemClick(position: Int) {
//                        sendToImageView(position)
//                    }
//                })
//                binding.recyclerview.adapter = imageAdapter
//
//                getTotals()
//
//                break
//            }
//        }
    }

    private fun sendToImageView(position: Int) {
        editor.putString(CATEGORY, default)
        editor.apply()

        val actualPosition = if (default == "both") {
            // In "both" mode, convert the position to the original list position
            position / 2
        } else {
            position
        }

//        Toast.makeText(this@GalleryActivity, "category: $default, Position: $actualPosition", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, ImageViewActivity::class.java)
        intent.putExtra("Position", actualPosition)
        intent.putExtra("Model", "No")
        startActivity(intent)
    }

    private fun getTotals () {
        imagesTotal = urlList.size
        imagesAfb = urlList.sumOf { it.afb }
        val times = urlList.sumOf { it.inferenceTime }
        imagesTime = millisecondsToMinutesSeconds(times)
    }

    private fun millisecondsToMinutesSeconds(milliseconds: Long): String {
        val seconds = milliseconds / (60000.0)
        return String.format("%.2f minutes", seconds)
    }

    private fun dispatchTakePictureIntent() {
        imagePath?.let {
            if (it.isNotEmpty()) {
                File(it).delete()
                Log.d("Ocular", "File deleted")
            }
        }

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            var photoFile: File? = null
            try {
                photoFile = createTempImageFile()
                Log.d("Ocular", "Temp file created")
            } catch (e: IOException) {
                Log.e("Ocular", "Error creating temp file", e)
            }

            photoFile?.let { file ->
                val photoURI = FileProvider.getUriForFile(
                    this,
                    "com.ug.air.ocular_tuberculosis",
                    file
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                cameraLauncher.launch(takePictureIntent)
            }
        }
    }

    private fun createTempImageFile(): File {
        val uuid = Functions.generate_uuid()
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "Ocular_${uuid}_$timeStamp"

        // Get the pictures directory in internal storage
        val name = "Ocular/Tuberculosis/${slideName}/original"
        val storageDir = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), name)

        // Create the directory if it doesn't exist
        if (!storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                Log.e("Ocular", "Failed to create directory")
                throw IOException("Failed to create directory")
            }
        }

        // Create the file in the Pictures/OcularDiagnosis directory
        val imageFile = File(storageDir, "$imageFileName.jpg")

        imagePath = imageFile.absolutePath
        Log.d("Ocular", "Image file path: $imagePath")

        return imageFile
    }

    private fun askCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                HomeActivity.CAMERA_PERM_CODE
            )
        } else {
            dispatchTakePictureIntent()
        }
    }

    private fun saveData() {
        val gson = Gson()
        val jsonList = sharedPreferences.getString(IMAGES, null)
        val type = object : TypeToken<ArrayList<Image?>?>() {}.type

        imagesArrayList = if (jsonList != null) {
            gson.fromJson(jsonList, type) ?: ArrayList()
        }else {
            ArrayList()
        }

        Log.d("chodrine", "saveData: $imagePath")

        var slideExists : Boolean = false
        for (image in imagesArrayList) {
            if (image.slideName == slideName) {
                val urls = Urls(imagePath, "", 0, 0, false, false)
                image.images.add(urls)
                slideExists = true
                break
            }
        }

        if (!slideExists) {
            val imagesList = ArrayList<Urls>()
            val urls = Urls(imagePath, "", 0, 0, false, false)
            imagesList.add(urls)
            imagesArrayList.add(Image(slideName, "", imagesList))
        }

        val json2 = gson.toJson(imagesArrayList)
        val slideCount = slideCount + 1
        editor.putInt(SLIDE_COUNT, slideCount)
        editor.putString(IMAGES, json2)
        editor.apply()

        Log.d("chodrines", "saveData: $imagesArrayList")

    }

    private fun loadMediaItemsForBoth(): List<Urls> {
        val originalList = loadMediaItems()
        if (originalList.isEmpty()) return emptyList()

        val combinedList = mutableListOf<Urls>()

        for (url in originalList) {
            combinedList.add(Urls(
                url.original,
                url.analysed,
                url.afb,
                url.inferenceTime,
                true,  // isBothCategory
                false  // isAnalysedView
            ))

            combinedList.add(Urls(
                url.original,
                url.analysed,
                url.afb,
                url.inferenceTime,
                true,  // isBothCategory
                true   // isAnalysedView
            ))
        }

        return combinedList
    }
}