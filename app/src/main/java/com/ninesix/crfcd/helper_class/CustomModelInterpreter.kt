  package com.ninesix.crfcd.helper_class

import android.app.AlertDialog
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
import kotlinx.coroutines.Dispatchers.IO
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.collections.ArrayList
import kotlin.coroutines.CoroutineContext


  class CustomModelInterpreter(private val context: Context) : CoroutineScope {
	  
	  private val job = Job()
	  override val coroutineContext: CoroutineContext get() = Dispatchers.Default + job
	
	private var startTime : Long = 0L
	  private lateinit var alertDialog: AlertDialog
	
	companion object {
		private const val TAG = "CustomModelInterpreter"
	}
	
	
	fun cancel() {
		job.cancel()
	}
	
	
	suspend fun detailedExecute(bitmaps: ArrayList<Bitmap>, modelNames: ArrayList<String>) = CoroutineScope(IO).async {
		onPreExecute()
		val allPredictions = ArrayList<List<ObjectPrediction>>()
		(context as AppCompatActivity).runOnUiThread {
			alertDialog = AlertDialog.Builder(context).create()
			alertDialog.setTitle("Analyzing")
			alertDialog.setMessage("Analyzing the images")
			alertDialog.show()
		}
		for((index, bitmap) in bitmaps.withIndex()) {
			val interpreter = loadInterpreter(modelNames[index])
			val labelList = loadLabelList(modelNames[index])
			allPredictions.add(index, doInBackground(interpreter, labelList, bitmap, modelNames[index]))
		}
		context.runOnUiThread {
			alertDialog.cancel()
		}
		onPostExecute()
		return@async allPredictions
	}.await()
	
	
	suspend fun execute(imagePath: String, modelName: String) = CoroutineScope(IO).async {
		onPreExecute()
		(context as AppCompatActivity).runOnUiThread {
			alertDialog = AlertDialog.Builder(context).create()
			alertDialog.setTitle("Analyzing")
			alertDialog.setMessage("Analyzing the image")
			alertDialog.show()
		}
		val interpreter = loadInterpreter(modelName)
		val labelList = loadLabelList("all")
		val bitmap = BitmapFactory.decodeFile(Uri.parse(imagePath).path)
		val predictions = doInBackground(interpreter, labelList, bitmap, modelName)
		context.runOnUiThread {
			alertDialog.cancel()
		}
		onPostExecute()
		interpreter.close()
		return@async predictions
	}.await()
	
	
	/*suspend fun advancedExecute(imagePath: String, modelName: String) = CoroutineScope(IO).async {
		onPreExecute()
		val advancedPredictions = ArrayList<List<ObjectPrediction>>()
		val bitmap = BitmapFactory.decodeFile(Uri.parse(imagePath).path)
		val width = bitmap.width
		val height = bitmap.height
		val x = arrayOf(0, width / 4, width / 2, width / 4, 0, width / 4, width / 2)
		val y = arrayOf(0, 0, 0, height / 4, height / 2, height / 2, height / 2)
		
		Log.d(TAG, "advancedExecute: $width $height")
		x.forEach { println(it) }
		y.forEach { println(it) }
		
		val interpreter = loadInterpreter(modelName)
		val labelList = loadLabelList(modelName)
		
		for(i in 0 until 7) {
			var smallBitmap = Bitmap.createBitmap(bitmap, x[i], y[i], width / 2, height / 2)
			smallBitmap = Bitmap.createScaledBitmap(smallBitmap, 640, 640, false)
			advancedPredictions.add(i, doInBackground(interpreter, labelList, smallBitmap, modelName))
		}
		onPostExecute()
		interpreter.close()
		return@async advancedPredictions
	}.await()*/
	
	
	private fun onPreExecute() {
		startTime = System.currentTimeMillis()
	}
	
	
	private suspend fun loadInterpreter(modelName: String) : Interpreter = withContext(IO) {
		(context as AppCompatActivity).runOnUiThread {
			if (!context.getSharedPreferences("modelDownloads", Context.MODE_PRIVATE).getBoolean(modelName, false)) {
				alertDialog = AlertDialog.Builder(context).create()
				alertDialog.setTitle("Downloading")
				alertDialog.setMessage("Please wait, model '${modelName}' is downloading \nFor details see notification area")
				alertDialog.show()
			}
		}
		val done = CountDownLatch(1)
		var interpreter: Interpreter? = null
		val conditions = CustomModelDownloadConditions.Builder().build()
		FirebaseModelDownloader.getInstance()
			.getModel(modelName, DownloadType.LOCAL_MODEL_UPDATE_IN_BACKGROUND, conditions)
			.addOnFailureListener {
				Log.d(TAG, "loadInterpreter: ${it.localizedMessage}")
			}
			.addOnSuccessListener { model: CustomModel ->
				context.getSharedPreferences("modelDownloads", Context.MODE_PRIVATE).edit().putBoolean(modelName, true).apply()
				context.runOnUiThread {
					try {
						alertDialog.cancel()
					} catch (e: Exception) {  }
				}
				val modelFile = model.file
				if (modelFile != null) {
					interpreter = Interpreter(modelFile)
					done.countDown()
				}
			}
		done.await()
		return@withContext interpreter!!
	}
	
	
	private suspend fun doInBackground(interpreter: Interpreter, labelList: ArrayList<String>, bitmap: Bitmap, modelName: String): List<ObjectPrediction> = withContext(IO) {
		
		val input = ByteBuffer.allocateDirect(640 * 640 * 3 * 4).order(ByteOrder.nativeOrder())
		
		for (y in 0 until 640) {
			for (x in 0 until 640) {
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
		
		val numberOfResults = if(modelName.contains("all")) 1 else labelList.size
//		val numberOfResults = labelList.size
		
		val locations = arrayOf(Array(numberOfResults) { FloatArray(4) })
		val labelIndices =  arrayOf(FloatArray(numberOfResults))
		val scores =  arrayOf(FloatArray(numberOfResults))
		val outputBuffer = mapOf(0 to locations, 1 to labelIndices, 2 to scores, 3 to FloatArray(1))
		
		try {
			interpreter.runForMultipleInputsOutputs(arrayOf(input), outputBuffer)
		} catch (e: Exception) {
			Log.e(TAG, "doInBackground: ${e.localizedMessage} \n $e")
		}
		
		val predictions = (0 until numberOfResults).map { it ->
			ObjectPrediction(
				location = locations[0][it].let {                   // The locations are an array of [0, 1] floats for [top, left, bottom, right]
					RectF(it[1], it[0], it[3], it[2])
				},
				// SSD MobileNet V1 Model assumes class 0 is background class in label file and class labels start from 1 to number_of_classes+1,
				// while outputClasses correspond to class index from 0 to number_of_classes
				label = labelList[0 + labelIndices[0][it].toInt()],
				score = scores[0][it] // Score is a single value of [0, 1]
			)
		}
		
		Log.d(TAG, "doInBackground: $predictions")
		
		return@withContext predictions
	}
	
	
	private fun onPostExecute(){
		Log.d(TAG, "onPostExecute: EXECUTION TOOK ${(System.currentTimeMillis() - startTime).toFloat() / 1000f} seconds")
	}
	
	
	private suspend fun loadLabelList(modelName: String) = withContext(IO) {
		val done = CountDownLatch(1)
		val labelList = ArrayList<String>()
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
			.addOnFailureListener {
				Log.d(TAG, "loadLabelList: $it")
			}
		done.await()
		return@withContext labelList
	}
	
}
