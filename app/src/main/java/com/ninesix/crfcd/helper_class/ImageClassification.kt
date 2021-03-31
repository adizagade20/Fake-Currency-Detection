package com.ninesix.crfcd.helper_class

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Build
import androidx.annotation.RequiresApi
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder


enum class ClassifierModel {
	FLOAT,
	QUANTIZED
}

abstract class ImageClassification protected constructor(
	val interpreter: Interpreter,
	val labelList: List<String>,
	private val inputSize: Int,
	private val numberOfResults: Int,
	private val confidenceThreshold: Float
) {
	
	protected val imageByteBuffer: ByteBuffer by lazy {
		ByteBuffer.allocateDirect(byteNumbersPerChannel() * BATCH_SIZE * inputSize * inputSize * PIXEL_SIZE).order(ByteOrder.nativeOrder())
	}

	fun classifyImage(bitmap: Bitmap): List<ObjectPrediction> {
		convertBitmapToByteBuffer(bitmap)
		return runInterpreter()
	}
	
	fun close() {
		interpreter.close()
	}
	
	protected abstract fun byteNumbersPerChannel(): Int

	protected abstract fun addPixelValueToBuffer(pixelValue: Int)

	protected abstract fun normalizedProbability(labelIndex: Int): Float

	protected abstract fun runInterpreter() : List<ObjectPrediction>

	private fun convertBitmapToByteBuffer(bitmap: Bitmap) {
		imageByteBuffer.rewind()
//		Log.d(TAG, "convertBitmapToByteBuffer: ${bitmap.height}x${bitmap.width}")

		val emptyIntArray = IntArray(inputSize * inputSize)
		bitmap.getPixels(emptyIntArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
		var pixel = 0
		for (x in 0 until inputSize) {
			for (y in 0 until inputSize) {
				val pixelValue = emptyIntArray[pixel++]
				addPixelValueToBuffer(pixelValue)
			}
		}
	}


	companion object {
		private const val DEFAULT_MAX_RESULTS = 10
		private const val DEFAULT_CONFIDENCE_THRESHOLD = 0.1f
		private const val DEFAULT_INPUT_SIZE = 640
		private const val BATCH_SIZE = 1
		private const val PIXEL_SIZE = 3
		
		private const val TAG = "ImageClassification"
		


		fun create(
			classifierModel: ClassifierModel,
			assetManager: AssetManager,
			modelPath: String,
			labelPath: String,
			inputSize: Int = DEFAULT_INPUT_SIZE,
			interpreterOptions: Interpreter.Options = Interpreter.Options(),
			numberOfResults: Int = DEFAULT_MAX_RESULTS,
			confidenceThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD
		): ImageClassification {

			val interpreter = Interpreter(assetManager.loadModelFile(modelPath), interpreterOptions)

			return FloatClassifier(
				interpreter = interpreter,
				labelList = assetManager.loadLabelList(labelPath),
				inputSize = inputSize,
				numberOfResults = numberOfResults,
				confidenceThreshold = confidenceThreshold
			)
		}
	}
}


private class FloatClassifier(
	interpreter: Interpreter,
	labelList: List<String>,
	inputSize: Int,
	val numberOfResults: Int,
	confidenceThreshold: Float
) : ImageClassification(interpreter, labelList, inputSize, numberOfResults, confidenceThreshold) {
	
	companion object {
		private const val TAG = "FloatClassifier"
	}
	
	private val labelResults = Array(1) { FloatArray(labelList.size) }
	
	override fun byteNumbersPerChannel(): Int {
		return 4
	}
	
	override fun addPixelValueToBuffer(pixelValue: Int) {
		imageByteBuffer.putFloat(((pixelValue shr 16) and 0xFF) / 255f)
		imageByteBuffer.putFloat(((pixelValue shr 8) and 0xFF) / 255f)
		imageByteBuffer.putFloat((pixelValue and 0xFF) / 255f)
	}
	
	override fun normalizedProbability(labelIndex: Int): Float {
		return labelResults[0][labelIndex]
	}
	
	private val locations = arrayOf(Array(numberOfResults) { FloatArray(4) })
	private val labelIndices = arrayOf(FloatArray(numberOfResults))
	private val scores = arrayOf(FloatArray(numberOfResults))
	
	private val outputBuffer = mapOf(0 to locations, 1 to labelIndices, 2 to scores, 3 to FloatArray(1))
	
	@RequiresApi(Build.VERSION_CODES.N)
	override fun runInterpreter(): List<ObjectPrediction> {
		interpreter.runForMultipleInputsOutputs(arrayOf(imageByteBuffer), outputBuffer)
		return predictions
	}
	
	val predictions
		get() = (0 until numberOfResults).map { it ->
			ObjectPrediction(
				location = locations[0][it].let {                   // The locations are an array of [0, 1] floats for [top, left, bottom, right]
					RectF(it[1], it[0], it[3], it[2])
				},
				// SSD MobileNet V1 Model assumes class 0 is background class in label file and class labels start from 1 to number_of_classes+1,
				// while outputClasses correspond to class index from 0 to number_of_classes
				label = labelList[1 + labelIndices[0][it].toInt()],
				score = scores[0][it]                               // Score is a single value of [0, 1]
			)
		}
	
}
