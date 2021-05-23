package com.ninesix.crfcd

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.*
import android.view.View.OnTouchListener
import android.widget.*
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.ninesix.crfcd.databinding.ActivityMainBinding
import com.ninesix.crfcd.databinding.LayoutMainCurrencyChooserBinding
import com.ninesix.crfcd.helper_class.*
import kotlinx.coroutines.*
import me.pqpo.smartcropperlib.SmartCropper
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
		private val CAMERA_PERMISSION_REQUEST_CODE = Random.nextInt(1000)
		private val BLUETOOTH_PERMISSION_REQUEST_CODE = Random.nextInt(1000)
		private val IMAGE_TYPE = arrayOf("NORMAL", "WHITE LIGHT", "BACK SIDE")
	}
	
	//---------------------------------------------------------------------------------------- VARIABLES ----------------------------------------------------------------------------------------//
	private val job = Job()
	override val coroutineContext: CoroutineContext get() = Dispatchers.Default + job
	
	private var width: Int = 0
	private var height: Int = 0
	private var focusOverlayTimer: TimerTask? = null
	
	
	//----------------------------- CAMERA -----------------------------//
	private val imageUriList: HashMap<String, String> = HashMap()
	private lateinit var imageAnalysis: ImageAnalysis
	private lateinit var imageCapture: ImageCapture
	
	private lateinit var executor: ExecutorService
	private lateinit var cameraProvider: ProcessCameraProvider
	
	private lateinit var camera: androidx.camera.core.Camera
	
	private var isCaptureInProgress = false
	
	
	//----------------------------- BLUETOOTH -----------------------------//
	private val myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
	private var bluetoothSocket: BluetoothSocket? = null
	private var isBluetoothConnected = false
	private var bluetoothAdapter: BluetoothAdapter? = null
	private lateinit var connectBluetooth: ConnectBluetooth
	lateinit var address: String
	
	
	//----------------------------- OBJECT DETECTION -----------------------------//
//	private lateinit var imageClassification: ImageClassification
	
	
	//----------------------------- OBJECTS -----------------------------//
//	private val modelLabelHelper = ModelLabelHelper()
	private lateinit var binding: ActivityMainBinding
	private lateinit var detectedCurrency: ObjectPrediction
	
	
	//----------------------------- OCR -----------------------------//
//	private lateinit var recognizer: TextRecognizer
	
	
	//----------------------------- ON CREATE -----------------------------//
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		binding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(binding.root)
		setSupportActionBar(binding.mainToolbarInclude.materialToolbar)
		
		getModelNameFromUser(ObjectPrediction(RectF(0f,0f,0f,0f), "rs_2000_new", 10.0f))
		
		SmartCropper.buildImageDetector(this);
		
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
		if (ContextCompat.checkSelfPermission(baseContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
			startCamera()
		} else {
			ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
		}
		
		
		//----------------------------- CAPTURE BUTTON -----------------------------//
		binding.cameraControls.takePhoto.animate().scaleX(1f).scaleY(1f).setDuration(1000).start()
		binding.cameraControls.takePhoto.visibility = View.VISIBLE
		binding.imageTypeTextView.visibility = View.VISIBLE
		
		binding.cameraControls.takePhoto.setOnClickListener {
			if (isCaptureInProgress) {
//				val snackBar = Snackbar.make(binding.root, "Image capture in progress", Snackbar.LENGTH_SHORT)
//				snackBar.setAction("Ok") {}
//				snackBar.show()
				return@setOnClickListener
			} else {
				isCaptureInProgress = true
				binding.cameraControls.takePhoto.isClickable = false
				binding.cameraControls.takePhoto.isActivated = false
				when (imageUriList.size) {
					0 -> {
						takePhoto("front")
					}
					1 -> {
						camera.cameraControl.enableTorch(false).addListener({
							binding.cameraControls.flash.background = ResourcesCompat.getDrawable(resources, R.drawable.flash_off, null)
							runOnUiThread { bluetoothListAlertBox() }
						}, executor)
					}
					2 -> {
//						camera.cameraControl.enableTorch(false).addListener({
						takePhoto("back")
//						}, executor)
					}
				}
			}
		}
		
		
		//----------------------------- OCR -----------------------------//
//		recognizer = TextRecognition.getClient()
		
		
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
			} else {
				Toast.makeText(applicationContext, "No flash unit available", Toast.LENGTH_LONG).show()
			}
		}
		
		//----------------------------- CAMERA SWITCH BUTTON -----------------------------//
		
		
	}


