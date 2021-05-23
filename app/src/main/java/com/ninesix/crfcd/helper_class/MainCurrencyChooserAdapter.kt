package com.ninesix.crfcd.helper_class

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.ninesix.crfcd.R
import com.ninesix.crfcd.databinding.LayoutMainCurrencyChooserRecyclerItemBinding
import java.util.*
import kotlin.concurrent.schedule

class MainCurrencyChooserAdapter(private val context: Context, private var detectedCurrency: ObjectPrediction, private val intent: Intent) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
	
	private val currencyNames = arrayOf("rs_100_new", "rs_100_old", "rs_200_new", "rs_500_new", "rs_2000_new")
	private val currencyImages = arrayOf(R.drawable.rs_100_new, R.drawable.rs_100_old, R.drawable.rs_200, R.drawable.rs_500, R.drawable.rs_2000)
	private var selectedCurrency = 0
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		return ViewHolder(LayoutMainCurrencyChooserRecyclerItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
	}
	
	
	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		holder as ViewHolder
		with(holder.binding) {
			mainCurrencyChooserItemTextView.text = currencyNames[position]
			mainCurrencyChooserItemImageView.setImageResource(currencyImages[position])
			
			if(currencyNames[position] == detectedCurrency.label) {
//				"${currencyNames[position]} ${detectedCurrency.score.toString().substring(0, 4)} %".also { mainCurrencyChooserItemTextView.text = it }
				mainCurrencyChooserItemRootLayout.setBackgroundColor(ResourcesCompat.getColor(context.resources, R.color.gray, null))
				selectedCurrency = position
			}
			
			mainCurrencyChooserItemRootLayout.setOnClickListener {
				notifyItemChanged(selectedCurrency)
				notifyItemChanged(position)
				
				intent.putExtra("currency", currencyNames[position])
				Timer().schedule(500) { context.startActivity(intent) }
			}
		}
	}
	
	
	override fun getItemCount(): Int {
		return currencyNames.size
	}
	
	
	private inner class ViewHolder(val binding: LayoutMainCurrencyChooserRecyclerItemBinding) : RecyclerView.ViewHolder(binding.root)
	
}