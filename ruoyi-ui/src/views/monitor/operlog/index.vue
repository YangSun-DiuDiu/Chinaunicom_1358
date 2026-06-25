<template>
  <div class="app-container">
    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column label="序号" type="index" width="60" align="center" />
      <el-table-column label="系统模块" prop="title" />
      <el-table-column label="操作类型" prop="businessType" width="100" />
      <el-table-column label="操作人员" prop="operName" width="120" />
      <el-table-column label="操作时间" prop="operTime" width="180" />
      <el-table-column label="操作状态" prop="status" width="100" />
      <el-table-column label="操作" width="120" align="center">
        <template slot-scope="scope">
          <el-button size="mini" type="text" @click="handleDetail(scope.row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script>
import { list as listOperLog } from '@/api/monitor/operlog'

export default {
  name: 'OperLog',
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
      listOperLog(this.queryParams).then(res => {
        this.list = res.rows
        this.total = res.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleDetail(row) {
      this.$alert(
        `<p>操作模块：${row.title}</p><p>请求方法：${row.method}</p><p>请求参数：${row.operParam}</p><p>返回参数：${row.jsonResult}</p>`,
        '操作日志详情', { dangerouslyUseHTMLString: true }
      )
    }
  }
}
</script>