//--------------------------------------------------------------------------------------------- BLUETOOTH --------------------------------------------------------------------------------------------//
	
	private fun bluetoothListAlertBox() {
		
		if (bluetoothSocket?.isConnected == true) {
			sendSignalToBluetooth()
			return
		}
		
		val rootView: View = LayoutInflater.from(applicationContext).inflate(R.layout.layout_main_bluetooth_list_view, null)
		
		val builder = AlertDialog.Builder(this)
		builder.setView(rootView)
		val alert = builder.create()
		alert.setCancelable(false)
		alert.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
		alert.show()
		
		val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
		
		val listView: ListView = rootView.findViewById(R.id.alert_box_list_view)
		
		if (bluetoothAdapter == null) {
			Toast.makeText(applicationContext, "Bluetooth device not available", Toast.LENGTH_LONG).show()
			finish()
		} else if (!bluetoothAdapter.isEnabled) {
			val turnOnBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
			startActivityForResult(turnOnBluetoothIntent, BLUETOOTH_PERMISSION_REQUEST_CODE)
			alert.dismiss()
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
			connectBluetooth = ConnectBluetooth(binding)
			connectBluetooth.execute()
			alert.dismiss()
		}
		
	}
	
	
	private fun sendSignalToBluetooth() {
		try {
			bluetoothSocket!!.outputStream.write("A".toByteArray())
			if (bluetoothSocket!!.inputStream.read() == 65) {
				takePhoto("WL")
			}
		} catch (e: Exception) {
			Toast.makeText(applicationContext, "There is some error contacting bluetooth, try again!", Toast.LENGTH_LONG).show()
			bluetoothListAlertBox()
		}
	}
	
	
	override fun onDestroy() {
		super.onDestroy()
		executor.shutdown()
	}
	
	
	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
			if (ContextCompat.checkSelfPermission(baseContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
				startCamera()
			} else {
				Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
				finish()
			}
		} else if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
			if (ContextCompat.checkSelfPermission(baseContext, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED) {
				Log.d(TAG, "onRequestPermissionsResult: aditya")
				bluetoothListAlertBox()
			} else {
				Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
			}
		}
	}
	
	
	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE && resultCode == RESULT_OK) {
			bluetoothListAlertBox()
		} else if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE && resultCode == RESULT_CANCELED) {
			
			val snackBar = Snackbar.make(binding.root, "Bluetooth permission required", Snackbar.LENGTH_INDEFINITE)
			snackBar.setAction("Turn ON") {
				val turnOnBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
				startActivityForResult(turnOnBluetoothIntent, BLUETOOTH_PERMISSION_REQUEST_CODE)
			}
			snackBar.show()
		}
	}

