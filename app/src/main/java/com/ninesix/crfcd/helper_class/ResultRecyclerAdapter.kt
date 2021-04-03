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
					binding.resultRecyclerLabelTv.text = this.label
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
			predictions.forEach { if (it.score > 0.7f) count++ }
			with(holder) {
				setImageWithMarkings(bitmap, binding.resultRecyclerFirstImageView)
				binding.resultRecyclerFirstTextView.text = when (count) {
					0 -> "No point matched!"
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
		paint.textSize = 30f
		paint.strokeWidth = 2f
		
		predictions.forEach {
			Log.d(TAG, "setImageWithMarkings: ${it.location} : ${it.score}")
			if (it.score >= 0.01f) {
				canvas.drawRect(
					RectF(
						it.location.left * 640,
						it.location.top * 640,
						it.location.right * 640,
						it.location.bottom * 640
					), paint
				)
				canvas.drawText(it.label, it.location.left * 640, it.location.top * 640 - 5, paint)
			}
		}
		
		(context as AppCompatActivity).runOnUiThread {
			imageView.setImageBitmap(Bitmap.createScaledBitmap(bitmap2, 400, 300, false))
		}
	}
	
	
	private fun setSmallImage(position: Int, imageView: ImageView) {
		val left = max((predictions[position].location.left * 600).toInt(), 0)
		val top = max((predictions[position].location.top * 600).toInt(), 0)
		val smallWidth = ((predictions[position].location.right - predictions[position].location.left) * 600).toInt()
		val smallHeight = ((predictions[position].location.bottom - predictions[position].location.top) * 600).toInt()
		
		try {
			var smallBitmap = Bitmap.createBitmap(bitmap, left, top, smallWidth, smallHeight)
			smallBitmap = Bitmap.createScaledBitmap(smallBitmap, 150, 150, false)
			
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