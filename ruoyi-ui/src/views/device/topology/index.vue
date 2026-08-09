<template>
  <div class="app-container topology-container">
    <!-- 顶部工具栏 -->
    <div class="topology-toolbar">
      <div class="toolbar-left">
        <h4 class="topology-title">网络设备拓扑图</h4>
      </div>
      <div class="toolbar-right">
        <el-button-group>
          <el-button size="mini" icon="el-icon-zoom-in" @click="handleZoomIn" :disabled="zoomLevel >= maxZoom">放大</el-button>
          <el-button size="mini" icon="el-icon-zoom-out" @click="handleZoomOut" :disabled="zoomLevel <= minZoom">缩小</el-button>
          <el-button size="mini" icon="el-icon-refresh" @click="handleZoomReset">重置</el-button>
        </el-button-group>
        <span class="zoom-label">{{ Math.round(zoomLevel * 100) }}%</span>
        <el-button size="mini" icon="el-icon-refresh-right" @click="fetchTopology">刷新</el-button>
      </div>
    </div>

    <!-- 拓扑画布区域 -->
    <div
      v-loading="loading"
      class="topology-canvas-wrapper"
      element-loading-text="正在加载拓扑数据..."
      element-loading-spinner="el-icon-loading"
    >

      <!-- 空状态提示 -->
      <div v-if="!loading && topologyNodes.length === 0" class="topology-empty">
        <i class="el-icon-connection empty-icon"></i>
        <p class="empty-title">暂无拓扑数据</p>
        <p class="empty-hint">请先在设备管理中配置设备的端口绑定关系，系统将自动生成网络拓扑结构。</p>
        <el-button type="primary" size="small" @click="goToDeviceList">前往设备管理</el-button>
      </div>

      <!-- 拓扑树形视图 -->
      <div
        v-else-if="!loading && topologyNodes.length > 0"
        ref="topologyContent"
        class="topology-content"
        :style="{ transform: 'scale(' + zoomLevel + ')', transformOrigin: 'top center' }"
      >
        <div class="tree-wrapper">
          <topology-node
            v-for="node in topologyNodes"
            :key="node.deviceId || node.id"
            :node="node"
            @node-click="handleNodeClick"
          />
        </div>
      </div>
    </div>

    <!-- 设备详情对话框 -->
    <el-dialog
      :title="detailNode.deviceName ? '设备详情 - ' + detailNode.deviceName : '设备详情'"
      :visible.sync="detailOpen"
      width="550px"
      append-to-body
      :close-on-click-modal="false"
    >
      <div v-if="detailNode" class="device-detail">
        <div class="detail-header">
          <img v-if="detailIcon" :src="detailIcon" class="detail-icon" :alt="detailNode.deviceName" />
          <span class="detail-status-dot" :class="statusClass(detailNode.status)"></span>
          <span class="detail-name">{{ detailNode.deviceName }}</span>
          <el-tag :type="statusTagType(detailNode.status)" size="small" style="margin-left: 10px;">
            {{ detailNode.status }}
          </el-tag>
        </div>
        <el-divider />
        <el-descriptions :column="2" border size="medium">
          <el-descriptions-item label="IP 地址">
            <template v-if="detailNode.ipAddress">{{ detailNode.ipAddress }}</template>
            <template v-else>-</template>
          </el-descriptions-item>
          <el-descriptions-item label="设备类型">
            <template v-if="detailNode.deviceType">
              {{ deviceTypeMap[detailNode.deviceType] || detailNode.deviceType }}
            </template>
            <template v-else>-</template>
          </el-descriptions-item>
          <el-descriptions-item label="设备型号">
            <template v-if="detailNode.model">{{ detailNode.model }}</template>
            <template v-else>-</template>
          </el-descriptions-item>
          <el-descriptions-item label="所在位置">
            <template v-if="detailNode.location">{{ detailNode.location }}</template>
            <template v-else>-</template>
          </el-descriptions-item>
          <el-descriptions-item label="负责人">
            <template v-if="detailNode.responsible">{{ detailNode.responsible }}</template>
            <template v-else>-</template>
          </el-descriptions-item>
          <el-descriptions-item label="子节点数">
            {{ detailNode.children ? detailNode.children.length : 0 }}
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">
            <template v-if="detailNode.remark">{{ detailNode.remark }}</template>
            <template v-else>-</template>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="detailOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getDeviceTopology, getDevice } from "@/api/device/device"
import TopologyNode from "./TopologyNode"

