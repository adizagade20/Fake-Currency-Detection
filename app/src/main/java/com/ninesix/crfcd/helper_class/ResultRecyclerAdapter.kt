package com.ninesix.crfcd.helper_class

import android.content.Context
import android.graphics.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.airbnb.lottie.parser.IntegerParser
import com.ninesix.crfcd.MainActivity
import com.ninesix.crfcd.R
import com.ninesix.crfcd.ResultActivity
import com.ninesix.crfcd.databinding.LayoutResultRecyclerBinding
import com.ninesix.crfcd.databinding.LayoutResultRecyclerFirstTileBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext
import kotlin.math.max


class ResultRecyclerAdapter(private val context: Context, private val predictions: MutableList<ObjectPrediction>, private val bitmap: Bitmap) : RecyclerView.Adapter<ViewHolder>(), CoroutineScope {
	
	private val job = Job()
	override val coroutineContext: CoroutineContext get() = job + Dispatchers.Default
	
	companion object {
		const val TAG = "ResultAdapter"
	}
	
	
	override fun getItemViewType(position: Int): Int {
		return if (predictions[position].label != "null") 0 else 1
	}
	
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		return if (viewType == 0) {
			val binding = LayoutResultRecyclerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
			ViewHolderOne(binding)
		} else {
			val binding = LayoutResultRecyclerFirstTileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
			ViewHolderTwo(binding)
		}
	}
	
	
	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		if (predictions[position].label != "null") {
			holder as ViewHolderOne
			with(holder) {
				with(predictions[position]) {
					val label = this.label.subSequence(this.label.length - 2, this.label.length)
					("Feature " + label.replace("[^0-9]".toRegex(), "")).also { binding.resultRecyclerLabelTv.text = it }
					"${(this.score * 100).toInt()}%".also { binding.resultRecyclerScoreTv.text = it }
					binding.resultRecyclerScoreProgress.progress = (this.score * 100).toInt()
					setSmallImage(position, binding.resultRecyclerImageView)
				}
				
				holder.binding.resultRecyclerMainLayout.setOnClickListener {
					with(binding.resultExpandableLayout) {
						this.visibility = if(this.visibility == View.VISIBLE) View.GONE else View.VISIBLE
					}
				}
			}
		} else if (predictions[position].label == "null") {
			holder as ViewHolderTwo
			
			var count = 0
			predictions.forEach { if (it.score > 0.5f) count++ }
			with(holder) {
				setImageWithMarkings(bitmap, binding.resultRecyclerFirstImageView)
				binding.resultRecyclerFirstTextView.text = when (count) {
					0 -> "No points matched!"
					1 -> "Only 1 point is matched!"
					else -> "$count points are matched!"
				}
			}
			
		}
	}
	
	
	private fun setImageWithMarkings(bitmap: Bitmap, imageView: ImageView) {
		val bitmap2 = Bitmap.createScaledBitmap(bitmap, bitmap.width, bitmap.height, false)
		val canvas = Canvas(bitmap2)
		val paint = Paint()
		paint.alpha = 0xA0
		paint.color = Color.MAGENTA
		paint.style = Paint.Style.STROKE
		paint.textSize = 25f
		paint.strokeWidth = 2f
		
		
		predictions.forEach {
			if (it.score >= 0.01f) {
				canvas.drawRect(
					it.location.left * 640,
					it.location.top * 640,
					it.location.right * 640,
					it.location.bottom * 640,
					paint
				)
				val label = it.label.subSequence(it.label.length - 2, it.label.length)
				canvas.drawText(label.replace("[^0-9]".toRegex(), ""), it.location.left * 640, it.location.top * 640 - 5, paint)
			}
		}
		
		(context as AppCompatActivity).runOnUiThread {
			imageView.setImageBitmap(bitmap2)
		}
	}
	
	
	private fun setSmallImage(position: Int, imageView: ImageView) {
		val left = (predictions[position].location.left * 640).toInt()
		val top = (predictions[position].location.top * 640).toInt()
		val smallWidth = ((predictions[position].location.right * 640) - (predictions[position].location.left * 640)).toInt()
		val smallHeight = ((predictions[position].location.bottom * 640) - (predictions[position].location.top * 640)).toInt()
		
		try {
			val smallBitmap = Bitmap.createBitmap(bitmap, left, top, smallWidth, smallHeight)
			
			(context as AppCompatActivity).runOnUiThread {
				imageView.setImageBitmap(smallBitmap)
			}
		} catch (e: Exception) {
			Log.e(TAG, "setImage: ${e.localizedMessage}")
			(context as AppCompatActivity).runOnUiThread {
				imageView.setImageResource(R.drawable.error_outline)
			}
		}
	}
	
	
	override fun getItemCount(): Int {
		return predictions.size
	}
	
	
	private inner class ViewHolderOne(val binding: LayoutResultRecyclerBinding) : RecyclerView.ViewHolder(binding.root)
	
	
	private inner class ViewHolderTwo(val binding: LayoutResultRecyclerFirstTileBinding) : RecyclerView.ViewHolder(binding.root)
	
}

