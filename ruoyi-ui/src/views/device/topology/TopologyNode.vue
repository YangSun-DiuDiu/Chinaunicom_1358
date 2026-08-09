<template>
  <div class="tree-node-group" :class="cableClass">
    <div class="tree-node-row">
      <div class="tree-node-card" @click="onClick">
        <div class="node-status-dot" :class="statusDotClass"></div>
        <img :src="nodeIcon" class="node-icon" :alt="node.deviceName" />
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
</template>

<script>
/**
 * 拓扑树递归节点组件
 * - 支持递归渲染子节点（通过 name 自引用）
 * - 节点图标优先取设备配置的 icon，否则按设备类型映射默认图标
 * - 线缆连线按 cableType 着色
 * - 独立 SFC 由 vue-loader 预编译为 render 函数，避免 runtime-only Vue 无法编译字符串模板
 */
export default {
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
    },
    /** 节点图标 key：优先用设备配置的 icon，否则按设备类型映射默认图标 */
    nodeIcon() {
      const key = this.resolveIconKey(this.node.icon, this.node.deviceType)
      return require("@/assets/topo-icons/" + key + ".svg")
    },
    /** 线缆样式 class（表示该节点连向其父节点的线缆类型） */
    cableClass() {
      return "cable-" + (this.node.cableType || "default")
    }
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
    toggleExpand() {
      this.expanded = !this.expanded
    },
    onClick() {
      this.$emit("node-click", this.node)
    }
  }
}
</script>

<style scoped>
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
  background: #FFFFFF;
  border: 1px solid #DCE0E8;
  border-radius: 12px;
  padding: 12px 18px;
  min-width: 200px;
  max-width: 300px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 4px 12px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: box-shadow 0.2s ease, border-color 0.2s ease, transform 0.2s ease;
  position: relative;
  z-index: 1;
}

.tree-node-card:hover {
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06), 0 8px 24px rgba(59, 130, 246, 0.12);
  border-color: #3B82F6;
  transform: translateY(-3px);
}

/* 状态圆点 */
.node-status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.node-status-dot.dot-online {
  background: #10B981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.18), 0 0 6px rgba(16, 185, 129, 0.25);
}

.node-status-dot.dot-offline {
  background: #EF4444;
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.12);
}

.node-status-dot.dot-unknown {
  background: #9CA3AF;
  box-shadow: 0 0 0 3px rgba(156, 163, 175, 0.12);
}

/* 节点设备图标 */
.node-icon {
  width: 30px;
  height: 30px;
  flex-shrink: 0;
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
  color: #152433;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.node-ip {
  font-size: 12px;
  color: #697888;
  font-family: "SF Mono", "Consolas", "Menlo", monospace;
  letter-spacing: 0.01em;
}

/* 折叠/展开切换 */
.node-expand-toggle {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #EEF2FF;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #3B82F6;
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.2s ease, color 0.2s ease;
}

.node-expand-toggle:hover {
  background: #3B82F6;
  color: #fff;
}

/* ---- 连接线（CSS绘制 · 架构图风格） ---- */
/* 父节点卡片下方的垂直连接线：父卡片底部 → 水平线 */
.tree-line-connector {
  width: 2px;
  height: 28px;
  background: #98A6B6;
  position: relative;
  flex-shrink: 0;
}

.tree-line-connector::after {
  content: "";
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #98A6B6;
}

/* ---- 子节点容器（层级间距与节点间距） ---- */
.tree-children {
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  gap: 56px;
  padding-top: 12px;
  position: relative;
}

/* 子节点层顶部的水平连接线 */
.tree-children::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: #98A6B6;
}

/* 每个子项列上方的垂直连接线：水平线 → 子节点卡片顶部 */
.tree-node-group > .tree-node-row::before {
  content: "";
  position: absolute;
  top: -12px;
  left: 50%;
  transform: translateX(-50%);
  width: 2px;
  height: 12px;
  background: #98A6B6;
}

/* ---- 线缆类型着色（子节点连向父节点的垂直线，按 cable_type 渲染） ---- */
.cable-fiber > .tree-node-row::before {
  background: #D97706;
}

.cable-ethernet > .tree-node-row::before {
  background: #3B82F6;
}

.cable-coaxial > .tree-node-row::before {
  background: #9CA3AF;
}

.cable-wireless > .tree-node-row::before {
  background: transparent;
  border-left: 2px dashed #10B981;
  width: 0;
  left: calc(50% - 1px);
}
</style>
