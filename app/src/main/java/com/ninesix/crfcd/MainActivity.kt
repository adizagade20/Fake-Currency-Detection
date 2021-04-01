package com.ninesix.crfcd

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.media.Image
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewTreeObserver
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.ninesix.crfcd.databinding.ActivityMainBinding
import com.ninesix.crfcd.helper_class.ClassifierModel
import com.ninesix.crfcd.helper_class.CustomModelInterpreter
import com.ninesix.crfcd.helper_class.ImageClassification
import com.ninesix.crfcd.helper_class.ModelLabelHelper
import kotlinx.coroutines.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.HashMap
import kotlin.concurrent.schedule
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random


class MainActivity : AppCompatActivity(), CoroutineScope {
	
	companion object {
		private const val TAG = "MainActivity"
		private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
		private val REQUEST_CODE_PERMISSIONS = Random.nextInt(1000)
		private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
		private val IMAGE_TYPE = arrayOf("NORMAL", "WHITE LIGHT", "BACK SIDE")
	}
	
	//---------------------------------------------------------------------------------------- VARIABLES ----------------------------------------------------------------------------------------//
	private val job = Job()
	override val coroutineContext: CoroutineContext get() = job + Dispatchers.Default
	
	private var progress: ProgressDialog? = null
	private var width: Int = 0
	private var height: Int = 0
	private var focusOverlayTimer: TimerTask? = null
	
	
	//----------------------------- CAMERA -----------------------------//
	private val imageUriList: HashMap<String, String> = HashMap()
	private lateinit var imageAnalysis: ImageAnalysis
	private lateinit var imageCapture: ImageCapture
	
	private lateinit var executor: ExecutorService
	private lateinit var bitmap: Bitmap
	private lateinit var cameraProvider: ProcessCameraProvider
	
	private lateinit var camera: androidx.camera.core.Camera
	
	private var isCaptureInProgress = false
	
	
	//----------------------------- BLUETOOTH -----------------------------//
	private lateinit var labelForDetailedDetection: String
	private val myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
	private var bluetoothSocket: BluetoothSocket? = null
	private var isBluetoothConnected = false
	private var bluetoothAdapter: BluetoothAdapter? = null
	private lateinit var asyncTask: AsyncTask<Void, Void, Void>
	private lateinit var connectBluetooth: ConnectBluetooth
	lateinit var address: String
	
	
	//----------------------------- OBJECT DETECTION -----------------------------//
	private lateinit var imageClassification: ImageClassification
	
	
	//----------------------------- OBJECTS -----------------------------//
	private val modelLabelHelper = ModelLabelHelper()
	private lateinit var binding: ActivityMainBinding
	
	
	//----------------------------- OCR -----------------------------//
	private lateinit var recognizer: TextRecognizer
	
	
	//----------------------------- ON CREATE -----------------------------//
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		binding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
//		bluetoothListAlertBox()
		
