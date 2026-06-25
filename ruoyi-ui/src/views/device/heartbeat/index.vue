<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="设备名称" prop="deviceName">
        <el-input
          v-model="queryParams.deviceName"
          placeholder="请输入设备名称"
          clearable
          style="width: 200px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="心跳结果" prop="pingResult">
        <el-select
          v-model="queryParams.pingResult"
          placeholder="请选择心跳结果"
          clearable
          style="width: 200px"
        >
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAIL" />
        </el-select>
      </el-form-item>
      <el-form-item label="检测时间">
        <el-date-picker
          v-model="dateRange"
          style="width: 240px"
          value-format="yyyy-MM-dd"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="heartbeatList">
      <el-table-column label="设备名称" align="center" prop="deviceName" width="140" :show-overflow-tooltip="true" />
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

    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script>
import { listHeartbeatLog } from "@/api/device/device"
import { resetForm } from "@/utils/ruoyi"

export default {
  name: "Heartbeat",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 心跳日志表格数据
      heartbeatList: [],
      // 日期范围
      dateRange: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        deviceName: undefined,
        pingResult: undefined
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询心跳日志列表 */
    getList() {
      this.loading = true
      listHeartbeatLog(this.addDateRange(this.queryParams, this.dateRange)).then(response => {
        this.heartbeatList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.dateRange = []
      resetForm.call(this, "queryForm")
      this.handleQuery()
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('device/heartbeat/export', {
        ...this.addDateRange(this.queryParams, this.dateRange)
      }, `heartbeat_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
