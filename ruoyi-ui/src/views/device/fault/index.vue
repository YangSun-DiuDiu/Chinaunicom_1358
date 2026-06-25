<template>
  <div class="app-container">
    <!-- 统计卡片 -->
    <el-row :gutter="20" style="margin-bottom: 20px">
      <el-col :span="8">
        <el-card shadow="hover">
          <div style="text-align: center">
            <div style="font-size: 36px; color: #F56C6C">{{ stats.currentOffline }}</div>
            <div>当前离线设备</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <div style="text-align: center">
            <div style="font-size: 36px; color: #E6A23C">{{ stats.totalFaults }}</div>
            <div>累计离线告警</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" @click.native="loadFaultList" style="cursor: pointer">
          <div style="text-align: center">
            <div style="font-size: 36px; color: #409EFF">{{ offlineDevices.length }}</div>
            <div>故障设备列表</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 离线设备列表 -->
    <el-card>
      <div slot="header">
        <span>离线设备列表</span>
      </div>
      <el-table :data="offlineDevices" border stripe v-loading="loading">
        <el-table-column label="设备名称" prop="deviceName" width="140" :show-overflow-tooltip="true" />
        <el-table-column label="设备类型" width="100" align="center">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.deviceType === 'NETWORK'" type="info" size="small">网络设备</el-tag>
            <el-tag v-else-if="scope.row.deviceType === 'MONITOR'" type="warning" size="small">监控设备</el-tag>
            <el-tag v-else-if="scope.row.deviceType === 'NVR'" type="success" size="small">硬盘录像机</el-tag>
            <el-tag v-else size="small">其它设备</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="IP地址" prop="ipAddress" width="150" align="center" />
        <el-table-column label="负责人" prop="responsible" width="90" align="center" />
        <el-table-column label="联系电话" prop="responsiblePhone" width="130" align="center" />
        <el-table-column label="操作" width="80" align="center">
          <template slot-scope="scope">
            <el-button type="danger" size="mini" icon="el-icon-message"
              @click="handleRepair(scope.row)">报修</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 故障日志列表 -->
    <el-card style="margin-top: 20px">
      <div slot="header">
        <span>离线告警记录</span>
      </div>
      <el-table :data="faultList" border stripe v-loading="loadingLog">
        <el-table-column label="设备名称" prop="deviceName" width="140" :show-overflow-tooltip="true" />
        <el-table-column label="变更前" width="80" align="center">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.oldStatus === 'ONLINE'" type="success" size="small">在线</el-tag>
            <el-tag v-else-if="scope.row.oldStatus === 'OFFLINE'" type="danger" size="small">离线</el-tag>
            <el-tag v-else type="info" size="small">{{ scope.row.oldStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="变更后" width="80" align="center">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.newStatus === 'ONLINE'" type="success" size="small">在线</el-tag>
            <el-tag v-else-if="scope.row.newStatus === 'OFFLINE'" type="danger" size="small">离线</el-tag>
            <el-tag v-else type="info" size="small">{{ scope.row.newStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="变更时间" prop="changeTime" width="160" align="center" />
        <el-table-column label="短信" prop="smsSent" width="70" align="center">
          <template slot-scope="scope">
            <span v-if="scope.row.smsSent === 'Y'" style="color: green">已发送</span>
            <span v-else style="color: gray">未发送</span>
          </template>
        </el-table-column>
        <el-table-column label="短信接收人" prop="smsRecipient" width="130" />
      </el-table>
      <pagination
        v-show="total > 0" :total="total" :page.sync="queryParams.pageNum"
        :limit.sync="queryParams.pageSize" @pagination="loadFaultLogs"
      />
    </el-card>
  </div>
</template>

<script>
import { listDevice } from '@/api/device/device'
import { listStatusLog } from '@/api/device/device'
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
      this.$confirm('确认向 ' + row.responsiblePhone + ' 发送报修短信?', '报修确认', { type: 'warning' }).then(() => {
        request({ url: '/device/fault/repair/' + row.deviceId, method: 'post' }).then(() => {
          this.$message.success('报修短信已发送')
        })
      })
    }
  }
}
</script>
