<template>
  <div class="app-container map-page">
    <el-row :gutter="12">
      <!-- ============ 左侧：底图列表 ============ -->
      <el-col :span="6">
        <el-card class="map-list-card" shadow="hover">
          <div slot="header" class="clearfix">
            <span><i class="el-icon-picture-outline"></i> 底图列表</span>
            <el-button style="float:right;padding:3px 8px" type="primary" size="mini" icon="el-icon-plus"
              @click="openAddDialog" v-hasPermi="['smart:videoMap:add']">新增</el-button>
          </div>

          <!-- 筛选 -->
          <el-row :gutter="6" style="margin-bottom:10px">
            <el-col :span="12">
              <el-select v-model="filterType" placeholder="类型" size="mini" clearable @change="loadMaps">
                <el-option label="室外" value="OUTDOOR" /><el-option label="室内" value="INDOOR" />
              </el-select>
            </el-col>
            <el-col :span="12">
              <el-input v-model="filterName" placeholder="搜索名称" size="mini" clearable @input="loadMaps" />
            </el-col>
          </el-row>

          <!-- 地图列表 -->
          <div class="map-item-list" v-loading="mapLoading">
            <div v-for="m in mapList" :key="m.map_id"
              class="map-item" :class="{ active: currentMap && currentMap.map_id === m.map_id }"
              @click="selectMap(m)">
              <div class="map-item-thumb">
                <img v-if="m.map_image" :src="m.map_image" />
                <i v-else class="el-icon-picture" style="font-size:32px;color:#ccc" />
              </div>
              <div class="map-item-info">
                <div class="map-item-name">
                  {{ m.map_name }}
                  <el-tag :type="m.map_type==='INDOOR'?'warning':'success'" size="mini" effect="plain" style="margin-left:4px">
                    {{ m.map_type==='INDOOR'?'室内':'室外' }}
                  </el-tag>
                </div>
                <div class="map-item-sub" v-if="m.building_name || m.floor">
                  <span v-if="m.building_name">🏢 {{ m.building_name }}</span>
                  <span v-if="m.floor" style="margin-left:6px">📍 {{ m.floor }}</span>
                </div>
              </div>
              <div class="map-item-actions">
                <el-button size="mini" type="text" icon="el-icon-edit" @click.stop="openEditDialog(m)" v-hasPermi="['smart:videoMap:edit']" />
                <el-button size="mini" type="text" icon="el-icon-delete" style="color:#f56c6c" @click.stop="deleteMap(m)" v-hasPermi="['smart:videoMap:remove']" />
              </div>
            </div>
            <div v-if="mapList.length===0 && !mapLoading" style="text-align:center;color:#999;padding:30px">
              <i class="el-icon-picture-outline" style="font-size:36px;display:block;margin-bottom:8px" />
              暂无底图，点击右上角"新增"添加
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- ============ 右侧：地图查看器 ============ -->
      <el-col :span="18">
        <div v-if="!currentMap" class="no-map-selected">
          <i class="el-icon-map-location" style="font-size:80px;color:#ddd;display:block" />
          <p style="font-size:16px;color:#bbb;margin-top:16px">请在左侧选择一张底图</p>
          <p style="font-size:12px;color:#ddd">或新增一张园区/建筑平面底图</p>
        </div>

        <div v-else>
          <!-- 地图信息栏 -->
          <el-card shadow="never" class="map-info-bar">
            <el-row :gutter="10" align="middle">
              <el-col :span="12">
                <span style="font-size:16px;font-weight:600">{{ currentMap.map_name }}</span>
                <el-tag :type="currentMap.map_type==='INDOOR'?'warning':'success'" size="small" effect="plain" style="margin-left:8px">
                  {{ currentMap.map_type==='INDOOR'?'室内':'室外' }}
                </el-tag>
                <span v-if="currentMap.building_name" style="margin-left:8px;color:#666;font-size:13px">🏢 {{ currentMap.building_name }}</span>
                <span v-if="currentMap.floor" style="margin-left:4px;color:#666;font-size:13px">📍 {{ currentMap.floor }}</span>
              </el-col>
              <el-col :span="12" style="text-align:right">
                <el-button size="small" icon="el-icon-upload2" @click="$refs.mapUpload.click()" v-hasPermi="['smart:videoMap:edit']">更换底图</el-button>
                <input ref="mapUpload" type="file" accept="image/*" style="display:none" @change="onReplaceImage" />
                <el-button size="small" icon="el-icon-refresh" @click="selectMap(currentMap)">刷新</el-button>
                <el-button size="small" type="success" icon="el-icon-check" @click="savePositions">保存点位</el-button>
                <span style="color:#999;font-size:11px;margin-left:8px">摄像头: {{ markers.length }}</span>
              </el-col>
            </el-row>
          </el-card>

          <!-- 地图画布 -->
          <div class="map-canvas" ref="mapContainer" @click="onMapClick($event)">
            <img v-if="currentMap.map_image" :src="currentMap.map_image" class="map-image" ref="mapImg" @load="onImgLoad" />
            <div v-else class="map-placeholder">
              <i class="el-icon-picture" style="font-size:48px;color:#ddd" />
              <p>请先为"{{ currentMap.map_name }}"上传底图</p>
            </div>

            <!-- 摄像头点位 -->
            <div v-for="(cam, idx) in markers" :key="cam.device_id"
              class="camera-marker"
              :style="{left:cam.mapX+'%', top:cam.mapY+'%'}"
              @mousedown.stop="startDrag(idx,$event)"
              @click.stop="selectMarker(idx)">
              <div class="marker-dot" :class="cam.status==='ONLINE'?'online':'offline'"></div>
              <div class="marker-label">{{ cam.device_name }}</div>
              <div class="marker-info" v-if="selectedIdx===idx">
                <strong>{{ cam.device_name }}</strong><br/>
                IP: {{ cam.ip_address }}<br/>
                <a :href="'rtsp://'+cam.ip_address+':'+(cam.rtsp_port||554)+'/Streaming/Channels/'+(cam.channel||1)+'01'" style="color:#1890ff">RTSP地址</a>
                <br/><el-button size="mini" type="danger" @click.stop="removeMarker(idx)" style="margin-top:4px">删除点位</el-button>
              </div>
            </div>
          </div>

          <!-- 未放置摄像头 -->
          <el-card shadow="never" style="margin-top:8px" v-if="unplacedCams.length>0">
            <span style="font-size:12px;color:#999;margin-right:8px">未放置摄像头（点击添加到地图）:</span>
            <el-tag v-for="cam in unplacedCams" :key="cam.device_id" style="margin:2px;cursor:pointer"
              @click="placeOnMap(cam)" type="info" size="small">+ {{ cam.device_name }}</el-tag>
          </el-card>
        </div>
      </el-col>
    </el-row>

    <!-- ============ 新增/编辑底图弹窗 ============ -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="520px" :close-on-click-modal="false">
      <el-form :model="form" :rules="rules" ref="mapForm" label-width="80px" size="small">
        <el-form-item label="地图名称" prop="map_name">
          <el-input v-model="form.map_name" placeholder="例如：园区总平面图" />
        </el-form-item>
        <el-form-item label="地图类型" prop="map_type">
          <el-radio-group v-model="form.map_type">
            <el-radio label="OUTDOOR">室外 (园区/室外区域)</el-radio>
            <el-radio label="INDOOR">室内 (建筑/楼层内)</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="所属建筑" v-if="form.map_type==='INDOOR'">
          <el-input v-model="form.building_name" placeholder="例如：研发中心A栋" />
        </el-form-item>
        <el-form-item label="楼层" v-if="form.map_type==='INDOOR'">
          <el-select v-model="form.floor" placeholder="选择楼层" style="width:100%">
            <el-option label="B2 地下二层" value="B2" /><el-option label="B1 地下一层" value="B1" />
            <el-option label="1F 一层" value="1F" /><el-option label="2F 二层" value="2F" />
            <el-option label="3F 三层" value="3F" /><el-option label="4F 四层" value="4F" />
            <el-option label="5F 五层" value="5F" /><el-option label="6F 六层" value="6F" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.order_num" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio label="0">启用</el-radio><el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="底图图片">
          <el-upload :action="uploadUrl" :headers="uploadHeaders" :on-success="onUploadSuccess"
            :before-upload="beforeUpload" :show-file-list="false" accept="image/*" style="display:inline-block">
            <el-button size="small" icon="el-icon-upload2">{{ form.map_image ? '更换图片' : '上传底图' }}</el-button>
          </el-upload>
          <el-image v-if="form.map_image" :src="form.map_image" style="width:100%;max-height:180px;margin-top:8px;border-radius:4px" fit="contain" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="submitMap" :loading="submitting">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'
