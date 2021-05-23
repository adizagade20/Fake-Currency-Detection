package com.ninesix.crfcd

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.ninesix.crfcd.databinding.ActivityOnBoardingBinding
import com.ninesix.crfcd.helper_class.OnBoardingVIewPagerAdapter


class OnBoardingActivity : AppCompatActivity() {
	
	private var currentPos = 0
	
	
	companion object {
		private const val TAG = "OnBoardingActivity"
	}
	
	private lateinit var binding: ActivityOnBoardingBinding
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityOnBoardingBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
		val headings = arrayOf(R.string.first_slide_title, R.string.second_slide_title, R.string.third_slide_title)
		val images = arrayOf(R.drawable.onboarding_image1, R.drawable.onboarding_image2, R.drawable.onboarding_image3)
		val descriptions = arrayOf(R.string.first_slide_description, R.string.second_slide_description, R.string.third_slide_description)
		val primaryColor = arrayOf("#E69A8D", "#EEA47F", "#ADEFD1")
		val secondaryColor = arrayOf("#5F4B8B", "#00539C", "#00203F")
		
		val viewPagerAdapter = OnBoardingVIewPagerAdapter(this, headings, images, descriptions, primaryColor, secondaryColor)
		
		
		binding.boardingViewpager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
			override fun onPageSelected(position: Int) {
				super.onPageSelected(position)
				currentPos = position
				val fadeIn = AnimationUtils.loadAnimation(applicationContext, android.R.anim.fade_in)
				val fadeOut = AnimationUtils.loadAnimation(applicationContext, android.R.anim.fade_out)
				when (position) {
					0 -> {
						binding.boardingDot1.setTextColor(Color.parseColor(primaryColor[position]))
						binding.boardingDot2.setTextColor(ResourcesCompat.getColor(resources, R.color.design_default_color_on_secondary, null))
						binding.boardingDot3.setTextColor(ResourcesCompat.getColor(resources, R.color.design_default_color_on_secondary, null))
						binding.boardingSkip.setTextColor(Color.parseColor(primaryColor[position]))
						binding.boardingNext.setTextColor(Color.parseColor(primaryColor[position]))
					}
					1 -> {
						binding.boardingDot1.setTextColor(ResourcesCompat.getColor(resources, R.color.design_default_color_on_secondary, null))
						binding.boardingDot2.setTextColor(Color.parseColor(primaryColor[position]))
						binding.boardingDot3.setTextColor(ResourcesCompat.getColor(resources, R.color.design_default_color_on_secondary, null))
						
						if(binding.boardingLetsGetStarted.visibility == View.VISIBLE) {
							binding.boardingLetsGetStarted.visibility = View.INVISIBLE
							binding.boardingLetsGetStarted.animation = fadeOut
						}
						if(binding.boardingSkip.visibility == View.INVISIBLE && binding.boardingNext.visibility == View.INVISIBLE) {
							binding.boardingSkip.visibility = View.VISIBLE
							binding.boardingNext.visibility = View.VISIBLE
							binding.boardingSkip.animation = fadeIn
							binding.boardingNext.animation = fadeIn
							binding.boardingSkip.setTextColor(Color.parseColor(primaryColor[position]))
							binding.boardingNext.setTextColor(Color.parseColor(primaryColor[position]))
						}
					}
					2 -> {
						binding.boardingDot1.setTextColor(ResourcesCompat.getColor(resources, R.color.design_default_color_on_secondary, null))
						binding.boardingDot2.setTextColor(ResourcesCompat.getColor(resources, R.color.design_default_color_on_secondary, null))
						binding.boardingDot3.setTextColor(Color.parseColor(primaryColor[position]))
						if(binding.boardingLetsGetStarted.visibility == View.INVISIBLE) {
							binding.boardingLetsGetStarted.visibility = View.VISIBLE
							binding.boardingLetsGetStarted.animation = fadeIn
						}
						if(binding.boardingSkip.visibility == View.VISIBLE && binding.boardingNext.visibility == View.VISIBLE) {
							binding.boardingSkip.visibility = View.INVISIBLE
							binding.boardingNext.visibility = View.INVISIBLE
							binding.boardingSkip.animation = fadeOut
							binding.boardingNext.animation = fadeOut
							binding.boardingSkip.setTextColor(Color.parseColor(primaryColor[position]))
							binding.boardingNext.setTextColor(Color.parseColor(primaryColor[position]))
						}
					}
				}
			}
		})
		
		binding.boardingViewpager.adapter = viewPagerAdapter
		
		
		binding.boardingNext.setOnClickListener {
			binding.boardingViewpager.currentItem = currentPos + 1;
		}
		
		binding.boardingSkip.setOnClickListener {
			markAsVisited()
			startActivity(Intent(this, MainActivity::class.java))
			finish()
		}
		
		binding.boardingLetsGetStarted.setOnClickListener {
			markAsVisited()
			startActivity(Intent(this, MainActivity::class.java))
			finish()
		}
	}
	
	
	
	private  fun markAsVisited() {
		val sharedPrefs = getSharedPreferences("onBoarding", MODE_PRIVATE)
		val editor = sharedPrefs.edit()
		editor.putBoolean("isFirstTime", false)
		editor.apply()
	}
	
}
	
