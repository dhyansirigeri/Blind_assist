package com.example.blind_assist

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import org.tensorflow.lite.task.vision.detector.ObjectDetector.ObjectDetectorOptions
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var previewView: PreviewView
    private lateinit var statusText: TextView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var tts: TextToSpeech
    private lateinit var vibrator: Vibrator
    private lateinit var objectDetector: ObjectDetector

    private var lastSpeakTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        statusText = findViewById(R.id.statusText)

        tts = TextToSpeech(this, this)

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        val options = ObjectDetectorOptions.builder()
            .setMaxResults(3)
            .setScoreThreshold(0.5f)
            .build()

        objectDetector = ObjectDetector.createFromFileAndOptions(
            this,
            "detect.tflite",
            options
        )

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                100
            )
        }
    }

    private fun startCamera() {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                analyzeFrame(imageProxy)
                imageProxy.close()
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()

            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalyzer
            )

        }, ContextCompat.getMainExecutor(this))
    }

    private fun analyzeFrame(imageProxy: ImageProxy) {

        val currentTime = System.currentTimeMillis()

        if (currentTime - lastSpeakTime > 3000) {

            val bitmap = imageProxyToBitmap(imageProxy)

            val tensorImage = TensorImage.fromBitmap(bitmap)

            val results = objectDetector.detect(tensorImage)

            if (results.isEmpty()) {

                runOnUiThread {
                    statusText.text = "Path clear"
                }

                speak("Path clear, walk forward")

                lastSpeakTime = currentTime
                return
            }

            val detection = results[0]
            val label = detection.categories[0].label.lowercase()

            val box = detection.boundingBox
            val centerX = box.centerX()
            val width = bitmap.width

            val position = when {
                centerX < width / 3 -> "left"
                centerX < 2 * width / 3 -> "ahead"
                else -> "right"
            }

            val boxWidth = box.width()

            val distanceMeters = when {
                boxWidth > 600 -> 0.5
                boxWidth > 400 -> 1.0
                boxWidth > 250 -> 2.0
                else -> 3.0
            }

            // Detect door
            if (label.contains("door")) {

                if (distanceMeters <= 1.0) {
                    speak("Closed door ahead. Stop or turn back")
                    vibrateStrong()
                } else {
                    speak("Door ahead $distanceMeters meters")
                }

                runOnUiThread {
                    statusText.text = "Door ahead"
                }

                lastSpeakTime = currentTime
                return
            }

            // Detect wall (large object filling screen)
            if (box.width() > bitmap.width * 0.8) {

                speak("Wall ahead. Stop and turn")

                vibrateStrong()

                runOnUiThread {
                    statusText.text = "Wall ahead"
                }

                lastSpeakTime = currentTime
                return
            }

            runOnUiThread {
                statusText.text = "$label $position $distanceMeters m"
            }

            if (distanceMeters <= 1.0) {

                speak("Warning $label very close $position")

                vibrateStrong()

            } else {

                when (position) {

                    "left" -> speak("$label left $distanceMeters meters, move right")

                    "right" -> speak("$label right $distanceMeters meters, move left")

                    "ahead" -> speak("$label ahead $distanceMeters meters")

                }
            }

            lastSpeakTime = currentTime
        }
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {

        val yBuffer = imageProxy.planes[0].buffer
        val uBuffer = imageProxy.planes[1].buffer
        val vBuffer = imageProxy.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(
            nv21,
            ImageFormat.NV21,
            imageProxy.width,
            imageProxy.height,
            null
        )

        val out = ByteArrayOutputStream()

        yuvImage.compressToJpeg(
            Rect(0, 0, imageProxy.width, imageProxy.height),
            100,
            out
        )

        val imageBytes = out.toByteArray()

        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun vibrateStrong() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    300,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )

        } else {

            vibrator.vibrate(300)

        }
    }

    override fun onInit(status: Int) {

        if (status == TextToSpeech.SUCCESS) {

            tts.language = Locale.US

        }
    }
}