//************************************************************************************* BINDING CAMERA USE CASES *************************************************************************************//
	
	@SuppressLint("UnsafeExperimentalUsageError")
	private fun startCamera() {
		binding.imageTypeTextView.text = IMAGE_TYPE[0]
		
		val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
		
		cameraProviderFuture.addListener({
			cameraProvider = cameraProviderFuture.get()
			
			val preview = Preview.Builder()
				.setTargetAspectRatio(AspectRatio.RATIO_4_3)
				.setTargetRotation(binding.viewFinder.display.rotation)
				.build()
				.also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }
			
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
			
			focusOverlayConfig()
			
			/*imageAnalysis.setAnalyzer(executor, { imageProxy: ImageProxy ->
			
			})*/
			
		}, ContextCompat.getMainExecutor(this))
	}
	
	
	/*val options = ObjectDetectorOptions.Builder()
//				.setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
				.enableMultipleObjects()
				.enableClassification()  // Optional
				.build()
			
			val objectDetector = ObjectDetection.getClient(options)
			
			val paint = Paint().apply {
				isAntiAlias = true
				style = Paint.Style.STROKE
				color = Color.RED
				strokeWidth = 10f
			}*/
	
	/*bitmap = imageProxy.image!!.toBitmap()
				recognize(imageProxy)*/
	
	/*CoroutineScope(Dispatchers.IO).launch {
					val mediaImage = imageProxy.image
					if (mediaImage != null) {
						val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
						recognizer.process(image)
							.addOnSuccessListener {
								processOCRResult(it.text)
							}
							.addOnCompleteListener {
//								Timer().schedule(2000) { imageProxy.close() }
							}
						
						val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
						
						objectDetector.process(image)
							.addOnSuccessListener { objects ->
								val canvas = Canvas(bitmap)
								Log.d(TAG, "startCamera: $width x $height \t\t : \t\t ${mediaImage.width} x ${mediaImage.height} \t\t : \t\t ${bitmap.width} x ${bitmap.height}")
								for (detectedObject in objects) {
									Log.d(TAG, "startCamera: ${detectedObject.boundingBox}")
									Log.d(
										TAG, "startCamera:" +
												"${detectedObject.boundingBox.left * width / mediaImage.width} ${detectedObject.boundingBox.top * height / mediaImage.height} " +
												"${detectedObject.boundingBox.right * width / mediaImage.width} ${detectedObject.boundingBox.bottom * height / mediaImage.height}"
									)
									canvas.drawRect(
										(detectedObject.boundingBox.left * width / mediaImage.width).toFloat(),
										(detectedObject.boundingBox.top * height / mediaImage.height).toFloat(),
										(detectedObject.boundingBox.right * width / mediaImage.width).toFloat(),
										(detectedObject.boundingBox.bottom * height / mediaImage.height).toFloat(),
										paint
									)
								}
								binding.detectedObjects.setImageBitmap(bitmap)
								
							}
							.addOnCompleteListener {
								Timer().schedule(10000) { imageProxy.close() }
							}
					}
				}*/
	
	
	@SuppressLint("ClickableViewAccessibility")
	private fun focusOverlayConfig() {
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
					if (focusOverlayTimer != null) {
						binding.focusOverlayInclude.focusOverlay.scaleX = 0f
						binding.focusOverlayInclude.focusOverlay.scaleY = 0f
					}
					binding.focusOverlayInclude.focusOverlay.animate().scaleX(1f).scaleY(1f).setDuration(500).start()
					binding.focusOverlayInclude.focusOverlay.visibility = View.VISIBLE
					focusOverlayTimer?.cancel()
					focusOverlayTimer = Timer().schedule(2000) {
						runOnUiThread {
							binding.focusOverlayInclude.focusOverlay.animate().scaleX(0f).scaleY(0f).setDuration(250).start()
						}
						focusOverlayTimer = null
					}
					return@OnTouchListener true
				}
				else -> return@OnTouchListener false
			}
		})
	}
	
	
	private fun getOutputDirectory(): File {
		val mediaDir = externalMediaDirs.firstOrNull()?.let {
			File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
		}
		return if (mediaDir != null && mediaDir.exists())
			mediaDir else filesDir
	}
	
	
	private fun takePhoto(imageType: String) {
		
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
				Toast.makeText(applicationContext, "There is some error capturing the photo! Please try restarting the application", Toast.LENGTH_LONG).show()
			}
			
			override fun onImageSaved(output: ImageCapture.OutputFileResults) {
				val savedUri = Uri.fromFile(photoFile)
				Log.d(TAG, "onImageSaved: Photo capture succeeded ! $savedUri")
				
				/*CoroutineScope(Dispatchers.IO).launch {
					val customModelInterpreter = CustomModelInterpreter(this@MainActivity)
					val predictions = customModelInterpreter.execute(savedUri.toString(), "all_trial")
					Log.d(TAG, "onImageSavedWithoutCompression: $predictions")
				}*/
				
				CoroutineScope(Dispatchers.Main).launch {
					cropImage(savedUri, imageType)
				}
			}
		})
	}
	
	
	private fun cropImage(savedUri: Uri, imageType: String) {
		binding.mainCaptureLayout.visibility = View.GONE
		binding.mainCameraControlLayout.visibility = View.GONE
		binding.mainCropLayout.visibility = View.VISIBLE
			
		isCaptureInProgress = false
		binding.cameraControls.takePhoto.isClickable = true
		binding.cameraControls.takePhoto.isActivated = true
		
		binding.mainCropCrop.setImageToCrop(BitmapFactory.decodeFile(savedUri.path))
		
		binding.mainCropRetake.setOnClickListener {
			binding.mainCaptureLayout.visibility = View.VISIBLE
			binding.mainCameraControlLayout.visibility = View.VISIBLE
			binding.mainCropLayout.visibility = View.GONE
		}
		
		binding.mainCropSave.setOnClickListener {
			if (binding.mainCropCrop.canRightCrop()) {
				val croppedBitmap = binding.mainCropCrop.crop()
				croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, FileOutputStream(File(savedUri.path!!)))
			} else {
				Snackbar.make(binding.root, "Can not crop image, please retry cropping or retake", Snackbar.LENGTH_SHORT).show()
			}
			saveImageAndChangeType(savedUri, imageType)
		}
	}
	
	
	private fun saveImageAndChangeType(savedUri: Uri, imageType: String) {
		imageUriList[imageType] = savedUri.toString()
		
		when (imageType) {
			"front" -> {
				binding.imageTypeTextView.text = IMAGE_TYPE[1]
				CoroutineScope(Dispatchers.IO).launch {
					val customModelInterpreter = CustomModelInterpreter(this@MainActivity)
					val predictions = customModelInterpreter.execute(savedUri.toString(), "all")
					detectedCurrency = predictions[0]
					customModelInterpreter.cancel()
				}
			}
			"WL" -> {
				binding.imageTypeTextView.text = IMAGE_TYPE[2]
			}
			"back" -> {
				cameraProvider.unbindAll()
				runOnUiThread { getModelNameFromUser(detectedCurrency) }
			}
		}
		
		binding.mainCaptureLayout.visibility = View.VISIBLE
		binding.mainCameraControlLayout.visibility = View.VISIBLE
		binding.mainCropLayout.visibility = View.GONE
	}
	
	
	
	
	
	private fun getModelNameFromUser(detectedCurrency: ObjectPrediction) {
		val rootBinding = LayoutMainCurrencyChooserBinding.inflate(layoutInflater)
		val intent = Intent(this@MainActivity, ResultActivity::class.java)
		intent.putExtra("front", imageUriList["front"])
		intent.putExtra("WL", imageUriList["WL"])
		intent.putExtra("back", imageUriList["back"])
		rootBinding.mainCurrencyChooserRecycler.layoutManager = LinearLayoutManager(this)
		rootBinding.mainCurrencyChooserTextView.text = Html.fromHtml("According to the system, given note is of <u><b>${detectedCurrency.label}</u></b>, but final call will be yours")
		rootBinding.mainCurrencyChooserRecycler.adapter = MainCurrencyChooserAdapter(this, detectedCurrency, intent)
		
		val builder = AlertDialog.Builder(this)
		builder.setView(rootBinding.root)
		val alert = builder.create()
		alert.setCancelable(false)
		alert.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
		alert.show()
	}
	
	
	/*private fun loadModule(fileName: String, numberOfResults: Int, savedUri: Uri) = launch {
		withContext(Dispatchers.IO) {
			val imageClassification = ImageClassification.create(
				classifierModel = ClassifierModel.FLOAT,
				assetManager = assets,
				modelPath = "$fileName.tflite",
				labelPath = "$fileName.txt",
				numberOfResults = 1
			)
			var bitmap = BitmapFactory.decodeFile(savedUri.path)
			bitmap = Bitmap.createScaledBitmap(bitmap, 640, 640, false)
			val predictions = imageClassification.classifyImage(bitmap)
			Log.d(TAG, "loadModule: $predictions")
			Log.d(TAG, "onCreate: ${System.currentTimeMillis()}")
		}
	}*/
	
	
