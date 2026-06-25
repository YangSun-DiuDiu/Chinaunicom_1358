<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="68px">
      <el-form-item label="收件人" prop="recipient">
        <el-input v-model="queryParams.recipient" placeholder="收件人" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="手机号" prop="phoneNumber">
        <el-input v-model="queryParams.phoneNumber" placeholder="手机号" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="发送结果" prop="sendResult">
        <el-select v-model="queryParams.sendResult" placeholder="结果" clearable>
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAIL" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="logList" border stripe v-loading="loading" size="small">
      <el-table-column label="收件人" prop="recipient" width="100" align="center"/>
      <el-table-column label="手机号" prop="phoneNumber" width="130" align="center"/>
      <el-table-column label="短信内容" prop="content" min-width="180" :show-overflow-tooltip="true" />
      <el-table-column label="业务类型" prop="bizType" width="200" align="center" />
      <el-table-column label="发送结果" prop="sendResult" width="120" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.sendResult === 'SUCCESS' ? 'success' : 'danger'" size="small">
            {{ scope.row.sendResult === 'SUCCESS' ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发送时间" prop="sendTime" width="180" align="center" />
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script>
import { listSmsLog } from '@/api/smslog'

export default {
  name: 'SmsLog',
  data() {
    return {
      loading: false,
      logList: [],
      total: 0,
      queryParams: { pageNum: 1, pageSize: 10, recipient: null, phoneNumber: null, sendResult: null }
    }
  },
  created() { this.getList() },
  methods: {
    getList() {
      this.loading = true
      listSmsLog(this.queryParams).then(res => {
        this.logList = res.rows || []
        this.total = res.total || 0
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.queryParams = { pageNum: 1, pageSize: 10 }; this.getList() }
  }
}
</script>
