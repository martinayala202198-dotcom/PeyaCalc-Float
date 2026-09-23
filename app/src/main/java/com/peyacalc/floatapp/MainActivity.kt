package com.peyacalc.floatapp
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<MaterialButton>(R.id.btnActivar).setOnClickListener { checkOverlayPermission() }
    }
    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Activa 'Mostrar sobre otras apps' para PeyaCalc", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        } else {
            startService(Intent(this, CalculatorOverlayService::class.java))
            Toast.makeText(this, "Burbuja activada! Abri Peya Rider", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}
