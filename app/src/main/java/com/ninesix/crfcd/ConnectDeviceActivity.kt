package com.ninesix.crfcd

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class ConnectDeviceActivity : AppCompatActivity() {
	
	companion object {
		 const val EXTRA_ADDRESS ="device_address"
	}
	
	//---------------------------------------------------------------------------------------- VARIABLES ----------------------------------------------------------------------------------------//
	
	//----------------------------- HOOKS -----------------------------//
	private lateinit var listView: ListView
	
	
	//----------------------------- BLUETOOTH -----------------------------//
	private var bluetoothAdapter: BluetoothAdapter ?= null
	private lateinit var pairedDevices: Set<BluetoothDevice>
	
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_connect_device)
		
		
		//----------------------------- HOOKS -----------------------------//
		listView = findViewById(R.id.alert_box_list_view)
		
		
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
		
		if (bluetoothAdapter == null) {
			Toast.makeText(applicationContext, "Bluetooth device not available", Toast.LENGTH_LONG).show()
			finish()
		} else if (!bluetoothAdapter!!.isEnabled) {
			val turnOnBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
			startActivityForResult(turnOnBluetoothIntent, 1)
		}
		
		pairedDevicesList()
		
	}
	
	private fun pairedDevicesList() {
		pairedDevices = bluetoothAdapter!!.bondedDevices
		val list: ArrayList<String> = ArrayList<String>()
		
		if (pairedDevices.isNotEmpty())
			for (bt in pairedDevices)
				list.add("${bt.name} \n ${bt.address}")
		else
			Toast.makeText(applicationContext, "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show()
		
		val adapter: ArrayAdapter<*> = ArrayAdapter(this@ConnectDeviceActivity, android.R.layout.simple_list_item_1, list)
		listView.adapter = adapter
		listView.onItemClickListener = myListClickListener
		
	}
	
	
	private val myListClickListener = OnItemClickListener { _, view, _, _ ->
		val info = (view as TextView).text.toString()
		val address = info.substring(info.length - 17)
		val intent = Intent(this@ConnectDeviceActivity, MainActivity::class.java)
		intent.putExtra(EXTRA_ADDRESS, address)
		startActivity(intent)
	}
	
	
	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		if(requestCode == 1 && resultCode == RESULT_OK) {
			pairedDevicesList()
		}
	}
	
}