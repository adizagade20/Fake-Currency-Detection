package com.ninesix.crfcd.helper_class

import java.util.ArrayList

class ModelLabelHelper {
	
	
	
	fun getLabelPrimaryPrediction(label: String): String {
		println("Label : $label")
		var primaryDetectionLabel: String = "Unknown"
		when (label) {
		
/*			"rs_10_new_f" -> primaryDetectionLabel = "Indian 10 Rs. Front"
			"rs_10_new_b" -> primaryDetectionLabel = "Indian 10 Rs. Back"
			"rs_10_old_f" -> primaryDetectionLabel = "Indian 10 Rs. Front (Old)"
			"rs_10_old_b" -> primaryDetectionLabel = "Indian 10 Rs. Back (Old)"*/

/*			"rs_20_new_f" -> primaryDetectionLabel = "Indian 20 Rs. Front"
			"rs_20_new_b" -> primaryDetectionLabel = "Indian 20 Rs. Back"
			"rs_20_old_f" -> primaryDetectionLabel = "Indian 20 Rs. Front (Old)"
			"rs_20_old_b" -> primaryDetectionLabel = "Indian 20 Rs. Back (Old)"*/
			
/*			"rs_50_new_f" -> primaryDetectionLabel = "Indian 50 Rs. Front"
			"rs_50_new_b" -> primaryDetectionLabel = "Indian 50 Rs. Back"
			"rs_50_old_f" -> primaryDetectionLabel = "Indian 50 Rs. Front (Old)"
			"rs_50_old_b" -> primaryDetectionLabel = "Indian 50 Rs. Back (Old)"*/
			
			"rs_100_o_f" -> primaryDetectionLabel = "Indian 100 Rs. Front (Old)"
			"rs_100_b" -> primaryDetectionLabel = "Indian 100 Rs. Back (Old)"
			"rs_100_n_f" -> primaryDetectionLabel = "Indian 100 Rs. Front"
			"rs_100_n_b" -> primaryDetectionLabel = "Indian 100 Rs. Back"
			
			"rs_200_n_f" -> primaryDetectionLabel = "Indian 200 Rs. Front"
			"rs_200_n_b" -> primaryDetectionLabel = "Indian 200 Rs. Back"
			
			"rs_500_n_f" -> primaryDetectionLabel = "Indian 500 Rs. Front"
			"rs_500_n_b" -> primaryDetectionLabel = "Indian 500 Rs. Back"
			
			"rs_2000_n_f" -> primaryDetectionLabel = "Indian 2000 Rs. Front"
			"rs_2000_n_b" -> primaryDetectionLabel = "Indian 2000 Rs. Back"
		}
		return primaryDetectionLabel
	}
	
	fun getModelAndLabelFileName(label: String): ArrayList<String> {
		val data = ArrayList<String>()
		when (label) {
		
/*			"rs_10_new_f" -> {
				data.add(0, "10newfront.tflite")
				data.add(1, "10newfront.txt")
			}
			"rs_10_new_b" -> {
				data.add(0, "10newback.tflite")
				data.add(1, "10newback.txt")
			}
			"rs_10_old_f" -> {
				data.add(0, "10oldfront.tflite")
				data.add(1, "10oldfront.txt")
			}
			"rs_10_old_b" -> {
				data.add(0, "10oldback.tflite")
				data.add(1, "10oldback.txt")
			}
			
			"rs_20_new_f" -> {
				data.add(0, "20newfront.tflite")
				data.add(1, "20newfront.txt")
			}
			"rs_20_new_b" -> {
				data.add(0, "20newback.tflite")
				data.add(1, "20newback.txt")
			}
			"rs_20_old_f" -> {
				data.add(0, "20oldfront.tflite")
				data.add(1, "20oldfront.txt")
			}
			"rs_20_old_b" -> {
				data.add(0, "20oldback.tflite")
				data.add(1, "20oldback.txt")
			}
			
			"rs_50_new_f" -> {
				data.add(0, "50newfront.tflite")
				data.add(1, "50newfront.txt")
			}
			"rs_50_new_b" -> {
				data.add(0, "50newback.tflite")
				data.add(1, "50newback.txt")
			}
			"rs_50_old_f" -> {
				data.add(0, "50oldfront.tflite")
				data.add(1, "50oldfront.txt")
			}
			"rs_50_old_b" -> {
				data.add(0, "50oldback.tflite")
				data.add(1, "50oldback.txt")
			}*/
			
			"rs_100_n_f" -> {
				data.add(0, "100newfront.tflite")
				data.add(1, "100newfront.txt")
				data.add(2, "16")
				
				data.add(3, "100newfront.tflite")
				data.add(4, "100newfront.txt")
				data.add(5, "16")
				
				data.add(6, "100newfront.tflite")
				data.add(7, "100newfront.txt")
				data.add(8, "16")
			}
			
			"rs_100_n_b" -> {
				data.add(0, "100newback.tflite")
				data.add(1, "100newback.txt")
				data.add(2, "16")
			}
			"rs_100_o_f" -> {
				data.add(0, "100oldfront.tflite")
				data.add(1, "100oldfront.txt")
				data.add(2, "16")
			}
			"rs_100_b" -> {
				data.add(0, "100oldback.tflite")
				data.add(1, "100oldback.txt")
				data.add(2, "16")
			}
			
			"rs_200_n_f" -> {
				data.add(0, "200newfront.tflite")
				data.add(1, "200newfront.txt")
				data.add(2, "16")
			}
			"rs_200_n_b" -> {
				data.add(0, "200newback.tflite")
				data.add(1, "200newback.txt")
				data.add(2, "16")
			}
			
			"rs_500_n_f" -> {
				data.add(0, "500newfront.tflite")
				data.add(1, "500newfront.txt")
				data.add(2, "16")
			}
			"rs_500_n_b" -> {
				data.add(0, "500newback.tflite")
				data.add(1, "500newback.txt")
				data.add(2, "16")
			}
			
			"rs_2000_n_f" -> {
				data.add(0, "2000newfront.tflite")
				data.add(1, "2000newfront.txt")
				data.add(2, "16")
			}
			"rs_2000_n_b" -> {
				data.add(0, "2000newback.tflite")
				data.add(1, "2000newback.txt")
				data.add(2, "16")
			}
		}
		return data
	}
}