export default {
  name: "DeviceTopology",
  components: { TopologyNode },
  data() {
    return {
      loading: false,
      topologyNodes: [],
      zoomLevel: 1,
      minZoom: 0.25,
      maxZoom: 2,
      zoomStep: 0.1,
      detailOpen: false,
      detailNode: {},
      deviceTypeMap: {
        NETWORK: "网络设备",
        MONITOR: "监控设备",
        NVR: "硬盘录像机",
        OTHER: "其他设备"
      }
    }
  },
  computed: {
    /** 详情弹窗设备图标 */
    detailIcon() {
      if (!this.detailNode) return ""
      const key = this.resolveIconKey(this.detailNode.icon, this.detailNode.deviceType)
      return require("@/assets/topo-icons/" + key + ".svg")
    }
  },
  created() {
    this.fetchTopology()
  },
  methods: {
    /** 根据 icon 与设备类型解析最终图标 key */
    resolveIconKey(icon, deviceType) {
      const validIcons = ["router", "switch", "firewall", "ap", "server", "camera", "nvr", "other"]
      if (icon && validIcons.indexOf(icon) !== -1) return icon
      const t = (deviceType || "").toUpperCase()
      if (t === "NETWORK") return "switch"
      if (t === "MONITOR") return "camera"
      if (t === "NVR") return "nvr"
      return "other"
    },

    /** 获取拓扑数据 */
    fetchTopology() {
      this.loading = true
      getDeviceTopology().then(response => {
        const data = response.data || response
        if (Array.isArray(data)) {
          this.topologyNodes = data
        } else if (data && typeof data === "object") {
          // 后端可能返回单根对象，包装成数组
          this.topologyNodes = [data]
        } else {
          this.topologyNodes = []
        }
        this.loading = false
      }).catch(() => {
        this.topologyNodes = []
        this.loading = false
      })
    },

    /** 放大 */
    handleZoomIn() {
      const next = this.zoomLevel + this.zoomStep
      this.zoomLevel = Math.min(next, this.maxZoom)
    },

    /** 缩小 */
    handleZoomOut() {
      const next = this.zoomLevel - this.zoomStep
      this.zoomLevel = Math.max(next, this.minZoom)
    },

    /** 重置缩放 */
    handleZoomReset() {
      this.zoomLevel = 1
    },

    /** 点击节点查看详情 */
    handleNodeClick(node) {
      const deviceId = node.deviceId || node.id
      if (!deviceId) {
        this.detailNode = node
        this.detailOpen = true
        return
      }
      getDevice(deviceId).then(response => {
        this.detailNode = response.data || response
        this.detailOpen = true
      }).catch(() => {
        // 获取详情失败，展示节点本地数据
        this.detailNode = node
        this.detailOpen = true
      })
    },

    /** 状态对应的CSS类 */
    statusClass(status) {
      const s = (status || "").toUpperCase()
      if (s === "ONLINE") return "dot-online"
      if (s === "OFFLINE") return "dot-offline"
      return "dot-unknown"
    },

    /** 状态对应的 el-tag type */
    statusTagType(status) {
      const s = (status || "").toUpperCase()
      if (s === "ONLINE") return "success"
      if (s === "OFFLINE") return "danger"
      return "info"
    },

    /** 跳转到设备列表 */
    goToDeviceList() {
      this.$router.push("/device/list")
    }
  }
}
</script>

<style scoped>
/* ===== 容器与工具栏 ===== */
.topology-container {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 86px);
  padding: 0;
}

.topology-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 28px;
  background: transparent;
  border-bottom: none;
  flex-shrink: 0;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 工具栏按钮收拢，悬停才显色 */
.toolbar-right .el-button-group .el-button {
  background: transparent;
  border-color: transparent;
  color: #697888;
}
.toolbar-right .el-button-group .el-button:hover {
  background: #fff;
  border-color: #DCE0E8;
  color: #152433;
}
.toolbar-right > .el-button {
  background: transparent;
  border-color: #DCE0E8;
  color: #697888;
}
.toolbar-right > .el-button:hover {
  background: #fff;
  border-color: #3B82F6;
  color: #3B82F6;
}

.zoom-label {
  font-size: 12px;
  color: #697888;
  min-width: 42px;
  text-align: center;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.02em;
}

.topology-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #152433;
  letter-spacing: 0.01em;
}

/* ===== 画布 ===== */
.topology-canvas-wrapper {
  flex: 1;
  overflow: auto;
  background-color: #EEF1F6;
  background-image: radial-gradient(circle, #D2D7E0 1px, transparent 1px);
  background-size: 20px 20px;
  position: relative;
  padding: 40px 48px;
}

.topology-content {
  display: inline-block;
  min-width: 100%;
  transition: transform 0.25s ease;
}

/* ===== 空状态 ===== */
.topology-empty {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  color: #697888;
  max-width: 420px;
}

.empty-icon {
  font-size: 72px;
  color: #D2D7E0;
}

.empty-title {
  font-size: 18px;
  color: #152433;
  margin: 14px 0 8px;
  font-weight: 600;
}

.empty-hint {
  font-size: 13px;
  line-height: 1.6;
  margin-bottom: 18px;
  color: #697888;
}

/* ===== 树形布局 ===== */
.tree-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0;
  padding: 6px 0;
}

/* ===== 设备详情弹窗 ===== */
.device-detail {
  padding: 0 4px;
}

.detail-header {
  display: flex;
  align-items: center;
  padding: 6px 0;
}

.detail-icon {
  width: 34px;
  height: 34px;
  margin-right: 8px;
  flex-shrink: 0;
}

.detail-status-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin-right: 10px;
  flex-shrink: 0;
}

.detail-status-dot.dot-online {
  background: #10B981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.18);
}

.detail-status-dot.dot-offline {
  background: #EF4444;
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.15);
}

.detail-status-dot.dot-unknown {
  background: #9CA3AF;
}

.detail-name {
  font-size: 17px;
  font-weight: 600;
  color: #152433;
}

/* ===== 滚动条美化 ===== */
.topology-canvas-wrapper::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.topology-canvas-wrapper::-webkit-scrollbar-thumb {
  background: #CDD4DE;
  border-radius: 3px;
}

.topology-canvas-wrapper::-webkit-scrollbar-thumb:hover {
  background: #A8B2BF;
}

.topology-canvas-wrapper::-webkit-scrollbar-track {
  background: transparent;
}
</style>
