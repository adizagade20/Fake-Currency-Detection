package com.ninesix.crfcd.helper_class

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.ninesix.crfcd.R
import kotlin.collections.ArrayList

class ResultViewPagerAdapter(private val context: Context, private val dataForViewPager: ArrayList<ViewPagerData?>): RecyclerView.Adapter<ViewHolder>(){
	
	companion object {
		private const val TAG = "ViewPagerAdapter"
	}
	

	override fun getItemViewType(position: Int): Int {
		return if (dataForViewPager[position] != null) 0 else 1
	}
	
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		return if (viewType == 0) {
			val inflater = LayoutInflater.from(parent.context)
			val view = inflater.inflate(R.layout.layout_result_view_pager_item, parent, false)
			NormalViewHolder(view)
		} else {
			val inflater = LayoutInflater.from(parent.context)
			val view = inflater.inflate(R.layout.fragment_final_result, parent, false)
			FinalTabViewHolder(view)
		}
	}
	
	
	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		if (dataForViewPager[position] != null) {
			holder as NormalViewHolder
			holder.recyclerView.layoutManager = LinearLayoutManager(context)
			val data = dataForViewPager[position]
			if (data != null) {
				holder.recyclerView.adapter = ResultRecyclerAdapter(context, data.predictions, data.bitmap)
			}
		} else {
			holder as FinalTabViewHolder
			val data: BarData = createChartData()
			configureChartAppearance(holder.barChart)
			prepareChartData(data, holder.barChart)
		}
	}
	
	
	override fun getItemCount(): Int {
		return dataForViewPager.size
	}
	
	
	private fun createChartData(): BarData {
		val values: ArrayList<BarEntry> = ArrayList()
		for (i in 0 until 3) {
			val x = i.toFloat()
			var sum = 0f
			var index = 0
			for(j in dataForViewPager[i]!!.predictions) {
				sum += (j.score * 100)
				index++
			}
			val y = sum / index
			values.add(BarEntry(x, y))
		}
		val set1 = BarDataSet(values, "Analysis")
		val dataSets: ArrayList<IBarDataSet> = ArrayList()
		dataSets.add(set1)
		
		return BarData(dataSets)
	}
	
	
	private fun configureChartAppearance(chart: BarChart) {
		chart.description.isEnabled = false
		chart.setDrawValueAboveBar(false)
		val xAxis = chart.xAxis
		xAxis.granularity = 1f
		chart.setScaleEnabled(false)
		chart.setPinchZoom(false)
		
		val results = arrayOf("Normal", "White", "UV")
		
		xAxis.valueFormatter = object : ValueFormatter() {
			override fun getFormattedValue(value: Float): String {
				return results[value.toInt()]
			}
		}
		
		val axisLeft = chart.axisLeft
		axisLeft.granularity = 5f
		axisLeft.axisMinimum = 0f
		
		val axisRight = chart.axisRight
		axisRight.granularity = 10f
		axisRight.axisMinimum = 0f
	}
	
	
	private fun prepareChartData(data: BarData, chart: BarChart) {
		data.setValueTextSize(16f)
		chart.data = data
		chart.invalidate()
	}
	
	
	
	
	
	class NormalViewHolder(itemView: View): ViewHolder(itemView) {
		val recyclerView: RecyclerView = itemView.findViewById(R.id.viewPager_recyclerView)
	}
	
	
	class FinalTabViewHolder(itemView: View) : ViewHolder(itemView) {
		val barChart: BarChart = itemView.findViewById(R.id.barChart)
	}
	
	
}
