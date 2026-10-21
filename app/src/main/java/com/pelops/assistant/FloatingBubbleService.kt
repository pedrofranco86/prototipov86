package com.pelops.assistant

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView

class FloatingBubbleService : Service() {
    private lateinit var wm: WindowManager
    private var bubble: TextView? = null
    private var params: WindowManager.LayoutParams? = null
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        if (bubble != null) return START_STICKY
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val v = TextView(this).apply {
            text = "P"; textSize = 22f; setTextColor(Color.WHITE); gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.rgb(32, 91, 180)); setStroke(2, Color.WHITE) }
            elevation = 12f
        }
        val p = WindowManager.LayoutParams(58, 58, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START; x = 20; y = 180
        }
        var sx = 0f; var sy = 0f; var ox = 0; var oy = 0
        v.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { sx = event.rawX; sy = event.rawY; ox = p.x; oy = p.y; true }
                MotionEvent.ACTION_UP -> {
                    if (kotlin.math.abs(event.rawX - sx) < 8 && kotlin.math.abs(event.rawY - sy) < 8) {
                        val i = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        startActivity(i)
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> { p.x = ox + (event.rawX - sx).toInt(); p.y = oy + (event.rawY - sy).toInt(); wm.updateViewLayout(v, p); true }
                else -> false
            }
        }
        wm.addView(v, p); bubble = v; params = p
        return START_STICKY
    }
    override fun onDestroy() { if (bubble != null) { try { wm.removeView(bubble) } catch (_: Exception) {} }; bubble = null; super.onDestroy() }
}
