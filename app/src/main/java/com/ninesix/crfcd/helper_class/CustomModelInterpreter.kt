package com.ninesix.crfcd.helper_class

import android.app.ProgressDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.ml.modeldownloader.CustomModel
import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions
import com.google.firebase.ml.modeldownloader.DownloadType
import com.google.firebase.ml.modeldownloader.FirebaseModelDownloader
import kotlinx.coroutines.*
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.collections.ArrayList
import kotlin.coroutines.CoroutineContext

class CustomModelInterpreter(private val context: Context, private val modelName: String): CoroutineScope {
	
	private val job = Job()
	override val coroutineContext: CoroutineContext get() = Dispatchers.Default + job
	
	private val labelList = ArrayList<String>()
	private var numberOfResults: Int = 0
	
	private var interpreter: Interpreter ?= null
	private lateinit var progress: ProgressDialog
	
	companion object {
		private const val TAG = "CustomModelInterpreter"
	}
	
	
	fun cancel() {
		job.cancel()
	}
	
	
	/*fun detailedExecute(imagePath1: String, imagePath2: String, imagePath3: String) = CoroutineScope(Dispatchers.IO).async {
		loadLabelList()
		onPreExecute()
		return@async doInBackground(imagePath)
	}*/
	
	
	suspend fun execute(imagePath: String) = CoroutineScope(Dispatchers.IO).async {
		onPreExecute()
		if(interpreter == null) {
			loadInterpreter()
			loadLabelList()
		}
		return@async doInBackground(imagePath)
	}.await()
	
	
	private fun onPreExecute() {
		(context as AppCompatActivity).runOnUiThread {
			progress = ProgressDialog.show(context, "Analysing", "Please wait, if model is not available locally, will download it first")
		}
	}
	
	
	private suspend fun loadInterpreter(): String = withContext(Dispatchers.IO) {
		val done = CountDownLatch(1)
		val conditions = CustomModelDownloadConditions.Builder().build()
		FirebaseModelDownloader.getInstance()
			.getModel(modelName, DownloadType.LOCAL_MODEL_UPDATE_IN_BACKGROUND, conditions)
			.addOnFailureListener {
				Log.e(TAG, "create: ${it.localizedMessage}")
			}
			.addOnSuccessListener { model: CustomModel ->
				val modelFile = model.file
				if (modelFile != null) {
					interpreter = Interpreter(modelFile)
					done.countDown()
					progress.dismiss()
				}
			}
		done.await()
		return@withContext "Success"
	}
	
	
	private suspend fun doInBackground(imagePath: String): List<ObjectPrediction> = withContext(Dispatchers.IO) {
		
		var bitmap = BitmapFactory.decodeFile(Uri.parse(imagePath).path)
		bitmap = Bitmap.createScaledBitmap(bitmap, 640, 640, false)
		val input = ByteBuffer.allocateDirect(640 * 640 * 3 * 4).order(ByteOrder.nativeOrder())
		
		for (y in 0 until 224) {
			for (x in 0 until 224) {
				val px = bitmap.getPixel(x, y)
				
				// Get channel values from the pixel value.
				val r = Color.red(px)
				val g = Color.green(px)
				val b = Color.blue(px)
				
				// Normalize channel values to [-1.0, 1.0]. This requirement depends on the model.
				// For example, some models might require values to be normalized to the range
				// [0.0, 1.0] instead.
				val rf = (r - 127) / 255f
				val gf = (g - 127) / 255f
				val bf = (b - 127) / 255f
				
				input.putFloat(rf)
				input.putFloat(gf)
				input.putFloat(bf)
			}
		}
		
								/*input[batchNum][x][y][0] = Color.red(pixel) / 255.0f;
								input[batchNum][x][y][1] = Color.green(pixel) / 255.0f;
								input[batchNum][x][y][2] = Color.blue(pixel) / 255.0f;
																													
								input[batchNum][x][y][0] = (Color.red(pixel) - 127) / 128.0f;
								input[batchNum][x][y][1] = (Color.green(pixel) - 127) / 128.0f;
								input[batchNum][x][y][2] = (Color.blue(pixel) - 127) / 128.0f;*/
		
		val locations = arrayOf(Array(numberOfResults) { FloatArray(4) })
		val labelIndices =  arrayOf(FloatArray(numberOfResults))
		val scores =  arrayOf(FloatArray(numberOfResults))
		val outputBuffer = mapOf(0 to locations, 1 to labelIndices, 2 to scores, 3 to FloatArray(1))
		
		Log.d(TAG, "doInBackground: $input")
		
		interpreter?.runForMultipleInputsOutputs(arrayOf(input), outputBuffer)
		
		val predictions = (0 until numberOfResults).map { it ->
			ObjectPrediction(location = locations[0][it].let {                   // The locations are an array of [0, 1] floats for [top, left, bottom, right]
				RectF(it[1], it[0], it[3], it[2])
															 },
				// SSD MobileNet V1 Model assumes class 0 is background class in label file and class labels start from 1 to number_of_classes+1,
				// while outputClasses correspond to class index from 0 to number_of_classes
				label = labelList[0 + labelIndices[0][it].toInt()],
				score = scores[0][it]                               // Score is a single value of [0, 1]
			)
		}
		
		interpreter?.close()
		
		return@withContext predictions
	}
	
	
	private fun loadLabelList() {
		labelList.add("???")
		val db = Firebase.firestore
		
		db.firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()
		
		db.collection("labelList").document(modelName)
			.get(Source.CACHE)
			.addOnSuccessListener { snapshot ->
				snapshot.data?.forEach {
					labelList.add(it.value as String)
				}
				Log.d(TAG, "loadLabelList: $labelList")
			}
			.addOnCompleteListener {
				if (it.isSuccessful) {
					numberOfResults = labelList.size - 1
				}
			}
	}
	
}


		
		/*private val predictions  get() = (0 until numberOfResults).map { it ->
			ObjectPrediction(
				location = locations[0][it].let {                   // The locations are an array of [0, 1] floats for [top, left, bottom, right]
					RectF(it[1], it[0], it[3], it[2])
				},
				// SSD MobileNet V1 Model assumes class 0 is background class in label file and class labels start from 1 to number_of_classes+1,
				// while outputClasses correspond to class index from 0 to number_of_classes
				label = labelList[0 + labelIndices[0][it].toInt()],
				score = scores[0][it]                               // Score is a single value of [0, 1]
			)
		}
		
		

	private fun loadLabelList() {
		labelList.add("???")
		val db = Firebase.firestore
		
		db.firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()
		
		db.collection("labelList").document(modelName)
			.get(Source.CACHE)
			.addOnSuccessListener { snapshot ->
				snapshot.data?.forEach {
					labelList.add(it.value as String)
				}
				Log.d(TAG, "loadLabelList: $labelList")
			}
			.addOnCompleteListener {
				if (it.isSuccessful) {
					numberOfResults = labelList.size - 1
					downloadModel()
				}
			}
		
		*//*val fileInputStream = FileInputStream(File("$modelName.txt"))
		val reader = BufferedReader(InputStreamReader(fileInputStream))
		val labelList = ArrayList<String>()
		reader.use {
			while (true) {
				val line = reader.readLine() ?: break
				labelList.add(line)
			}
		}
		return labelList
	}
	
	
	private fun downloadModel() {
//		progress.setTitle("Downloading")
//		progress.setMessage("Please wait, downloading the latest version of the model")
//		val progress = ProgressDialog.show(context, "Downloading", "Please wait, downloading the latest version of the model")
		val conditions = CustomModelDownloadConditions.Builder().build()
		FirebaseModelDownloader.getInstance()
			.getModel(modelName, DownloadType.LOCAL_MODEL_UPDATE_IN_BACKGROUND, conditions)
			.addOnFailureListener {
				Log.e(TAG, "create: ${it.localizedMessage}")
			}
			.addOnSuccessListener { model: CustomModel ->
				val modelFile = model.file
				if(modelFile!= null) {
					interpreter = Interpreter(modelFile)
					isInterpreterReady = true
//					progress.dismiss()
				}
			}
	}
	
	fun checkForInterpreter(imagePath: String) {
		CoroutineScope(IO).launch {
			delay(500)
			CoroutineScope(Main).launch {
				if(isInterpreterReady)
					runInterpreter()
				else
					checkForInterpreter(imagePath)
			}
		}
	}
	
	
	
	fun runInterpreter(imagePath: String): List<ObjectPrediction> {
		var bitmap = BitmapFactory.decodeFile(Uri.parse(imagePath).path)
		bitmap = Bitmap.createScaledBitmap(bitmap, 640, 640, false)
		val input = ByteBuffer.allocateDirect(640 * 640 * 3 * 4).order(ByteOrder.nativeOrder())
		
		for (y in 0 until 224) {
			for (x in 0 until 224) {
				val px = bitmap.getPixel(x, y)
				
				// Get channel values from the pixel value.
				val r = Color.red(px)
				val g = Color.green(px)
				val b = Color.blue(px)
				
				// Normalize channel values to [-1.0, 1.0]. This requirement depends on the model.
				// For example, some models might require values to be normalized to the range
				// [0.0, 1.0] instead.
				val rf = (r - 127) / 255f
				val gf = (g - 127) / 255f
				val bf = (b - 127) / 255f
				
				input.putFloat(rf)
				input.putFloat(gf)
				input.putFloat(bf)
			}
		}
		
																													/*input[batchNum][x][y][0] = Color.red(pixel) / 255.0f;
																													input[batchNum][x][y][1] = Color.green(pixel) / 255.0f;
																													input[batchNum][x][y][2] = Color.blue(pixel) / 255.0f;
																													
																													input[batchNum][x][y][0] = (Color.red(pixel) - 127) / 128.0f;
																													input[batchNum][x][y][1] = (Color.green(pixel) - 127) / 128.0f;
																													input[batchNum][x][y][2] = (Color.blue(pixel) - 127) / 128.0f;*/
		
		val locations = arrayOf(Array(numberOfResults) { FloatArray(4) })
		val labelIndices =  arrayOf(FloatArray(numberOfResults))
		val scores =  arrayOf(FloatArray(numberOfResults))
		val outputBuffer = mapOf(0 to locations, 1 to labelIndices, 2 to scores, 3 to FloatArray(1))
		
		interpreter.runForMultipleInputsOutputs(arrayOf(input), outputBuffer)
		
		return (0 until numberOfResults).map { it ->
			ObjectPrediction(
				location = locations[0][it].let {                   // The locations are an array of [0, 1] floats for [top, left, bottom, right]
					RectF(it[1], it[0], it[3], it[2])
				},
				// SSD MobileNet V1 Model assumes class 0 is background class in label file and class labels start from 1 to number_of_classes+1,
				// while outputClasses correspond to class index from 0 to number_of_classes
				label = labelList[0 + labelIndices[0][it].toInt()],
				score = scores[0][it]                               // Score is a single value of [0, 1]
			)
//		}
//	}

}
*/