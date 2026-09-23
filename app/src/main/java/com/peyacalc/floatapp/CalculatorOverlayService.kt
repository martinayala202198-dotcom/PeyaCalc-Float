package com.peyacalc.floatapp
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.*
import android.widget.*
import java.util.*
class CalculatorOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private val puntos = mapOf("lv_dia" to Pair(590, 305), "lj_noche" to Pair(995, 595), "vd_noche" to Pair(1820, 1060), "sd_dia" to Pair(815, 500))
    private val tramos = mapOf("0-2" to Pair(220, 220), "2-4" to Pair(400, 400), "4-6" to Pair(660, 660), "6+" to Pair(745, 745))
    private val grupos = mapOf("Ninguno" to 0, "Grupo 1 ($275)" to 275, "Grupo 2 ($175)" to 175, "Grupo 3 ($105)" to 105, "Grupo 4 ($55)" to 55, "Grupo 8 ($105)" to 105)
    private val minimos = mapOf("lv_dia" to 400, "lj_noche" to 500, "vd_noche" to 600, "sd_dia" to 450)
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_calculator, null)
        val params = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT)
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 20; params.y = 300
        val head = floatingView.findViewById<View>(R.id.bubble_head)
        var initialX = 0; var initialY = 0; var initialTouchX = 0f; var initialTouchY = 0f
        head.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { initialX = params.x; initialY = params.y; initialTouchX = event.rawX; initialTouchY = event.rawY; true }
                MotionEvent.ACTION_MOVE -> { params.x = initialX + (event.rawX - initialTouchX).toInt(); params.y = initialY + (event.rawY - initialTouchY).toInt(); windowManager.updateViewLayout(floatingView, params); true }
                else -> false
            }
        }
        val spinnerFranja = floatingView.findViewById<Spinner>(R.id.spinnerFranja)
        val franjas = listOf("Auto (según hora)", "LUN-VIE 07:00-20:00 ($590/$305)", "LUN-JUE 20:00-fin ($995/$595)", "VIE-DOM 20:00-fin ($1820/$1060)", "SAB-DOM 07:00-20:00 ($815/$500)")
        spinnerFranja.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, franjas)
        val spinnerGrupo = floatingView.findViewById<Spinner>(R.id.spinnerGrupo)
        spinnerGrupo.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, grupos.keys.toList())
        floatingView.findViewById<View>(R.id.btnClose).setOnClickListener { stopSelf() }
        floatingView.findViewById<View>(R.id.btnCalcular).setOnClickListener { calcular() }
        windowManager.addView(floatingView, params)
    }
    private fun calcular() {
        val inputRetiro = floatingView.findViewById<EditText>(R.id.inputRetiro)
        val inputEntrega = floatingView.findViewById<EditText>(R.id.inputEntrega)
        val spinnerFranja = floatingView.findViewById<Spinner>(R.id.spinnerFranja)
        val spinnerGrupo = floatingView.findViewById<Spinner>(R.id.spinnerGrupo)
        val checkPeak = floatingView.findViewById<CheckBox>(R.id.checkPeak)
        val checkLluvia = floatingView.findViewById<CheckBox>(R.id.checkLluvia)
        val checkDoble = floatingView.findViewById<CheckBox>(R.id.checkDoble)
        val txtResultado = floatingView.findViewById<TextView>(R.id.txtResultado)
        val txtDesglose = floatingView.findViewById<TextView>(R.id.txtDesglose)
        val distRetiro = inputRetiro.text.toString().toDoubleOrNull() ?: 0.0
        val distEntrega = inputEntrega.text.toString().toDoubleOrNull() ?: 0.0
        val franjaKey = when (spinnerFranja.selectedItemPosition) {
            1 -> "lv_dia"; 2 -> "lj_noche"; 3 -> "vd_noche"; 4 -> "sd_dia"; else -> getFranjaActual()
        }
        val base = puntos[franjaKey] ?: Pair(590, 305)
        fun getMontoTramo(dist: Double, tipo: Int): Int {
            if (dist <= 0) return 0
            val key = when { dist <= 2 -> "0-2"; dist <= 4 -> "2-4"; dist <= 6 -> "4-6"; else -> "6+" }
            val par = tramos[key] ?: Pair(0,0)
            return if (tipo == 0) par.first else par.second
        }
        val montoKmRetiro = getMontoTramo(distRetiro, 0)
        val montoKmEntrega = getMontoTramo(distEntrega, 1)
        val grupoKey = spinnerGrupo.selectedItem as String
        val extraGrupo = grupos[grupoKey] ?: 0
        val bonoPeak = if (checkPeak.isChecked) 100 else 0
        val bonoLluvia = if (checkLluvia.isChecked) 150 else 0
        val extraDoble = if (checkDoble.isChecked) 200 else 0
        val total = base.first + base.second + montoKmRetiro + montoKmEntrega + extraGrupo + bonoPeak + bonoLluvia + extraDoble
        val distTotal = distRetiro + distEntrega
        val rendimientoKm = if (distTotal > 0) total / distTotal else total.toDouble()
        val minimo = minimos[franjaKey] ?: 400
        val esRentable = rendimientoKm >= minimo
        txtResultado.text = "$${total} | ${if (esRentable) "✅ RENTABLE" else "❌ NO"}"
        txtResultado.setBackgroundColor(if (esRentable) 0xFF00C853.toInt() else 0xFFFF1744.toInt())
        txtDesglose.text = "Franja: ${franjaKey.uppercase()} -> Base $${base.first}+$${base.second}\nKm Retiro: ${distRetiro}km -> $${montoKmRetiro}\nKm Entrega: ${distEntrega}km -> $${montoKmEntrega}\nGrupo: ${grupoKey} +$${extraGrupo}\n$${rendimientoKm.toInt()}/km vs Min $${minimo}/km"
    }
    private fun getFranjaActual(): String {
        val cal = Calendar.getInstance()
        val day = cal.get(Calendar.DAY_OF_WEEK)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val dayJS = when(day) { 1 -> 0; 2 -> 1; 3 -> 2; 4 -> 3; 5 -> 4; 6 -> 5; 7 -> 6; else -> 1 }
        return if (hour in 7..19) { if (dayJS in 1..5) "lv_dia" else "sd_dia" } else if (hour >= 20) { if (dayJS in 1..4) "lj_noche" else "vd_noche" } else { val prevDay = if (dayJS == 0) 6 else dayJS - 1; if (prevDay in 1..4) "lj_noche" else "vd_noche" }
    }
    override fun onDestroy() { super.onDestroy(); if (::floatingView.isInitialized) windowManager.removeView(floatingView) }
    override fun onBind(intent: Intent?): IBinder? = null
}
