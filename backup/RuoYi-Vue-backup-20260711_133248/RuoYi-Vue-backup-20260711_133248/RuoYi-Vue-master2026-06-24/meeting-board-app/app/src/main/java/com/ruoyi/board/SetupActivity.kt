package com.ruoyi.board

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

data class RoomItem(val roomId: Long, val roomName: String, val location: String) {
    override fun toString(): String = if (location.isNotEmpty()) "$roomName ($location)" else roomName
}

class SetupActivity : AppCompatActivity() {

    private var rooms = listOf<RoomItem>()
    private var selectedRoomId: Long? = null
    private lateinit var btnConfirm: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        val spinner = findViewById<Spinner>(R.id.roomSpinner)
        btnConfirm = findViewById(R.id.btnConfirm)

        // Set initial placeholder
        spinner.adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_item, listOf("加载中..."))
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        loadRooms(spinner)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedRoomId = if (position > 0) rooms.getOrNull(position - 1)?.roomId else null
                btnConfirm.isEnabled = selectedRoomId != null
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnConfirm.setOnClickListener {
            selectedRoomId?.let { rid ->
                getSharedPreferences(BoardApplication.PREFS_NAME, Context.MODE_PRIVATE)
                    .edit().putString(BoardApplication.KEY_ROOM_ID, rid.toString()).apply()
                startActivity(Intent(this, KioskActivity::class.java))
                finish()
            }
        }
    }

    private fun loadRooms(spinner: Spinner) {
        thread {
            try {
                val url = URL("${BoardApplication.SERVER_URL}/prod-api/meeting/room/simpleList")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.requestMethod = "GET"
                conn.connect()
                if (conn.responseCode == 200) {
                    val text = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(text)
                    val data = json.optJSONArray("data") ?: JSONArray()
                    val list = mutableListOf<RoomItem>()
                    for (i in 0 until data.length()) {
                        val obj = data.getJSONObject(i)
                        list.add(RoomItem(
                            roomId = obj.getLong("roomId"),
                            roomName = obj.getString("roomName"),
                            location = obj.optString("location", "")
                        ))
                    }
                    rooms = list
                    runOnUiThread {
                        val adapter = ArrayAdapter(this@SetupActivity,
                            android.R.layout.simple_spinner_item, rooms)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spinner.adapter = adapter
                        btnConfirm.isEnabled = false
                    }
                }
            } catch (_: Exception) { }
        }
    }
}