		onCreateContinued()
		
	}
	
	private fun onCreateContinued() {
		
		//----------------------------- DIMENSIONS -----------------------------//
		val vto: ViewTreeObserver = binding.focusOverlayInclude.focusOverlayLinearLayout.viewTreeObserver
		vto.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
			override fun onPreDraw(): Boolean {
				binding.focusOverlayInclude.focusOverlayLinearLayout.viewTreeObserver.removeOnPreDrawListener(this)
				width = binding.focusOverlayInclude.focusOverlayLinearLayout.measuredWidth
				height = binding.focusOverlayInclude.focusOverlayLinearLayout.measuredHeight
				Log.d(TAG, "onPreDraw: $width x $height")
				return false
			}
		})
		
		
		//----------------------------- CAMERA PERMISSION -----------------------------//
		executor = Executors.newSingleThreadExecutor()
		if (allPermissionsGranted()) {
			startCamera()
		} else {
			ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
		}
		
		
		//----------------------------- CAPTURE BUTTON -----------------------------//
		binding.cameraControls.takePhoto.animate().scaleX(1f).scaleY(1f).setDuration(1000).start()
		binding.cameraControls.takePhoto.visibility = View.VISIBLE
		binding.imageTypeTextView.visibility = View.VISIBLE
		
		binding.cameraControls.takePhoto.setOnClickListener {
			if (isCaptureInProgress) {
				Toast.makeText(applicationContext, "Capture in progress", Toast.LENGTH_SHORT).show()
				return@setOnClickListener
			} else {
				isCaptureInProgress = true
				when (imageUriList.size) {
					0 -> {
						takePhoto(true, "front")
					}
					1 -> {
						val isBTReady = sendSignalToBluetooth("A")
						camera.cameraControl.enableTorch(false).addListener({
							Timer().schedule(2000) { takePhoto(isBTReady, "WL") }
						}, executor)
					}
					2 -> {
						val isBTReady = sendSignalToBluetooth("B")
						camera.cameraControl.enableTorch(false).addListener({
							Timer().schedule(2000) { takePhoto(isBTReady, "back") }
						}, executor)
					}
				}
			}
		}
		
		
		//----------------------------- OCR -----------------------------//
		recognizer = TextRecognition.getClient()
		
		
		//----------------------------- FLASH BUTTON -----------------------------//
		binding.cameraControls.flash.setOnClickListener {
			if (camera.cameraInfo.hasFlashUnit()) {
				if (camera.cameraInfo.torchState.value == TorchState.ON) {
					camera.cameraControl.enableTorch(false).addListener({
						binding.cameraControls.flash.background = ResourcesCompat.getDrawable(resources, R.drawable.flash_off, null)
					}, executor)
				} else if (camera.cameraInfo.torchState.value == TorchState.OFF) {
					camera.cameraControl.enableTorch(true).addListener({
						binding.cameraControls.flash.background = ResourcesCompat.getDrawable(resources, R.drawable.flash_on, null)
					}, executor)
				}
			}
		}
		
	}
	
	
	//----------------------------- BLUETOOTH -----------------------------//
	private fun bluetoothListAlertBox() {
		binding.mainToolbarInclude.bluetoothStatusToolbarIv.setImageResource(R.drawable.bluetooth_disabled)
		
		val rootView: View = LayoutInflater.from(applicationContext).inflate(R.layout.layout_paired_devices, null)
		
		val builder = AlertDialog.Builder(this)
		builder.setView(rootView)
		val alert = builder.create()
		alert.setCancelable(false)
		alert.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
		alert.show()
		
		val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
		
		val listView: ListView = rootView.findViewById(R.id.listView)
		
		if (bluetoothAdapter == null) {
			Toast.makeText(applicationContext, "Bluetooth device not available", Toast.LENGTH_LONG).show()
			finish()
		} else if (!bluetoothAdapter.isEnabled) {
			val turnOnBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
			startActivityForResult(turnOnBluetoothIntent, 1)
		}
		
		val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter!!.bondedDevices
		val list: ArrayList<String> = ArrayList<String>()
		
		if (pairedDevices.isNotEmpty()) {
			for (bt in pairedDevices)
				list.add("${bt.name} \n ${bt.address}")
		} else
			Toast.makeText(applicationContext, "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show()
		
		val adapter: ArrayAdapter<*> = ArrayAdapter(applicationContext, android.R.layout.simple_list_item_1, list)
		listView.adapter = adapter
		
		listView.onItemClickListener = AdapterView.OnItemClickListener { _, view, _, _ ->
			val info = (view as TextView).text.toString()
			address = info.substring(info.length - 17)

//			asyncTask = ConnectBT().execute(*arrayOfNulls<Void>(0))
			connectBluetooth = ConnectBluetooth(binding.mainParentLayout)
			connectBluetooth.execute()
			alert.dismiss()
			onCreateContinued()
		}
		
	}
	
	
	private fun sendSignalToBluetooth(value: String): Boolean {
		return try {
			bluetoothSocket!!.outputStream.write(value.toByteArray())
			true
		} catch (e: Exception) {
			Toast.makeText(applicationContext, "There is some error contacting bluetooth, try again!", Toast.LENGTH_LONG).show()
			false
		}
	}
	
	
	private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
		ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
	}
	
	override fun onDestroy() {
		super.onDestroy()
		executor.shutdown()
	}
	
	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
		if (requestCode == REQUEST_CODE_PERMISSIONS) {
			if (allPermissionsGranted()) {
				startCamera()
			} else {
				Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
				finish()
			}
		}
	}
	
	
	@SuppressLint("UnsafeExperimentalUsageError", "ClickableViewAccessibility")
	private fun startCamera() {
		binding.imageTypeTextView.text = IMAGE_TYPE[0]
		
		val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
		
		cameraProviderFuture.addListener({
			// Used to bind the lifecycle of cameras to the lifecycle owner
			cameraProvider = cameraProviderFuture.get()
			
			
			// Preview
			val preview = Preview.Builder()
				.setTargetAspectRatio(AspectRatio.RATIO_4_3)
				.setTargetRotation(binding.viewFinder.display.rotation)
				.build()
				.also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }
			
			// Select back camera as a default
			val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
			
			imageAnalysis = ImageAnalysis.Builder()
				.setTargetAspectRatio(AspectRatio.RATIO_4_3)
				.setTargetRotation(binding.viewFinder.display.rotation)
				.setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
				.build()
			
			imageCapture = ImageCapture.Builder().build()
			
			try {
				cameraProvider.unbindAll()
				camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis, imageCapture)
			} catch (exc: Exception) {
				Log.e(TAG, "Use case binding failed", exc)
			}
			
			
			binding.viewFinder.setOnTouchListener(OnTouchListener { _: View, motionEvent: MotionEvent ->
				when (motionEvent.action) {
					MotionEvent.ACTION_DOWN -> return@OnTouchListener true
					MotionEvent.ACTION_UP -> {
						val factory = binding.viewFinder.meteringPointFactory
						val point = factory.createPoint(motionEvent.x, motionEvent.y)
						val action = FocusMeteringAction.Builder(point).build()
						camera.cameraControl.startFocusAndMetering(action)
						
						var x = motionEvent.x - 64
						var y = motionEvent.y - 64
						
						x = if (x < 0) motionEvent.x else if (x > (width - 128)) (width - 128).toFloat() else motionEvent.x - 64
						
						y = if (y < 0) motionEvent.y else if (y > (height - 128)) (height - 128).toFloat() else motionEvent.y - 64
						
						binding.focusOverlayInclude.focusOverlay.x = x
						binding.focusOverlayInclude.focusOverlay.y = y
						binding.focusOverlayInclude.focusOverlay.visibility = View.VISIBLE
						focusOverlayTimer?.cancel()
						focusOverlayTimer = Timer().schedule(3000) {
							runOnUiThread { binding.focusOverlayInclude.focusOverlay.visibility = View.GONE }
						}
						return@OnTouchListener true
					}
					else -> return@OnTouchListener false
				}
			})
			
			imageAnalysis.setAnalyzer(executor, { imageProxy: ImageProxy ->
				
				/*bitmap = imageProxy.image!!.toBitmap()
				recognize(imageProxy)*/
				
				/*val options = ObjectDetectorOptions.Builder()
					.setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
					.enableMultipleObjects()
					.build()
				val objectDetector = ObjectDetection.getClient(options)*/

//				CoroutineScope(Dispatchers.IO).launch {
//					val predictions = CustomModelInterpreter(applicationContext, "100NewFrontNormal", progress).execute(bitmap)
//				}
				
				val mediaImage = imageProxy.image
				
				if (mediaImage != null) {
					val image = InputImage.fromMediaImage(mediaImage, 0)
					Log.d(TAG, "startCamera: Rotation: ${imageProxy.imageInfo.rotationDegrees}")
					recognizer.process(image)
						.addOnSuccessListener {
							processOCRResult(it.text)
						}
						.addOnCompleteListener {
							
							Timer().schedule(2000) { imageProxy.close() }
							
							/*objectDetector.process(image)
							.addOnSuccessListener { it1 ->
								
								Log.d(TAG, "startCameraObject: ${it1.size}")
								
								if (it1.size != 0) {
									
									val overlay = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
									
									val canvas = Canvas(overlay)
									
									val paint = Paint()
									paint.alpha = 0xA0
									paint.color = Color.MAGENTA
									paint.style = Paint.Style.STROKE
									paint.textSize = 58f
									paint.strokeWidth = 2f
									
									canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
									
									it1.forEach { it2 ->
										Log.d(TAG, "startCamera: ${it2.boundingBox}")
//											Log.d(TAG, "startCamera: ${it2.boundingBox.left}")
//											Log.d(TAG, "startCamera: ${it2.boundingBox.top}")
//											Log.d(TAG, "startCamera: ${it2.boundingBox.right}")
//											Log.d(TAG, "startCamera: ${it2.boundingBox.bottom}")
//											Log.d(TAG, "startCamera: ${it2.boundingBox.left * width / mediaImage.width}")
//											Log.d(TAG, "startCamera: ${it2.boundingBox.top * height / mediaImage.height}")
//											Log.d(TAG, "startCamera: ${it2.boundingBox.right * width / mediaImage.width}")
//											Log.d(TAG, "startCamera: ${it2.boundingBox.bottom * height / mediaImage.height}")
										
										canvas.drawRect(
//												(it2.boundingBox.left * width / mediaImage.width).toFloat(),
//												(it2.boundingBox.top * height / mediaImage.height).toFloat(),
//												(it2.boundingBox.right * width / mediaImage.width).toFloat(),
//												(it2.boundingBox.bottom * height / mediaImage.height).toFloat()
											RectF(it2.boundingBox), paint
//												, paint
										)
									}
									canvas.drawText("i   t", 100f, 100f, paint)
									val matrix = Matrix()
//										matrix.setRotate(-90f)
									
									Log.d(TAG, "startCamera: overlay : ${overlay.width} x ${overlay.height}")
//										overlay = Bitmap.createBitmap(overlay, 0, 0, overlay.width, overlay.height, matrix, false)
									rectBoxOverlay.setImageBitmap(overlay)
									Log.d(TAG, "startCamera: overlay : ${overlay.width} x ${overlay.height}")
								}
							}*/
						}
				}
			})
			
		}, ContextCompat.getMainExecutor(this))
	}
	
	
	private fun getOutputDirectory(): File {
		val mediaDir = externalMediaDirs.firstOrNull()?.let {
			File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
		}
		return if (mediaDir != null && mediaDir.exists())
			mediaDir else filesDir
	}
	
	private fun takePhoto(isBTReady: Boolean, imageType: String) {
		if (!isBTReady) {
			isCaptureInProgress = false
			Snackbar.make(findViewById(R.id.main_parent_layout), "There is some error connecting to Bluetooth, try restarting the Device and App!", Snackbar.LENGTH_INDEFINITE)
				.setAction("Continue") {
					recreate()
				}
			return
		}
		if (imageType == "front") {
			imageAnalysis.clearAnalyzer()
			cameraProvider.unbind(imageAnalysis)
		}
		
		val outputDirectory = getOutputDirectory()
		
		val photoFile = File(outputDirectory, SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg")
		val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
		
		imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
			override fun onError(exc: ImageCaptureException) {
				Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
				Toast.makeText(applicationContext, "There is some error capturing the photo! Please try restarting the device", Toast.LENGTH_LONG).show()
			}
			
			override fun onImageSaved(output: ImageCapture.OutputFileResults) {
				val savedUri = Uri.fromFile(photoFile)
				val msg = "Photo capture succeeded !"
				Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
				Log.d(TAG, "onImageSaved: $msg $savedUri")
				
				isCaptureInProgress = false
				
				imageUriList[imageType] = savedUri.toString()
				
				when (imageType) {
					"front" -> {
						binding.imageTypeTextView.text = IMAGE_TYPE[1]
						CoroutineScope(Dispatchers.IO).launch {
							val customModelInterpreter = CustomModelInterpreter(this@MainActivity)
							val predictions = customModelInterpreter.execute(savedUri.toString())
							customModelInterpreter.cancel()
						}
					}
					"WL" -> {
						binding.imageTypeTextView.text = IMAGE_TYPE[2]
					}
					"back" -> {
						cameraProvider.unbindAll()
						connectBluetooth.cancel()
						bluetoothSocket?.close()
						
						Timer().schedule(1000) {
							recognizer.close()
							val intent = Intent(this@MainActivity, ResultActivity::class.java)
							intent.putExtra("NormalImage", imageUriList["front"])
							intent.putExtra("WhiteLightImage", imageUriList["WL"])
							intent.putExtra("UVLightImage", imageUriList["back"])
							intent.putExtra("modelName", "100NewFront")
							startActivity(intent)
						}
					}
				}
			}
		})
	}
	
	private fun processOCRResult(text: String) {
//		Log.d(TAG, "processOCRResult: $text")
	}
	
	
	
	
	
	
	private fun Image.toBitmap(): Bitmap {
		val yBuffer = planes[0].buffer // Y
		val vuBuffer = planes[2].buffer // VU
		
		val ySize = yBuffer.remaining()
		val vuSize = vuBuffer.remaining()
		
		val nv21 = ByteArray(ySize + vuSize)
		
		yBuffer.get(nv21, 0, ySize)
		vuBuffer.get(nv21, ySize, vuSize)
		
		val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
		val out = ByteArrayOutputStream()
		yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
		val imageBytes = out.toByteArray()
		return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
	}
	
	private fun recognize(imageProxy: ImageProxy) = launch {
		
		Timer().schedule(2000) {
			imageProxy.close()
		}
		bitmap = Bitmap.createBitmap(bitmap, bitmap.width * 5 / 100, bitmap.height * 33 / 100, bitmap.width * 90 / 100, bitmap.height * 34 / 100)
		bitmap = Bitmap.createScaledBitmap(bitmap, 640, 640, false)
		
		val predictions = async { imageClassification.classifyImage(bitmap) }
//		val predictions = imageClassification.classifyImage(bitmap)
		Log.d(TAG, "recognize: ${predictions.await()}")
		
		runOnUiThread {
			if (binding.cameraControls.takePhoto.visibility != View.VISIBLE) {
				binding.cameraControls.takePhoto.animate().scaleX(1f).scaleY(1f).setDuration(1000).start()
				binding.cameraControls.takePhoto.visibility = View.VISIBLE
				binding.imageTypeTextView.visibility = View.VISIBLE
			}
		}
		
		labelForDetailedDetection = "rs_100_n_f"
		
		val label = "${modelLabelHelper.getLabelPrimaryPrediction(predictions.await()[0].label)} : ${(predictions.await()[0].score * 100).toInt()} %"
		
	}
	
	private fun loadModule() = launch {
		withContext(Dispatchers.IO) {
			imageClassification = ImageClassification.create(
				classifierModel = ClassifierModel.FLOAT,
				assetManager = assets,
				modelPath = "all.tflite",
				labelPath = "all.txt",
				numberOfResults = 1
			)
		}
	}
	
	
	
	
	
	
	private inner class ConnectBluetooth(private val mainLayout: ConstraintLayout) : CoroutineScope {
		val job = Job()
		override val coroutineContext: CoroutineContext get() = Dispatchers.Main + job
		
		private var connectSuccess: Boolean = true
		
		fun cancel() {
			job.cancel()
		}
		
		fun execute() = launch {
			onPreExecute()
			val result = doInBackground()
			onPostExecute(result)
		}
		
		private suspend fun doInBackground(): String = withContext(Dispatchers.IO) {
			try {
				if (this@MainActivity.bluetoothSocket == null || !this@MainActivity.isBluetoothConnected) {
					this@MainActivity.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
					val dispositivo: BluetoothDevice = this@MainActivity.bluetoothAdapter!!.getRemoteDevice(this@MainActivity.address)
//					this@MainActivity.bluetoothSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(this@MainActivity.myUUID)
					this@MainActivity.bluetoothSocket = dispositivo.createRfcommSocketToServiceRecord(this@MainActivity.myUUID)
					BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
					this@MainActivity.bluetoothSocket!!.connect()
				}
			} catch (e: IOException) {
				connectSuccess = false
			}
			return@withContext "Success"
		}
		
		private fun onPreExecute() {
			this@MainActivity.progress = ProgressDialog.show(this@MainActivity, "Connecting...", "Please wait !!!")
		}
		
		private fun onPostExecute(result: String) {
			if (connectSuccess) {
				Log.d(TAG, "onPostExecute: Connected.")
				this@MainActivity.isBluetoothConnected = true
				binding.mainToolbarInclude.bluetoothStatusToolbarIv.setImageResource(R.drawable.bluetooth_connected)
			} else {
				Log.d(TAG, "onPostExecute: Connection Failed. Is it a SPP Bluetooth? Try again.")
				val snackBar = Snackbar.make(mainLayout, "Connection Failed. Is it a SPP Bluetooth?", Snackbar.LENGTH_INDEFINITE)
				snackBar.animationMode = Snackbar.ANIMATION_MODE_SLIDE
				snackBar.setAction("Retry") {
					bluetoothListAlertBox()
				}
				snackBar.show()
			}
			this@MainActivity.progress!!.dismiss()
		}
		
	}
	
	
	
}

