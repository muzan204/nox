package com.muzan204.nox

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    private lateinit var face: NoxFaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        face = NoxFaceView(this)
        setContentView(face)

        window.statusBarColor = Color.rgb(5, 7, 13)
        window.navigationBarColor = Color.rgb(5, 7, 13)
        window.decorView.systemUiVisibility = 0
    }

    fun onFaceTapped() {
        // O toque será ligado ao microfone na próxima etapa.
        // Por enquanto, a carinha permanece como a interface principal.
    }
}
