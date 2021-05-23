package com.ninesix.crfcd.helper_class

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ninesix.crfcd.databinding.LayoutOnboardingViewpagerBinding

class OnBoardingVIewPagerAdapter(
	val context: Context,
	val headings: Array<Int>,
	private val images: Array<Int>,
	private val descriptions: Array<Int>,
	val primaryColor: Array<String>,
	private val secondaryColor: Array<String>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		return ViewHolder(LayoutOnboardingViewpagerBinding.inflate(LayoutInflater.from(parent.context), parent, false))
	}
	
	
	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		holder as ViewHolder
		with(holder) {
			binding.boardingViewpagerImage.setImageResource(images[position])
			binding.boardingViewpagerTitle.text = context.getString(headings[position])
			binding.boardingViewpagerDescription.text = context.getString(descriptions[position])
			
			binding.boardingViewpagerTitle.setTextColor(Color.parseColor(primaryColor[position]))
			binding.boardingViewpagerDescription.setTextColor(Color.parseColor(primaryColor[position]))
			
			binding.boardingViewpagerRootLayout.setBackgroundColor(Color.parseColor(secondaryColor[position]))
		}
	}
	
	
	override fun getItemCount(): Int {
		return headings.size
	}
	
	
	private inner class ViewHolder(val binding: LayoutOnboardingViewpagerBinding) : RecyclerView.ViewHolder(binding.root)
	
	
}