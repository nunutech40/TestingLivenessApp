package com.example.testinglivenessapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.komerce.liveness.LivenessFactory
import com.komerce.liveness.api.LivenessDetector
import com.komerce.liveness.api.LivenessStep
import com.komerce.liveness.api.LivenessResult
import com.komerce.liveness.api.LivenessError

class MainActivity : AppCompatActivity() {

    // UI Components
    private lateinit var cameraPreview: PreviewView
    private lateinit var tvInstruction: TextView
    private lateinit var btnStart: Button

    // SDK Instance
    private val livenessDetector: LivenessDetector by lazy {
        LivenessFactory.create(this)
    }

    // --- 1. SETUP SKENARIO (CONFIG) DI SINI ---
    // Enak dibaca: Kita mau urutannya Kiri -> Kanan -> Senyum
    private val livenessScenario = listOf(
        LivenessStep.LOOK_LEFT,
        LivenessStep.LOOK_RIGHT,
        LivenessStep.SMILE
    )

    // Permission Launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) startLivenessProcess()
        else Toast.makeText(this, "Butuh ijin kamera bos!", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupUI()

        // Bind SDK (Wajib)
        livenessDetector.bind(this, cameraPreview)
    }

    private fun setupUI() {
        cameraPreview = findViewById(R.id.cameraPreview)
        tvInstruction = findViewById(R.id.tvInstruction)
        btnStart = findViewById(R.id.btnStart)

        btnStart.setOnClickListener {
            checkPermissionAndStart()
        }
    }

    private fun checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startLivenessProcess()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Memisahkan "Teks Instruksi" dari "Logic SDK".
    // Kalau mau ganti kata-kata, cukup ganti di sini, gak usah ngudek-ngudek startDetection.
    private fun getInstructionText(step: LivenessStep): String {
        return when (step) {
            LivenessStep.LOOK_LEFT -> "Mohon Tengok KIRI ⬅️"
            LivenessStep.LOOK_RIGHT -> "Sekarang Tengok KANAN ➡️"
            LivenessStep.SMILE -> "Terakhir, SENYUM Lebar! 😁"
            LivenessStep.BLINK -> "Coba Kedipkan Mata 😉"
            else -> "Processing..."
        }
    }

    // --- 3. EKSEKUSI (CLEAN VERSION) ---
    private fun startLivenessProcess() {
        // UI Preparation
        btnStart.isEnabled = false

        // Ambil instruksi pertama dari skenario
        val firstStep = livenessScenario.first()
        tvInstruction.text = getInstructionText(firstStep)

        // START SDK
        livenessDetector.startDetection(
            challenges = livenessScenario, // Inject Skenario

            onStepSuccess = { completedStep ->
                // Cari step selanjutnya apa
                val nextStepIndex = livenessScenario.indexOf(completedStep) + 1
                if (nextStepIndex < livenessScenario.size) {
                    val nextStep = livenessScenario[nextStepIndex]

                    // Update UI di Main Thread
                    runOnUiThread {
                        tvInstruction.text = getInstructionText(nextStep)
                    }
                } else {
                    runOnUiThread { tvInstruction.text = "Mengambil Foto..." }
                }
            },

            onStepError = { error ->
                // Optional: Handle error UI per frame (misal: "Wajah Hilang!")
            },

            onComplete = { result ->
                handleResult(result)
            }
        )
    }

    private fun handleResult(result: LivenessResult) {
        runOnUiThread {
            btnStart.isEnabled = true
            if (result.isSuccess) {
                // Tampilkan sukses + jumlah foto bukti
                tvInstruction.text = "VERIFIKASI SUKSES! ✅\nBukti: ${result.evidencePhotos.size} Foto"

                // Contoh: Akses foto senyum
                // val smilePhoto = result.evidencePhotos[LivenessStep.SMILE]
            } else {
                tvInstruction.text = "Verifikasi Gagal ❌"
            }
        }
    }
}