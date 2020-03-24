package ru.vilture.covid_19

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.webview.*

class Russia : AppCompatActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.webview)

        // грузим карту
        webView.settings.javaScriptEnabled = true
        webView.setBackgroundColor(Color.WHITE)
        webView.clearCache(true)

        webView.loadUrl("https://covid.2gis.ru/")
    }
}
