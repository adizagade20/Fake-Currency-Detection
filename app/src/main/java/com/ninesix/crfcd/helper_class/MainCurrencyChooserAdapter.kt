package com.ninesix.crfcd.helper_class

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.ninesix.crfcd.R
import com.ninesix.crfcd.databinding.LayoutMainCurrencyChooserRecyclerItemBinding

class MainCurrencyChooserAdapter(private val context: Context, private var currency: String, private val intent: Intent) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
	
	private val currencyNames = arrayOf("Rs. 100 New", "Rs. 100 Old", "Rs. 200", "Rs. 500", "Rs. 2000")
	private val currencyImages = arrayOf(R.drawable.rs_100_new, R.drawable.rs_100_old, R.drawable.rs_200, R.drawable.rs_500, R.drawable.rs_2000)
	
	private var currentSelected = -1
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		return ViewHolder(LayoutMainCurrencyChooserRecyclerItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
	}
	
	
	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		holder as ViewHolder
		with(holder) {
			binding.mainCurrencyChooserItemTextView.text = currencyNames[position]
			binding.mainCurrencyChooserItemImageView.setImageResource(currencyImages[position])
			
			if(currencyNames[position].contains(currency)) {
				binding.mainCurrencyChooserItemRootLayout.setBackgroundColor(ResourcesCompat.getColor(context.resources, R.color.gray, null))
				currentSelected = position
			}
			
			binding.mainCurrencyChooserItemRootLayout.setOnClickListener {
				binding.mainCurrencyChooserItemRootLayout.setBackgroundColor(ResourcesCompat.getColor(context.resources, R.color.gray, null))
				
				intent.putExtra("currency", currencyNames[position])
				context.startActivity(intent)
			}
		}
	}
	
	
	override fun getItemCount(): Int {
		return currencyNames.size
	}
	
	
	private inner class ViewHolder(val binding: LayoutMainCurrencyChooserRecyclerItemBinding) : RecyclerView.ViewHolder(binding.root)
	
}