//********************************************************************************** BLUETOOTH CONNECTION COROUTINE **********************************************************************************//

	private inner class ConnectBluetooth(private val binding: ActivityMainBinding) : CoroutineScope {
		val job = Job()
		override val coroutineContext: CoroutineContext get() = Dispatchers.Main + job
		
		private var connectSuccess: Boolean = true
//		private lateinit var builder: AlertDialog
		
		fun cancel() {
			job.cancel()
		}
		
		fun execute() = launch {
			onPreExecute()
			doInBackground()
			onPostExecute()
		}
		
		private suspend fun doInBackground() = withContext(Dispatchers.IO) {
			Log.d(TAG, "doInBackground: start")
			try {
				if (this@MainActivity.bluetoothSocket == null || !this@MainActivity.isBluetoothConnected) {
					this@MainActivity.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
					val bluetoothDevice: BluetoothDevice = this@MainActivity.bluetoothAdapter!!.getRemoteDevice(this@MainActivity.address)
					this@MainActivity.bluetoothSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(this@MainActivity.myUUID)
//					this@MainActivity.bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(this@MainActivity.myUUID)
					BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
					this@MainActivity.bluetoothSocket!!.connect()
					Log.d(TAG, "doInBackground: connect")
				}
			} catch (e: Exception) {
				Log.d(TAG, "doInBackground: $e")
				connectSuccess = false
			}
			return@withContext
		}
		
		private fun onPreExecute() {
//			this@MainActivity.progress = ProgressDialog.show(this@MainActivity, "Connecting...", "Please wait !!!")
//			val alert = AlertDialog.Builder(applicationContext)
//			alert.setView(LayoutMainBtProgressBinding.inflate(layoutInflater).root)
//			builder = alert.create()
//			builder.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//			builder.setCancelable(false)
//			builder.show()
		}
		
		private fun onPostExecute() {
//			builder.cancel()
			if (connectSuccess) {
				Log.d(TAG, "onPostExecute: Connected.")
				sendSignalToBluetooth()
				this@MainActivity.isBluetoothConnected = true
			} else {
				val snackBar = Snackbar.make(binding.root, "Connection Failed. Is it a SPP Bluetooth?", Snackbar.LENGTH_INDEFINITE)
				snackBar.animationMode = Snackbar.ANIMATION_MODE_SLIDE
				snackBar.setAction("Retry") {
					bluetoothListAlertBox()
				}
				snackBar.show()
			}
		}
		
	}
	
}



//****************************************************************************************** OLD INTERPRETER *****************************************************************************************//
	
	/*private fun loadModule() = launch {
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
	}*/
	

//************************************************************************************ BLUETOOTH USING ASYNC TASK ************************************************************************************//

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