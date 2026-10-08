package com.muzan204.nox

import android.os.Bundle
import android.view.Window
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)

        window.statusBarColor = android.graphics.Color.rgb(5, 7, 13)
        window.navigationBarColor = android.graphics.Color.rgb(5, 7, 13)
        window.decorView.systemUiVisibility = 0
    }
}
