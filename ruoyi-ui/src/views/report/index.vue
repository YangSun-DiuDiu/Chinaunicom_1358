<template>
  <div class="app-container">
    <el-radio-group v-model="queryParams.reportType" @change="handleQuery" style="margin-bottom:16px">
      <el-radio-button label="">全部</el-radio-button>
      <el-radio-button label="DAILY">日报</el-radio-button>
      <el-radio-button label="WEEKLY">周报</el-radio-button>
      <el-radio-button label="MONTHLY">月报</el-radio-button>
      <el-radio-button label="QUARTERLY">季度报</el-radio-button>
      <el-radio-button label="HALF_YEAR">半年报</el-radio-button>
      <el-radio-button label="YEARLY">年报</el-radio-button>
    </el-radio-group>
    <el-button type="primary" size="mini" style="margin-left:12px" @click="handleGenerate">立即生成日报</el-button>

    <el-table v-loading="loading" :data="reportList">
      <el-table-column label="周期" prop="reportType" :formatter="typeFmt" width="90" />
      <el-table-column label="健康分" prop="healthScore" width="90">
        <template slot-scope="s"><span :style="scoreStyle(s.row.healthScore)">{{ s.row.healthScore }}</span></template>
      </el-table-column>
      <el-table-column label="设备" prop="deviceScore" width="80" />
      <el-table-column label="线路" prop="lineScore" width="80" />
      <el-table-column label="流量" prop="flowScore" width="80" />
      <el-table-column label="统计区间" width="300">
        <template slot-scope="s">{{ parseTime(s.row.periodStart) }} ~ {{ parseTime(s.row.periodEnd) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template slot-scope="s">
          <el-button size="mini" type="text" @click="viewReport(s.row)">查看</el-button>
          <el-button size="mini" type="text" style="color:#f56c6c" @click="handleDelete(s.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="dialogTitle" :visible.sync="open" width="800px">
      <div v-html="renderedMd" style="max-height:60vh;overflow:auto"></div>
    </el-dialog>
  </div>
</template>

<script>
import { listReport, getReport, generateReport, delReport } from '@/api/report/report'

export default {
  name: 'NetReport',
  data() {
    return {
      loading: false, reportList: [], total: 0,
      queryParams: { pageNum: 1, pageSize: 10, reportType: '' },
      open: false, dialogTitle: '', renderedMd: ''
    }
  },
  created() { this.getList() },
  methods: {
    getList() {
      this.loading = true
      listReport(this.queryParams).then(res => {
        this.reportList = res.rows; this.total = res.total; this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    typeFmt(row) {
      const m = { DAILY: '日报', WEEKLY: '周报', MONTHLY: '月报', QUARTERLY: '季度报', HALF_YEAR: '半年报', YEARLY: '年报' }
      return m[row.reportType] || row.reportType
    },
    scoreStyle(s) {
      const color = s >= 90 ? '#0ecb7e' : s >= 75 ? '#409EFF' : s >= 60 ? '#e6a23c' : '#f56c6c'
      return `color:${color};font-weight:600`
    },
    viewReport(row) {
      getReport(row.reportId).then(res => {
        this.dialogTitle = this.typeFmt(row) + ' - 健康分 ' + res.data.healthScore
        this.renderedMd = this.mdToHtml(res.data.contentMd || '')
        this.open = true
      })
    },
    mdToHtml(md) {
      return md
        .replace(/^### (.*)$/gm, '<h3>$1</h3>')
        .replace(/^## (.*)$/gm, '<h2>$1</h2>')
        .replace(/^# (.*)$/gm, '<h1>$1</h1>')
        .replace(/\*\*(.*?)\*\*/g, '<b>$1</b>')
        .replace(/\n/g, '<br/>')
    },
    handleGenerate() {
      this.$modal.loading('正在生成报告，请稍候...')
      generateReport('DAILY').then(() => {
        this.$modal.closeLoading(); this.$modal.msgSuccess('生成成功'); this.getList()
      }).catch(() => this.$modal.closeLoading())
    },
    handleDelete(row) {
      this.$modal.confirm('确认删除该报告？').then(() => delReport(row.reportId)).then(() => {
        this.$modal.msgSuccess('删除成功'); this.getList()
      }).catch(() => {})
    }
  }
}
</script>
