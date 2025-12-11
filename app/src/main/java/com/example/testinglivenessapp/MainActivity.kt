package com.example.testinglivenessapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.camera.view.PreviewView

import com.komerce.liveness.LivenessFactory
import com.komerce.liveness.api.LivenessDetector
import com.komerce.liveness.api.LivenessStep

class MainActivity : AppCompatActivity() {

    // Definisikan tipe UI Component biar gak bingung
    private lateinit var cameraPreview: PreviewView
    private lateinit var tvInstruction: TextView
    private lateinit var btnStart: Button

    // Tambahin ": LivenessDetector" biar jelas tipenya
    private val livenessDetector: LivenessDetector by lazy {
        LivenessFactory.create(this)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startLivenessProcess()
        } else {
            Toast.makeText(this, "Perlu ijin kamera!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Casting Explicit biar error "Cannot infer type" hilang
        cameraPreview = findViewById<PreviewView>(R.id.cameraPreview)
        tvInstruction = findViewById<TextView>(R.id.tvInstruction)
        btnStart = findViewById<Button>(R.id.btnStart)

        livenessDetector.bind(this, cameraPreview)

        btnStart.setOnClickListener {
            checkPermissionAndStart()
        }
    }

    private fun checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startLivenessProcess()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startLivenessProcess() {
        btnStart.isEnabled = false

        val challenges = listOf(
            LivenessStep.LOOK_LEFT,
            LivenessStep.LOOK_RIGHT,
            LivenessStep.SMILE
        )

        tvInstruction.text = "Mohon Tengok KIRI"

        livenessDetector.startDetection(
            challenges = challenges,
            onStepSuccess = { step ->
                runOnUiThread {
                    val nextText = when(step) {
                        LivenessStep.LOOK_LEFT -> "Mantap! Sekarang Tengok KANAN"
                        LivenessStep.LOOK_RIGHT -> "Oke, Sekarang SENYUM :)"
                        LivenessStep.SMILE -> "Tahan..."
                        else -> "Lanjut..."
                    }
                    tvInstruction.text = nextText
                }
            },
            onStepError = { },
            onComplete = { result ->
                runOnUiThread {
                    val message = if (result.isSuccess) {
                        "SUKSES! Dapat ${result.evidencePhotos.size} Foto."
                    } else {
                        "GAGAL!"
                    }
                    tvInstruction.text = message
                    btnStart.isEnabled = true
                }
            }
        )
    }
}