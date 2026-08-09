<template>
  <div class="doc-center">
    <!-- 顶栏 -->
    <div style="height:48px;line-height:48px;display:flex;align-items:center;padding:0 24px;border-bottom:1px solid var(--atlas-rule);background:#FFF;gap:16px;">
      <a href="/" style="color:var(--atlas-muted);text-decoration:none;font-size:14px;">← 返回系统</a>
      <span style="font-size:16px;font-weight:600;color:var(--atlas-text);">文档中心</span>
    </div>

    <!-- 主内容区 -->
    <div class="doc-center__body">
      <!-- 左侧目录树 -->
      <div class="doc-center__sidebar">
        <DocTree :tree="tree" @select="onSelectFile" />
      </div>

      <!-- 右侧阅读区 -->
      <div class="doc-center__content" ref="contentArea">
        <DocReader :filePath="selectedFilePath" />
      </div>
    </div>

    <!-- 状态栏 -->
    <div class="doc-center__statusbar">
      <span v-if="status.totalModules">📁 {{ status.totalModules }} 个模块</span>
      <span v-if="status.totalFiles">📄 {{ status.totalFiles }} 个文档</span>
      <span v-if="status.filledFiles !== undefined && status.autoGenFiles !== undefined">
        📝 {{ status.autoGenFiles }} 个待填充
      </span>
      <span v-if="status.lastSyncTime">🕐 上次同步: {{ status.lastSyncTime }}</span>
      <span style="margin-left:auto;cursor:pointer;color:var(--atlas-accent);" @click="doSync">
        {{ syncing ? '同步中...' : '立即同步' }}
      </span>
    </div>
  </div>
</template>

<script>
import DocTree from './components/DocTree'
import DocReader from './components/DocReader'
import { getDocTree, getDocStatus, syncDoc } from '@/api/doc/doc'

export default {
  name: 'DocCenter',
  components: { DocTree, DocReader },
  data() {
    return {
      tree: [],
      status: {},
      selectedFilePath: '',
      syncing: false
    }
  },
  created() {
    this.loadTree()
    this.loadStatus()
  },
  methods: {
    async loadTree() {
      try {
        const res = await getDocTree()
        if (res.data) {
          this.tree = Array.isArray(res.data) ? res.data : []
        }
      } catch (e) {
        console.error('加载文档树失败', e)
      }
    },
    async loadStatus() {
      try {
        const res = await getDocStatus()
        if (res.data) {
          this.status = res.data
        }
      } catch (e) {
        console.error('加载同步状态失败', e)
      }
    },
    onSelectFile(file) {
      this.selectedFilePath = file.path
    },
    async doSync() {
      this.syncing = true
      try {
        await syncDoc()
        this.$message.success('同步完成')
        await Promise.all([this.loadTree(), this.loadStatus()])
      } catch (e) {
        this.$message.error('同步失败: ' + (e.message || '未知错误'))
      } finally {
        this.syncing = false
      }
    }
  }
}
</script>

<style lang="scss" src="./styles/atlas.scss"></style>
