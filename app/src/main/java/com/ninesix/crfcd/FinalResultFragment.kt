package com.ninesix.crfcd

import android.view.View
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.ninesix.crfcd.helper_class.CustomModelInterpreter

class FinalResultFragment(private val allPredictions : ArrayList<List<CustomModelInterpreter.ObjectPrediction>>, private val rootView : View) {

	companion object {
		private val RESULTS = arrayOf("Normal", "White", "UV")
	}
	
	private val barChart: BarChart = rootView.findViewById(R.id.barChart)
	
	
	init {
		val barChart1 = barChart
		val data: BarData = createChartData()
		configureChartAppearance(barChart1)
		prepareChartData(data, barChart1)
	}
	
	
	private fun createChartData(): BarData {
		val values: ArrayList<BarEntry> = ArrayList()
		for (i in 0 until 3) {
			val x = i.toFloat()
//			val y: Float = Random.nextFloat() * (50 - 5) + 5
			val data = allPredictions[0]
			var sum = 0f
			val index = 0
			for(j in data)
				sum += (j.score * 100)
			val average = sum / index
			val y : Float = average
			values.add(BarEntry(x, y))
		}
		val set1 = BarDataSet(values, "SET_LABEL")
		val dataSets: ArrayList<IBarDataSet> = ArrayList()
		dataSets.add(set1)
		
		return BarData(dataSets)
	}
	
	
	private fun configureChartAppearance(chart: BarChart) {
		chart.description.isEnabled = false
		chart.setDrawValueAboveBar(false)
		val xAxis = chart.xAxis
		
		xAxis.valueFormatter = object : ValueFormatter() {
			override fun getFormattedValue(value: Float): String {
				return RESULTS[value.toInt()]
			}
		}
		
		val axisLeft = chart.axisLeft
		axisLeft.granularity = 10f
		axisLeft.axisMinimum = 0f
		
		val axisRight = chart.axisRight
		axisRight.granularity = 10f
		axisRight.axisMinimum = 0f
	}
	
	
	private fun prepareChartData(data: BarData, chart: BarChart) {
		data.setValueTextSize(12f)
		chart.data = data
		chart.invalidate()
	}
	
	
}