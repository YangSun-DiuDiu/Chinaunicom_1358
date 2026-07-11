<template>
  <div class="heartbeat-container">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="heartbeat-stats" v-if="stats.total > 0">
      <el-col :xs="12" :sm="6">
        <div class="stat-card stat-total">
          <div class="stat-num">{{ stats.total }}</div>
          <div class="stat-label">检测总次数</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card stat-success">
          <div class="stat-num">{{ stats.success }}</div>
          <div class="stat-label">成功</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card stat-fail">
          <div class="stat-num">{{ stats.fail }}</div>
          <div class="stat-label">失败</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card stat-rate">
          <div class="stat-num">{{ successRate }}%</div>
          <div class="stat-label">成功率</div>
        </div>
      </el-col>
    </el-row>

    <!-- 搜索 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="设备名称" prop="deviceName">
        <el-input v-model="queryParams.deviceName" placeholder="请输入设备名称" clearable style="width: 200px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="心跳结果" prop="pingResult">
        <el-select v-model="queryParams.pingResult" placeholder="请选择心跳结果" clearable style="width: 200px">
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAIL" />
        </el-select>
      </el-form-item>
      <el-form-item label="检测时间">
        <el-date-picker v-model="dateRange" style="width: 240px" value-format="yyyy-MM-dd" type="daterange" range-separator="-" start-placeholder="开始日期" end-placeholder="结束日期" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport">导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="heartbeatList" stripe size="small">
      <el-table-column label="设备名称" align="center" prop="deviceName" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="IP地址" align="center" prop="ipAddress" width="160" />
      <el-table-column label="心跳结果" align="center" prop="pingResult" width="100">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.pingResult === 'SUCCESS'" type="success" size="small">成功</el-tag>
          <el-tag v-else-if="scope.row.pingResult === 'FAIL'" type="danger" size="small">失败</el-tag>
          <el-tag v-else type="info" size="small">{{ scope.row.pingResult }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="延迟(ms)" align="center" prop="pingLatency" width="120" />
      <el-table-column label="检测时间" align="center" prop="detectTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.detectTime) }}</span>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <div v-if="!loading && heartbeatList.length === 0" class="empty-state">
      <i class="el-icon-info"></i>
      <p>暂无心跳记录</p>
    </div>
  </div>
</template>

<script>
import { listHeartbeatLog } from "@/api/device/device"
import { resetForm } from "@/utils/ruoyi"

export default {
  name: "Heartbeat",
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      heartbeatList: [],
      dateRange: [],
      stats: { total: 0, success: 0, fail: 0 },
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        deviceName: undefined,
        pingResult: undefined
      }
    }
  },
  computed: {
    successRate() {
      if (!this.stats.total) return 0
      return Math.round(this.stats.success / this.stats.total * 100)
    }
  },
  created() {
    this.getList()
    this.loadStats()
  },
  methods: {
    getList() {
      this.loading = true
      listHeartbeatLog(this.addDateRange(this.queryParams, this.dateRange)).then(response => {
        this.heartbeatList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    loadStats() {
      listHeartbeatLog({ pageNum: 1, pageSize: 1 }).then(res => { this.stats.total = res.total || 0 })
      listHeartbeatLog({ pageNum: 1, pageSize: 1, pingResult: 'SUCCESS' }).then(res => { this.stats.success = res.total || 0 })
      listHeartbeatLog({ pageNum: 1, pageSize: 1, pingResult: 'FAIL' }).then(res => { this.stats.fail = res.total || 0 })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.dateRange = []
      resetForm.call(this, "queryForm")
      this.handleQuery()
    },
    handleExport() {
      this.download('device/heartbeat/export', {
        ...this.addDateRange(this.queryParams, this.dateRange)
      }, `heartbeat_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>

<style scoped>
.heartbeat-container {
  padding: 0;
}

.heartbeat-stats {
  margin-bottom: 16px;
}

.stat-card {
  padding: 16px 20px;
  border-radius: 4px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  transition: transform 0.15s;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-num {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 2px;
}

.stat-label {
  font-size: 13px;
  color: #909399;
}

.stat-total .stat-num { color: #409eff; }
.stat-success .stat-num { color: #67c23a; }
.stat-fail .stat-num { color: #f56c6c; }
.stat-rate .stat-num { color: #409eff; }

.empty-state {
  text-align: center;
  padding: 48px 0;
  color: #909399;
}

.empty-state i {
  font-size: 48px;
  display: block;
  margin-bottom: 12px;
}

.empty-state p {
  font-size: 14px;
  margin: 0;
}
</style>