//************************************************************************************** BLUETOOTH USING ASYNC TASK **************************************************************************************//

/*	@SuppressLint("StaticFieldLeak")
	private inner class ConnectBT : AsyncTask<Void, Void, Void>() {
		
		private var connectSuccess: Boolean = true
		
		override fun onPreExecute() {
			super.onPreExecute()
			this@MainActivity.progress = ProgressDialog.show(this@MainActivity, "Connecting...", "Please wait!!!")
		}
		
		override fun doInBackground(vararg devices: Void): Void? {
			try {
				if (this@MainActivity.bluetoothSocket == null || !this@MainActivity.isBluetoothConnected) {
					this@MainActivity.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
					val dispositivo: BluetoothDevice = this@MainActivity.bluetoothAdapter!!.getRemoteDevice(this@MainActivity.address)
//					this@MainActivity.bluetoothSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(this@MainActivity.myUUID)
					this@MainActivity.bluetoothSocket = dispositivo.createRfcommSocketToServiceRecord(this@MainActivity.myUUID)
					BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
					this@MainActivity.bluetoothSocket!!.connect()
				}
			} catch (e: IOException) {
				this.connectSuccess = false
			}
			return null
		}
		
		
		override fun onPostExecute(result: Void?) {
			super.onPostExecute(result)
			if (connectSuccess) {
				Log.d(TAG, "onPostExecute: Connected.")
				this@MainActivity.isBluetoothConnected = true
			} else {
				Log.d(TAG, "onPostExecute: Connection Failed. Is it a SPP Bluetooth? Try again.")
				Toast.makeText(applicationContext, "Connection Failed. Is it a SPP Bluetooth? Try again.", Toast.LENGTH_LONG).show()
				
				bluetoothListAlertBox()
//				asyncTask = ConnectBT().execute(*arrayOfNulls<Void>(0))
//				Timer().schedule(2000) { runOnUiThread { this@MainActivity.finish() } }
			}
			this@MainActivity.progress!!.dismiss()
		}
	}*/