import { getToken } from '@/utils/auth'

export default {
  name: 'VideoMap',
  data() {
    const base = process.env.VUE_APP_BASE_API || '/dev-api'
    return {
      uploadUrl: base + '/common/upload',
      uploadHeaders: { Authorization: 'Bearer ' + getToken() },
      // 地图列表
      mapList: [], mapLoading: false,
      filterType: '', filterName: '',
      currentMap: null,
      // 摄像头
      allCams: [], markers: [],
      selectedIdx: -1, dragIdx: -1,
      lastClickX: 50, lastClickY: 50,
      containerW: 800, containerH: 600,
      // 弹窗
      dialogVisible: false, dialogTitle: '', submitting: false,
      form: { map_id: null, map_name: '', map_type: 'OUTDOOR', building_name: '', floor: '',
              order_num: 0, status: '0', map_image: '', remark: '' },
      rules: {
        map_name: [{ required: true, message: '请输入地图名称', trigger: 'blur' }],
        map_type: [{ required: true, message: '请选择地图类型', trigger: 'change' }]
      }
    }
  },
  computed: {
    placedIds() { return this.markers.map(m => m.device_id) },
    unplacedCams() { return this.allCams.filter(c => !this.placedIds.includes(c.device_id)) }
  },
  created() { this.loadMaps(); this.loadCameras() },
  methods: {
    // ===== 地图列表 =====
    loadMaps() {
      this.mapLoading = true
      const params = { pageSize: 100 }
      request({ url: '/smart/access/map/list', method: 'get', params }).then(res => {
        this.mapList = (res.rows || []).filter(m => {
          if (this.filterType && m.map_type !== this.filterType) return false
          if (this.filterName && !m.map_name.includes(this.filterName)) return false
          return true
        })
        this.mapLoading = false
      }).catch(() => { this.mapLoading = false })
    },
    selectMap(m) {
      this.currentMap = m
      this.markers = []
      this.selectedIdx = -1
      this.loadCamerasForMap()
    },
    openAddDialog() {
      this.form = { map_id: null, map_name: '', map_type: 'OUTDOOR', building_name: '', floor: '',
                    order_num: 0, status: '0', map_image: '', remark: '' }
      this.dialogTitle = '新增底图'
      this.dialogVisible = true
    },
    openEditDialog(m) {
      this.form = { ...m }
      this.dialogTitle = '编辑底图'
      this.dialogVisible = true
    },
    submitMap() {
      this.$refs.mapForm.validate(valid => {
        if (!valid) return
        this.submitting = true
        const method = this.form.map_id ? 'put' : 'post'
        request({ url: '/smart/access/map', method, data: this.form }).then(res => {
          if (res.code === 200) {
            this.$message.success(this.form.map_id ? '修改成功' : '新增成功')
            this.dialogVisible = false
            this.loadMaps()
          } else { this.$message.error(res.msg || '操作失败') }
          this.submitting = false
        }).catch(() => { this.submitting = false })
      })
    },
    deleteMap(m) {
      this.$confirm('确定删除底图「' + m.map_name + '」？', '提示', { type: 'warning' }).then(() => {
        request({ url: '/smart/access/map/' + m.map_id, method: 'delete' }).then(() => {
          this.$message.success('已删除')
          if (this.currentMap && this.currentMap.map_id === m.map_id) this.currentMap = null
          this.loadMaps()
        })
      }).catch(() => {})
    },
    // ===== 底图上传 =====
    beforeUpload(file) {
      const isImg = file.type.startsWith('image/')
      const isLt5M = file.size / 1024 / 1024 < 5
      if (!isImg) { this.$message.error('只能上传图片文件'); return false }
      if (!isLt5M) { this.$message.error('图片大小不能超过5MB'); return false }
      return true
    },
    onUploadSuccess(res) {
      if (res.code === 200) {
        const url = res.data || res.url || res.fileName
        this.form.map_image = url.startsWith('http') ? url : (process.env.VUE_APP_BASE_API + url)
      } else { this.$message.error(res.msg || '上传失败') }
    },
    onReplaceImage(e) {
      const file = e.target.files[0]
      if (!file) return
      const fd = new FormData(); fd.append('file', file)
      request({ url: '/common/upload', method: 'post', data: fd, headers: { 'Content-Type': 'multipart/form-data' } }).then(res => {
        if (res.code === 200) {
          const url = res.data || res.url || res.fileName
          const imgUrl = url.startsWith('http') ? url : (process.env.VUE_APP_BASE_API + url)
          request({ url: '/smart/access/map', method: 'put', data: { map_id: this.currentMap.map_id, map_image: imgUrl } }).then(() => {
            this.currentMap.map_image = imgUrl
            this.$message.success('底图已更新')
          })
        }
      })
      e.target.value = ''
    },
    // ===== 摄像头操作 =====
    loadCameras() {
      request({ url: '/smart/access/video-device/list', params: { pageSize: 500 } }).then(res => {
        this.allCams = (res.rows || []).map(c => ({
          device_id: c.device_id, device_name: c.device_name,
          ip_address: c.ip_address, rtsp_port: c.rtsp_port, channel: c.channel || 1,
          mapX: parseFloat(c.map_x) || 0, mapY: parseFloat(c.map_y) || 0, status: c.status || 'OFFLINE'
        }))
      })
    },
    loadCamerasForMap() {
      request({ url: '/smart/access/video-device/list', params: { pageSize: 500 } }).then(res => {
        this.allCams = (res.rows || []).map(c => ({
          device_id: c.device_id, device_name: c.device_name,
          ip_address: c.ip_address, rtsp_port: c.rtsp_port, channel: c.channel || 1,
          mapX: parseFloat(c.map_x) || 0, mapY: parseFloat(c.map_y) || 0, status: c.status || 'OFFLINE'
        }))
        this.markers = this.allCams.filter(c => c.mapX > 0 && c.mapY > 0)
      })
    },
    onImgLoad() {
      const img = this.$refs.mapImg
      if (img) { this.containerW = img.clientWidth; this.containerH = img.clientHeight }
    },
    onMapClick(e) {
      const rect = this.$refs.mapContainer.getBoundingClientRect()
      this.lastClickX = parseFloat(((e.clientX - rect.left) / rect.width * 100).toFixed(1))
      this.lastClickY = parseFloat(((e.clientY - rect.top) / rect.height * 100).toFixed(1))
      this.$message({ message: '已记录位置('+this.lastClickX+'%,'+this.lastClickY+'%)，请点击下方标签放置摄像头', type: 'success', duration: 2000 })
    },
    selectMarker(idx) { this.selectedIdx = (this.selectedIdx === idx) ? -1 : idx },
    removeMarker(idx) {
      const cam = this.markers[idx]
      request({ url: '/smart/access/video-device/position', method: 'put', data: { deviceId: cam.device_id, mapX: null, mapY: null } }).then(() => {
        this.markers.splice(idx, 1)
        this.selectedIdx = -1
        this.$message.success('点位已删除')
      })
    },
    startDrag(idx, e) {
      this.dragIdx = idx
      const move = (ev) => {
        const rect = this.$refs.mapContainer.getBoundingClientRect()
        this.markers[idx].mapX = parseFloat(((ev.clientX - rect.left) / rect.width * 100).toFixed(1))
        this.markers[idx].mapY = parseFloat(((ev.clientY - rect.top) / rect.height * 100).toFixed(1))
      }
      const up = () => { document.removeEventListener('mousemove', move); document.removeEventListener('mouseup', up) }
      document.addEventListener('mousemove', move)
      document.addEventListener('mouseup', up)
    },
    savePositions() {
      if (this.markers.length === 0) return
      let done = 0
      this.markers.forEach(m => {
        request({ url: '/smart/access/video-device/position', method: 'put',
          data: { deviceId: m.device_id, mapX: String(m.mapX), mapY: String(m.mapY) } }).then(() => {
          done++; if (done === this.markers.length) this.$message.success('所有点位已保存')
        })
      })
    },
    placeOnMap(cam) {
      cam.mapX = this.lastClickX || 50; cam.mapY = this.lastClickY || 50
      this.markers.push({ ...cam })
    }
  }
}
</script>

