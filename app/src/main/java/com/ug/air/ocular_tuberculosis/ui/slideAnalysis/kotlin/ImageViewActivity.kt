package com.ug.air.ocular_tuberculosis.ui.slideAnalysis.kotlin

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ug.air.ocular_tuberculosis.R
import com.ug.air.ocular_tuberculosis.adapters.ViewPagerAdapter
import com.ug.air.ocular_tuberculosis.databinding.ActivityImageViewBinding
import com.ug.air.ocular_tuberculosis.models.Image
import com.ug.air.ocular_tuberculosis.models.Urls
import com.ug.air.ocular_tuberculosis.ui.HomeActivity.CATEGORY
import com.ug.air.ocular_tuberculosis.ui.HomeActivity.IMAGES
import com.ug.air.ocular_tuberculosis.ui.HomeActivity.SLIDE_NAME
import com.ug.air.ocular_tuberculosis.ui.slideAnalysis.CameraActivity.GALLERY
import com.ug.air.ocular_tuberculosis.utils.kotlinFinder.Detector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImageViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageViewBinding
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var urlList: ArrayList<Urls>
    private lateinit var slideName: String
    private lateinit var category: String
    private var position: Int = 0
    private lateinit var dialog: AlertDialog
    private var autoScrollJob: Job? = null
    private var hasStartedAutoScroll = false
    private var isActive = true
    private var model: String? = null
    private val detector by lazy { Detector(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityImageViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPreferences = getSharedPreferences(GALLERY, MODE_PRIVATE)

        slideName = sharedPreferences.getString(SLIDE_NAME, "") ?: ""
        category = sharedPreferences.getString(CATEGORY, "") ?: ""

        val intValue = intent.getIntExtra("Position", 0)
        model = intent.getStringExtra("Model")
//        Toast.makeText(this, "All images processed $model", Toast.LENGTH_SHORT).show()

        loadData(intValue);

        binding.viewpager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
            }

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
            }
        })

    }

    private fun loadData(position: Int) {
        val gson = Gson()
        val jsonList = sharedPreferences.getString(IMAGES, null)

        val type = object : TypeToken<ArrayList<Image>>() {}.type
        val imagesArrayList: ArrayList<Image> = gson.fromJson(jsonList, type) ?: ArrayList()

        for (image in imagesArrayList) {
            if (image.slideName == slideName) {
                if (model.equals("Yes")) {
                    urlList = image.images
                        .reversed()
                        .filter { it.analysed == "" }
                        .toMutableList() as ArrayList<Urls>

                    viewPagerAdapter = ViewPagerAdapter(urlList, this, category, position)

                    binding.viewpager.adapter = viewPagerAdapter
                    binding.viewpager.setCurrentItem(position, false)
                    binding.cardView.visibility = View.VISIBLE

                    if (urlList.isNotEmpty()) {
                        updateProcessingText()

                        viewPagerAdapter.setOnImageLoadListener { bitmap, filePath ->
                            lifecycleScope.launch {
                                try {
                                    // Process the current image
                                    val processedBitmap = detector.processImage(bitmap, filePath)

                                    // Update UI and move to next image on UI thread
                                    withContext(Dispatchers.Main) {
                                        moveToNextImage()
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            this@ImageViewActivity,
                                            "Error processing image: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        moveToNextImage() // Still move to next image on error
                                    }
                                }
                            }
                        }
                    }
                    else {
                        Toast.makeText(this, "All images were processed", Toast.LENGTH_SHORT).show()
//                        finish()
                        putString()
                    }

                }else {
                    urlList = image.images.reversed().toMutableList() as ArrayList<Urls>

                    viewPagerAdapter = ViewPagerAdapter(urlList, this, category, position)

                    binding.viewpager.adapter = viewPagerAdapter
                    binding.viewpager.setCurrentItem(position, false)
                }

                break
            }
        }
    }

    private fun updateProcessingText() {
        "Analysing image ${binding.viewpager.currentItem + 1} / ${urlList.size}".also { binding.cardText.text = it }
    }

    private fun moveToNextImage() {
        val currentItem = binding.viewpager.currentItem

        // Check if we're at the last item
        if (currentItem < urlList.size - 1) {
            // Move to next item
            binding.viewpager.setCurrentItem(currentItem + 1, true)
            updateProcessingText()
        } else {
            // We've reached the end
            // Maybe show a message or go back to the first image
            binding.cardView.visibility = View.GONE
            Toast.makeText(this, "All images processed", Toast.LENGTH_SHORT).show()
//            startActivity(Intent(this, GalleryActivity::class.java))
//            finish()
            putString()
        }
    }

    private fun putString () {
        val intent = Intent(this, GalleryActivity::class.java)
        intent.putExtra("Screen", "both")
        startActivity(intent)
    }
}