//************************************************************************************** TESSERACT OCR WORKING **************************************************************************************//

/*implementation 'com.rmtheis:tess-two:9.0.0'

private var mTess: TessBaseAPI? = null
private lateinit var datapath : String
private val language = "hin"

Timer().schedule(100) {
	try {
		datapath = "$filesDir/tesseract/"
		mTess = TessBaseAPI()
		checkFile(File(datapath + "tessdata/"))
		mTess?.init(datapath, language, TessBaseAPI.OEM_TESSERACT_ONLY)
		Log.d(TAG, "onCreate: COMPLETE")
	} catch (e: Exception) {
		Log.e(TAG, "onCreate: ${e.localizedMessage}")
	}
}

private fun processImage(image: Bitmap) {
	Log.d(TAG, "processImage: ${mTess == null}")
	mTess?.setImage(image)
	val ocrResult = mTess!!.utF8Text
	Toast.makeText(baseContext, ocrResult, Toast.LENGTH_LONG).show()
	Log.d(TAG, "processImage: $ocrResult")
}

private fun checkFile(dir: File, datapath: String, language: String) {
	if (!dir.exists() && dir.mkdirs()) {
		copyFiles(datapath, language)
	}
	if (dir.exists()) {
		val dataFilePath = "${datapath}/tessdata/${language}.traineddata"
		val datafile = File(dataFilePath)
		if (!datafile.exists()) {
			copyFiles(datapath, language)
		}
	}
}

private fun copyFiles(datapath: String, language: String) {
	try {
		val filepath = "$datapath/tessdata/${language}.traineddata"
		val assetManager = assets
		val inputStream = assetManager.open("tessdata/${language}.traineddata")
		val outputStream: OutputStream = FileOutputStream(filepath)
		val buffer = ByteArray(1024)
		var read: Int
		
		while (inputStream.read(buffer).also { read = it } != -1) {
			outputStream.write(buffer, 0, read)
		}
		outputStream.flush()
		outputStream.close()
		inputStream.close()
		
		val file = File(filepath)
		if (!file.exists()) {
			throw FileNotFoundException()
		}
	} catch (e: FileNotFoundException) {
		e.printStackTrace()
	} catch (e: IOException) {
		e.printStackTrace()
	}
}*/


