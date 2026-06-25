<template>
  <div class="app-container">
    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column label="序号" type="index" width="60" align="center" />
      <el-table-column label="用户账号" prop="userName" width="120" />
      <el-table-column label="登录IP" prop="ipaddr" width="140" />
      <el-table-column label="登录地点" prop="loginLocation" />
      <el-table-column label="浏览器" prop="browser" />
      <el-table-column label="操作系统" prop="os" />
      <el-table-column label="登录状态" prop="status" width="100" />
      <el-table-column label="登录时间" prop="loginTime" width="180" />
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script>
import { list as listLoginInfo } from '@/api/monitor/logininfor'

export default {
  name: 'LoginLog',
  data() {
    return {
      loading: false,
      total: 0,
      list: [],
      queryParams: { pageNum: 1, pageSize: 10 }
    }
  },
  created() { this.getList() },
  methods: {
    getList() {
      this.loading = true
      listLoginInfo(this.queryParams).then(res => {
        this.list = res.rows
        this.total = res.total
        this.loading = false
      }).catch(() => { this.loading = false })
    }
  }
}
</script>
