<template>
  <div class="app-container">
    <el-row :gutter="16">
      <!-- ====== 左侧：设备列表 ====== -->
      <el-col :span="5">
        <el-card header="视频设备列表" shadow="hover" class="device-card">
          <el-input v-model="filterKeyword" placeholder="搜索设备" size="mini" clearable style="margin-bottom:8px" />
          <el-table :data="filteredDevices" @row-click="selectDevice" highlight-current-row
            size="mini" max-height="500" :row-class-name="tableRowClass">
            <el-table-column label="名称" prop="device_name" show-overflow-tooltip />
            <el-table-column label="状态" width="60">
              <template slot-scope="{row}">
                <i class="el-icon-video-camera" :style="{color: row.status==='ONLINE'?'#67c23a':'#f56c6c'}" />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- ====== 右侧：视频预览 ====== -->
      <el-col :span="19">
        <el-card shadow="hover">
          <div slot="header" class="clearfix">
            <span style="font-weight:600">
              <i class="el-icon-video-camera" style="margin-right:4px" />
              {{ selected ? selected.device_name : '请选择设备' }}
            </span>
            <span v-if="selected" style="float:right">
              <el-tag :type="streamMode==='websocket'?'success':'info'" size="mini" effect="plain" style="margin-right:8px">
                {{ streamMode==='websocket'?'WebSocket实时':'快照模式' }}
              </el-tag>
              <el-button size="mini" :type="playing?'danger':''" @click="togglePlay">
                <i :class="playing?'el-icon-video-pause':'el-icon-video-play'" /> {{ playing?'停止':'播放' }}
              </el-button>
              <el-button size="mini" type="warning" icon="el-icon-camera" @click="captureSnapshot">抓图</el-button>
            </span>
          </div>

          <!-- 视频画面区 -->
          <div class="video-container" ref="videoBox">
            <div v-if="!selected" class="video-placeholder">
              <i class="el-icon-video-camera" style="font-size:80px;color:#ddd;display:block" />
              <p style="color:#bbb;margin-top:16px">请在左侧选择视频设备</p>
            </div>
            <div v-else-if="!playing" class="video-placeholder">
              <i class="el-icon-video-play" style="font-size:64px;color:#ddd;display:block" />
              <p style="color:#bbb;margin-top:12px">点击"播放"按钮开始预览</p>
              <p style="font-size:11px;color:#ddd;margin-top:4px">RTSP: {{ computedRtspUrl }}</p>
            </div>
            <div v-else class="video-screen">
              <!-- WebSocket Canvas -->
              <canvas ref="videoCanvas" class="video-canvas" v-show="streamMode==='websocket'" />
              <!-- Snapshot IMG fallback -->
              <img ref="snapshotImg" class="video-canvas" v-show="streamMode==='snapshot'" />
              <!-- 加载状态 -->
              <div v-if="streaming" class="video-status">
                <i class="el-icon-loading" /> {{ streamMsg }}
              </div>
            </div>
          </div>

          <!-- RTSP 信息 -->
          <div v-if="selected" style="margin-top:10px;font-size:12px;color:#666">
            <span><b>设备:</b> {{ selected.ip_address }}:{{ selected.rtsp_port||554 }}</span>
            <span style="margin-left:16px"><b>RTSP:</b>
              <code style="font-size:11px;word-break:break-all">{{ computedRtspUrl }}</code>
              <el-button type="text" size="mini" @click="copyRtsp">复制</el-button>
            </span>
            <span style="margin-left:16px" v-if="selected.location"><b>位置:</b> {{ selected.location }}</span>
          </div>

          <!-- 抓图预览 -->
          <div v-if="snapshotImage" style="margin-top:10px">
            <el-dialog title="截图预览" :visible.sync="snapshotVisible" width="800px" append-to-body>
              <img :src="snapshotImage" style="width:100%" />
            </el-dialog>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'VideoPreview',
  data() {
    return {
      deviceList: [], filterKeyword: '', selected: null,
      playing: false, streaming: false, streamMsg: '连接中...',
      streamMode: 'websocket', // websocket | snapshot
      wsSocket: null, wsTimer: null, snapshotTimer: null,
      snapshotImage: null, snapshotVisible: false
    }
  },
  computed: {
    filteredDevices() {
      if (!this.filterKeyword) return this.deviceList
      const kw = this.filterKeyword.toLowerCase()
      return this.deviceList.filter(d => d.device_name.toLowerCase().includes(kw) || (d.location||'').toLowerCase().includes(kw))
    },
    computedRtspUrl() {
      if (!this.selected) return ''
      const s = this.selected
      if (s.rtsp_url) return s.rtsp_url
      const auth = (s.username && s.password) ? s.username+':'+s.password+'@' : ''
      return 'rtsp://'+auth+s.ip_address+':'+(s.rtsp_port||554)+'/Streaming/Channels/'+(s.channel||1)+'01'
    }
  },
  created() { this.loadDevices() },
  beforeDestroy() { this.stopStream() },
  methods: {
    loadDevices() {
      request({ url: '/smart/access/video-device/list', params: { pageSize: 500 } }).then(r => {
        this.deviceList = r.rows || []
      })
    },
    tableRowClass({ row }) {
      return row.device_id === (this.selected && this.selected.device_id) ? 'current-row' : ''
    },
    selectDevice(row) {
      if (this.playing) this.stopStream()
      this.selected = row
      this.snapshotImage = null
    },
    togglePlay() {
      if (this.playing) { this.stopStream() }
      else { this.startStream() }
    },
    // ===== 视频流 =====
    startStream() {
      if (!this.selected) return
      this.playing = true; this.streaming = true; this.streamMsg = '连接中...'

      // 优先尝试 WebSocket 模式
      const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
      const wsUrl = protocol + '//' + location.host + '/ws/video/' + this.selected.device_id

      try {
        this.wsSocket = new WebSocket(wsUrl)
        this.wsSocket.onopen = () => {
          this.streamMode = 'websocket'
          this.streamMsg = '已连接'
          this.streaming = false
          // 心跳
          this.wsTimer = setInterval(() => {
            if (this.wsSocket && this.wsSocket.readyState === WebSocket.OPEN) {
              this.wsSocket.send('PING')
            }
          }, 15000)
        }
        this.wsSocket.onmessage = (event) => {
          try {
            const msg = JSON.parse(event.data)
            if (msg.type === 'FRAME' && msg.image) {
              this.renderFrame(msg.image)
            } else if (msg.type === 'INFO') {
              this.streamMsg = '设备: ' + msg.deviceName
            }
          } catch(e) { /* ignore */ }
        }
        this.wsSocket.onerror = () => { this.fallbackToSnapshot() }
        this.wsSocket.onclose = () => {
          if (this.playing && this.streamMode === 'websocket') this.fallbackToSnapshot()
        }
      } catch (e) { this.fallbackToSnapshot() }
    },
    fallbackToSnapshot() {
      // WebSocket 失败，降级到快照刷新模式
      if (!this.playing) return
      this.streamMode = 'snapshot'
      this.streamMsg = '快照模式 (1s刷新)'
      this.streaming = false
      if (this.wsSocket) { this.wsSocket.close(); this.wsSocket = null }

      const doSnapshot = () => {
        if (!this.playing || this.streamMode !== 'snapshot') return
        const url = process.env.VUE_APP_BASE_API + '/smart/video/snapshot/' + this.selected.device_id + '?t=' + Date.now()
        this.$refs.snapshotImg.src = url
      }
      doSnapshot()
      this.snapshotTimer = setInterval(doSnapshot, 1000)
    },
    stopStream() {
      this.playing = false; this.streaming = false
      if (this.wsTimer) { clearInterval(this.wsTimer); this.wsTimer = null }
      if (this.snapshotTimer) { clearInterval(this.snapshotTimer); this.snapshotTimer = null }
      if (this.wsSocket) { this.wsSocket.close(); this.wsSocket = null }
    },
    renderFrame(base64) {
      const canvas = this.$refs.videoCanvas
      if (!canvas) return
      const img = new Image()
      img.onload = () => {
        canvas.width = img.width
        canvas.height = img.height
        const ctx = canvas.getContext('2d')
        ctx.drawImage(img, 0, 0, canvas.width, canvas.height)
      }
      img.src = 'data:image/jpeg;base64,' + base64
    },
    // ===== 抓图 =====
    captureSnapshot() {
      if (!this.selected) return
      const url = process.env.VUE_APP_BASE_API + '/smart/video/snapshot/' + this.selected.device_id + '?t=' + Date.now()
      this.snapshotImage = url
      this.snapshotVisible = true
    },
    // ===== 工具 =====
    copyRtsp() {
      const el = document.createElement('textarea')
      el.value = this.computedRtspUrl
      document.body.appendChild(el); el.select()
      document.execCommand('copy'); document.body.removeChild(el)
      this.$message.success('RTSP地址已复制')
    }
  }
}
</script>

<style scoped>
.device-card { height: calc(100vh - 100px); }
.video-container {
  background: #1a1a1a; border-radius: 8px; min-height: 420px;
  display: flex; align-items: center; justify-content: center;
  position: relative; overflow: hidden;
}
.video-placeholder { text-align: center; }
.video-screen { width: 100%; height: 100%; position: relative; }
.video-canvas { width: 100%; height: auto; display: block; }
.video-status {
  position: absolute; top: 10px; left: 10px;
  background: rgba(0,0,0,.6); color: #fff; padding: 4px 10px;
  border-radius: 4px; font-size: 12px;
}
</style>
