package com.ninesix.crfcd.helper_class

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ninesix.crfcd.R
import com.ninesix.crfcd.databinding.LayoutOnboardingViewpagerBinding

class OnBoardingVIewPagerAdapter(val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
	
	private val headings = arrayOf(R.string.first_slide_title, R.string.second_slide_title, R.string.third_slide_title)
	
	private val images = arrayOf(R.drawable.onboarding_image1, R.drawable.onboarding_image2, R.drawable.onboarding_image3)
	
	private val descriptions = arrayOf(R.string.first_slide_description, R.string.second_slide_description, R.string.third_slide_description)
	
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		return ViewHolder(LayoutOnboardingViewpagerBinding.inflate(LayoutInflater.from(parent.context), parent, false))
	}
	
	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		holder as ViewHolder
		with(holder) {
			binding.boardingViewpagerTitle.text = context.getString(headings[position])
			binding.boardingViewpagerImage.setImageResource(images[position])
			binding.boardingViewpagerDescription.text = context.getString(descriptions[position])
		}
	}
	
	override fun getItemCount(): Int {
		return headings.size
	}
	
	
	
	
	
	
	
	
	private inner class ViewHolder(val binding: LayoutOnboardingViewpagerBinding) : RecyclerView.ViewHolder(binding.root)
	
	
	/*override fun getCount(): Int {
		return headings.size
	}

	override fun isViewFromObject(view: View, `object`: Any): Boolean {
		return view == `object` as ConstraintLayout
	}

	override fun instantiateItem(container: ViewGroup, position: Int): Any {
		layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

		val view = layoutInflater.inflate(R.layout.layout_slides, container, false)

		val imageView = view.findViewById<ImageView>(R.id.slider_image)
		val headingTV = view.findViewById<TextView>(R.id.slider_heading)
		val descriptionTV = view.findViewById<TextView>(R.id.slider_desc)

		imageView.setImageResource(images[position])
		headingTV.text = headings[position].toString()
		descriptionTV.text = descriptions[position].toString()

		container.addView(view)

		return super.instantiateItem(container, position)
	}

	override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
		container.removeView(`object` as ConstraintLayout)
	}*/
	
}