<style scoped>
.map-page { height: calc(100vh - 84px); }
.map-list-card { height: calc(100vh - 100px); }
.map-item-list { max-height: calc(100vh - 220px); overflow-y: auto; }
.map-item {
  display: flex; align-items: center; padding: 8px; margin-bottom: 6px;
  border: 1px solid #ebeef5; border-radius: 6px; cursor: pointer; transition: all .2s;
}
.map-item:hover { border-color: #409eff; background: #ecf5ff; }
.map-item.active { border-color: #409eff; background: #d9ecff; box-shadow: 0 2px 6px rgba(64,158,255,.2); }
.map-item-thumb { width: 50px; height: 40px; border-radius: 4px; overflow: hidden; background: #f5f7fa; text-align: center; flex-shrink: 0; }
.map-item-thumb img { width: 100%; height: 100%; object-fit: cover; }
.map-item-info { flex: 1; margin-left: 8px; min-width: 0; }
.map-item-name { font-size: 13px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.map-item-sub { font-size: 11px; color: #999; margin-top: 2px; }
.map-item-actions { flex-shrink: 0; margin-left: 4px; }
.no-map-selected { text-align: center; padding: 120px 0; }
.map-info-bar { margin-bottom: 8px; }
.map-canvas {
  position: relative; border: 1px solid #e0e0e0; border-radius: 4px;
  background: #fafafa; min-height: 400px; overflow: hidden; cursor: crosshair;
}
.map-image { width: 100%; display: block; }
.map-placeholder { display:flex; flex-direction:column; align-items:center; justify-content:center; height:400px; color:#999; }
.camera-marker { position: absolute; transform: translate(-50%,-50%); cursor: pointer; z-index: 10; }
.marker-dot { width: 20px; height: 20px; border-radius: 50%; border: 2px solid #fff; box-shadow: 0 2px 6px rgba(0,0,0,.3); margin: 0 auto; }
.marker-dot.online { background: #67c23a; }
.marker-dot.offline { background: #f5222d; }
.marker-label { font-size: 11px; color: #fff; text-shadow: 0 1px 3px #000; text-align: center; margin-top: 2px; white-space: nowrap; font-weight: 500; }
.marker-info { position: absolute; top: 30px; left: 0; background: #fff; border: 1px solid #ddd; border-radius: 6px; padding: 10px 14px; box-shadow: 0 4px 12px rgba(0,0,0,.15); white-space: nowrap; z-index: 100; font-size: 13px; }
</style>
