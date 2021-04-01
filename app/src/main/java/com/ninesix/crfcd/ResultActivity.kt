package com.ninesix.crfcd

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.ninesix.crfcd.helper_class.*
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule
import kotlin.coroutines.CoroutineContext

class ResultActivity : AppCompatActivity(), CoroutineScope {
	
	companion object {
		private const val TAG = "ResultActivity"
	}
	
	//---------------------------------------------------------------------------------------- VARIABLES ----------------------------------------------------------------------------------------//
	override val coroutineContext: CoroutineContext get() = Job() + Dispatchers.Default
	
	
	//----------------------------- HOOKS -----------------------------//
	private lateinit var viewPager2 : ViewPager2
	private lateinit var tabLayout : TabLayout
	
	
	//----------------------------- VIEWPAGER / TAB LAYOUT -----------------------------//
	private lateinit var viewPagerAdapter: ViewPagerAdapter
	private val dataForViewPager = ArrayList<ViewPagerData?>()
	
	
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_result)
		
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		
		
		//----------------------------- HOOKS -----------------------------//
		viewPager2 = findViewById(R.id.viewPager2)
		tabLayout = findViewById(R.id.tabLayout)
		
		viewPagerAdapter = ViewPagerAdapter(this, dataForViewPager)
		
		viewPager2.adapter = viewPagerAdapter
		
		TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
			when (position) {
				0 -> tab.text = "Front"
				1 -> tab.text = "White Light"
				2 -> tab.text = "Back"
				3 -> tab.text = "Result"
			}
		}.attach()
		tabLayout.tabGravity = TabLayout.GRAVITY_FILL
		
		
		val modelName = intent.getStringExtra("modelName")
		var frontPath = intent.getStringExtra("front")
		var whiteLightPath = intent.getStringExtra("WL")
		var backPath = intent.getStringExtra("back")
		
		frontPath = "file:///storage/emulated/0/Android/media/com.ninesix.crfcd/CRFCD/1.jpg"
		whiteLightPath = "file:///storage/emulated/0/Android/media/com.ninesix.crfcd/CRFCD/2021-04-01-11-10-24-439.jpg"
		backPath = "file:///storage/emulated/0/Android/media/com.ninesix.crfcd/CRFCD/2021-04-01-11-11-08-752.jpg"
		
		
		val options = BitmapFactory.Options()
		options.inMutable = true
		
		val imagePaths = ArrayList<String>()
		imagePaths.add(frontPath)
		imagePaths.add(whiteLightPath)
		imagePaths.add(backPath)
		val modelNames = ArrayList<String>()
		modelNames.add(modelName + "FrontNormal")
		modelNames.add(modelName + "WhiteLight")
		modelNames.add(modelName + "WhiteLight")
		
		CoroutineScope(Dispatchers.IO).launch {
			val customModelInterpreter = CustomModelInterpreter()
			val allPredictions = customModelInterpreter.detailedExecute(imagePaths, modelNames)
			for ((index, data) in allPredictions.withIndex()) {
				val predictions = data.toMutableList()
				Log.d(TAG, "onCreate: ${predictions.size}")
				predictions.add(0, ObjectPrediction(RectF(0f, 0f, 0f, 0f), "null", 0f))
				var bitmap = BitmapFactory.decodeFile(Uri.parse(frontPath).path, options)
				bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.width / 3, bitmap.height / 3, false)
				dataForViewPager.add(index, ViewPagerData(predictions, bitmap))
				runOnUiThread { viewPagerAdapter.notifyItemChanged(index) }
			}
			dataForViewPager.add(3, null)
			runOnUiThread { viewPagerAdapter.notifyItemChanged(2) }
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	/*private fun loadModel(model_path: String, label_path: String, numberOfResults: Int, type: String, imagePath: String) = launch {
		withContext(Dispatchers.IO) {
			val imageClassification = ImageClassification.create(
				classifierModel = ClassifierModel.FLOAT,
				assetManager = assets,
				modelPath = model_path,
				labelPath = label_path,
				numberOfResults = numberOfResults
			)
			
			retrieveBitmap(imagePath, imageClassification, type)
		}
	}
	
	
	private fun retrieveBitmap(path: String, imageClassification: ImageClassification, type: String) {
		val options = BitmapFactory.Options()
		options.inMutable = true
		Log.d(TAG, "retrieveBitmap: $path")
		var bitmap = BitmapFactory.decodeFile(path.substring(8), options)
		val width = bitmap.width
		val height = bitmap.height
		bitmap = Bitmap.createBitmap(bitmap, 0, bitmap.height * 30 / 100, bitmap.width, bitmap.height * 40 / 100)
		bitmap = Bitmap.createScaledBitmap(bitmap, 640, 640, false)
		
		analyze(bitmap, width, height, imageClassification, type)
	}
	
	
	private fun analyze(bitmap: Bitmap, width: Int, height: Int, imageClassification: ImageClassification, type: String) = launch {
		val predictions = async { imageClassification.classifyImage(bitmap) }
		
		val modifiedPredictions = predictions.await().toMutableList()
		
//		modifiedPredictions.add(0, CustomModelInterpreter.ObjectPrediction(modifiedPredictions[1].location, "null", 0f))
		
		Timer().schedule(2000) { imageClassification.close() }
		
		var index: Int? = null
		when (type) {
			"normal" -> {
				dataForViewPager.add(0, ViewPagerData(modifiedPredictions, bitmap))
				index = 0
			}
			
			"white" -> {
				dataForViewPager.add(1, ViewPagerData(modifiedPredictions, bitmap))
				index= 1
			}
			"uv" -> {
				dataForViewPager.add(2, ViewPagerData(modifiedPredictions, bitmap))
				index =2
			}
		}
		if (index != null) {
			runOnUiThread { viewPagerAdapter.notifyItemInserted(index) }
		}
		
		if(dataForViewPager.size == 3) {
			dataForViewPager.add(3, null)
			runOnUiThread { viewPagerAdapter.notifyItemInserted(3) }
		}
		
	}*/
	
	
}
