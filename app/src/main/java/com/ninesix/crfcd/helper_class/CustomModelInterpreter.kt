package com.ninesix.crfcd.helper_class

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.ml.modeldownloader.CustomModel
import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions
import com.google.firebase.ml.modeldownloader.DownloadType
import com.google.firebase.ml.modeldownloader.FirebaseModelDownloader
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.collections.ArrayList
import kotlin.coroutines.CoroutineContext

class CustomModelInterpreter : CoroutineScope {
	
	private val job = Job()
	override val coroutineContext: CoroutineContext get() = Dispatchers.Default + job
	
	private var startTime : Long = 0L
	
	companion object {
		private const val TAG = "CustomModelInterpreter"
	}
	
	
	fun cancel() {
		job.cancel()
	}
	
	
	suspend fun detailedExecute(imagePaths: ArrayList<String>, modelNames: ArrayList<String>) = CoroutineScope(IO).async {
		onPreExecute()
		val allPredictions = ArrayList<List<ObjectPrediction>>()
		for((index, imagePath) in imagePaths.withIndex()) {
			modelNames[index] = "100NewFrontNormal"
			val interpreter = loadInterpreter(modelNames[index])
			val labelList = loadLabelList(modelNames[index])
			allPredictions.add(index, doInBackground(imagePath, interpreter,  labelList))
		}
		onPostExecute()
		return@async allPredictions
	}.await()
	
	
	suspend fun execute(imagePath: String, modelName: String = "MainModel") = CoroutineScope(IO).async {
		onPreExecute()
		val interpreter = loadInterpreter(modelName)
		val labelList = loadLabelList(modelName)
		val predictions = doInBackground(imagePath, interpreter, labelList)
		onPostExecute()
		return@async predictions
	}.await()
	
	
	private fun onPreExecute() {
		startTime = System.currentTimeMillis()
	}
	
	
	private suspend fun loadInterpreter(modelName: String) : Interpreter = withContext(IO) {
		val done = CountDownLatch(1)
		var interpreter: Interpreter? = null
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
				}
			}
		done.await()
		return@withContext interpreter!!
	}
	
	
	private suspend fun doInBackground(imagePath: String, interpreter: Interpreter, labelList: ArrayList<String>): List<ObjectPrediction> = withContext(IO) {
		
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
		
		val numberOfResults = labelList.size - 1
		
		val locations = arrayOf(Array(numberOfResults) { FloatArray(4) })
		val labelIndices =  arrayOf(FloatArray(numberOfResults))
		val scores =  arrayOf(FloatArray(numberOfResults))
		val outputBuffer = mapOf(0 to locations, 1 to labelIndices, 2 to scores, 3 to FloatArray(1))
		
		Log.d(TAG, "doInBackground: $input")
		
		try {
			interpreter.runForMultipleInputsOutputs(arrayOf(input), outputBuffer)
		} catch (e : Exception) {
			Log.e(TAG, "doInBackground: ${e.localizedMessage} \n ${e.stackTrace}")
		}
		
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
		
		interpreter.close()
		
		return@withContext predictions
	}
	
	
	private fun onPostExecute() {
		Log.d(TAG, "onPostExecute: EXECUTION TOOK ${(System.currentTimeMillis() - startTime).toFloat()/1000f} seconds")
	}
	
	
	private suspend fun loadLabelList(modelName: String) = withContext(IO) {
		val done = CountDownLatch(1)
		val labelList = ArrayList<String>()
		labelList.add("???")
		val db = Firebase.firestore
		
		db.firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()
		
		db.collection("labelList").document(modelName)
			.get(Source.CACHE)
			.addOnSuccessListener { snapshot ->
				snapshot.data?.forEach {
					labelList.add(it.value as String)
				}
				done.countDown()
			}
		done.await()
		return@withContext labelList
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