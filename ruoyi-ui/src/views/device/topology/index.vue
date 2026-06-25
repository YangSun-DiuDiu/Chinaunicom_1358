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
      :title="'设备详情 - ' + detailNode.deviceName"
      :visible.sync="detailOpen"
      width="550px"
      append-to-body
      :close-on-click-modal="false"
    >
      <div v-if="detailNode" class="device-detail">
        <div class="detail-header">
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

/**
 * 递归拓扑节点组件
 */
const TopologyNode = {
  name: "TopologyNode",
  props: {
    node: { type: Object, required: true },
    depth: { type: Number, default: 0 }
  },
  data() {
    return {
      expanded: true
    }
  },
  computed: {
    hasChildren() {
      return this.node.children && this.node.children.length > 0
    },
    statusDotClass() {
      const s = (this.node.status || "").toUpperCase()
      if (s === "ONLINE") return "dot-online"
      if (s === "OFFLINE") return "dot-offline"
      return "dot-unknown"
    }
  },
  methods: {
    toggleExpand() {
      this.expanded = !this.expanded
    },
    onClick() {
      this.$emit("node-click", this.node)
    }
  },
  template: `
    <div class="tree-node-group">
      <div class="tree-node-row">
        <div class="tree-node-card" @click="onClick">
          <div class="node-status-dot" :class="statusDotClass"></div>
          <div class="node-info">
            <div class="node-name" :title="node.deviceName">{{ node.deviceName }}</div>
            <div class="node-ip" v-if="node.ipAddress">{{ node.ipAddress }}</div>
          </div>
          <div
            v-if="hasChildren"
            class="node-expand-toggle"
            @click.stop="toggleExpand"
            :title="expanded ? '折叠' : '展开'"
          >
            <i :class="expanded ? 'el-icon-minus' : 'el-icon-plus'"></i>
          </div>
        </div>
        <div v-if="hasChildren" class="tree-line-connector"></div>
      </div>
      <div v-if="hasChildren && expanded" class="tree-children">
        <topology-node
          v-for="child in node.children"
          :key="child.deviceId || child.id"
          :node="child"
          :depth="depth + 1"
          @node-click="onClick"
        />
      </div>
    </div>
  `
}

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
      detailNode: null,
      deviceTypeMap: {
        NETWORK: "网络设备",
        MONITOR: "监控设备",
        OTHER: "其他设备"
      }
    }
  },
  created() {
    this.fetchTopology()
  },
  methods: {
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
  padding: 10px 20px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.zoom-label {
  font-size: 13px;
  color: #606266;
  min-width: 46px;
  text-align: center;
  font-variant-numeric: tabular-nums;
}

.topology-title {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

/* ===== 画布 ===== */
.topology-canvas-wrapper {
  flex: 1;
  overflow: auto;
  background: #f2f5fa;
  position: relative;
  padding: 30px 40px;
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
  color: #909399;
  max-width: 420px;
}

.empty-icon {
  font-size: 72px;
  color: #dcdfe6;
}

.empty-title {
  font-size: 18px;
  color: #606266;
  margin: 14px 0 8px;
  font-weight: 500;
}

.empty-hint {
  font-size: 13px;
  line-height: 1.6;
  margin-bottom: 18px;
  color: #909399;
}

/* ===== 树形布局 ===== */
.tree-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0;
  padding: 10px 0;
}

/* ===== 树节点组 ===== */
.tree-node-group {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}

.tree-node-row {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}

/* 节点卡片 */
.tree-node-card {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #fff;
  border: 1.5px solid #d8dce5;
  border-radius: 10px;
  padding: 12px 18px;
  min-width: 200px;
  max-width: 300px;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: box-shadow 0.2s, border-color 0.2s, transform 0.15s;
  position: relative;
  z-index: 1;
}

.tree-node-card:hover {
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.2);
  border-color: #409eff;
  transform: translateY(-2px);
}

/* 状态圆点 */
.node-status-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
  box-shadow: 0 0 0 3px rgba(0, 0, 0, 0.04);
}

.node-status-dot.dot-online {
  background: #67c23a;
  box-shadow: 0 0 0 3px rgba(103, 194, 58, 0.15), 0 0 8px rgba(103, 194, 58, 0.35);
}

.node-status-dot.dot-offline {
  background: #f56c6c;
  box-shadow: 0 0 0 3px rgba(245, 108, 108, 0.15);
}

.node-status-dot.dot-unknown {
  background: #c0c4cc;
  box-shadow: 0 0 0 3px rgba(192, 196, 204, 0.15);
}

/* 节点文字信息 */
.node-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.node-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.node-ip {
  font-size: 12px;
  color: #909399;
  font-family: "Consolas", "Menlo", monospace;
}

/* 折叠/展开切换 */
.node-expand-toggle {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #ecf5ff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #409eff;
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.2s;
}

.node-expand-toggle:hover {
  background: #409eff;
  color: #fff;
}

/* ---- 连接线（CSS绘制） ---- */
.tree-line-connector {
  width: 2px;
  height: 28px;
  background: #c8ccd4;
  position: relative;
  flex-shrink: 0;
}

.tree-line-connector::after {
  content: "";
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #c8ccd4;
}

/* ---- 子节点容器 ---- */
.tree-children {
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  gap: 20px;
  padding-top: 4px;
  position: relative;
}

/* 子节点之间的水平连线 */
.tree-children::before {
  content: "";
  position: absolute;
  top: -2px;
  left: 20%;
  right: 20%;
  height: 2px;
  background: #c8ccd4;
}

/* 每个子项列上方的垂直连线 */
.tree-children > .tree-node-group > .tree-node-row::before {
  content: "";
  position: absolute;
  top: -7px;
  left: 50%;
  transform: translateX(-50%);
  width: 2px;
  height: 8px;
  background: #c8ccd4;
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

.detail-status-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  margin-right: 10px;
  flex-shrink: 0;
}

.detail-status-dot.dot-online {
  background: #67c23a;
  box-shadow: 0 0 6px rgba(103, 194, 58, 0.45);
}

.detail-status-dot.dot-offline {
  background: #f56c6c;
  box-shadow: 0 0 6px rgba(245, 108, 108, 0.4);
}

.detail-status-dot.dot-unknown {
  background: #c0c4cc;
}

.detail-name {
  font-size: 17px;
  font-weight: 600;
  color: #303133;
}

/* ===== 滚动条美化 ===== */
.topology-canvas-wrapper::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.topology-canvas-wrapper::-webkit-scrollbar-thumb {
  background: #c8ccd4;
  border-radius: 4px;
}

.topology-canvas-wrapper::-webkit-scrollbar-thumb:hover {
  background: #a8abb2;
}

.topology-canvas-wrapper::-webkit-scrollbar-track {
  background: transparent;
}
</style>