//************************************************************************************ TESSERACT OCR NOT WORKING ************************************************************************************//

/*private fun prepareDirectory(path: String) {
val dir = File(path)
if (!dir.exists()) {
	if (!dir.mkdirs()) {
		Log.e(TAG, "ERROR: Creation of directory $path failed, check does Android Manifest have permission to write to external storage.")
	}
} else {
	Log.i(TAG, "Created directory $path")
}
}

private fun copyTessDataFiles(path: String) {
try {
	val fileList = assets.list(path)
	for (fileName in fileList!!) {
		
		// open file within the assets folder
		// if it is not already there copy it to the sdcard
		val pathToDataFile: String = "$dataPath$path/$fileName"
		if (!File(pathToDataFile).exists()) {
			val `in` = assets.open("$path/$fileName")
			val out: OutputStream = FileOutputStream(pathToDataFile)
			
			// Transfer bytes from in to out
			val buf = ByteArray(1024)
			var len: Int
			while (`in`.read(buf).also { len = it } > 0) {
				out.write(buf, 0, len)
			}
			`in`.close()
			out.close()
			Log.d(TAG, "Copied " + fileName + "to tessdata")
		}
	}
} catch (e: IOException) {
	Log.e(TAG, "Unable to copy files to tessdata $e")
}
}

private fun prepareTesseract() {
try {
	prepareDirectory(dataPath + "tessdata")
} catch (e: java.lang.Exception) {
	e.printStackTrace()
}
copyTessDataFiles("tessdata")
}

private fun startOCR(imgUri: Uri) {
try {
	val options = BitmapFactory.Options()
	options.inSampleSize = 4 // 1 - means max size. 4 - means maxsize/4 size. Don't use value <4, because you need more memory in the heap to store your data.
	val bitmap = BitmapFactory.decodeFile(imgUri.toString().substring(8), options)
	val result = extractText(bitmap)
	Log.d(TAG, "startOCR: Aditya $result")
} catch (e: java.lang.Exception) {
	Log.e(TAG, e.message!!)
}
}

private fun extractText(bitmap: Bitmap): String? {
var tessBaseApi : TessBaseAPI?= null
try {
	tessBaseApi = TessBaseAPI()
} catch (e: java.lang.Exception) {
	Log.e(TAG, e.message!!)
	if (tessBaseApi == null) {
		Log.e(TAG, "TessBaseAPI is null. TessFactory not returning tess object.")
	}
}
tessBaseApi!!.init(dataPath, "hin")

//       //EXTRA SETTINGS
//        //For example if we only want to detect numbers
//        tessBaseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "1234567890");
//
//        //blackList Example
//        tessBaseApi.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "!@#$%^&*()_+=-qwertyuiop[]}{POIU" +
//                "YTRWQasdASDfghFGHjklJKLl;L:'\"\\|~`xcvXCVbnmBNM,./<>?");
Log.d(TAG, "Training file loaded")
tessBaseApi!!.setImage(bitmap)
var extractedText = "empty result"
try {
	extractedText = tessBaseApi.utF8Text
} catch (e: java.lang.Exception) {
	Log.e(TAG, "Error in recognizing text.")
}
tessBaseApi.end()
return extractedText
}*/