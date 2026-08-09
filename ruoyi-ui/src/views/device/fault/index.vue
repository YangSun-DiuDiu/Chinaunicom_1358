<template>
  <div class="fault-container">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="fault-stats">
      <el-col :xs="24" :sm="8">
        <div class="stat-card stat-danger" @click="loadFaultList">
          <div class="stat-num">{{ stats.currentOffline }}</div>
          <div class="stat-label">当前离线设备</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="8">
        <div class="stat-card stat-warning">
          <div class="stat-num">{{ stats.totalFaults }}</div>
          <div class="stat-label">累计离线告警</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="8">
        <div class="stat-card stat-primary">
          <div class="stat-num">{{ offlineDevices.length }}</div>
          <div class="stat-label">故障设备列表</div>
        </div>
      </el-col>
    </el-row>

    <!-- 离线设备列表 -->
    <el-card shadow="never" class="fault-card">
      <div slot="header" class="card-header">
        <span>离线设备列表</span>
        <el-button type="text" icon="el-icon-refresh" @click="loadFaultList" :loading="loading">刷新</el-button>
      </div>
      <el-table :data="offlineDevices" stripe v-loading="loading" size="small">
        <el-table-column label="设备名称" prop="deviceName" width="300" align="center" :show-overflow-tooltip="true" />
        <el-table-column label="设备类型" width="300" align="center">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.deviceType === 'NETWORK'" type="primary" size="small">网络设备</el-tag>
            <el-tag v-else-if="scope.row.deviceType === 'MONITOR'" type="warning" size="small">监控设备</el-tag>
            <el-tag v-else-if="scope.row.deviceType === 'NVR'" type="success" size="small">硬盘录像机</el-tag>
            <el-tag v-else size="small" type="info">其它</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="IP地址" prop="ipAddress" width="300" align="center" />
        <el-table-column label="位置" prop="location" min-width="300" align="center" :show-overflow-tooltip="true" />
        <el-table-column label="负责人" prop="responsible" width="300" align="center" />
        <el-table-column label="联系电话" prop="responsiblePhone" width="300" align="center" />
        <el-table-column label="操作" width="300" align="center" fixed="right">
          <template slot-scope="scope">
            <el-button type="danger" size="mini" icon="el-icon-message" @click="handleRepair(scope.row)">报修</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && offlineDevices.length === 0" class="empty-state">
        <i class="el-icon-circle-check"></i>
        <p>所有设备运行正常，无离线设备</p>
      </div>
    </el-card>

    <!-- 故障日志 -->
    <el-card shadow="never" class="fault-card" style="margin-top: 16px">
      <div slot="header" class="card-header">
        <span>离线告警记录</span>
      </div>
      <el-table :data="faultList" stripe v-loading="loadingLog" size="small">
        <el-table-column label="设备名称" prop="deviceName" width="330" align="center" :show-overflow-tooltip="true" />
        <el-table-column label="变更前" width="330" align="center">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.oldStatus === 'ONLINE'" type="success" size="small">在线</el-tag>
            <el-tag v-else-if="scope.row.oldStatus === 'OFFLINE'" type="danger" size="small">离线</el-tag>
            <el-tag v-else type="info" size="small">{{ scope.row.oldStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="变更后" width="330" align="center">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.newStatus === 'ONLINE'" type="success" size="small">在线</el-tag>
            <el-tag v-else-if="scope.row.newStatus === 'OFFLINE'" type="danger" size="small">离线</el-tag>
            <el-tag v-else type="info" size="small">{{ scope.row.newStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="变更时间" prop="changeTime" width="330" align="center" />
        <el-table-column label="短信通知" width="330" align="center">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.smsSent === 'Y'" type="success" size="small">已发送</el-tag>
            <el-tag v-else type="info" size="small">未发送</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="接收人" prop="smsRecipient" width="330" align="center" :show-overflow-tooltip="true" />
      </el-table>
      <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="loadFaultLogs" />
    </el-card>
  </div>
</template>

<script>
import { listDevice, listStatusLog } from '@/api/device/device'
import request from '@/utils/request'

export default {
  name: 'DeviceFault',
  data() {
    return {
      loading: false,
      loadingLog: false,
      stats: { currentOffline: 0, totalFaults: 0 },
      offlineDevices: [],
      faultList: [],
      total: 0,
      queryParams: { pageNum: 1, pageSize: 10 }
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    loadData() {
      this.loadStats()
      this.loadFaultList()
      this.loadFaultLogs()
    },
    loadStats() {
      request({ url: '/device/fault/stats', method: 'get' }).then(res => {
        this.stats = res
        this.offlineDevices = res.faultList || []
      })
    },
    loadFaultList() {
      this.loading = true
      listDevice({ status: 'OFFLINE' }).then(res => {
        this.offlineDevices = res.rows || []
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    loadFaultLogs() {
      this.loadingLog = true
      listStatusLog({ changeType: 'OFFLINE', pageNum: this.queryParams.pageNum, pageSize: this.queryParams.pageSize }).then(res => {
        this.faultList = res.rows || []
        this.total = res.total || 0
        this.loadingLog = false
      }).catch(() => { this.loadingLog = false })
    },
    handleRepair(row) {
      this.$confirm('确认向 ' + row.responsiblePhone + ' 发送报修短信？', '报修确认', { type: 'warning' }).then(() => {
        request({ url: '/device/fault/repair/' + row.deviceId, method: 'post' }).then(() => {
          this.$message.success('报修短信已发送')
        })
      })
    }
  }
}
</script>

<style scoped>
.fault-container {
  padding: 0;
}

/* 统计卡片 */
.fault-stats {
  margin-bottom: 16px;
}

.stat-card {
  padding: 18px 20px;
  border-radius: 4px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 3px 12px rgba(0, 0, 0, 0.1);
}

.stat-num {
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 2px;
}

.stat-label {
  font-size: 13px;
  color: #909399;
}

.stat-danger .stat-num { color: #f56c6c; }
.stat-warning .stat-num { color: #e6a23c; }
.stat-primary .stat-num { color: #409eff; }

/* 卡片 */
.fault-card {
  border-radius: 4px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  font-weight: 600;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 48px 0;
  color: #909399;
}

.empty-state i {
  font-size: 48px;
  color: #67c23a;
  display: block;
  margin-bottom: 12px;
}

.empty-state p {
  font-size: 14px;
  margin: 0;
}
</style>
