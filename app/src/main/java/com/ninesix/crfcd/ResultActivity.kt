package com.ninesix.crfcd

import android.graphics.*
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.ninesix.crfcd.helper_class.CustomModelInterpreter
import com.ninesix.crfcd.helper_class.ObjectPrediction
import com.ninesix.crfcd.helper_class.ResultViewPagerAdapter
import com.ninesix.crfcd.helper_class.ViewPagerData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
	private lateinit var viewPagerAdapter: ResultViewPagerAdapter
	private val dataForViewPager = ArrayList<ViewPagerData?>()
	
	
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_result)
		
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		
		
		//----------------------------- HOOKS -----------------------------//
		viewPager2 = findViewById(R.id.viewPager2)
		tabLayout = findViewById(R.id.tabLayout)
		
		viewPagerAdapter = ResultViewPagerAdapter(this, dataForViewPager)
		
		viewPager2.adapter = viewPagerAdapter
		
		TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
			when (position) {
				0 -> tab.text = "Front"
				1 -> tab.text = "White Light"
				2 -> tab.text = "Back"
				3 -> tab.text = "Advanced"
			}
		}.attach()
		tabLayout.tabGravity = TabLayout.GRAVITY_FILL
		
		
//		val currency = intent.getStringExtra("currency")
//		val frontPath = intent.getStringExtra("front")
//		val whiteLightPath = intent.getStringExtra("WL")
//		val backPath = intent.getStringExtra("back")
		
//		Log.d(TAG, "onCreate: $currency $frontPath $whiteLightPath $backPath")
		
		val currency = "rs_100_new"
		val frontPath = "file:///storage/emulated/0/Android/media/com.ninesix.crfcd/CRFCD/100NF1.jpg"
		val whiteLightPath = "file:///storage/emulated/0/Android/media/com.ninesix.crfcd/CRFCD/100NFW1.jpg"
		val backPath = "file:///storage/emulated/0/Android/media/com.ninesix.crfcd/CRFCD/100NB1.jpg"
		
//		/mnt/sdcard/Android/media/com.ninesix.crfcd/CRFCD/100NF1.jpg
		
		/*CoroutineScope(Dispatchers.IO).launch {
			var bitmap = BitmapFactory.decodeFile(Uri.parse(frontPath).path)
			bitmap = Bitmap.createBitmap(bitmap, 0, bitmap.height * 30 / 100, bitmap.width, bitmap.height * 70 / 100)
			if (frontPath != null) {
				CustomModelInterpreter().execute(frontPath, "all")
			}
		}*/
		
		/*CoroutineScope(Dispatchers.IO).launch {
			val customModelInterpreter = CustomModelInterpreter()
			val advancedPredictions = customModelInterpreter.advancedExecute(frontPath, "100NewFrontNormal")
			advancedPredictions.forEach { it ->
				it.forEach {
					println("${it.label} : ${it.score} : ${it.location}")
				}
				println("\n\n\n\n")
			}
		}*/
		
		val options = BitmapFactory.Options()
		options.inMutable = true
		
		/*val imagePaths = ArrayList<String>()
		if (frontPath != null) {
			imagePaths.add(frontPath)
		}
		if (whiteLightPath != null) {
			imagePaths.add(whiteLightPath)
		}
		if (backPath != null) {
			imagePaths.add(backPath)
		}
		var modelNames = arrayListOf<String>()
		if (currency != null) {
			modelNames = getModelNames(currency)
		}*/
		
		CoroutineScope(Dispatchers.IO).launch {
			val customModelInterpreter = CustomModelInterpreter(this@ResultActivity)
			val bitmaps = ArrayList<Bitmap>()
			
			
			/*val recognizer = TextRecognition.getClient()
			recognizer.process(InputImage.fromBitmap(bitmap, 0))
				.addOnSuccessListener {
					Log.d(TAG, "doInBackground: ${it.text}")
				}*/
			
			/*fun Bitmap.rotate(degrees: Float): Bitmap {
				val matrix = Matrix().apply { postRotate(degrees) }
				return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
			}
			val rotatedBitmap = bitmap.rotate(-90f)*/
			
			/*runOnUiThread { imageView.setImageBitmap(rotatedBitmap) }
			recognizer.process(InputImage.fromBitmap(rotatedBitmap, 0))
				.addOnSuccessListener {
					Log.d(TAG, "doInBackground: rotated: ${it.text}")
				}*/
			
			var bitmap = BitmapFactory.decodeFile(frontPath.substring(8), options)
			bitmap = Bitmap.createScaledBitmap(bitmap, 640, 640, false)
			bitmaps.add(bitmap)
			
			bitmap = BitmapFactory.decodeFile(whiteLightPath.substring(8), options)
			bitmap = Bitmap.createScaledBitmap(bitmap, 640, 640, false)
			bitmaps.add(bitmap)
			
			bitmap = BitmapFactory.decodeFile(backPath.substring(8), options)
			bitmap = Bitmap.createScaledBitmap(bitmap, 640, 640, false)
			bitmaps.add(bitmap)
			
			
			val allPredictions = customModelInterpreter.detailedExecute(bitmaps, getModelNames(currency!!))
			/*val canvas = Canvas(bitmap)
			val paint = Paint()
			paint.alpha = 0xA0
			paint.color = Color.MAGENTA
			paint.style = Paint.Style.STROKE
			paint.textSize = 30f
			paint.strokeWidth = 2f
			for(prediction in allPredictions[0]) {
				Log.d(TAG, "onCreate: prediction: $prediction : ${prediction.location.left * 640} ${prediction.location.top * 640} ${prediction.location.right * 640} ${prediction.location.bottom * 640}")
				canvas.drawRect(
					prediction.location.left * 640,
					prediction.location.top * 640,
					prediction.location.right * 640,
					prediction.location.bottom * 640,
					paint
				)
				canvas.drawText(prediction.label, prediction.location.left * 640, prediction.location.top * 640 - 5, paint)
			}
			runOnUiThread { imageView.setImageBitmap(bitmap) }*/
			
			
			for ((index, data) in allPredictions.withIndex()) {
				val predictions = data.toMutableList()
				Log.d(TAG, "onCreate: ${predictions.size}")
				predictions.add(0, ObjectPrediction(RectF(0f, 0f, 0f, 0f), "null", 0f))
				dataForViewPager.add(index, ViewPagerData(predictions, bitmaps[index]))
				runOnUiThread { viewPagerAdapter.notifyItemChanged(index) }
			}
//			dataForViewPager.add(3, null)
//			runOnUiThread { viewPagerAdapter.notifyItemChanged(2) }
		}
	}
	
	
	
	
	private fun getModelNames(currency: String) : ArrayList<String> {
		when(currency) {
			"rs_100_new" -> {
				return arrayListOf<String>("100NewFront", "white100NewFront", "100NewBack")
			}
			"rs_100_old" -> {
				return arrayListOf("100OldFront", "white100OldFront", "100OldBack")
			}
			"rs_200_new" -> {
				return arrayListOf("200NewFront", "white200NewFront", "200NewBack")
			}
			"rs_500_new" -> {
				return arrayListOf("all", "500NewFront", "white500NewFront", "500NewBack")
			}
			"rs_2000_new" -> {
				return arrayListOf("2000NewFront", "white2000NewFront", "2000NewBack")
			}
		}
		return arrayListOf()
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
