package com.ruoyi.board

import android.app.Application

class BoardApplication : Application() {
    companion object {
        const val PREFS_NAME = "meeting_board_prefs"
        const val KEY_ROOM_ID = "room_id"
        const val BOARD_URL = "http://1.94.26.126/board.html"
        const val SERVER_URL = "http://1.94.26.126"
    }
}
