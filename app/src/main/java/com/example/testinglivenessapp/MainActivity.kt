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
import com.komerce.liveness.api.LivenessConfig
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

    // --- 1. SETUP CONFIGURATION (CLEAN PATTERN) ---
    // Semua settingan Liveness diatur di satu objek ini.
    private val myLivenessConfig = LivenessConfig(
        steps = listOf(
            LivenessStep.LOOK_LEFT,
            LivenessStep.LOOK_RIGHT,
            LivenessStep.SMILE
        ),
        isAuditMode = true // Set 'true' kalau mau simpan foto tiap step, 'false' kalau cuma foto akhir
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

        // Bind SDK (Wajib dipanggil di onCreate)
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

    // Helper: Mapping Text UI
    private fun getInstructionText(step: LivenessStep): String {
        return when (step) {
            LivenessStep.LOOK_LEFT -> "Mohon Tengok KIRI ⬅️"
            LivenessStep.LOOK_RIGHT -> "Sekarang Tengok KANAN ➡️"
            LivenessStep.SMILE -> "Terakhir, SENYUM Lebar! 😁"
            LivenessStep.BLINK -> "Coba Kedipkan Mata 😉"
            else -> "Processing..."
        }
    }

    // --- 2. EKSEKUSI (MENGGUNAKAN CONFIG) ---
    private fun startLivenessProcess() {
        // Disable tombol biar gak di-spam
        btnStart.isEnabled = false

        // Ambil instruksi pertama dari Config
        val steps = myLivenessConfig.steps
        if (steps.isNotEmpty()) {
            tvInstruction.text = getInstructionText(steps.first())
        }

        // START SDK (Sekarang cuma butuh lempar config object)
        livenessDetector.startDetection(
            config = myLivenessConfig,

            onStepSuccess = { completedStep ->
                // Cari step selanjutnya
                val nextStepIndex = steps.indexOf(completedStep) + 1

                if (nextStepIndex < steps.size) {
                    val nextStep = steps[nextStepIndex]
                    // Update UI (Wajib runOnUiThread karena callback dari background)
                    runOnUiThread {
                        tvInstruction.text = getInstructionText(nextStep)
                    }
                } else {
                    runOnUiThread { tvInstruction.text = "Mengambil Foto..." }
                }
            },

            onStepError = { error ->
                // Optional: Handle error UI per frame (misal: kasih toast atau text merah)
                // Log.e("Liveness", "Error: $error")
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
                // Sukses! Cek mode apa yang dipakai
                if (myLivenessConfig.isAuditMode) {
                    // Mode Audit: Ada banyak foto bukti
                    val buktiCount = result.stepEvidence.size
                    tvInstruction.text = "VERIFIKASI SUKSES (AUDIT)! ✅\nDapat $buktiCount Foto Bukti + 1 Selfie Lurus"
                } else {
                    // Mode Standard: Cuma 1 foto
                    tvInstruction.text = "VERIFIKASI SUKSES (STD)! ✅\nDapat 1 Foto Selfie Lurus"
                }

                // Note: Foto ada di result.totalBitmap
            } else {
                tvInstruction.text = "Verifikasi Gagal ❌"
            }
        }
    }
}