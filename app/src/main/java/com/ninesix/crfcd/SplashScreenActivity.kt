	package com.ninesix.crfcd

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
		
		CoroutineScope(Dispatchers.IO).launch {
			updateLabels()
		}
		
		
		/*CoroutineScope(Dispatchers.IO).launch {
			var data = hashMapOf(
				"a" to "rs_100_new_back_1",
				"b" to "rs_100_new_back_2",
				"c" to "rs_100_new_back_3",
				"d" to  "rs_100_new_back_4",
				"e" to   "rs_100_new_back_5",
				"f" to   "rs_100_new_back_6",
				"g" to   "rs_100_new_back_7",
				"h" to   "rs_100_new_back_8",
				"i" to   "rs_100_new_back_9",
				"j" to   "rs_100_new_back_10"
			)
			Firebase.firestore.collection("labelList").document("100NewBack").set(data)
			
			data = hashMapOf(
				"a" to "rs_100_new_1",
				"b" to "rs_100_new_2",
				"c" to "rs_100_new_3",
				"d" to  "rs_100_new_4",
				"e" to   "rs_100_new_5",
				"f" to   "rs_100_new_6",
				"g" to   "rs_100_new_7",
				"h" to   "rs_100_new_8",
				"i" to   "rs_100_new_9",
				"j" to   "rs_100_new_10",
				"k" to   "rs_100_new_11",
				"l" to  "rs_100_new_12",
				"m" to   "rs_100_new_13",
				"n" to   "rs_100_new_14",
				"o" to   "rs_100_new_15",
				"p" to "rs_100_new_16"
			)
			Firebase.firestore.collection("labelList").document("100NewFront").set(data)
			
			data = hashMapOf(
				"a" to "rs_100_old_b_1",
				"b" to "rs_100_old_b_2",
				"c" to "rs_100_old_b_3",
				"d" to  "rs_100_old_b_4",
				"e" to   "rs_100_old_b_5",
				"f" to   "rs_100_old_b_6",
				"g" to   "rs_100_old_b_7",
				"h" to   "rs_100_old_b_8",
			)
			Firebase.firestore.collection("labelList").document("100OldBack").set(data)
			
			data = hashMapOf(
				"a" to "rs_100_old_1",
				"b" to "rs_100_old_2",
				"c" to "rs_100_old_3",
				"d" to "rs_100_old_4",
				"e" to "rs_100_old_5",
				"f" to "rs_100_old_6",
				"g" to "rs_100_old_7",
				"H" to "rs_100_old_8",
				"i" to "rs_100_old_9",
				"j" to "rs_100_old_10",
				"k" to "rs_100_old_11",
				"l" to "rs_100_old_12",
				"m" to "rs_100_old_13",
				"n" to "rs_100_old_14",
				"o" to "rs_100_old_15",
				"p" to "rs_100_old_16",
				"q" to "rs_100_old_17"
			)
			Firebase.firestore.collection("labelList").document("100OldFront").set(data)
			
			
			
			data = hashMapOf(
				"a" to "rs-200-b1",
				"b" to "rs-200-b2",
				"c" to "rs-200-b3",
				"d" to  "rs-200-b4",
				"e" to   "rs-200-b5",
				"f" to   "rs-200-b6",
				"g" to   "rs-200-b7",
				"h" to   "rs-200-b8",
				"i" to   "rs-200-b9",
				"j" to   "rs-200-b10"
			)
			Firebase.firestore.collection("labelList").document("200NewBack").set(data)
			
			data = hashMapOf(
				"a" to "rs-200-f1",
				"b" to "rs-200-f2",
				"c" to "rs-200-f3",
				"d" to  "rs-200-f4",
				"e" to   "rs-200-f5",
				"f" to   "rs-200-f6",
				"g" to   "rs-200-f7",
				"h" to   "rs-200-f8",
				"i" to   "rs-200-f9",
				"j" to   "rs-200-f10",
				"k" to   "rs-200-f11",
				"l" to  "rs-200-f12",
				"m" to   "rs-200-f13",
				"n" to   "rs-200-f14"
			)
			Firebase.firestore.collection("labelList").document("200NewFront").set(data)
			
			
			
			data = hashMapOf(
				"a" to "rs_500_b1",
				"b" to "rs_500_b2",
				"c" to "rs_500_b3",
				"d" to  "rs_500_b4",
				"e" to   "rs_500_b5",
				"f" to   "rs_500_b6",
				"g" to   "rs_500_b7"
			)
			Firebase.firestore.collection("labelList").document("500NewBack").set(data)
			
			data = hashMapOf(
				"a" to "rs_500_f1",
				"b" to "rs_500_f2",
				"c" to "rs_500_f3",
				"d" to  "rs_500_f4",
				"e" to   "rs_500_f5",
				"f" to   "rs_500_f6",
				"g" to   "rs_500_f7",
				"h" to   "rs_500_f8",
				"i" to   "rs_500_f9",
				"j" to   "rs_500_f10",
				"k" to   "rs_500_f11",
				"l" to  "rs_500_f12",
				"m" to   "rs_500_f13",
				"n" to   "rs_500_f14"
			)
			Firebase.firestore.collection("labelList").document("500NewFront").set(data)
			
			
			
			data = hashMapOf(
				"a" to "rs2000newback01",
				"b" to "rs2000newback02",
				"c" to "rs2000newback03",
				"d" to  "rs2000newback04",
				"e" to   "rs2000newback05",
				"f" to   "rs2000newback06",
				"g" to   "rs2000newback07",
				"h" to   "rs2000newback08",
				"i" to   "rs2000newback09",
				"j" to   "rs2000newback10",
				"k" to   "rs2000newback11",
				"l" to  "rs2000newback12",
				"m" to   "rs2000newback13"
			)
			Firebase.firestore.collection("labelList").document("2000NewBack").set(data)
			
			data = hashMapOf(
				"a" to "rs2000newfront01",
				"b" to "rs2000newfront02",
				"c" to "rs2000newfront03",
				"d" to  "rs2000newfront04",
				"e" to   "rs2000newfront05",
				"f" to   "rs2000newfront06",
				"g" to   "rs2000newfront07",
				"h" to   "rs2000newfront08",
				"i" to   "rs2000newfront09",
				"j" to   "rs2000newfront10",
				"k" to   "rs2000newfront11"
			)
			Firebase.firestore.collection("labelList").document("2000NewFront").set(data)
			
			
			
			data = hashMapOf(
				"a" to "rs_100_wnf1",
				"b" to "rs_100_wnf2",
				"c" to "rs_100_wnf3",
				"d" to  "rs_100_wnf4"
			)
			Firebase.firestore.collection("labelList").document("white100NewFront").set(data)
			
			data = hashMapOf(
				"a" to "nw_100f_01",
				"b" to "nw_100f_02",
				"c" to "nw_100f_03",
				"d" to  "nw_100f_04"
			)
			Firebase.firestore.collection("labelList").document("white100OldFront").set(data)
			
			data = hashMapOf(
				"a" to "",
				"b" to "",
				"c" to "",
				"d" to  "",
				"e" to   ""
			)
			Firebase.firestore.collection("labelList").document("white200NewFront").set(data)
			
			data = hashMapOf(
				"a" to "rs_500_nwf1",
				"b" to "rs_500_nwf2",
				"c" to "rs_500_nwf3",
				"d" to  "rs_500_nwf4",
				"e" to   "rs_500_nwf5"
			)
			Firebase.firestore.collection("labelList").document("white500NewFront").set(data)
			
			data = hashMapOf(
				"a" to "rs_2000_wf1",
				"b" to "rs_2000_wf2",
				"c" to "rs_2000_wf3",
				"d" to  "rs_2000_wf4"
			)
			Firebase.firestore.collection("labelList").document("white2000NewFront").set(data)
			
		}*/
		
		
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
		
		val sharedPrefs = getSharedPreferences("onBoarding", MODE_PRIVATE)
		val isFirstTime = sharedPrefs.getBoolean("isFirstTime", true)
		
		Timer().schedule(2000) {
//		startActivity(Intent(this@SplashScreenActivity, if(isFirstTime) ResultActivity::class.java else ResultActivity::class.java))
		startActivity(Intent(this@SplashScreenActivity, if(isFirstTime) MainActivity::class.java else MainActivity::class.java))
			finish()
		}
	}
	
	
	//----------------------------- FETCH LABELS FROM FIRESTORE -----------------------------//
	private suspend fun updateLabels() = withContext(Dispatchers.IO) {
		Firebase.firestore.collection("labelList").addSnapshotListener { snapshot, error ->
			if(error != null) {
				Log.e(TAG, "updateLabels: ${error.localizedMessage}")
				return@addSnapshotListener
			}
		}
	}
	
	
}