package com.ysn.sampleopticalcharacterrecognition

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.io.InputStream

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private val TAG = javaClass.simpleName
    private var mSelectedImage: Bitmap? = null

    // Max width (portrait mode)
    private var mImageMaxWidth: Int? = null

    // Max height (portrait mode)
    private var mImageMaxHeight: Int? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
    }

    private fun initViews() {
        // init spinner items
        val items = arrayOf("Image 1", "Image 2", "Image 3")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)
        spinner.adapter = adapter
        spinner.onItemSelectedListener = this

        // setButton click
        button_text.setOnClickListener {
            runTextRecognition()
        }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        /* nothing to do in here */
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        graphic_overlay.clear()
        when (position) {
            0 -> mSelectedImage = getBitmapFromAsset(this, "test_image_one.jpg")
            1 -> mSelectedImage = getBitmapFromAsset(this, "test_image_two.jpg")
            2 -> mSelectedImage = getBitmapFromAsset(this, "test_image_three.jpg")
        }

        mSelectedImage?.let {
            // Get the dimensions of the View
            val targetedSize = getTargetWidthHeight()

            val targetWidth = targetedSize.first
            val maxHeight = targetedSize.second

            // Determine how much to scale down the image
            val scaleFactor = Math.max(
                    it.width.toFloat() / targetWidth.toFloat(),
                    it.height.toFloat() / maxHeight.toFloat()
            )

            val resizeBitmap = Bitmap.createScaledBitmap(
                    it,
                    (it.width / scaleFactor).toInt(),
                    (it.height / scaleFactor).toInt(),
                    true
            )

            image_view.setImageBitmap(resizeBitmap)
            mSelectedImage = resizeBitmap
        }
    }

    private fun getTargetWidthHeight(): Pair<Int, Int> {
        val targetWidth: Int
        val targetHeight: Int
        val maxWidthForPortraitMode = getImageMaxWidth()
        val maxHeightForPortraitMode = getImageMaxHeight()
        targetWidth = maxWidthForPortraitMode!!
        targetHeight = maxHeightForPortraitMode!!
        return Pair(targetWidth, targetHeight)
    }

    private fun getImageMaxWidth(): Int? {
        if (mImageMaxWidth == null) {
            mImageMaxWidth = image_view.width
        }
        return mImageMaxWidth
    }

    private fun getImageMaxHeight(): Int? {
        if (mImageMaxHeight == null) {
            mImageMaxHeight = image_view.height
        }
        return mImageMaxHeight
    }

    private fun getBitmapFromAsset(context: Context, filePath: String): Bitmap? {
        val assetManager = context.assets

        val inputStream: InputStream
        var bitmap: Bitmap? = null
        try {
            inputStream = assetManager.open(filePath)
            bitmap = BitmapFactory.decodeStream(inputStream)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return bitmap
    }

    private fun runTextRecognition() {
        val image = mSelectedImage?.let { FirebaseVisionImage.fromBitmap(it) }
        val textRecognizer = FirebaseVision.getInstance()
                .onDeviceTextRecognizer
        button_text.isEnabled = false
        textRecognizer.processImage(image!!)
                .addOnSuccessListener {
                    button_text.isEnabled = true
                    processTextRecognitionResult(it)
                }
                .addOnFailureListener {
                    button_text.isEnabled = true
                    showToast(it.localizedMessage)
                }
    }

    private fun processTextRecognitionResult(firebaseVisionText: FirebaseVisionText?) {
        firebaseVisionText?.let {
            Log.d(TAG, "firebaseVisionText: ${it.text}")
            val blocks = firebaseVisionText.textBlocks
            when (blocks.size) {
                0 -> showToast("No texts found")
                else -> {
                    graphic_overlay.clear()
                    for (block in blocks.indices) {
                        val lines = blocks[block].lines
                        for (line in lines.indices) {
                            val elements = lines[line].elements
                            for (element in elements.indices) {
                                val textGraphic = TextGraphic(graphic_overlay, elements[element])
                                graphic_overlay.add(textGraphic)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun Context.showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT)
                .show()
    }

}
