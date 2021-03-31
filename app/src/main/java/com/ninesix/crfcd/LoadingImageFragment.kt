package com.ninesix.crfcd

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.coroutines.*

class LoadingImageFragment : Fragment() {                               //, CoroutineScope {

//	private val job = Job()
//	override val coroutineContext: CoroutineContext get() = job + Dispatchers.Default
//
//	companion object {
//		private val TAG: String = "LoadingImageFragment"
//	}
//
//	//----------------------------- HOOKS -----------------------------//
//	private lateinit var rootView: View
//	private lateinit var animation : Animation
//	private lateinit var loadingModel
//
//
//	//----------------------------- OBJECT DETECTION -----------------------------//
//	private lateinit var secondaryImageClassification: ImageClassification
//
//
	
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		return inflater.inflate(R.layout.fragment_loading, container, false)
	}
}
//
//		//----------------------------- HOOKS -----------------------------//
//
//
//
//		val bundle = arguments
//		val labelForDetailedDetection = bundle!!.getString("labelForDetailedDetection")
//
//		animation = AnimationUtils.loadAnimation(context, R.anim.slide_out_right)
//
//		val data = ModelLabelHelper().getModelAndLabelFileName(labelForDetailedDetection!!)
//
//		loadSecondaryModule(data[0], data[1], data[2].toInt())
//
//		return rootView
//	}
//
//
//	private fun loadSecondaryModule(MODEL_PATH: String, LABEL_PATH: String, numberOfResults: Int) = launch {
//
//		if(activity != null) {
//			Log.d(TAG, "loadSecondaryModule: ADITYA")
//		}
//		else {
//			Log.d(TAG, "loadSecondaryModule: aditya")
//		}
//
//		withContext(Dispatchers.IO) {
//			secondaryImageClassification = ImageClassification.create(
//				classifierModel = ClassifierModel.FLOAT,
//				assetManager = activity!!.assets,
//				modelPath = MODEL_PATH,
//				labelPath = LABEL_PATH,
//				numberOfResults = numberOfResults
//			)
//		}
//
//		Timer().schedule(2000) {
//			activity!!.runOnUiThread {
//				rootView.loading_model_progress.visibility = View.GONE
//				rootView.loading_model_check.visibility = View.VISIBLE
//				Timer().schedule(1000) {
//					activity!!.runOnUiThread {
//						rootView.loading_model_layout.startAnimation(animation)
//						Timer().schedule(500) {
//							activity!!.runOnUiThread { rootView.loading_model_layout.visibility = View.GONE }
//						}
//					}
//				}
//			}
//		}
//	}
//
//
//	fun retrieveBitmap(path: String) {
//
//		activity!!.runOnUiThread {
//			rootView.save_progress.visibility = View.GONE
//			rootView.save_check.visibility = View.VISIBLE
//			Timer().schedule(1000) {
//				activity!!.runOnUiThread {
//					rootView.save_layout.startAnimation(animation)
//					Timer().schedule(500) {
//						activity!!.runOnUiThread { rootView.save_layout.visibility = View.GONE }
//					}
//				}
//			}
//		}
//
//		val options = BitmapFactory.Options()
//		options.inMutable = true
//		var bitmap = BitmapFactory.decodeFile(path.substring(8), options)
//		val width = bitmap.width * 90 / 100
//		val height = bitmap.height * 34 / 100
//		bitmap = Bitmap.createBitmap(bitmap, bitmap.width * 5 / 100, bitmap.height * 33 / 100, width, height)
//		bitmap = Bitmap.createScaledBitmap(bitmap, 640, 640, false)
//
//		Log.d(TAG, "retrieveBitmap: $width x $height")
//
//		activity!!.runOnUiThread {
//			rootView.resize_progress.visibility = View.GONE
//			rootView.resize_check.visibility = View.VISIBLE
//			Timer().schedule(1000) {
//				activity!!.runOnUiThread {
//					rootView.resize_layout.startAnimation(animation)
//					Timer().schedule(500) {
//						activity!!.runOnUiThread { rootView.resize_layout.visibility = View.GONE }
//					}
//				}
//			}
//		}
//
//		analyze(bitmap, width, height)
//
//	}
//
//
//	private fun analyze(bitmap: Bitmap, width: Int, height: Int) {
//		val predictions = secondaryImageClassification.classifyImage(bitmap)
//		secondaryImageClassification.close()
//
//
//		activity!!.runOnUiThread {
//			rootView.recognizing_progress.visibility = View.GONE
//			rootView.recognizing_check.visibility = View.VISIBLE
//			Timer().schedule(1000) {
//				activity!!.runOnUiThread {
//					rootView.recognizing_layout.startAnimation(animation)
//					Timer().schedule(500) {
//						activity!!.runOnUiThread { rootView.recognizing_layout.visibility = View.GONE }
//
//						showResult(bitmap, predictions, width, height)
//					}
//				}
//			}
//		}
//
//		println(predictions.size)
//		println("${predictions.forEach { println("${it.label} : ${it.score} : ${it.location}")}}")
//
//	}
//
//
//	private fun showResult(bitmap: Bitmap, predictions: List<ObjectPrediction>, width: Int, height: Int) {
//		activity!!.runOnUiThread {
//			rootView.viewPager_recyclerView.layoutManager = LinearLayoutManager(context)
//			rootView.viewPager_recyclerView.setHasFixedSize(true)
////			val adapter = ResultAdapter(predictions, bitmap, width, height, context!!)
////			rootView.recyclerView.adapter = adapter
//		}
//	}
//
//}
