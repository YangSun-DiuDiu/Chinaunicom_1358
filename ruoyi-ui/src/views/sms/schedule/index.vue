<template>
  <div class="app-container">
    <el-form :model="query" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="业务编码">
        <el-input v-model="query.bizCode" placeholder="业务编码" clearable style="width:160px" @keyup.enter.native="getList"/>
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="query.phone" placeholder="手机号" clearable style="width:160px" @keyup.enter.native="getList"/>
      </el-form-item>
      <el-form-item label="任务状态">
        <el-select v-model="query.taskStatus" placeholder="状态" clearable style="width:130px">
          <el-option label="待执行" value="WAIT"/>
          <el-option label="成功" value="SUCCESS"/>
          <el-option label="失败" value="FAIL"/>
          <el-option label="已撤销" value="CANCEL"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="getList">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="reset">重置</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="list" border stripe size="small">
      <el-table-column label="业务编码" prop="biz_code" min-width="140" :show-overflow-tooltip="true"/>
      <el-table-column label="手机号" prop="phone" min-width="130" align="center"/>
      <el-table-column label="定时发送时间" prop="send_time" min-width="160" align="center"/>
      <el-table-column label="任务状态" min-width="100" align="center">
        <template slot-scope="{row}">
          <el-tag size="small" :type="row.task_status === 'SUCCESS' ? 'success' : row.task_status === 'FAIL' ? 'danger' : row.task_status === 'CANCEL' ? 'info' : 'warning'">
            {{ row.task_status === 'WAIT' ? '待执行' : row.task_status === 'SUCCESS' ? '成功' : row.task_status === 'FAIL' ? '失败' : row.task_status === 'CANCEL' ? '已撤销' : row.task_status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="重试次数" prop="retry_count" min-width="80" align="center"/>
      <el-table-column label="创建时间" prop="create_time" min-width="140" align="center"/>
      <el-table-column label="操作" min-width="160" fixed="right" align="center">
        <template slot-scope="{row}">
          <el-button size="mini" type="text" icon="el-icon-close" @click="handleCancel(row)" v-hasPermi="['sms:schedule:cancel']" v-if="row.task_status === 'WAIT'">撤销</el-button>
          <el-button size="mini" type="text" icon="el-icon-refresh" @click="handleRetry(row)" v-hasPermi="['sms:schedule:retry']" v-if="row.task_status === 'FAIL'">重发</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="getList"/>
  </div>
</template>
<script>
import request from '@/utils/request'
export default {
  data() {
    return {
      loading: false, showSearch: true, total: 0, list: [],
      query: { pageNum: 1, pageSize: 10, bizCode: undefined, phone: undefined, taskStatus: undefined }
    }
  },
  created() { this.getList() },
  methods: {
    getList() {
      this.loading = true
      request({ url: '/sms/log/list', method: 'get', params: this.query }).then(r => {
        this.list = r.rows || []
        this.total = r.total || 0
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    reset() {
      this.query = { pageNum: 1, pageSize: 10, bizCode: undefined, phone: undefined, taskStatus: undefined }
      this.getList()
    },
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
