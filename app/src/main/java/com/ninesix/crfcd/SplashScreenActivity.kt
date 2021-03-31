package com.ninesix.crfcd

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.airbnb.lottie.LottieAnimationView
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*
import kotlin.concurrent.schedule

class SplashScreenActivity : AppCompatActivity() {
	
	companion object {
		private const val TAG: String = "SplashScreen"
	}
	
	private lateinit var topAnimation: Animation
	private lateinit var bottomAnimation: Animation
	private lateinit var middleAnimation: Animation
	
	private lateinit var first: View
	private lateinit var second: View
	private lateinit var third: View
	private lateinit var fourth: View
	private lateinit var fifth: View
	private lateinit var sixth: View
	
	private lateinit var rupeeSymbol: TextView
	private lateinit var splashText: TextView
	private lateinit var splashAppName: TextView
	
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_splash_screen)
		
		doSplashWork()
		
		updateLabels()
		
	}
	
	private fun doSplashWork() {
		
		/*------------------------- LOAD ANIMATION ----------------------------*/
		topAnimation = AnimationUtils.loadAnimation(this, R.anim.splash_top_animation)
		bottomAnimation = AnimationUtils.loadAnimation(this, R.anim.splash_bottom_animation)
		middleAnimation = AnimationUtils.loadAnimation(this, R.anim.splash_middle_animation)
		
		/*------------------------- HOOKS ----------------------------*/
		first = findViewById(R.id.first_line)
		second = findViewById(R.id.second_line)
		third = findViewById(R.id.third_line)
		fourth = findViewById(R.id.fourth_line)
		fifth = findViewById(R.id.fifth_line)
		sixth = findViewById(R.id.sixth_line)
		
		rupeeSymbol = findViewById(R.id.splash_rupee_symbol)
		splashAppName = findViewById(R.id.splash_app_name)
		splashText = findViewById(R.id.splash_text)
		
		
		/*------------------------- ASSIGN ANIMATION ----------------------------*/
		first.animation = topAnimation
		second.animation = topAnimation
		third.animation = topAnimation
		fourth.animation = topAnimation
		fifth.animation = topAnimation
		sixth.animation = topAnimation
		
		rupeeSymbol.animation = middleAnimation
		splashAppName.animation = middleAnimation
		splashText.animation = bottomAnimation
		
		Timer().schedule(2000) {
			startActivity(Intent(this@SplashScreenActivity, MainActivity::class.java))
			finish()
		}
	}
	
	
	
	//----------------------------- FETCH LABELS FROM FIRESTORE -----------------------------//
	
	private lateinit var listener: ListenerRegistration
	
	private fun updateLabels() {
		listener = Firebase.firestore.collection("labelList").addSnapshotListener { snapshot, error ->
			if(error != null) {
				Log.e(TAG, "updateLabels: ${error.localizedMessage}")
				return@addSnapshotListener
			}
			
			if (snapshot != null) {
				Log.d(TAG, "updateLabels: all: ${snapshot.documents.forEach {
					println(it.data)
				}}")
				Log.d(TAG, "updateLabels: ${snapshot.metadata.isFromCache}")
				for(dc in snapshot.documentChanges) {
					when(dc.type) {
						DocumentChange.Type.ADDED -> Log.d(TAG, "updateLabels: New LabelList Added : ${dc.document.data}")
						DocumentChange.Type.MODIFIED -> Log.d(TAG, "updateLabels: LabelList Modified : ${dc.document.data}")
						DocumentChange.Type.REMOVED -> Log.d(TAG, "updateLabels: LabelList Deleted : ${dc.document.data}")
					}
				}
			}
		}
	}
	
	
	override fun onDestroy() {
		super.onDestroy()
		listener.remove()
	}
	
}