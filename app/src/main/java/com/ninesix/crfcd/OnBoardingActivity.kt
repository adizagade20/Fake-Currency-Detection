package com.ninesix.crfcd

import android.content.Intent
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
		
		val viewPagerAdapter = OnBoardingVIewPagerAdapter(this)
		
		binding.boardingViewpager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
			override fun onPageSelected(position: Int) {
				super.onPageSelected(position)
				Log.d(TAG, "onPageSelected: $position")
				addDots(position)
				currentPos = position
				val fadeIn = AnimationUtils.loadAnimation(applicationContext, android.R.anim.fade_in)
				val fadeOut = AnimationUtils.loadAnimation(applicationContext, android.R.anim.fade_out)
				when (position) {
					1 -> {
						if(binding.boardingLetsGetStarted.visibility == View.VISIBLE) {
							binding.boardingLetsGetStarted.visibility = View.INVISIBLE
							binding.boardingLetsGetStarted.animation = fadeOut
						}
						if(binding.boardingSkip.visibility == View.INVISIBLE && binding.boardingNext.visibility == View.INVISIBLE) {
							binding.boardingSkip.visibility = View.VISIBLE
							binding.boardingNext.visibility = View.VISIBLE
							binding.boardingSkip.animation = fadeIn
							binding.boardingNext.animation = fadeIn
						}
					}
					2 -> {
						if(binding.boardingLetsGetStarted.visibility == View.INVISIBLE) {
							binding.boardingLetsGetStarted.visibility = View.VISIBLE
							binding.boardingLetsGetStarted.animation = fadeIn
						}
						if(binding.boardingSkip.visibility == View.INVISIBLE && binding.boardingNext.visibility == View.INVISIBLE) {
							binding.boardingSkip.visibility = View.INVISIBLE
							binding.boardingNext.visibility = View.INVISIBLE
							binding.boardingSkip.animation = fadeOut
							binding.boardingNext.animation = fadeOut
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
			startActivity(Intent(this, MainActivity::class.java))
			finish()
		}
	}
	
	
	private fun addDots(position: Int) {
		val dots = arrayOfNulls<TextView>(3)
		binding.boardingDotsLayout.removeAllViews()
		for (i in dots.indices) {
			dots[i] = TextView(this)
			dots[i]?.text = " •"
			dots[i]?.textSize = 35f
			binding.boardingDotsLayout.addView(dots[i])
		}
		dots[position]?.setTextColor(ResourcesCompat.getColor(resources, R.color.design_default_color_secondary, null))
	}
	
	
}
	
