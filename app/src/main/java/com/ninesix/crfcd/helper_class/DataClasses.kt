package com.ninesix.crfcd.helper_class

import android.graphics.Bitmap
import android.graphics.RectF

data class ObjectPrediction(val location: RectF, val label: String, val score: Float)

data class ViewPagerData(val predictions: MutableList<ObjectPrediction>, val bitmap: Bitmap)