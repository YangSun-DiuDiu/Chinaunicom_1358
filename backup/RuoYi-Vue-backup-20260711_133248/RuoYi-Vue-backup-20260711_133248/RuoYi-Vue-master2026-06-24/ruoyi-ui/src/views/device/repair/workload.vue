<template>
  <div class="app-container">
    <el-row :gutter="20" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-back" size="mini" @click="$router.push('/device/repair')">返回工单列表</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button icon="el-icon-refresh" size="mini" @click="loadStats">刷新</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="statsList" :default-sort="{prop:'total',order:'descending'}">
      <el-table-column label="维护人员" align="center" prop="name" sortable min-width="120"/>
      <el-table-column label="总工单数" align="center" prop="total" sortable width="110"/>
      <el-table-column label="已完成" align="center" prop="completed" sortable width="110">
        <template slot-scope="scope"><el-tag type="success" size="small">{{ scope.row.completed }}</el-tag></template>
      </el-table-column>
      <el-table-column label="进行中" align="center" prop="inProgress" sortable width="110">
        <template slot-scope="scope"><el-tag type="warning" size="small">{{ scope.row.inProgress }}</el-tag></template>
      </el-table-column>
      <el-table-column label="已拒绝" align="center" prop="rejected" sortable width="110">
        <template slot-scope="scope"><el-tag type="danger" size="small">{{ scope.row.rejected }}</el-tag></template>
      </el-table-column>
      <el-table-column label="完成率" align="center" width="110">
        <template slot-scope="scope">
          <el-progress :percentage="calcRate(scope.row)" :color="rateColor(scope.row)" :stroke-width="14"/>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="!loading && statsList.length===0" style="text-align:center;padding:60px;color:#999">暂无维修工单数据</div>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'RepairWorkload',
  data() { return { loading: false, statsList: [] } },
  created() { this.loadStats() },
  methods: {
    loadStats() {
      this.loading = true
      request({ url: '/device/repair/workload', method: 'get' }).then(res => {
        this.statsList = res.data || []; this.loading = false
      }).catch(() => { this.loading = false })
    },
    calcRate(row) {
      const t = Number(row.total) || 1
      return Math.round((Number(row.completed) || 0) / t * 100)
    },
    rateColor(row) {
      const r = this.calcRate(row)
      return r >= 80 ? '#67c23a' : r >= 50 ? '#e6a23c' : '#f56c6c'
    }
  }
}
</script>
