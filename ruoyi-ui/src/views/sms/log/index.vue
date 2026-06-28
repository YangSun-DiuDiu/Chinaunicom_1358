<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="68px">
      <el-form-item label="业务编码" prop="bizCode">
        <el-input v-model="queryParams.bizCode" placeholder="业务编码" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="queryParams.phone" placeholder="手机号" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="任务状态" prop="taskStatus">
        <el-select v-model="queryParams.taskStatus" placeholder="状态" clearable>
          <el-option label="待执行" value="WAIT" />
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAIL" />
          <el-option label="已撤销" value="CANCEL" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="logList" border stripe v-loading="loading" size="small">
      <el-table-column label="业务编码" prop="biz_code" width="140" align="center" :show-overflow-tooltip="true"/>
      <el-table-column label="手机号" prop="phone" width="130" align="center"/>
      <el-table-column label="渠道" prop="channel_type" width="90" align="center"/>
      <el-table-column label="短信内容" prop="param" min-width="180" :show-overflow-tooltip="true" />
      <el-table-column label="发送模式" prop="send_mode" width="90" align="center">
        <template slot-scope="scope">
          <el-tag size="small" :type="scope.row.send_mode === 1 ? '' : 'warning'">{{ scope.row.send_mode === 1 ? '即时' : '定时' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="任务状态" prop="task_status" width="100" align="center">
        <template slot-scope="scope">
          <el-tag size="small" :type="scope.row.task_status === 'SUCCESS' ? 'success' : scope.row.task_status === 'FAIL' ? 'danger' : scope.row.task_status === 'CANCEL' ? 'info' : 'warning'">
            {{ scope.row.task_status === 'WAIT' ? '待执行' : scope.row.task_status === 'SUCCESS' ? '成功' : scope.row.task_status === 'FAIL' ? '失败' : scope.row.task_status === 'CANCEL' ? '已撤销' : scope.row.task_status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="重试次数" prop="retry_count" width="90" align="center"/>
      <el-table-column label="结果" prop="result_msg" min-width="160" :show-overflow-tooltip="true" />
      <el-table-column label="发送时间" prop="send_time" width="160" align="center" />
      <el-table-column label="创建时间" prop="create_time" width="160" align="center" />
      <el-table-column label="操作" width="140" fixed="right" align="center">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-close" @click="handleCancel(scope.row)" v-hasPermi="['sms:log:cancel']" v-if="scope.row.task_status === 'WAIT' && scope.row.send_mode === 2">撤销</el-button>
          <el-button size="mini" type="text" icon="el-icon-refresh" @click="handleRetry(scope.row)" v-hasPermi="['sms:log:retry']" v-if="scope.row.task_status === 'FAIL'">重发</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'SmsLog',
  data() {
    return {
      loading: false,
      logList: [],
      total: 0,
      queryParams: { pageNum: 1, pageSize: 10, bizCode: null, phone: null, taskStatus: null }
    }
  },
  created() { this.getList() },
  methods: {
    getList() {
      this.loading = true
      request({ url: '/sms/log/list', method: 'get', params: this.queryParams }).then(res => {
        this.logList = res.rows || []
        this.total = res.total || 0
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.queryParams = { pageNum: 1, pageSize: 10, bizCode: null, phone: null, taskStatus: null }; this.getList() },
    handleCancel(row) {
      this.$confirm('确认撤销该定时短信任务?', '提示', { type: 'warning' }).then(() =>
        request({ url: '/sms/log/cancel/' + row.log_id, method: 'post' }).then(() => { this.$message.success('撤销成功'); this.getList() })
      ).catch(() => {})
    },
    handleRetry(row) {
      this.$confirm('确认重发该短信?', '提示', { type: 'warning' }).then(() =>
        request({ url: '/sms/log/retry/' + row.log_id, method: 'post' }).then(() => { this.$message.success('重发成功'); this.getList() })
      ).catch(() => {})
    }
  }
}
</script>
