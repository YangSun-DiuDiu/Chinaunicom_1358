package com.ruoyi.board

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class KioskActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var touchDownTime = 0L
    private val handler = Handler(Looper.getMainLooper())
    private var exitRunnable: Runnable? = null

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Fullscreen immersive
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }

        setContentView(R.layout.activity_kiosk)

        val prefs = getSharedPreferences(BoardApplication.PREFS_NAME, Context.MODE_PRIVATE)
        val roomId = prefs.getString(BoardApplication.KEY_ROOM_ID, null)

        if (roomId.isNullOrEmpty()) {
            Toast.makeText(this, "未配置会议室", Toast.LENGTH_LONG).show()
            startActivity(android.content.Intent(this, SetupActivity::class.java))
            finish()
            return
        }

        webView = findViewById(R.id.kioskWebView)
        setupWebView()
        webView.loadUrl("${BoardApplication.BOARD_URL}?roomId=$roomId")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.setBackgroundColor(Color.parseColor("#1a1a2e"))

        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        // Corner long-press exit detection
        webView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchDownTime = System.currentTimeMillis()
                    val w = webView.width.toFloat()
                    val h = webView.height.toFloat()
                    val cornerSize = 100f
                    val inCorner = (event.x < cornerSize || event.x > w - cornerSize) &&
                                   (event.y < cornerSize || event.y > h - cornerSize)
                    if (inCorner) {
                        exitRunnable?.let { handler.removeCallbacks(it) }
                        exitRunnable = Runnable { showExitDialog() }
                        handler.postDelayed(exitRunnable!!, 5000)
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    exitRunnable?.let { handler.removeCallbacks(it) }
                }
            }
            false
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?,
                                         error: WebResourceError?) {
                view?.evaluateJavascript(
                    "document.getElementById('errorOverlay').className='error-overlay visible';", null)
            }
        }
    }

    private fun showExitDialog() {
        val editText = EditText(this).apply {
            hint = "请输入管理员密码"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        AlertDialog.Builder(this)
            .setTitle("管理员验证")
            .setView(editText)
            .setPositiveButton("确认") { _, _ ->
                if (editText.text.toString() == "admin888") {
                    getSharedPreferences(BoardApplication.PREFS_NAME, Context.MODE_PRIVATE)
                        .edit().remove(BoardApplication.KEY_ROOM_ID).apply()
                    startActivity(Intent(this, SetupActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "密码错误", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
