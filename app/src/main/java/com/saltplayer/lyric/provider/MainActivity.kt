package com.saltplayer.lyric.provider

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val textView: TextView = findViewById(R.id.text_view)
        textView.text = "SaltPlayer Lyric Provider\n\n" +
                "This is an LSPosed module that provides lyrics functionality for SaltPlayer.\n\n" +
                "Please enable this module in LSPosed and select SaltPlayer as the scope.\n\n" +
                "Then restart the module and SaltPlayer to apply changes."
    }
}
